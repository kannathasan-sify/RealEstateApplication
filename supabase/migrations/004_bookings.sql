-- ============================================================
-- 004_bookings.sql
-- Property visit bookings
-- ============================================================

CREATE TABLE IF NOT EXISTS bookings (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID        REFERENCES properties(id) ON DELETE CASCADE,
  buyer_id    UUID        REFERENCES profiles(id)   ON DELETE CASCADE,
  visit_date  DATE,
  visit_time  TIME,
  status      TEXT        NOT NULL DEFAULT 'pending'
              CHECK (status IN ('pending','confirmed','cancelled','completed')),
  message     TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_buyer_id    ON bookings(buyer_id);
CREATE INDEX idx_bookings_property_id ON bookings(property_id);
CREATE INDEX idx_bookings_status      ON bookings(status);
