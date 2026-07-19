-- 025_fraud_alerts.sql
-- Admin dashboard "Fraud & complaint alerts" feed. Rows are raised by moderation/heuristics
-- (duplicate photos, price anomalies, mismatched payout claims, …) and worked by admins.
-- For now rows are seeded/inserted manually or by future detection jobs; the admin dashboard
-- reads open ones and the "Fraud Alerts" KPI counts unresolved.

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_type  TEXT,                                  -- duplicate_listing | price_anomaly | suspicious_payout | ...
    title       TEXT NOT NULL,
    details     TEXT,
    severity    TEXT NOT NULL DEFAULT 'pending'
                CHECK (severity IN ('flagged', 'pending', 'resolved')),
    property_id UUID REFERENCES properties(id) ON DELETE SET NULL,
    user_id     UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fraud_alerts_severity ON fraud_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_created  ON fraud_alerts(created_at);

ALTER TABLE fraud_alerts ENABLE ROW LEVEL SECURITY;

-- Admin-only surface. Backend reads/writes with the service-role client.
CREATE POLICY "admin_all_fraud_alerts" ON fraud_alerts
    USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'));
