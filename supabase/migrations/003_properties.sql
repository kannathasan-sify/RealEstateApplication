-- ============================================================
-- 003_properties.sql
-- Property listings table
-- ============================================================

CREATE TABLE IF NOT EXISTS properties (
  id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id           UUID          REFERENCES profiles(id) ON DELETE SET NULL,
  agency_id          UUID          REFERENCES agencies(id) ON DELETE SET NULL,
  listed_by          TEXT          NOT NULL DEFAULT 'landlord'
                                   CHECK (listed_by IN ('landlord','agent','agency','developer')),
  title              TEXT          NOT NULL,
  description        TEXT,
  price              NUMERIC(15,2) NOT NULL,
  price_frequency    TEXT          NOT NULL DEFAULT 'yearly'
                                   CHECK (price_frequency IN ('yearly','monthly','weekly')),
  property_type      TEXT
                     CHECK (property_type IN (
                       'apartment','villa','townhouse','penthouse','hotel_apartment',
                       'residential_building','villa_compound','residential_floor',
                       'office','shop','warehouse','labour_camp','commercial_building',
                       'commercial_floor','commercial_villa','factory','land',
                       'industrial_land','other'
                     )),
  listing_type       TEXT          CHECK (listing_type IN ('rent','sale','off_plan')),
  bedrooms           INT,
  bathrooms          INT,
  area_sqft          NUMERIC(10,2),
  address            TEXT,
  neighborhood       TEXT,
  city               TEXT,
  latitude           FLOAT,
  longitude          FLOAT,
  images             TEXT[]        DEFAULT '{}',
  video_url          TEXT,
  amenities          TEXT[]        DEFAULT '{}',
  furnishing         TEXT          NOT NULL DEFAULT 'unfurnished'
                                   CHECK (furnishing IN ('furnished','unfurnished','semi')),
  completion_status  TEXT          CHECK (completion_status IN ('ready','off_plan')),
  payment_plan       TEXT,
  handover_date      DATE,
  developer_name     TEXT,
  permit_number      TEXT,
  rera_number        TEXT,
  reference_id       TEXT          UNIQUE,
  brn_dld            TEXT,
  zone_name          TEXT,
  is_verified        BOOLEAN       NOT NULL DEFAULT FALSE,
  is_featured        BOOLEAN       NOT NULL DEFAULT FALSE,
  status             TEXT          NOT NULL DEFAULT 'active'
                                   CHECK (status IN ('active','sold','rented','inactive')),
  created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TRIGGER properties_updated_at
  BEFORE UPDATE ON properties
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Indexes for common filter queries
CREATE INDEX idx_properties_listing_type   ON properties(listing_type);
CREATE INDEX idx_properties_property_type  ON properties(property_type);
CREATE INDEX idx_properties_city           ON properties(city);
CREATE INDEX idx_properties_status         ON properties(status);
CREATE INDEX idx_properties_owner_id       ON properties(owner_id);
CREATE INDEX idx_properties_agency_id      ON properties(agency_id);
CREATE INDEX idx_properties_is_featured    ON properties(is_featured);
CREATE INDEX idx_properties_price          ON properties(price);
CREATE INDEX idx_properties_created_at     ON properties(created_at DESC);
CREATE INDEX idx_properties_amenities      ON properties USING GIN(amenities);
