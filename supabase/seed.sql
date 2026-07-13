-- ============================================================
-- seed.sql — NestX India / Tamil Nadu
-- Run AFTER all migrations (001–008)
-- ============================================================

-- ── Agencies ──────────────────────────────────────────────────────────────────

INSERT INTO agencies (id, name, license_number, phone, email, city, is_verified)
VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001',
   'Prime Realty Tamil Nadu', 'TN-RERA-AG-2024-0001',
   '+91 44 4567 8901', 'info@primerealty.tn', 'Chennai', true),
  ('aaaaaaaa-0000-0000-0000-000000000002',
   'Karur Property Hub', 'TN-RERA-AG-2024-0012',
   '+91 4324 221234', 'sales@karurhub.in', 'Karur', true),
  ('aaaaaaaa-0000-0000-0000-000000000003',
   'Coimbatore Homes', 'TN-RERA-AG-2024-0025',
   '+91 422 2345678', 'info@cbehomes.in', 'Coimbatore', true),
  ('aaaaaaaa-0000-0000-0000-000000000004',
   'Trichy Real Estate', 'TN-RERA-AG-2024-0033',
   '+91 431 2345000', 'contact@trichyre.in', 'Tiruchirappalli', false)
ON CONFLICT (id) DO NOTHING;


-- ── Properties ────────────────────────────────────────────────────────────────
-- Note: owner_id left NULL so seed runs without a real auth user.
--       Set owner_id to a real UUID after user registration.
--       agent_name / agent_phone / agent_photo require migration 008.

INSERT INTO properties (
  listed_by, title, description, price, price_frequency,
  property_type, listing_type,
  bedrooms, bathrooms, area_sqft,
  district, neighborhood, city, address,
  latitude, longitude,
  images, amenities, furnishing,
  is_verified, is_featured, status, approval_status,
  agent_name, agent_phone, agent_photo,
  reference_id
)
VALUES

-- ── Chennai ──────────────────────────────────────────────────────────────────

(
  'agent',
  '3 BHK Apartment for Rent — Anna Nagar',
  'Well-ventilated 3 BHK on 4th floor with city view. Premium flooring, modular kitchen, 2 covered parking. 5 mins to Anna Nagar East metro. Society: gym, pool, 24/7 security.',
  18000, 'monthly',
  'apartment', 'rent',
  3, 2, 1450,
  'Chennai', 'Anna Nagar', 'Chennai', 'Block 7, 4th Avenue, Anna Nagar West, Chennai 600040',
  13.0878, 80.2107,
  ARRAY['https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800',
        'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800'],
  ARRAY['COVERED_PARKING','SHARED_GYM','SHARED_POOL','SECURITY','BUILTIN_WARDROBES'],
  'semi',
  true, true, 'active', 'approved',
  'Rajesh Kumar', '+91 98401 23456',
  'https://randomuser.me/api/portraits/men/32.jpg',
  'NX-TN-00001'
),
(
  'agent',
  '2 BHK Flat for Rent — T Nagar',
  'Spacious 2 BHK flat in T Nagar near Pondy Bazaar. Well connected to Mambalam railway station. Gated community with 24/7 security.',
  14000, 'monthly',
  'apartment', 'rent',
  2, 2, 1100,
  'Chennai', 'T Nagar', 'Chennai', '12, Thyagaraya Road, T Nagar, Chennai 600017',
  13.0418, 80.2341,
  ARRAY['https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800'],
  ARRAY['SECURITY','COVERED_PARKING','BALCONY','BUILTIN_WARDROBES'],
  'unfurnished',
  true, false, 'active', 'approved',
  'Suresh Anand', '+91 98412 34567',
  'https://randomuser.me/api/portraits/men/54.jpg',
  'NX-TN-00002'
),
(
  'builder',
  '3 BHK Luxury Apartment for Sale — Velachery',
  'RERA-registered luxury project in Velachery. 3 BHK with modular kitchen, vitrified flooring, balcony and clubhouse access. Possession: Dec 2026.',
  8500000, 'yearly',
  'apartment', 'sale',
  3, 2, 1580,
  'Chennai', 'Velachery', 'Chennai', 'Greenfield Residences, Velachery Main Rd, Chennai 600042',
  12.9785, 80.2209,
  ARRAY['https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800',
        'https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800'],
  ARRAY['COVERED_PARKING','SHARED_GYM','SHARED_POOL','SECURITY','BUILTIN_WARDROBES','BALCONY','CONCIERGE_SERVICE'],
  'unfurnished',
  true, true, 'active', 'approved',
  'Priya Builders Pvt Ltd', '+91 44 4521 6789',
  'https://randomuser.me/api/portraits/women/44.jpg',
  'NX-TN-00003'
),
(
  'agent',
  'Commercial Shop for Rent — T Nagar Main Road',
  'Prime ground-floor shop on T Nagar main road. High footfall. Suitable for retail, restaurant, or showroom. 3-vehicle dedicated parking.',
  75000, 'monthly',
  'shop', 'rent',
  0, 1, 850,
  'Chennai', 'T Nagar', 'Chennai', 'Ground Floor, 45 Usman Road, T Nagar, Chennai 600017',
  13.0395, 80.2374,
  ARRAY['https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=800'],
  ARRAY['SECURITY','COVERED_PARKING'],
  'unfurnished',
  true, false, 'active', 'approved',
  'Vijayan Properties', '+91 98403 45678',
  'https://randomuser.me/api/portraits/men/21.jpg',
  'NX-TN-00004'
),

-- ── Coimbatore ───────────────────────────────────────────────────────────────

(
  'builder',
  '2 BHK Independent House for Sale — RS Puram',
  'Ground-floor independent house in prime RS Puram area. Newly built, ready to occupy. Near KMCH hospital, reputed schools and shopping. DTCP approved.',
  6500000, 'yearly',
  'villa', 'sale',
  2, 2, 1100,
  'Coimbatore', 'RS Puram', 'Coimbatore', '14, 3rd Street, RS Puram, Coimbatore 641002',
  11.0115, 76.9545,
  ARRAY['https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800',
        'https://images.unsplash.com/photo-1588880331179-bc9b93a8cb5e?w=800'],
  ARRAY['PRIVATE_GARDEN','COVERED_PARKING','SECURITY'],
  'unfurnished',
  true, false, 'active', 'approved',
  'Priya Builders', '+91 99420 56789',
  'https://randomuser.me/api/portraits/women/44.jpg',
  'NX-TN-00005'
),
(
  'agent',
  '3 BHK Apartment for Rent — Saibaba Colony',
  'Fully furnished 3 BHK in gated community. AC in all rooms, modular kitchen, covered parking. Walking distance to PSG College and major hospitals.',
  22000, 'monthly',
  'apartment', 'rent',
  3, 2, 1350,
  'Coimbatore', 'Saibaba Colony', 'Coimbatore', 'Block C, Sri Sakthi Nagar, Saibaba Colony, Coimbatore 641011',
  11.0139, 76.9597,
  ARRAY['https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800'],
  ARRAY['COVERED_PARKING','SECURITY','BUILTIN_WARDROBES','CENTRAL_AC_HEATING','BALCONY'],
  'furnished',
  false, false, 'active', 'approved',
  'Murugan Real Estate', '+91 99422 12345',
  'https://randomuser.me/api/portraits/men/62.jpg',
  'NX-TN-00006'
),

-- ── Karur — Multiple areas ───────────────────────────────────────────────────

(
  'agent',
  '2 BHK Apartment for Rent — Karur Town',
  'Neat 2 BHK in central Karur town. Ground floor. Close to Karur bus stand, railway station and textile market. Ideal for families and working professionals.',
  7500, 'monthly',
  'apartment', 'rent',
  2, 1, 850,
  'Karur', 'Karur Town', 'Karur', '3rd Cross Street, Karur Town, Karur 639001',
  10.9601, 78.0766,
  ARRAY['https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800'],
  ARRAY['SECURITY','ELECTRICITY_BACKUP'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Karur Homes', '+91 4324 221100',
  'https://randomuser.me/api/portraits/men/33.jpg',
  'NX-TN-00007'
),
(
  'agent',
  '3 BHK Flat for Sale — Thanthoni, Karur',
  'Spacious 3 BHK in newly constructed building at Thanthoni. DTCP approved. UDS included. Great connectivity to NH544 (Coimbatore–Trichy highway). Ready to occupy.',
  3800000, 'yearly',
  'apartment', 'sale',
  3, 2, 1200,
  'Karur', 'Thanthoni', 'Karur', 'Sathya Residency, Thanthoni Main Road, Karur 639005',
  10.9472, 78.0631,
  ARRAY['https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800',
        'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800'],
  ARRAY['COVERED_PARKING','SECURITY','LIFT','BUILTIN_WARDROBES','ELECTRICITY_BACKUP'],
  'unfurnished',
  false, true, 'active', 'approved',
  'Balan Properties, Karur', '+91 4324 235678',
  'https://randomuser.me/api/portraits/men/40.jpg',
  'NX-TN-00008'
),
(
  'agent',
  'Commercial Shop for Rent — Old Bus Stand, Karur',
  'Ground-floor commercial space near Karur old bus stand. High-traffic area. Suitable for textile showroom, pharmacy, or grocery. Immediate possession.',
  12000, 'monthly',
  'shop', 'rent',
  0, 1, 450,
  'Karur', 'Old Bus Stand', 'Karur', 'Main Bazaar, Near Old Bus Stand, Karur 639001',
  10.9618, 78.0742,
  ARRAY['https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=800'],
  ARRAY['SECURITY'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Selvi Commercial', '+91 4324 241000',
  'https://randomuser.me/api/portraits/women/29.jpg',
  'NX-TN-00009'
),
(
  'agent',
  '1 BHK Apartment for Rent — Kattipalayam, Karur',
  'Budget-friendly 1 BHK near Kattipalayam junction. Suitable for bachelors and small families. Water tank, bore well, power backup. Near Karur bus stand (2 km).',
  5500, 'monthly',
  'apartment', 'rent',
  1, 1, 580,
  'Karur', 'Kattipalayam', 'Karur', 'Vijaya Street, Kattipalayam, Karur 639006',
  10.9559, 78.1001,
  ARRAY['https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800'],
  ARRAY['SECURITY','ELECTRICITY_BACKUP'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Ravi Homes Karur', '+91 4324 261234',
  'https://randomuser.me/api/portraits/men/71.jpg',
  'NX-TN-00010'
),
(
  'builder',
  '30 Cents DTCP Plot for Sale — Kovai Road, Karur',
  'Prime residential plot on Kovai Road (NH544) near Karur bypass. DTCP approved, clear title, water and EB available. Suitable for villa, apartments or commercial development. Excellent connectivity to Coimbatore and Trichy.',
  4200000, 'yearly',
  'land', 'sale',
  0, 0, 13068,
  'Karur', 'Kovai Road', 'Karur', 'Survey No 56/3, Kovai Road, Near Bypass, Karur 639002',
  10.9724, 78.0344,
  ARRAY['https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800'],
  ARRAY[]::TEXT[],
  'unfurnished',
  false, false, 'active', 'approved',
  'Karthik Lands Karur', '+91 4324 252000',
  'https://randomuser.me/api/portraits/men/88.jpg',
  'NX-TN-00011'
),
(
  'agent',
  '2 BHK House for Rent — Aravakurichi, Karur District',
  'Independent 2 BHK house in Aravakurichi town. Separate ground floor with garden. Near government hospital and market. Municipality water supply.',
  5000, 'monthly',
  'villa', 'rent',
  2, 1, 900,
  'Karur', 'Aravakurichi', 'Karur', '7, Gandhi Street, Aravakurichi, Karur District 639101',
  10.9858, 78.2188,
  ARRAY['https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800'],
  ARRAY['PRIVATE_GARDEN','SECURITY'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Anbu Real Estate', '+91 9944 112233',
  'https://randomuser.me/api/portraits/men/45.jpg',
  'NX-TN-00012'
),
(
  'agent',
  'Warehouse for Rent — SIDCO Industrial Area, Karur',
  'Industrial warehouse near SIDCO estate, Karur. Ideal for textile storage, yarn or manufacturing. Ground-floor with loading dock.',
  35000, 'monthly',
  'warehouse', 'rent',
  0, 2, 8000,
  'Karur', 'SIDCO Industrial Area', 'Karur', 'Plot No 14, SIDCO Industrial Estate, Karur 639006',
  10.9511, 78.1133,
  ARRAY['https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=800'],
  ARRAY['COVERED_PARKING','SECURITY','ELECTRICITY_BACKUP'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Industrial Props Karur', '+91 4324 271500',
  'https://randomuser.me/api/portraits/men/57.jpg',
  'NX-TN-00013'
),
(
  'agent',
  '2 BHK Apartment for Sale — Vanjipalayam, Karur',
  'New 2 BHK near Vanjipalayam junction. Close to top schools and hospital. RERA registered. Bank loan approved. Handover ready.',
  2800000, 'yearly',
  'apartment', 'sale',
  2, 2, 980,
  'Karur', 'Vanjipalayam', 'Karur', 'GreenView Apartments, Vanjipalayam, Karur 639003',
  10.9663, 78.0882,
  ARRAY['https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800'],
  ARRAY['COVERED_PARKING','LIFT','SECURITY','ELECTRICITY_BACKUP'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Karur Property Hub', '+91 4324 221234',
  'https://randomuser.me/api/portraits/women/38.jpg',
  'NX-TN-00014'
),
(
  'agent',
  '3 BHK New Villa for Sale — Krishnarayapuram, Karur',
  'Independent villa on 5 cents in Krishnarayapuram block, Karur district. 3 BHK, car parking, compound wall, borewell. DTCP approved. Village panchayat area, low property tax.',
  2500000, 'yearly',
  'villa', 'sale',
  3, 2, 1400,
  'Karur', 'Krishnarayapuram', 'Karur', 'Survey No 88, Krishnarayapuram Rd, Karur District 639120',
  10.9903, 78.2756,
  ARRAY['https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800'],
  ARRAY['PRIVATE_GARDEN','COVERED_PARKING','SECURITY'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Karur Rural Properties', '+91 4324 281100',
  'https://randomuser.me/api/portraits/men/66.jpg',
  'NX-TN-00015'
),

-- ── Madurai ──────────────────────────────────────────────────────────────────

(
  'agent',
  '1 BHK Furnished Flat — Madurai City Centre',
  'Fully furnished 1 BHK bachelor flat at Meenakshi Nagar. AC, TV, fridge, washing machine. Walking distance to bus stand and railway station.',
  8500, 'monthly',
  'apartment', 'rent',
  1, 1, 550,
  'Madurai', 'City Centre', 'Madurai', 'Block C, 2nd Floor, Meenakshi Nagar, Madurai 625001',
  9.9252, 78.1198,
  ARRAY['https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800'],
  ARRAY['SECURITY','BUILTIN_WARDROBES','ELECTRICITY_BACKUP'],
  'furnished',
  false, false, 'active', 'approved',
  'Murugan Selva', '+91 99440 23456',
  'https://randomuser.me/api/portraits/men/71.jpg',
  'NX-TN-00016'
),
(
  'builder',
  '3 BHK Villa for Sale — Anna Nagar Madurai',
  'Premium villa in gated community. 3 BHK, 2400 sqft. Individual villa with private garden, 2 car parking. Near Madurai airport and Aravind Eye Hospital. DTCP + RERA approved.',
  9500000, 'yearly',
  'villa', 'sale',
  3, 3, 2400,
  'Madurai', 'Anna Nagar', 'Madurai', 'Palm Valley Villas, Anna Nagar, Madurai 625020',
  9.9349, 78.1003,
  ARRAY['https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=800',
        'https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800'],
  ARRAY['PRIVATE_GARDEN','COVERED_PARKING','SECURITY','SHARED_GYM','SHARED_POOL'],
  'unfurnished',
  true, true, 'active', 'approved',
  'Madurai Builders', '+91 99441 67890',
  'https://randomuser.me/api/portraits/men/85.jpg',
  'NX-TN-00017'
),

-- ── Thanjavur ──────────────────────────────────────────────────────────────

(
  'agent',
  '30 Cents Land for Sale — Thanjavur NH67 Bypass',
  'DTCP-approved land along Thanjavur–Kumbakonam highway. Clear title. Water and EB available on-site. Suitable for residential plots, farm house, or commercial development.',
  12000000, 'yearly',
  'land', 'sale',
  0, 0, 13068,
  'Thanjavur', 'NH67 Bypass', 'Thanjavur', 'Survey No 45/2, Thanjavur-Kumbakonam Highway, Thanjavur 613001',
  10.7869, 79.1378,
  ARRAY['https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800'],
  ARRAY[]::TEXT[],
  'unfurnished',
  false, false, 'active', 'approved',
  'Balamurugan TN', '+91 98410 65432',
  'https://randomuser.me/api/portraits/men/88.jpg',
  'NX-TN-00018'
),

-- ── Erode ──────────────────────────────────────────────────────────────────

(
  'builder',
  '2 BHK New Apartment for Sale — Perundurai Road, Erode',
  'Brand new 2 BHK near Erode bus stand. Ready to occupy. UDS included. RERA registered. Loan facilities available.',
  3200000, 'yearly',
  'apartment', 'sale',
  2, 2, 980,
  'Erode', 'Perundurai Road', 'Erode', 'Green Valley Apartments, Perundurai Road, Erode 638011',
  11.3427, 77.7272,
  ARRAY['https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800'],
  ARRAY['LIFT','COVERED_PARKING','SECURITY','ELECTRICITY_BACKUP'],
  'unfurnished',
  true, false, 'active', 'approved',
  'Erode Constructions Pvt Ltd', '+91 96550 34567',
  'https://randomuser.me/api/portraits/men/45.jpg',
  'NX-TN-00019'
),

-- ── Tiruchirappalli ────────────────────────────────────────────────────────

(
  'agent',
  '2 BHK Apartment for Rent — Srirangam, Trichy',
  'Neat 2 BHK near Srirangam temple. Ground floor. Vastu compliant. Close to BHEL and National College. Metro water supply.',
  9000, 'monthly',
  'apartment', 'rent',
  2, 1, 950,
  'Tiruchirappalli', 'Srirangam', 'Tiruchirappalli', '5, Teppakulam Street, Srirangam, Trichy 620006',
  10.8566, 78.6937,
  ARRAY['https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800'],
  ARRAY['SECURITY','ELECTRICITY_BACKUP','COVERED_PARKING'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Trichy Properties', '+91 431 2321001',
  'https://randomuser.me/api/portraits/men/22.jpg',
  'NX-TN-00020'
),

-- ── Tiruppur ────────────────────────────────────────────────────────────────

(
  'agent',
  '3 BHK New Flat for Rent — Avinashi Road, Tiruppur',
  'Brand new 3 BHK on Avinashi Road. Close to textile export units and hospitals. Covered parking, gym, security.',
  14000, 'monthly',
  'apartment', 'rent',
  3, 2, 1200,
  'Tiruppur', 'Avinashi Road', 'Tiruppur', 'Sunshine Apartments, Avinashi Road, Tiruppur 641604',
  11.1085, 77.3411,
  ARRAY['https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?w=800'],
  ARRAY['COVERED_PARKING','SHARED_GYM','SECURITY'],
  'unfurnished',
  false, false, 'active', 'approved',
  'Vijay Agent Tiruppur', '+91 97890 12345',
  'https://randomuser.me/api/portraits/men/15.jpg',
  'NX-TN-00021'
),

-- ── Salem ────────────────────────────────────────────────────────────────────

(
  'agent',
  '2 BHK Apartment for Rent — Fairlands, Salem',
  'Semi-furnished 2 BHK in Salem Fairlands near Vinayaka Mission University. AC in master bedroom. Covered parking.',
  11000, 'monthly',
  'apartment', 'rent',
  2, 2, 1000,
  'Salem', 'Fairlands', 'Salem', 'No 8, Rajaji Road, Fairlands, Salem 636016',
  11.6792, 78.1486,
  ARRAY['https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800'],
  ARRAY['COVERED_PARKING','SECURITY','BALCONY','CENTRAL_AC_HEATING'],
  'semi',
  false, false, 'active', 'approved',
  'Salem Homes', '+91 427 2342000',
  'https://randomuser.me/api/portraits/men/34.jpg',
  'NX-TN-00022'
),

-- ── Vellore ──────────────────────────────────────────────────────────────────

(
  'agent',
  '1 BHK Furnished Room — Sathuvachari, Vellore',
  'Furnished 1 BHK near CMC Vellore and VIT University. AC, attached bath, daily housekeeping. Wifi included. Suitable for students and doctors.',
  6000, 'monthly',
  'apartment', 'rent',
  1, 1, 400,
  'Vellore', 'Sathuvachari', 'Vellore', '3B Rathna Complex, Sathuvachari, Vellore 632009',
  12.9278, 79.1428,
  ARRAY['https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800'],
  ARRAY['SECURITY','ELECTRICITY_BACKUP','CONCIERGE_SERVICE'],
  'furnished',
  false, false, 'active', 'approved',
  'VIT Area Homes', '+91 416 2254321',
  'https://randomuser.me/api/portraits/women/31.jpg',
  'NX-TN-00023'
)

ON CONFLICT (reference_id) DO NOTHING;
