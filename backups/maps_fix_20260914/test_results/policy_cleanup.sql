DROP POLICY IF EXISTS favorite_owner_select ON public.favorite_places;
DROP POLICY IF EXISTS favorite_owner_insert ON public.favorite_places;
DROP POLICY IF EXISTS favorite_owner_update ON public.favorite_places;
DROP POLICY IF EXISTS favorite_owner_delete ON public.favorite_places;
DROP POLICY IF EXISTS favorite_places_all_own ON public.favorite_places;
CREATE POLICY favorite_places_all_own
  ON public.favorite_places
  FOR ALL
  USING ( user_id::text = (auth.jwt() ->> 'sub') )
  WITH CHECK ( user_id::text = (auth.jwt() ->> 'sub') );
