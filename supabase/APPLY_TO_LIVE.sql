-- ===========================================================================
-- FreeMind AI - apply the maps-only pivot to the live database.
--
-- Paste this whole file into: Supabase Dashboard -> SQL Editor -> Run.
-- (Same content as supabase/migrations/20260914160000_maps_only_pivot.sql -
-- kept here too since that's where this project's manual-apply docs live.)
--
-- Drops the chat feature's tables (conversations, messages, ai_usage_logs)
-- and trims the chat-only columns off `profiles`, but never drops or
-- recreates `profiles` itself - existing profile rows are left alone.
-- ===========================================================================

drop trigger if exists trg_touch_conversation_on_new_message on public.messages;
drop function if exists public.touch_conversation_on_new_message();
drop trigger if exists trg_conversations_updated_at on public.conversations;

drop table if exists public.ai_usage_logs cascade;
drop table if exists public.messages cascade;
drop table if exists public.conversations cascade;

alter table public.profiles drop column if exists memory_enabled;
alter table public.profiles drop column if exists model_pref;
alter table public.profiles drop column if exists subscription_tier;

create table if not exists public.place_search_history (
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

create index if not exists place_search_history_user_id_idx on public.place_search_history (user_id, searched_at desc);

alter table public.place_search_history enable row level security;

drop policy if exists "place_search_history_all_own" on public.place_search_history;
create policy "place_search_history_all_own" on public.place_search_history for all to authenticated
  using ( (select auth.jwt()->>'sub') = user_id ) with check ( (select auth.jwt()->>'sub') = user_id );

create table if not exists public.favorite_places (
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

create index if not exists favorite_places_user_id_idx on public.favorite_places (user_id, favorited_at desc);

alter table public.favorite_places enable row level security;

drop policy if exists "favorite_places_all_own" on public.favorite_places;
create policy "favorite_places_all_own" on public.favorite_places for all to authenticated
  using ( (select auth.jwt()->>'sub') = user_id ) with check ( (select auth.jwt()->>'sub') = user_id );
