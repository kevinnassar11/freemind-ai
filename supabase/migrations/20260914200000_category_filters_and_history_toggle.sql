-- Category Filters: widen place_search_history.mode to accept the three new
-- PlaceSearchMode entries (food/culture/shopping) alongside the original four.
alter table public.place_search_history drop constraint if exists place_search_history_mode_check;
alter table public.place_search_history add constraint place_search_history_mode_check
  check (mode in ('prompt', 'cities', 'countries', 'places', 'food', 'culture', 'shopping'));

-- Privacy Controls: a per-user switch for whether searches get recorded at
-- all. Defaults to true (today's behavior) so existing users see no change
-- until they explicitly opt out.
alter table public.profiles add column if not exists history_enabled boolean not null default true;
