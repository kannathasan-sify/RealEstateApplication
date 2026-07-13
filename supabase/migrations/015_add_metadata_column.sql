-- ─────────────────────────────────────────────────────────────────────────────
-- Migration 015 — Add metadata JSONB column to properties
-- Purpose: Stores category-specific extra fields for Ground, Contractor, and
--          Holiday Stay listings (e.g. ground_type, surface, facilities,
--          work_types, check_in, cancellation policy, etc.)
-- ─────────────────────────────────────────────────────────────────────────────

-- Add the column (idempotent)
ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT NULL;

-- GIN index enables fast key/value lookups inside the JSONB object,
-- e.g. metadata @> '{"ground_type":"cricket"}' or metadata ? 'facilities'
CREATE INDEX IF NOT EXISTS idx_properties_metadata
    ON properties
    USING GIN(metadata);

-- ── Comments ──────────────────────────────────────────────────────────────────
COMMENT ON COLUMN properties.metadata IS
    'Category-specific extra fields stored as JSONB. '
    'Keys used by listing_type: '
    'ground      → ground_type, length_m, width_m, surface, floodlights, '
    '              available_from, available_to, capacity, advance_booking, '
    '              cancellation, facilities '
    'contractor  → work_category, work_types, experience_yrs, service_areas, '
    '              pricing_model, license_no, team_size, timeline, '
    '              warranty, warranty_dur '
    'holiday_stay → stay_type, max_guests, check_in, check_out, min_nights, '
    '               facilities, house_rules, cancellation';
