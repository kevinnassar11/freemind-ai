-- Trip plans that follow the user across devices, and named lists for favorites.
-- Additive only: two new tables and one new nullable column. Safe to run against
-- the live project; nothing existing is dropped or rewritten.

-- Saved trip plans. `plan` is the app's own JSON for the whole plan (days, stops,
-- notes, checklist, start date), so the server never needs to understand its shape.
create table if not exists public.saved_plans (
  id           uuid primary key,
  user_id      text not null,
  destination  text not null,
  day_count    int  not null check (day_count between 0 and 60),
  plan         jsonb not null check (octet_length(plan::text) < 1000000),
  saved_at     timestamptz not null default now()
);

create index if not exists saved_plans_user_id_idx on public.saved_plans (user_id, saved_at desc);

alter table public.saved_plans enable row level security;

drop policy if exists "saved_plans_all_own" on public.saved_plans;
create policy "saved_plans_all_own" on public.saved_plans for all to authenticated
  using ( (select auth.jwt()->>'sub') = user_id ) with check ( (select auth.jwt()->>'sub') = user_id );

-- Named groups of favorites ("Beirut food", "Paris ideas"). A favorite belongs to at
-- most one list; deleting a list just un-files its places, it never deletes them.
create table if not exists public.favorite_lists (
  id          uuid primary key default gen_random_uuid(),
  user_id     text not null,
  name        text not null check (char_length(btrim(name)) between 1 and 60),
  created_at  timestamptz not null default now(),
  unique (user_id, name)
);

create index if not exists favorite_lists_user_id_idx on public.favorite_lists (user_id, created_at);

alter table public.favorite_lists enable row level security;

drop policy if exists "favorite_lists_all_own" on public.favorite_lists;
create policy "favorite_lists_all_own" on public.favorite_lists for all to authenticated
  using ( (select auth.jwt()->>'sub') = user_id ) with check ( (select auth.jwt()->>'sub') = user_id );

alter table public.favorite_places
  add column if not exists list_id uuid references public.favorite_lists (id) on delete set null;

create index if not exists favorite_places_list_id_idx on public.favorite_places (list_id);
