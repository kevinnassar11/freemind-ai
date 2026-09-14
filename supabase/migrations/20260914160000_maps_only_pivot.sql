-- Maps-only pivot: drop the chat feature's tables, trim the chat-only
-- profiles columns (without touching existing profile rows), and add the
-- two tables the Places tab now needs. Safe to run against a live project
-- that already has `profiles` populated - unlike a from-scratch schema
-- script, this never drops or recreates `profiles` itself.

drop trigger if exists trg_touch_conversation_on_new_message on public.messages;
drop function if exists public.touch_conversation_on_new_message();
drop trigger if exists trg_conversations_updated_at on public.conversations;

drop table if exists public.ai_usage_logs cascade;
drop table if exists public.messages cascade;
drop table if exists public.conversations cascade;

alter table public.profiles drop column if exists memory_enabled;
alter table public.profiles drop column if exists model_pref;
alter table public.profiles drop column if exists subscription_tier;

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
