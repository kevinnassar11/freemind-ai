// FreeMind AI - notify-favorites Edge Function
//
// NOT DEPLOYED. This is source code only - deploying and scheduling it needs
// a Firebase service-account credential this environment doesn't have, and
// enabling pg_cron on the live project, both of which are the operator's call
// to make (see the scheduling SQL at the bottom of this comment block, which
// is likewise not run). Once you have the credential:
//   supabase secrets set FIREBASE_SERVICE_ACCOUNT="$(cat service-account.json)" \
//     FIREBASE_PROJECT_ID=your-firebase-project-id
//   supabase functions deploy notify-favorites
//
// What it does: once invoked, finds users whose favorited places match a
// currently-trending search (via the trending_searches security definer
// function from this same migration), and sends each a push notification
// through FCM's HTTP v1 API - skipping anyone already notified about that
// place in the last 7 days (notification_log).
//
// Auth model: this function is meant to be invoked only by a scheduled job
// (pg_cron below) carrying the project's own service_role key, never by an
// end user - so unlike stamp-role/chat, it does not verify a Firebase ID
// token. It instead checks the caller's bearer token against this function's
// own SUPABASE_SERVICE_ROLE_KEY (auto-injected into every Edge Function),
// rejecting anything else. verify_jwt is off in config.toml for the same
// reason the other two functions have it off: this function verifies the
// caller itself, its own way.
//
// Scheduling (run manually once deployed, requires the pg_cron extension -
// Database -> Extensions -> enable pg_cron):
//
//   select cron.schedule(
//     'notify-favorites-daily',
//     '0 9 * * *', -- 9am UTC daily
//     $$
//     select net.http_post(
//       url := 'https://<project-ref>.supabase.co/functions/v1/notify-favorites',
//       headers := jsonb_build_object('Authorization', 'Bearer <service-role-key>')
//     );
//     $$
//   );
//
// That inline service-role key is itself a secret sitting in plain text
// inside pg_cron's job table - acceptable only because pg_cron jobs are only
// readable by the database owner, but worth knowing before you run it.

import { createClient } from "npm:@supabase/supabase-js@^2";
import { importPKCS8, SignJWT } from "npm:jose@^6";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const SERVICE_ACCOUNT_RAW = Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "";
const NOTIFICATION_COOLDOWN_DAYS = 7;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
};

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
}

/** Same OAuth2 service-account exchange as stamp-role, scoped to FCM instead of Identity Toolkit. */
let cachedToken: { value: string; expiresAtMs: number } | null = null;

async function getFcmAccessToken(sa: ServiceAccount): Promise<string> {
  const now = Date.now();
  if (cachedToken && cachedToken.expiresAtMs - 60_000 > now) return cachedToken.value;

  const key = await importPKCS8(sa.private_key, "RS256");
  const nowSec = Math.floor(now / 1000);
  const assertion = await new SignJWT({ scope: "https://www.googleapis.com/auth/firebase.messaging" })
    .setProtectedHeader({ alg: "RS256" })
    .setIssuer(sa.client_email)
    .setSubject(sa.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(nowSec)
    .setExpirationTime(nowSec + 3600)
    .sign(key);

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) throw new Error(`FCM token exchange failed (${res.status}): ${await res.text()}`);

  const body = await res.json() as { access_token: string; expires_in: number };
  cachedToken = { value: body.access_token, expiresAtMs: now + body.expires_in * 1000 };
  return body.access_token;
}

interface FavoriteRow {
  user_id: string;
  place_id: string;
  title: string;
  description: string | null;
  latitude: number | null;
  longitude: number | null;
  fcm_token: string;
}

async function sendPush(
  accessToken: string,
  projectId: string,
  fcmToken: string,
  favorite: FavoriteRow,
): Promise<boolean> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: fcmToken,
          notification: {
            title: "Trending near a favorite",
            body: `${favorite.title} is trending right now.`,
          },
          data: {
            place_id: favorite.place_id,
            place_title: favorite.title,
            ...(favorite.latitude != null ? { place_latitude: String(favorite.latitude) } : {}),
            ...(favorite.longitude != null ? { place_longitude: String(favorite.longitude) } : {}),
          },
        },
      }),
    },
  );
  if (!res.ok) {
    console.error(`[notify-favorites] send failed for user=${favorite.user_id} place=${favorite.place_id}`, res.status, await res.text());
    return false;
  }
  return true;
}

async function handle(req: Request): Promise<Response> {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });

  const bearer = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!SERVICE_ROLE_KEY || bearer !== SERVICE_ROLE_KEY) {
    return jsonResponse({ error: "unauthorized" }, 401);
  }

  if (!SERVICE_ACCOUNT_RAW) {
    console.error("[notify-favorites] FIREBASE_SERVICE_ACCOUNT is unset");
    return jsonResponse({ error: "not_configured" }, 503);
  }
  const serviceAccount = JSON.parse(SERVICE_ACCOUNT_RAW) as ServiceAccount;

  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

  const { data: trending, error: trendingError } = await supabase.rpc("trending_searches", { result_limit: 20 });
  if (trendingError) return jsonResponse({ error: "trending_query_failed", detail: trendingError.message }, 500);
  const trendingQueries: string[] = (trending ?? []).map((row: { query: string }) => row.query.toLowerCase());
  if (trendingQueries.length === 0) return jsonResponse({ status: "no_trending_searches", sent: 0 }, 200);

  // Favorites joined to a token-bearing profile - anyone without a token
  // (never granted notification permission) is naturally excluded here.
  const { data: favorites, error: favoritesError } = await supabase
    .from("favorite_places")
    .select("user_id, place_id, title, description, latitude, longitude, profiles!inner(fcm_token)")
    .not("profiles.fcm_token", "is", null);
  if (favoritesError) return jsonResponse({ error: "favorites_query_failed", detail: favoritesError.message }, 500);

  const cooldownSince = new Date(Date.now() - NOTIFICATION_COOLDOWN_DAYS * 24 * 60 * 60 * 1000).toISOString();
  let sent = 0;

  for (const row of favorites ?? []) {
    const favorite = row as unknown as FavoriteRow & { profiles: { fcm_token: string } };
    const matchesTrending = trendingQueries.some((q) => favorite.title.toLowerCase().includes(q) || q.includes(favorite.title.toLowerCase()));
    if (!matchesTrending) continue;

    const { data: alreadyNotified } = await supabase
      .from("notification_log")
      .select("id")
      .eq("user_id", favorite.user_id)
      .eq("place_id", favorite.place_id)
      .gte("sent_at", cooldownSince)
      .limit(1);
    if (alreadyNotified && alreadyNotified.length > 0) continue;

    const accessToken = await getFcmAccessToken(serviceAccount);
    const ok = await sendPush(accessToken, serviceAccount.project_id, favorite.profiles.fcm_token, favorite);
    if (!ok) continue;

    await supabase.from("notification_log").insert({
      user_id: favorite.user_id,
      place_id: favorite.place_id,
      notification_type: "trending_favorite",
    });
    sent += 1;
  }

  return jsonResponse({ status: "ok", sent }, 200);
}

Deno.serve(async (req) => {
  try {
    return await handle(req);
  } catch (error) {
    console.error("[notify-favorites] UNHANDLED", error);
    return jsonResponse({ error: "unexpected_error", detail: String(error).slice(0, 500) }, 500);
  }
});
