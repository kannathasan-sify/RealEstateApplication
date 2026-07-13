-- ============================================================
-- 002_agencies.sql
-- Real estate agencies table
-- ============================================================

CREATE TABLE IF NOT EXISTS agencies (
  id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  name           TEXT        NOT NULL,
  logo_url       TEXT,
  license_number TEXT,
  rera_number    TEXT,
  phone          TEXT,
  email          TEXT,
  city           TEXT,
  is_verified    BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Now that agencies exists, add the FK constraint to profiles
ALTER TABLE profiles
  ADD CONSTRAINT profiles_agency_id_fkey
  FOREIGN KEY (agency_id) REFERENCES agencies(id)