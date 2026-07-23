-- seed_ads.sql — sample data for the Advertisement Ranking Engine (GET /ads/home).
-- Run AFTER migration 026_advertisements_engine.sql. Re-runnable (ON CONFLICT DO NOTHING).
-- Gives the home feed a realistic ranked mix: paid + organic, priorities 1-3, distinct CTAs.

-- ── Advertisers ───────────────────────────────────────────────────────────────
INSERT INTO ad_advertisers (id, name, advertiser_type, is_verified, government_approved, rating, years_in_business, lead_success_rate)
VALUES
  ('a1111111-1111-1111-1111-111111111101', 'Prestige Estates',    'builder',   TRUE,  FALSE, 4.6, 12, 0.42),
  ('a1111111-1111-1111-1111-111111111102', 'HDFC Home Loans',     'bank',      TRUE,  TRUE,  4.4, 25, 0.55),
  ('a1111111-1111-1111-1111-111111111103', 'Skyline Developers',  'builder',   TRUE,  FALSE, 4.1,  8, 0.38),
  ('a1111111-1111-1111-1111-111111111104', 'TN Legal Associates', 'legal',     FALSE, FALSE, 4.3,  6, 0.30),
  ('a1111111-1111-1111-1111-111111111105', 'Casagrand Builder',   'builder',   TRUE,  FALSE, 4.5, 20, 0.48),
  ('a1111111-1111-1111-1111-111111111106', 'DAC Developers',      'builder',   TRUE,  FALSE, 4.0, 15, 0.35)
ON CONFLICT (id) DO NOTHING;

-- ── Campaigns (only paid ads need one; organic ads have campaign_id = NULL) ────
INSERT INTO ad_campaigns (id, advertiser_id, name, revenue_model, bid_amount, daily_budget, remaining_budget, plan, subscription_level, status)
VALUES
  ('c2222222-2222-2222-2222-222222222201', 'a1111111-1111-1111-1111-111111111101', 'Prestige Featured', 'cpc', 320, 5000, 4200, 'featured', 'gold',     'active'),
  ('c2222222-2222-2222-2222-222222222202', 'a1111111-1111-1111-1111-111111111102', 'HDFC Loans CPC',    'cpc', 210, 8000, 6800, 'premium',  'platinum', 'active'),
  ('c2222222-2222-2222-2222-222222222203', 'a1111111-1111-1111-1111-111111111103', 'Skyline Launch',    'cpc', 180, 4000, 3100, 'premium',  'gold',     'active'),
  ('c2222222-2222-2222-2222-222222222204', 'a1111111-1111-1111-1111-111111111105', 'Casagrand Featured','cpc', 240, 6000, 5200, 'featured', 'gold',     'active')
ON CONFLICT (id) DO NOTHING;

-- ── Advertisements ────────────────────────────────────────────────────────────
INSERT INTO advertisements (
    id, campaign_id, advertiser_id, title, subtitle, image_url,
    ad_type, category, priority_level, sponsored_status, cta, cta_target,
    target_districts, target_listing_types,
    quality_score, ctr, conversion_rate, is_verified, is_urgent, has_price_drop, status
) VALUES
  -- Featured property (priority 1, paid)
  ('d3333333-3333-3333-3333-333333333301', 'c2222222-2222-2222-2222-222222222201', 'a1111111-1111-1111-1111-111111111101',
   'Sea-View Villas — ECR', '3–5 BHK · Ready to move',
   'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800',
   'property', 'villa', 1, 'featured', 'book_site_visit', 'mock-001',
   ARRAY['Chennai','Kancheepuram'], ARRAY['sale','rent'],
   0.8, 0.08, 0.03, TRUE, FALSE, FALSE, 'active'),

  -- Organic urgent price-drop (priority 1, no campaign)
  ('d3333333-3333-3333-3333-333333333302', NULL, 'a1111111-1111-1111-1111-111111111101',
   'Urgent Sale — 2BHK OMR', 'Price dropped ₹6L this week',
   'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800',
   'property', 'apartment', 1, 'organic', 'view_property', 'mock-002',
   ARRAY['Chennai'], ARRAY['sale'],
   0.6, 0.06, 0.02, FALSE, TRUE, TRUE, 'active'),

  -- Sponsored home loan (priority 2, paid)
  ('d3333333-3333-3333-3333-333333333303', 'c2222222-2222-2222-2222-222222222202', 'a1111111-1111-1111-1111-111111111102',
   'Home Loans @ 8.35%', 'Instant eligibility · 0 processing fee',
   'https://images.unsplash.com/photo-1450101499163-c8848c66ca85?w=800',
   'financial', 'home_loan', 2, 'sponsored', 'apply_home_loan', 'https://hdfc.example/apply',
   NULL, NULL,
   0.7, 0.05, 0.04, TRUE, FALSE, FALSE, 'active'),

  -- Organic legal service (priority 2)
  ('d3333333-3333-3333-3333-333333333304', NULL, 'a1111111-1111-1111-1111-111111111104',
   'Property Verification & EC', 'Title check · Registration support',
   'https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=800',
   'legal', 'property_verification', 2, 'organic', 'get_legal_verification', '9198000000',
   NULL, NULL,
   0.55, 0.04, 0.02, FALSE, FALSE, FALSE, 'active'),

  -- Promoted builder (priority 3, paid)
  ('d3333333-3333-3333-3333-333333333305', 'c2222222-2222-2222-2222-222222222203', 'a1111111-1111-1111-1111-111111111103',
   'Skyline Towers — New Launch', 'Pre-launch pricing · Coimbatore',
   'https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800',
   'construction', 'builder', 3, 'promoted', 'visit_builder', 'https://skyline.example',
   ARRAY['Coimbatore'], ARRAY['sale'],
   0.65, 0.05, 0.02, TRUE, FALSE, FALSE, 'active'),

  -- Organic owner listing (priority 1)
  ('d3333333-3333-3333-3333-333333333306', NULL, 'a1111111-1111-1111-1111-111111111101',
   '3BHK Flat — Call Owner', 'No brokerage · Immediate',
   'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800',
   'property', 'apartment', 1, 'organic', 'call_owner', '9199000000',
   ARRAY['Chennai'], ARRAY['rent'],
   0.5, 0.04, 0.01, FALSE, FALSE, FALSE, 'active'),

  -- Featured builder (Casagrand, priority 3, paid)
  ('d3333333-3333-3333-3333-333333333307', 'c2222222-2222-2222-2222-222222222204', 'a1111111-1111-1111-1111-111111111105',
   'Casagrand Elite — Villas', 'Gated community · Chennai OMR',
   'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800',
   'construction', 'builder', 3, 'featured', 'visit_builder', 'https://casagrand.example',
   ARRAY['Chennai'], ARRAY['sale'],
   0.72, 0.06, 0.03, TRUE, FALSE, FALSE, 'active'),

  -- Organic builder (DAC, priority 3)
  ('d3333333-3333-3333-3333-333333333308', NULL, 'a1111111-1111-1111-1111-111111111106',
   'DAC Smart Homes', 'Solar-ready · Coimbatore',
   'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800',
   'construction', 'builder', 3, 'organic', 'visit_builder', 'https://dac.example',
   ARRAY['Coimbatore'], ARRAY['sale'],
   0.6, 0.04, 0.02, TRUE, FALSE, FALSE, 'active')
ON CONFLICT (id) DO NOTHING;
