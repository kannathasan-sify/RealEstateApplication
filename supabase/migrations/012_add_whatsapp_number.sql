-- Migration 012: Add whatsapp_number to properties table
-- WhatsApp number is required for landlord listings so buyers can contact directly.
-- Agents/builders may optionally provide it; if omitted their agent_phone is used.

ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS whatsapp_number TEXT;

-- Index for quick lookup when filtering landlord listings by contact availability
CREATE INDEX IF NOT EXISTS idx_properties_whatsapp
    ON properties (whatsapp_number)
    WHERE whatsapp_number IS NOT NULL;

-- Backfill: for existing landlord listings, copy agent_phone into whatsapp_number
UPDATE properties
    SET whatsapp_number = agent_phone
WHERE listed_by = 'landlord'
  AND agent_phone IS NOT NULL
  AND whatsapp_number IS NULL;

COMMENT ON COLUMN properties.whatsapp_number IS
    'WhatsApp-enabled phone number for this listing. Required for landlord postings. '
    'Displayed as a WhatsApp chat button on the Prope