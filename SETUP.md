# FreeMind AI — Setup

The app builds and runs without any of this (you'll see the sign-in screen, and
everything degrades gracefully) but nothing that talks to a backend will work
until you complete these steps. They involve your own accounts/billing, so
they're not something that can be automated for you.

Do them roughly in order — each step depends on the ones before it.

## 1. Firebase project

1. Create a project at [console.firebase.google.com](https://console.firebase.google.com)
   — free (Spark) plan is fine, no billing method needed. Everything below
   only uses standard Firebase Authentication, not Cloud Functions.
2. **Authentication → Sign-in method → enable Google.** Note the **Web client
   ID** shown there (an OAuth client, type "Web application") — you'll need it
   in step 3.
3. **Project settings → Service accounts → Generate new private key.** Downloads
   a JSON file — needed for step 2 below. Treat it like a password: it grants
   admin access to this Firebase project. Never commit it or paste it into a
   source file.
4. **Project settings → Add app → Android.** Package name: `com.freemind.ai`
   (this must match `applicationId` in `app/build.gradle.kts` exactly).
   Download **`google-services.json`** and replace the placeholder file at
   `app/google-services.json` with it (the placeholder there now is fake data
   that only exists so the project builds before you've done this).
5. **Get the debug SHA-1 and register it** under the Android app you just
   added (Project settings → your app → Add fingerprint):
   ```bash
   cd "FreeMind AI"
   ./gradlew signingReport
   ```
   Look for the `SHA1` line under the `debug` variant. Without this, Google
   Sign-In will fail even with a correct `google-services.json`.

## 2. Stamp the `authenticated` role claim (Edge Function)

Supabase (step 3) needs every Firebase ID token to carry `role: "authenticated"`
or it treats the caller as anonymous and every Row Level Security policy
silently rejects the request — including the very first profile-row write on
sign-in. Firebase doesn't add this claim on its own.

Firebase's documented fix is a blocking Cloud Function, but that requires the
Blaze (pay-as-you-go) plan — a real cost/setup barrier for a personal project.
This project uses a Supabase Edge Function instead (`supabase/functions/
stamp-role`): it verifies the caller's own Firebase ID token, then calls
Firebase's Identity Toolkit REST API to set the claim. Same result, but it
runs on Supabase's free tier and needs no Firebase billing at all.

1. Set the service account key from step 1.3 as a function secret. Do this
   from a file, not inline — an inline `KEY=value` on the command line lands
   in shell history:
   ```bash
   # one line in the file: FIREBASE_SERVICE_ACCOUNT=<paste the whole JSON>
   supabase secrets set --env-file path\to\temp-secrets.env
   ```
   Delete the temp file afterward. `FIREBASE_PROJECT_ID` (used by both Edge
   Functions) should already be set from step 4 below — if not, add it as a
   secret too, e.g. `freemind-ai-a9c14`.
2. Deploy: `supabase functions deploy stamp-role`.
3. `verify_jwt` is already set to `false` for this function in
   `supabase/config.toml`, with an explanation of why in the comment there —
   the function verifies the token itself.

The app already handles the resulting flow: `SignInViewModel` calls the
function right after Firebase sign-in and awaits its result before
force-refreshing the ID token (see `RoleStampClient.kt` and
`authRepository.idToken(forceRefresh = true)` in `SignInViewModel.kt`), so the
very next Supabase call — `ensureProfile` — carries a token that already has
the claim.

## 3. Supabase project

1. Create a project at [supabase.com](https://supabase.com).
2. **SQL Editor** → run [`supabase/migrations/20260820000000_init_schema.sql`](supabase/migrations/20260820000000_init_schema.sql)
   from this repo. This creates `profiles`, `place_search_history`,
   `favorite_places`, and their RLS policies.
   Then run the later files in `supabase/migrations/` in date order. The newest,
   `20261114120000_saved_plans_and_favorite_lists.sql`, adds `saved_plans` (trip plans
   synced across devices) and `favorite_lists` (named groups of favorites). On an
   existing project, `supabase/APPLY_TO_LIVE.sql` has the same statements.
3. **Authentication → Sign In / Providers → Third-Party Auth → Add integration
   → Firebase.** Enter your Firebase **Project ID** (Project settings →
   General, not the project number).
4. **Project settings → API** → copy the **Project URL** and **anon public
   key** into `local.properties` at the repo root:
   ```
   SUPABASE_URL=https://your-project-ref.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   ```
   (The anon key is safe here — it's a public identifier, not a secret. RLS is
   the actual access boundary, which is why step 2 above matters so much.)

## 4. Google Maps key (Places section)

The Places tab needs its own key, separate from everything above.

1. [Google Cloud Console](https://console.cloud.google.com) → same project as
   Firebase (or a new one) → **APIs & Services → Enable APIs** → enable both
   **Maps SDK for Android** and **Places API**. Both are required — the map
   view and the search use different APIs.
2. **Credentials → Create credentials → API key.**
3. **Restrict the key** (do this now, not later): Application restrictions →
   *Android apps* → add package `com.freemind.ai` with the debug SHA-1 from
   step 1.5. API restrictions → limit it to the two APIs above.
4. Put it in `local.properties`:
   ```
   MAPS_API_KEY=AIza...
   ```
5. Rebuild. Until the key is set, the Places tab renders an empty map and the
   search shows a "Maps isn't configured yet" banner rather than crashing.

**Why this key ships in the app at all**: the Maps SDK draws the map
in-process on the device, so it has to read the key locally — there's no
server to hide it behind. That's the normal, documented model for Maps keys;
the protection is the *restriction* in step 3 (a key locked to your package +
signing certificate is useless to anyone else), not secrecy.

## What's already handled for you

- The app builds, installs, and runs without any of the above — you'll land
  on the sign-in screen and every backend call fails gracefully (offline
  banner, "not configured" messaging) rather than crashing.
- The debug Gradle wrapper, dependency versions, and `google-services.json`
  placeholder are already in place.
- RLS policies and the FCM notification channel are already wired into the
  code — steps 1–4 above just supply the credentials they run against.
