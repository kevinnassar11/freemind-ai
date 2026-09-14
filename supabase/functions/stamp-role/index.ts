// FreeMind AI - stamp-role Edge Function
//
// Supabase's RLS policies are all `to authenticated`, and Supabase decides a
// caller's Postgres role from the `role` claim on the incoming JWT. Firebase
// does not put one there, so without this every policy sees `anon` and
// silently rejects the request - which is exactly the bug that left the
// profiles table empty.
//
// Firebase's own fix for this is a blocking Cloud Function (beforeSignIn),
// but Cloud Functions require the Blaze plan. This does the same job from
// Supabase's free tier instead: it calls Firebase's Identity Toolkit REST API
// to set a custom claim, which is part of Firebase Auth and free on Spark.
//
// SECURITY: this endpoint grants a claim, so it must never take the uid from
// the request body. It verifies the caller's own Firebase ID token against
// Firebase's JWKS and stamps ONLY `payload.sub`. A caller can therefore only
// ever act on themselves. `verify_jwt` is off for this function in
// config.toml because we do that verification here rather than at the gateway
// (same reason as the `chat` function).

import { createRemoteJWKSet, jwtVerify, importPKCS8, SignJWT } from "npm:jose@^6";

const FIREBASE_PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID")!;
/** Whole service-account JSON, stored via `supabase secrets set`. */
const SERVICE_ACCOUNT_RAW = Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "";

const ROLE_CLAIM = "authenticated";

const JWKS = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

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
  token_uri: string;
  project_id: string;
}

/** sha256 of the given text, hex-encoded. Never logs the input itself. */
async function sha256Hex(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

type LoadResult =
  | { ok: true; account: ServiceAccount }
  | { ok: false; reason: "missing" | "unparseable" | "missing_fields" };

/**
 * Distinguishes exactly how the secret is broken, since "not_configured" alone
 * gave no signal for a whole debugging session. Every branch logs enough to
 * diagnose without ever printing the secret itself.
 */
async function loadServiceAccount(): Promise<LoadResult> {
  if (!SERVICE_ACCOUNT_RAW) {
    console.error("[stamp-role] FIREBASE_SERVICE_ACCOUNT is unset or empty");
    return { ok: false, reason: "missing" };
  }

  let parsed: ServiceAccount;
  try {
    parsed = JSON.parse(SERVICE_ACCOUNT_RAW) as ServiceAccount;
  } catch (e) {
    // The most common cause: the JSON was pasted multi-line into a one-line
    // .env value, so the parser only captured up to the first line break.
    console.error(
      `[stamp-role] FIREBASE_SERVICE_ACCOUNT is not valid JSON (length=${SERVICE_ACCOUNT_RAW.length}, ` +
        `starts="${SERVICE_ACCOUNT_RAW.slice(0, 20)}", ends="${SERVICE_ACCOUNT_RAW.slice(-20)}"): ${e}`,
    );
    return { ok: false, reason: "unparseable" };
  }

  if (!parsed.client_email || !parsed.private_key || !parsed.token_uri) {
    console.error(
      "[stamp-role] FIREBASE_SERVICE_ACCOUNT parsed but is missing required fields " +
        `(has client_email=${!!parsed.client_email}, private_key=${!!parsed.private_key}, token_uri=${!!parsed.token_uri})`,
    );
    return { ok: false, reason: "missing_fields" };
  }

  // Firebase's own JSON stores private_key with literal "\n" *escapes* inside a
  // single JSON string - JSON.parse always turns those into real newline
  // characters. If a copy/paste round-trip (e.g. through Notepad) re-escaped
  // the value, private_key ends up containing the two literal characters
  // backslash+n instead of an actual newline, and JSON.parse has no way to
  // catch this - the JSON itself is still syntactically valid. PEM parsers
  // require real newlines, so this fails later with an opaque crypto error.
  // Detected here specifically so that failure has a clear cause instead.
  const hasRealNewlines = parsed.private_key.includes("\n");
  const hasEscapedNewlines = parsed.private_key.includes("\\n");
  if (!hasRealNewlines && hasEscapedNewlines) {
    console.error(
      "[stamp-role] private_key contains literal backslash-n instead of real newlines - " +
        "the secret was double-escaped (common when hand-editing JSON in a text editor). " +
        "Auto-normalizing for this call, but the stored secret should be fixed.",
    );
    parsed.private_key = parsed.private_key.replace(/\\n/g, "\n");
  }

  const hashPreview = await sha256Hex(JSON.stringify(parsed, Object.keys(parsed).sort()));
  console.log(`[stamp-role] service account loaded OK, sha256=${hashPreview}`);

  return { ok: true, account: parsed };
}

/**
 * Mints a short-lived Google access token from the service account.
 *
 * Deliberately not using firebase-admin: it's Node-oriented and unreliable on
 * Deno's edge runtime. Signing the JWT assertion ourselves with jose (already
 * a dependency of the chat function) and exchanging it at the token endpoint
 * is the documented OAuth2 service-account flow and needs no Node shims.
 *
 * Cached in-memory for the isolate's lifetime so a burst of sign-ins doesn't
 * mint a new token per request.
 */
let cachedToken: { value: string; expiresAtMs: number } | null = null;

async function getAccessToken(sa: ServiceAccount): Promise<string> {
  const now = Date.now();
  // 60s safety margin so a token can't expire mid-flight.
  if (cachedToken && cachedToken.expiresAtMs - 60_000 > now) return cachedToken.value;

  const key = await importPKCS8(sa.private_key, "RS256");
  const nowSec = Math.floor(now / 1000);
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/identitytoolkit",
  })
    .setProtectedHeader({ alg: "RS256" })
    .setIssuer(sa.client_email)
    .setSubject(sa.client_email)
    .setAudience(sa.token_uri)
    .setIssuedAt(nowSec)
    .setExpirationTime(nowSec + 3600)
    .sign(key);

  const res = await fetch(sa.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });

  if (!res.ok) {
    throw new Error(`token exchange failed (${res.status}): ${await res.text()}`);
  }

  const body = await res.json() as { access_token: string; expires_in: number };
  cachedToken = {
    value: body.access_token,
    expiresAtMs: now + body.expires_in * 1000,
  };
  return body.access_token;
}

/**
 * Reads the user's existing claims so we don't clobber any others.
 * Identity Toolkit's accounts:update replaces customAttributes wholesale.
 */
async function existingClaims(
  accessToken: string,
  uid: string,
): Promise<Record<string, unknown>> {
  const res = await fetch(
    `https://identitytoolkit.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/accounts:lookup`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ localId: [uid] }),
    },
  );
  if (!res.ok) return {};
  const body = await res.json() as { users?: Array<{ customAttributes?: string }> };
  const raw = body.users?.[0]?.customAttributes;
  if (!raw) return {};
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return {};
  }
}

async function handle(req: Request): Promise<Response> {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });

  const loaded = await loadServiceAccount();
  if (!loaded.ok) {
    return jsonResponse({ error: "not_configured", reason: loaded.reason }, 503);
  }
  const serviceAccount = loaded.account;

  // --- Verify the caller, and take the uid from the TOKEN, never the body ---
  let uid: string;
  try {
    const token = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
    if (!token) return jsonResponse({ error: "missing_token" }, 401);

    const { payload } = await jwtVerify(token, JWKS, {
      issuer: `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`,
      audience: FIREBASE_PROJECT_ID,
    });
    if (typeof payload.sub !== "string") return jsonResponse({ error: "invalid_token" }, 401);
    uid = payload.sub;

    // Already stamped - nothing to do. Saves an Identity Toolkit round-trip on
    // every subsequent sign-in.
    if (payload.role === ROLE_CLAIM) {
      return jsonResponse({ status: "already_set", uid }, 200);
    }
  } catch (error) {
    console.error("[stamp-role] Token verification failed", error);
    return jsonResponse({ error: "invalid_token" }, 401);
  }

  try {
    const accessToken = await getAccessToken(serviceAccount);
    const claims = await existingClaims(accessToken, uid);
    claims.role = ROLE_CLAIM;

    const res = await fetch(
      `https://identitytoolkit.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/accounts:update`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          localId: uid,
          customAttributes: JSON.stringify(claims),
        }),
      },
    );

    if (!res.ok) {
      const detail = await res.text();
      console.error("[stamp-role] accounts:update failed", res.status, detail);
      return jsonResponse({ error: "stamp_failed", detail: detail.slice(0, 500) }, 502);
    }

    // The caller must now force-refresh its ID token: the claim only appears on
    // tokens minted after this point.
    return jsonResponse({ status: "stamped", uid }, 200);
  } catch (error) {
    console.error("[stamp-role] Unexpected failure stamping role", error);
    return jsonResponse({ error: "unexpected_error", detail: String(error).slice(0, 500) }, 500);
  }
}

Deno.serve(async (req) => {
  // Last-resort net: every branch above already returns a clean Response, but
  // this guarantees a crash is at least logged with its real message and
  // stack trace instead of surfacing as a bare, unexplained platform 503.
  try {
    return await handle(req);
  } catch (error) {
    console.error("[stamp-role] UNHANDLED - this should be unreachable", error);
    return jsonResponse({ error: "unexpected_error", detail: String(error).slice(0, 500) }, 500);
  }
});
