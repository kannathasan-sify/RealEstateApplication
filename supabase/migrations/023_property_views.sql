-- 023_property_views.sql
-- Per-property view tracking. One row per property-detail open (the "Total Views" /
-- "Views by property" / "Total views trend" metrics on the Owner dashboard). Recorded by
-- the backend via POST /properties/{id}/view when a listing detail screen is opened.
--
-- Kept as raw event rows (not a counter column) so trends over time can be computed and
-- so a viewer can later be de-duplicated / rate-limited without losing history.

CREATE TABLE IF NOT EXISTS property_views (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID REFERENCES properties(id) ON DELETE CASCADE,
    -- Null viewer_id = anonymous/logged-out view.
    viewer_id   UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_property_views_property ON property_views(property_id);
CREATE INDEX IF NOT EXISTS idx_property_views_created  ON property_views(created_at);
-- Composite index that serves the owner dashboard's "views per property over last N days".
CREATE INDEX IF NOT EXISTS idx_property_views_prop_created ON property_views(property_id, created_at);

ALTER TABLE property_views ENABLE ROW LEVEL SECURITY;

-- Anyone (incl. anonymous) may log a view; the backend inserts with the service-role client.
CREATE POLICY "insert_any_view" ON property_views
    FOR INSERT WITH CHECK (true);

-- A property's owner may read its views; admins may read all.
CREATE POLICY "owner_read_views" ON property_views
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM properties p WHERE p.id = property_id AND p.owner_id = auth.uid())
        OR EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin')
    );
