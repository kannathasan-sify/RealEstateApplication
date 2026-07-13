-- ============================================================
-- 006_saved.sql
-- Saved properties (favourites) + saved searches
-- ============================================================

-- ── Saved Properties ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_properties (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES profiles(id)    ON DELETE CASCADE,
  property_id UUID        NOT NULL REFERENCES properties(id)  ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, property_id)
);

CREATE INDEX idx_saved_properties_user_id ON saved_properties(user_id);

-- ── Saved Searches ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_searches (
  id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  label        TEXT,                       -- e.g. "All Residential"
  listing_type TEXT,                       -- e.g. "Property for Rent"
  filters      JSONB       DEFAULT '{}',  -- all filter params as JSON
  thumbnail_url TEXT,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saved_searches_user_id ON saved_searches(user_id);
