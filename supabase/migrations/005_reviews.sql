-- ============================================================
-- 005_reviews.sql
-- Property reviews and ratings
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id UUID        REFERENCES properties(id) ON DELETE CASCADE,
  reviewer_id UUID        REFERENCES profiles(id)   ON DELETE CASCADE,
  rating      INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment     TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  -- One review per user per property
  UNIQUE (property_id, reviewer_id)
);

CREATE INDEX idx_reviews_property_id ON reviews(property_id);
CREATE INDEX idx_reviews_reviewer_id ON reviews(reviewer_id);
