# FreeMind AI — Product Vision & Extended Architecture

## Positioning, honestly

"More special than ChatGPT, Claude, DeepSeek, Codex, and Replit" isn't achievable by matching their feature lists — those are foundation-model companies with thousands of engineers and their own frontier models. Bolting on voice + image + code execution + plugins + trading tools all at once produces something that does everything a little worse than the specialists, not something special.

The actual differentiator available to a focused build: **depth in economics/finance** (your stated career focus) **+ real personal memory + a coherent, small plugin ecosystem**, on top of a genuinely solid general chat experience. Recommended sequencing: make the economics vertical excellent first — that's the wedge nothing else on this list has — then generalize outward. The roadmap below is ordered on that premise.

## What already exists (real, in progress)

Don't re-architect this — extend it:

- **Auth**: Firebase Authentication + Credential Manager Google Sign-In → [SignInScreen.kt](app/src/main/java/com/freemind/ai/ui/auth/SignInScreen.kt), [SignInViewModel.kt](app/src/main/java/com/freemind/ai/ui/auth/SignInViewModel.kt), [AuthRepositoryImpl.kt](app/src/main/java/com/freemind/ai/data/repository/AuthRepositoryImpl.kt)
- **Chat**: streaming responses via a Supabase Edge Function that holds the OpenAI key server-side → [ChatEdgeFunctionClient.kt](app/src/main/java/com/freemind/ai/data/remote/openai/ChatEdgeFunctionClient.kt), [supabase/functions/chat/index.ts](supabase/functions/chat/index.ts)
- **Data**: Supabase Postgres + RLS, trusting Firebase as a third-party auth provider (no separate Supabase login) → [SupabaseModule.kt](app/src/main/java/com/freemind/ai/di/SupabaseModule.kt), [supabase/migrations/20260820000000_init_schema.sql](supabase/migrations/20260820000000_init_schema.sql)
- **History / Favorites / Settings / Voice input (STT) / FCM push**: built, following the same repository + Hilt DI pattern throughout

Everything below is designed to extend this shape — same layers, same DI conventions, same "OpenAI/external keys never touch the client" rule — not replace it.

## Extended architecture (text diagram)

```
┌─ ui/ (Compose screens + ViewModels, one package per feature) ───────────┐
│  home · history · favorites · settings · auth                          │
│  + new: image · personas · plugins · economics                        │
└───────────────────────────┬──────────────────────────────────────────┘
                             │ (StateFlow / events)
┌─ data/repository/ (interfaces + impls, Hilt-provided singletons) ──────┐
│  AuthRepository · OpenAiRepository · SupabaseRepository · Settings     │
│  + new: MemoryRepository · PersonaRepository · PluginRepository        │
│         ImageRepository · EconomicsRepository                          │
└───────────────────────────┬──────────────────────────────────────────┘
                             │
        ┌────────────────────┼─────────────────────┬───────────────┐
        ▼                    ▼                     ▼               ▼
┌───────────────┐  ┌──────────────────┐  ┌──────────────────┐  ┌──────────┐
│ Firebase       │  │ Supabase          │  │ Edge Functions    │  │ External │
│ Auth · FCM     │  │ Postgrest + RLS   │  │ (Deno, hold all   │  │ APIs     │
│                │  │ (per-user data)   │  │  provider keys)   │  │ (market  │
└───────────────┘  └──────────────────┘  │  chat · vision ·   │  │  data)   │
                                          │  economics-sim ·   │  └──────────┘
                                          │  plugin-exec (v2+) │
                                          └──────────────────┘
                                                    │
                                          ┌──────────────────┐
                                          │ Sandboxed code-run │  ← separate,
                                          │ service (Phase 3,  │    hardened
                                          │ NOT client-side)   │    service
                                          └──────────────────┘
```

The one structural addition: a **`KnowledgeFusionRepository`** that composes context from memory + personas + (later) external data before a query hits the chat Edge Function, rather than every screen assembling its own prompt context ad hoc.

```kotlin
// data/repository/KnowledgeFusionRepository.kt
interface KnowledgeFusionRepository {
    suspend fun buildContext(userId: String, persona: Persona?): PromptContext
}

data class PromptContext(
    val systemPrompt: String,          // persona + memory summary, composed once
    val recentMemory: List<ChatMessage>,
    val externalFacts: List<String> = emptyList(), // e.g. economics data snippets
)
```

`HomeViewModel` calls this once per query instead of hand-assembling context; the Edge Function's `input` array is built from `PromptContext` rather than raw message history.

---

## 1. Identity & memory

Memory toggle and delete already have a natural home; export is new.

```kotlin
// data/local/MemoryPreferences.kt — same DataStore pattern as ThemePreferenceDataStore
class MemoryPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val memoryEnabled: Flow<Boolean> = dataStore.data.map { it[MEMORY_ENABLED] ?: true }
    suspend fun setMemoryEnabled(enabled: Boolean) = dataStore.edit { it[MEMORY_ENABLED] = enabled }
    private companion object { val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled") }
}
```

`OpenAiRepositoryImpl` checks `memoryEnabled` before including prior messages in `PromptContext` — when off, every query is stateless (still saved to history only if the user separately opts into logging; keep those two toggles distinct).

```kotlin
// data/repository/MemoryRepository.kt
interface MemoryRepository {
    suspend fun exportUserData(userId: String): ExportBundle   // JSON: profile + conversations + favorites
    suspend fun deleteAllUserData(userId: String)              // extends existing "clear history"
}
```

Export is a Postgrest read across `conversations`/`messages`/`favorites` filtered by the RLS-scoped user, serialized to JSON, handed to Android's share sheet — no new backend needed. Delete extends the existing Settings "clear history" action to also drop `profiles` and `ai_usage_logs` rows.

## 2. Multi-modal

**Voice output (TTS)** — smallest addition, do this first:

```kotlin
// voice/TextToSpeechManager.kt
class TextToSpeechManager @Inject constructor(@ApplicationContext context: Context) {
    private var tts: TextToSpeech? = null
    // init { tts = TextToSpeech(context) { status -> ... } }, speak(text), stop(), release()
}
```

Android's built-in `TextToSpeech` — free, on-device, no new backend, mirrors how `SpeechRecognizerManager` already works.

**Image upload/analysis** — real new surface, moderate scope:

```kotlin
// data/remote/openai/ImageAnalysisClient.kt
interface ImageAnalysisClient {
    suspend fun analyze(imageBytes: ByteArray, prompt: String): Flow<ChatStreamEvent>
}
```

Route through a new `supabase/functions/vision/index.ts` Edge Function (same JWT-verification + rate-limit pattern as `chat/index.ts`), calling a vision-capable model. Client-side: `ImagePicker` via `ActivityResultContracts.PickVisualMedia`, downscale before upload (cap payload size), same streaming-response UI as chat.

**Code execution sandbox** — flagging this honestly: this is not scaffolding-sized. Running arbitrary code safely requires real isolation (gVisor/Firecracker microVMs, or a vetted third-party sandbox API) with CPU/memory/time/network limits, not a library you `implementation()` into the Edge Function. Recommendation: use an existing hardened sandbox provider behind a `code-exec` Edge Function rather than building isolation from scratch. Client-side interface can be scaffolded now even though the backend is Phase 3:

```kotlin
// data/repository/CodeExecutionRepository.kt
interface CodeExecutionRepository {
    suspend fun run(language: SupportedLanguage, code: String, stdin: String = ""): ExecutionResult
}
data class ExecutionResult(val stdout: String, val stderr: String, val exitCode: Int, val timedOut: Boolean)
```

## 3. Personas

Straightforward, real MVP feature — a persona is just a named system prompt plus display metadata:

```sql
create table public.personas (
  id           text primary key,        -- 'teacher', 'coder', 'economist', 'entertainer'
  display_name text not null,
  system_prompt text not null,
  icon         text not null,
  is_builtin   boolean not null default true
);
-- built-ins are readable by everyone; no RLS needed on this table (public reference data)
```

`KnowledgeFusionRepository.buildContext` takes the selected persona's `system_prompt` as the base of `PromptContext.systemPrompt`. Persona picker is a `SettingsScreen`-style list screen, selection persisted the same way theme preference is (DataStore, per-user if you want it synced — then it's a `profiles.selected_persona` column instead).

## 4. Offline mode — scoped honestly

Full offline *inference* isn't realistic on-device at usable quality. "Offline mode" here means: cache the last N conversations/responses locally (Room, new dependency) so History/Favorites are browsable without network; new queries still require connectivity (the existing `ConnectivityObserver` + `ErrorBanner` already handle that gracefully). Don't oversell this to users as "works offline" — be explicit it's "your history is available offline."

## 5. Economics tools (the actual differentiator)

This deserves more than a bullet point since it's the strategic bet:

```sql
create table public.economics_scenarios (
  id          uuid primary key default gen_random_uuid(),
  user_id     text not null,
  title       text not null,
  parameters  jsonb not null,   -- e.g. {"model": "supply_demand", "elasticity": 1.2, ...}
  result      jsonb,            -- computed output, cached
  created_at  timestamptz not null default now()
);
alter table public.economics_scenarios enable row level security;
create policy "economics_scenarios_all_own" on public.economics_scenarios for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id) with check ((select auth.jwt()->>'sub') = user_id);
```

- **Simulations**: start with well-understood closed-form models (supply/demand equilibrium, compound growth, simple Monte Carlo) computed in an Edge Function (`economics-sim`) — deterministic, cheap, no LLM call needed for the math itself; the LLM explains/narrates the result.
- **Forecasting dashboards**: need a real external data source (a market/economic data API) — same key-security rule as OpenAI applies: that API key lives in an Edge Function secret, never the client.
- **Visualizations**: Compose has no built-in charting; pull in a maintained charting library (evaluate at implementation time — this space moves fast) rather than hand-rolling Canvas drawing for line/bar charts.
- **Hard boundary**: this stays analysis/simulation/education. I won't build anything that executes real trades or gives personalized "buy/sell this" investment advice — that's true for me regardless of interface, and worth designing around from the start rather than hitting it later.

## 6. Plugins & extensibility

Two very different things get called "plugins" — keep them separate:

**MVP: first-party modules behind a common interface.** Math solver, language tutor — you write these, they ship in the app, no trust boundary crossed:

```kotlin
// plugins/Plugin.kt
interface Plugin {
    val id: String
    val displayName: String
    val icon: ImageVector
    suspend fun handle(input: String, context: PromptContext): PluginResult
}
sealed interface PluginResult {
    data class Text(val content: String) : PluginResult
    data class Structured(val data: Map<String, Any>) : PluginResult  // e.g. solved equation steps
}

// plugins/registry/PluginRegistry.kt
@Singleton
class PluginRegistry @Inject constructor(plugins: Set<@JvmSuppressWildcards Plugin>) {
    private val byId = plugins.associateBy { it.id }
    fun all(): List<Plugin> = byId.values.toList()
    fun get(id: String): Plugin? = byId[id]
}
// Hilt multibinding: each plugin (MathSolverPlugin, LanguageTutorPlugin, ...) is
// @Binds @IntoSet into this Set<Plugin> — adding one is a new class + one binding line.
```

**Phase 3+: user-installable third-party plugins.** This is the same untrusted-code problem as the sandbox above — a plugin is just code someone else wrote that you'd run. Don't build an "upload your own plugin" feature before the sandboxing problem (§2) is actually solved; doing it earlier is a real security hole, not a shortcut.

```sql
-- only meaningful once sandboxed execution exists
create table public.plugin_installs (
  user_id     text not null,
  plugin_id   text not null,
  enabled     boolean not null default true,
  installed_at timestamptz not null default now(),
  primary key (user_id, plugin_id)
);
alter table public.plugin_installs enable row level security;
create policy "plugin_installs_all_own" on public.plugin_installs for all to authenticated
  using ((select auth.jwt()->>'sub') = user_id) with check ((select auth.jwt()->>'sub') = user_id);
```

---

## Roadmap

| Phase | Scope | Why here |
|---|---|---|
| **Now** | Finish verifying the existing flow end-to-end (sign-in, RLS, real Supabase data) | Nothing below is worth building on an unverified foundation |
| **MVP** | Memory toggle + export/delete, TTS voice output, personas, first-party plugins (math solver, language tutor) | All are direct extensions of existing patterns — repository + Hilt + one new table each, no new infrastructure class |
| **Phase 2** | Image analysis (new Edge Function), economics simulations + charts (closed-form models first), offline history cache | New surfaces, but each is one bounded Edge Function or one new local-storage layer — no unsolved hard problems |
| **Phase 3** | Sandboxed code execution (via a hardened third-party sandbox service, not built in-house), third-party plugin installs, real market-data-backed forecasting | Gated on real infrastructure/vendor decisions — don't start until Phase 2 ships and there's a concrete sandbox provider chosen |

Don't parallelize across phases — each one's Edge Function + table + screen is small in isolation; the risk is context-switching across five half-built features instead of shipping one at a time, the same way Auth → Chat → History got built here.

## Next step

Once sign-in is verified end-to-end, tell me which MVP item to start on first — memory toggle, TTS, or personas are all a few hours of work each following patterns already in the codebase.
