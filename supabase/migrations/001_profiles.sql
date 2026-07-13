-- ============================================================
-- 001_profiles.sql
-- User profiles table (extends Supabase auth.users)
-- ============================================================

CREATE TABLE IF NOT EXISTS profiles (
  id                UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name         TEXT,
  phone             TEXT,
  avatar_url        TEXT,
  role              TEXT        NOT NULL DEFAULT 'buyer'
                                CHECK (role IN ('buyer','landlord','agent','agency','developer','admin')),
  user_id_code      TEXT        UNIQUE,
  is_verified       BOOLEAN     NOT NULL DEFAULT FALSE,
  agency_id         UUID,
  biometric_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
  city              TEXT        NOT NULL DEFAULT 'All Cities',
  language          TEXT        NOT NULL DEFAULT 'English',
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Auto-update updated_at on row change
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

CREATE TRIGGER profiles_updated_at
  BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();
