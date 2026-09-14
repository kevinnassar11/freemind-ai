-- FreeMind AI schema.
--
-- user_id is `text` everywhere, not `uuid`: Firebase UIDs are opaque strings,
-- not UUIDs. Supabase trusts Firebase as a third-party auth provider (see
-- SETUP.md), so every RLS policy compares user_id against the Firebase UID
-- embedded in the caller's JWT: (select auth.jwt()->>'sub').

create table public.profiles (
  user_id       text primary key,
  email         text,
  display_name  text,
  photo_url     text,
  theme_pref    text not null default 'system' check (theme_pref in ('light', 'dark', 'system')),
  fcm_token     text,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "profiles_select_own" on public.profiles for select to authenticated
  using ( (select auth.jwt()->>'sub') = user_id );
create policy "profiles_insert_own" on public.profiles for insert to authenticated
  with check ( (select auth.jwt()->>'sub') = user_id );
create policy "profiles_update_own" on public.profiles for update to authenticated
  using ( (select auth.jwt()->>'sub') = user_id ) with check ( (select auth.jwt()->>'sub') = user_id );
create policy "profiles_delete_own" on public.profiles for delete to authenticated
  using ( (select auth.jwt()->>'sub') = user_id );

-- One row per place search (Places tab). `mode` mirrors PlaceSearchMode
-- (prompt/cities/countries/places); result_* columns cache the top hit so
-- the History tab can show it without a second round trip.
create table public.place_search_history (
  id               uuid primary key default gen_random_uuid(),
  user_id          text not null,
  query            text not null,
  mode             text not null check (mode in ('prompt', 'cities', 'countries', 'places')),
  result_place_id  text,
  result_title     text,
  latitude         double precision,
  longitude        double precision,
  searched_at      timestamptz not null default now()
);

create index place_search_history_user_id_idx on public.place_search_history (user_id, searched_at desc);

alter table public.place_search_history enable row level security;

create policy "place_search_history_all_own" on public.place_search_history for all to authenticated
  using ( (select auth.jwt()->>'sub') = user_id ) with check ( (select auth.jwt()->>'sub') = user_id );

-- A saved place (Favorites tab). Unique per (user_id, place_id) so
-- favoriting the same place twice is a no-op, not a duplicate row.
create table public.favorite_places (
  id            uuid primary key default gen_random_uuid(),
  user_id       text not null,
  place_id      text not null,
  title         text not null,
  description   text,
  latitude      double precision,
  longitude     double precision,
  favorited_at  timestamptz not null default now(),
  unique (user_id, place_id)
);

create index favorite_places_user_id_idx on public.favorite_places (user_id, favorited_at desc);

alter table public.favorite_places enable row level security;

create policy "favorite_places_all_own" on public.favorite_places for all to authenticated
  using ( (select auth.jwt()->>'sub') = user_id ) with check ( (select auth.jwt()->>'sub') = user_id );

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger trg_profiles_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();
