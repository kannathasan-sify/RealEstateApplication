-- ============================================================
-- 007_rls_policies.sql
-- Row Level Security policies for all tables
-- ============================================================

-- ── profiles ─────────────────────────────────────────────────
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "profiles_own_select"
  ON profiles FOR SELECT
  USING (auth.uid() = id);

CREATE POLICY "profiles_own_update"
  ON profiles FOR UPDATE
  USING (auth.uid() = id);

-- Allow the backend (service role) to insert on first login
CREATE POLICY "profiles_service_insert"
  ON profiles FOR INSERT
  WITH CHECK (true);


-- ── agencies ─────────────────────────────────────────────────
ALTER TABLE agencies ENABLE ROW LEVEL SECURITY;

-- Anyone can read agencies
CREATE POLICY "agencies_public_read"
  ON agencies FOR SELECT
  USING (true);

-- Only admin (service role bypasses RLS; admin role check done in API)
CREATE POLICY "agencies_service_write"
  ON agencies FOR ALL
  USING (true)
  WITH CHECK (true);


-- ── properties ───────────────────────────────────────────────
ALTER TABLE properties ENABLE ROW LEVEL SECURITY;

-- Anyone can read active listings
CREATE POLICY "properties_public_read"
  ON properties FOR SELECT
  USING (status = 'active');

-- Owner can read their own listings regardless of status
CREATE POLICY "properties_owner_read_all"
  ON properties FOR SELECT
  USING (auth.uid() = owner_id);

-- Owner can insert their own listings
CREATE POLICY "properties_owner_insert"
  ON properties FOR INSERT
  WITH CHECK (auth.uid() = owner_id);

-- Owner can update their own listings
CREATE POLICY "properties_owner_update"
  ON properties FOR UPDATE
  USING (auth.uid() = owner_id);

-- Owner can delete their own listings
CREATE POLICY "properties_owner_delete"
  ON properties FOR DELETE
  USING (auth.uid() = owner_id);


-- ── bookings ─────────────────────────────────────────────────
ALTER TABLE bookings ENABLE ROW LEVEL SECURITY;

-- Buyer can read their own bookings
CREATE POLICY "bookings_buyer_select"
  ON bookings FOR SELECT
  USING (auth.uid() = buyer_id);

-- Buyer can create bookings
CREATE POLICY "bookings_buyer_insert"
  ON bookings FOR INSERT
  WITH CHECK (auth.uid() = buyer_id);

-- Buyer can update (cancel) their own bookings
CREATE POLICY "bookings_buyer_update"
  ON bookings FOR UPDATE
  USING (auth.uid() = buyer_id);

-- Buyer can delete their own bookings
CREATE POLICY "bookings_buyer_delete"
  ON bookings FOR DELETE
  USING (auth.uid() = buyer_id);

-- Property owner can read bookings for their listings
CREATE POLICY "bookings_property_owner_read"
  ON bookings FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM properties p
      WHERE p.id = bookings.property_id
        AND p.owner_id = auth.uid()
    )
  );


-- ── reviews ──────────────────────────────────────────────────
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- Anyone can read reviews
CREATE POLICY "reviews_public_read"
  ON reviews FOR SELECT
  USING (true);

-- Authenticated users can write their own reviews
CREATE POLICY "reviews_own_insert"
  ON reviews FOR INSERT
  WITH CHECK (auth.uid() = reviewer_id);

CREATE POLICY "reviews_own_update"
  ON reviews FOR UPDATE
  USING (auth.uid() = reviewer_id);

CREATE POLICY "reviews_own_delete"
  ON reviews FOR DELETE
  USING (auth.uid() = reviewer_id);


-- ── saved_properties ─────────────────────────────────────────
ALTER TABLE saved_properties ENABLE ROW LEVEL SECURITY;

CREATE POLICY "saved_properties_own_select"
  ON saved_properties FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "saved_properties_own_insert"
  ON saved_properties FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "saved_properties_own_delete"
  ON saved_properties FOR DELETE
  USING (auth.uid() = user_id);


-- ── saved_searches ────────────────────────────────────────────
ALTER TABLE saved_searches ENABLE ROW LEVEL SECURITY;

CREATE POLICY "saved_searches_own_select"
  ON saved_searches FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "saved_searches_own_insert"
  ON saved_searches FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "saved_searches_own_delete"
  ON saved_searches FOR DELETE
  USING (auth.uid() = user_id);
