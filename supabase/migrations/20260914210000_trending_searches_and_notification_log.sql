-- Trending Searches: a security definer function that aggregates
-- place_search_history across ALL users. This deliberately bypasses that
-- table's per-user RLS - the only way to compute a cross-user aggregate - but
-- it only ever returns (query, count), never user_id or a row id, so no
-- individual user's search history is exposed to another user.
--
-- search_path is pinned explicitly, which is required practice for any
-- security definer function: without it, a malicious search_path on the
-- calling session could redirect unqualified identifiers to attacker-controlled
-- objects. Every identifier here is already schema-qualified as an extra
-- guard on top of that.
create or replace function public.trending_searches(result_limit int default 10)
returns table(query text, search_count bigint)
language sql
security definer
set search_path = public
as $$
  select query, count(*) as search_count
  from public.place_search_history
  where searched_at > now() - interval '7 days'
  group by query
  order by search_count desc, query asc
  limit result_limit;
$$;

-- Only signed-in app users may call it - not the anon role.
revoke all on function public.trending_searches(int) from public;
grant execute on function public.trending_searches(int) to authenticated;

-- Push Notifications: a log of what's already been sent, so a scheduled job
-- can avoid re-notifying about the same favorite. Written only by the
-- notify-favorites Edge Function via the service_role key (same pattern as
-- the old ai_usage_logs table) - authenticated users may only read their own
-- rows, never write here directly.
create table public.notification_log (
  id                 uuid primary key default gen_random_uuid(),
  user_id            text not null,
  place_id           text not null,
  notification_type  text not null default 'trending_favorite',
  sent_at            timestamptz not null default now()
);

create index notification_log_user_place_idx on public.notification_log (user_id, place_id, sent_at desc);

alter table public.notification_log enable row level security;

create policy "notification_log_select_own" on public.notification_log for select to authenticated
  using ( (select auth.jwt()->>'sub') = user_id );
