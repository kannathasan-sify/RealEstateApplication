package com.realestate.app.data.models

object TamilNaduData {

    /** All 38 Tamil Nadu districts (sorted alphabetically after top cities) */
    val districts = listOf(
        "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem",
        "Tirunelveli", "Tiruppur", "Erode", "Vellore", "Thanjavur",
        "Ariyalur", "Chengalpattu", "Cuddalore", "Dindigul",
        "Kallakurichi", "Kancheepuram", "Kanniyakumari", "Karur",
        "Krishnagiri", "Mayiladuthurai", "Nagapattinam", "Namakkal",
        "Nilgiris", "Perambalur", "Pudukkottai", "Ramanathapuram",
        "Ranipet", "Sivaganga", "Sivagangai", "Tenkasi", "Theni",
        "Thoothukudi", "Tirivannamalai", "Tirupattur", "Tiruvarur",
        "Villupuram", "Virudhunagar", "Viridhunagar"
    )

    // ── District → Localities / Areas ─────────────────────────────────────────
    /**
     * Maps each Tamil Nadu district to its well-known localities/areas.
     * Used to populate the area dropdown in PropertyFilterScreen when a
     * district is selected (chained filter: District → Area/Neighborhood).
     *
     * The list for each district is sorted: most popular areas first.
     */
    val districtAreas: Map<String, List<String>> = mapOf(

        "Chennai" to listOf(
            "Anna Nagar", "T Nagar", "Velachery", "Adyar", "Porur",
            "Tambaram", "Perambur", "Mylapore", "Nungambakkam", "Kodambakkam",
            "Vadapalani", "Perungudi", "Sholinganallur", "Pallikaranai",
            "OMR (Old Mahabalipuram Road)", "ECR (East Coast Road)", "Chromepet",
            "Guindy", "Avadi", "Ambattur", "Tondiarpet"
        ),

        "Coimbatore" to listOf(
            "RS Puram", "Saibaba Colony", "Gandhipuram", "Peelamedu",
            "Singanallur", "Ukkadam", "Hopes College Road", "Kovaipudur",
            "Vadavalli", "Thudiyalur", "Sowripalayam", "Neelambur",
            "Avinashi Road", "Trichy Road", "Kalapatti", "Saravanampatty"
        ),

        "Madurai" to listOf(
            "Anna Nagar", "KK Nagar", "Narimedu", "Tallakulam",
            "Thirunagar", "Palanganatham", "Vilangudi", "Alagar Kovil Road",
            "Bypass Road", "Gomathipuram", "Iyer Bungalow", "Kochadai",
            "Mattuthavani", "Simmakkal", "Sellur"
        ),

        "Tiruchirappalli" to listOf(
            "Srirangam", "Woraiyur", "Ariyamangalam", "Thillai Nagar",
            "K K Nagar", "Palpannai", "Tennur", "Manachanallur",
            "Kattur", "Kailasapuram", "Bharathidasan University Road",
            "Melur Road", "Ponmalai"
        ),

        "Salem" to listOf(
            "Fairlands", "Shevapet", "Suramangalam", "Hasthampatti",
            "Kitchipalayam", "Kondalampatti", "Ammapet", "Attur Road",
            "Yercaud Road", "Omalur", "Rasipuram", "Mettur Road"
        ),

        "Tirunelveli" to listOf(
            "Palayamkottai", "Melapalayam", "Tirunelveli Town",
            "Pettai", "Ramayanpatti", "Vannarpettai", "Kurukalpatti",
            "Ambasamudram", "Sankarankovil", "Tenkasi Road"
        ),

        "Tiruppur" to listOf(
            "Avinashi Road", "Kangeyam Road", "Rayapuram",
            "Veerapandi", "Palladam Road", "Anaithapallam",
            "Tiruppur North", "Tiruppur South", "Mangalam",
            "Karamadai", "Dharapuram"
        ),

        "Erode" to listOf(
            "Perundurai Road", "Bhavani", "Erode Central", "Chithode",
            "Thindal", "Surampatti", "Bannariamman Kovil Road",
            "Gobichettipalayam", "Sathyamangalam", "Kavundapadi"
        ),

        "Vellore" to listOf(
            "Sathuvachari", "Katpadi", "Vellore Town", "Bagayam",
            "Gudiyatham", "Ranipet Road", "CMC Area",
            "VIT University Road", "Kaniyambadi", "Adukkamparai"
        ),

        "Thanjavur" to listOf(
            "Kumbakonam", "Papanasam", "Thanjavur Town", "Pattukottai",
            "Orathanadu", "Budalur", "Vallam", "NH67 Bypass",
            "Medical College Road", "Thiruvaiyaru"
        ),

        // ── Karur — detailed area list (textile hub district) ──────────────
        "Karur" to listOf(
            "Karur Town",
            "Thanthoni",
            "Kattipalayam",
            "Kovai Road",
            "Aravakurichi",
            "Old Bus Stand",
            "Vanjipalayam",
            "SIDCO Industrial Area",
            "Krishnarayapuram",
            "Kulithalai",
            "Karur Bypass Road",
            "Karur Junction",
            "Punnamchatram",
            "Pugalur",
            "Manmangalam"
        ),

        "Kancheepuram" to listOf(
            "Kancheepuram Town", "Sriperumbudur", "Walajabad",
            "Uthiramerur", "Madurantakam", "Kundrathur"
        ),

        "Chengalpattu" to listOf(
            "Chengalpattu Town", "Maraimalai Nagar", "GST Road",
            "Urapakkam", "Vandalur", "Perungalathur", "Tambaram"
        ),

        "Kanniyakumari" to listOf(
            "Nagercoil", "Marthandam", "Thuckalay", "Colachel",
            "Padmanabhapuram", "Kuzhithurai"
        ),

        "Dindigul" to listOf(
            "Dindigul Town", "Palani", "Oddanchatram", "Natham",
            "Batlagundu", "Kodaikanal Road"
        ),

        "Namakkal" to listOf(
            "Namakkal Town", "Rasipuram", "Tiruchengode", "Paramathi",
            "Sankagiri", "Kollidam"
        ),

        "Krishnagiri" to listOf(
            "Krishnagiri Town", "Hosur", "Pochampalli", "Bargur",
            "Denkanikottai", "Sun City"
        ),

        "Nilgiris" to listOf(
            "Ooty", "Coonoor", "Kotagiri", "Gudalur",
            "Kundah", "Nanjanadu"
        ),

        "Thoothukudi" to listOf(
            "Thoothukudi Town", "Tiruchendur", "Kovilpatti", "Ottapidaram",
            "Vilathikulam", "Ettayapuram"
        ),

        "Ramanathapuram" to listOf(
            "Ramanathapuram Town", "Rameswaram", "Paramakudi",
            "Mandapam", "Kadaladi"
        ),

        "Pudukkottai" to listOf(
            "Pudukkottai Town", "Aranthangi", "Karaikudi",
            "Thirumayam", "Alangudi"
        ),

        "Sivagangai" to listOf(
            "Sivagangai Town", "Karaikudi", "Manamadurai",
            "Ilayangudi", "Devakottai"
        ),

        "Cuddalore" to listOf(
            "Cuddalore Town", "Chidambaram", "Panruti",
            "Virudhachalam", "Nellikuppam"
        ),

        "Nagapattinam" to listOf(
            "Nagapattinam Town", "Mayiladuthurai", "Sirkali",
            "Vedaranyam", "Kuthalam"
        ),

        "Villupuram" to listOf(
            "Villupuram Town", "Tindivanam", "Gingee",
            "Pondicherry Border Area", "Kallakurichi Road"
        ),

        "Tenkasi" to listOf(
            "Tenkasi Town", "Kadayanallur", "Alangulam",
            "Sankarankovil", "Puliyangudi"
        ),

        "Theni" to listOf(
            "Theni Town", "Periyakulam", "Bodinayakanur",
            "Uthamapalayam", "Andipatti"
        ),

        "Virudhunagar" to listOf(
            "Virudhunagar Town", "Sivakasi", "Rajapalayam",
            "Srivilliputhur", "Aruppukottai"
        )
    )

    /** Returns areas for the given district, or empty list if unknown */
    fun areasForDistrict(district: String): List<String> =
        districtAreas[district] ?: emptyList()

    // ── District Centre Coordinates (for map picker initial view) ─────────────

    /** Lat/Lng centre points for all 38 Tamil Nadu districts */
    val districtCoordinates: Map<String, Pair<Double, Double>> = mapOf(
        "Chennai"          to Pair(13.0827,  80.2707),
        "Coimbatore"       to Pair(11.0168,  76.9558),
        "Madurai"          to Pair(9.9252,   78.1198),
        "Tiruchirappalli"  to Pair(10.7905,  78.7047),
        "Salem"            to Pair(11.6643,  78.1460),
        "Tirunelveli"      to Pair(8.7139,   77.7567),
        "Tiruppur"         to Pair(11.1085,  77.3411),
        "Erode"            to Pair(11.3410,  77.7172),
        "Vellore"          to Pair(12.9165,  79.1325),
        "Thanjavur"        to Pair(10.7870,  79.1378),
        "Ariyalur"         to Pair(11.1400,  79.0787),
        "Chengalpattu"     to Pair(12.6921,  79.9763),
        "Cuddalore"        to Pair(11.7480,  79.7714),
        "Dindigul"         to Pair(10.3624,  77.9695),
        "Kallakurichi"     to Pair(11.7377,  78.9596),
        "Kancheepuram"     to Pair(12.8352,  79.7036),
        "Kanniyakumari"    to Pair(8.0883,   77.5385),
        "Karur"            to Pair(10.9601,  78.0766),
        "Krishnagiri"      to Pair(12.5266,  78.2149),
        "Mayiladuthurai"   to Pair(11.1035,  79.6519),
        "Nagapattinam"     to Pair(10.7672,  79.8420),
        "Namakkal"         to Pair(11.2189,  78.1670),
        "Nilgiris"         to Pair(11.4102,  76.6950),
        "Perambalur"       to Pair(11.2333,  78.8833),
        "Pudukkottai"      to Pair(10.3797,  78.8214),
        "Ramanathapuram"   to Pair(9.3639,   78.8395),
        "Ranipet"          to Pair(12.9228,  79.3325),
        "Sivaganga"        to Pair(9.8450,   78.4826),
        "Sivagangai"       to Pair(9.8450,   78.4826),
        "Tenkasi"          to Pair(8.9596,   77.3152),
        "Theni"            to Pair(10.0104,  77.4770),
        "Thoothukudi"      to Pair(8.7642,   78.1348),
        "Tirivannamalai"   to Pair(12.2253,  79.0747),
        "Tirupattur"       to Pair(12.4967,  78.5713),
        "Tiruvarur"        to Pair(10.7666,  79.6369),
        "Villupuram"       to Pair(11.9392,  79.4930),
        "Virudhunagar"     to Pair(9.5850,   77.9624),
        "Viridhunagar"     to Pair(9.5850,   77.9624),
    )

    /**
     * Returns the centre coordinates for [district], or the geographic centre
     * of Tamil Nadu (11.1271, 78.6569) if the district is not found.
     */
    fun coordinatesForDistrict(district: String): Pair<Double, Double> =
        districtCoordinates[district] ?: Pair(11.1271, 78.6569)

    // ── District Summary (for DistrictListScreen / HomeScreen) ───────────────

    data class DistrictSummary(
        val name: String,
        val propertyCount: Int,
        val imageUrl: String,
        val topAreas: List<String> = emptyList()
    )

    val featuredDistricts = listOf(
        DistrictSummary("Chennai", 128,
            "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?w=400",
            listOf("Anna Nagar", "T Nagar", "Velachery", "Porur", "ECR")),
        DistrictSummary("Coimbatore", 64,
            "https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=400",
            listOf("RS Puram", "Gandhipuram", "Saibaba Colony", "Peelamedu")),
        DistrictSummary("Madurai", 31,
            "https://images.unsplash.com/photo-1586763263060-72b5cd8add52?w=400",
            listOf("Anna Nagar", "KK Nagar", "Narimedu", "Tallakulam")),
        DistrictSummary("Tiruchirappalli", 22,
            "https://images.unsplash.com/photo-1600607688969-a5bfcd646154?w=400",
            listOf("Srirangam", "Woraiyur", "Ariyamangalam")),
        DistrictSummary("Salem", 18,
            "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=400",
            listOf("Fairlands", "Shevapet", "Suramangalam")),
        DistrictSummary("Karur", 12,
            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=400",
            listOf("Karur Town", "Thanthoni", "Kovai Road", "Kattipalayam", "SIDCO")),
        DistrictSummary("Tirunelveli", 14,
            "https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=400",
            listOf("Palayamkottai", "Melapalayam")),
        DistrictSummary("Tiruppur", 11,
            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=400",
            listOf("Avinashi Road", "Kangeyam Road")),
        DistrictSummary("Vellore", 9,
            "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=400",
            listOf("Katpadi", "Sathuvachari")),
        DistrictSummary("Erode", 7,
            "https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=400",
            listOf("Perundurai Road", "Bhavani")),
        DistrictSummary("Thanjavur", 6,
            "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=400",
            listOf("Kumbakonam", "Papanasam"))
    )
}
