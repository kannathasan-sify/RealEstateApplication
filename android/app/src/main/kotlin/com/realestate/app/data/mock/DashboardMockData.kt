package com.realestate.app.data.mock

import com.realestate.app.data.models.AdminDashboardData
import com.realestate.app.data.models.AgentDashboardData
import com.realestate.app.data.models.BarChartData
import com.realestate.app.data.models.BarDatum
import com.realestate.app.data.models.ChartColors
import com.realestate.app.data.models.DashTable
import com.realestate.app.data.models.DeltaType
import com.realestate.app.data.models.KpiTile
import com.realestate.app.data.models.LineChartData
import com.realestate.app.data.models.LineSeries
import com.realestate.app.data.models.OwnerDashboardData
import com.realestate.app.data.models.PartnerDashboardData

/**
 * Sample data for the role dashboards, used in [com.realestate.app.BuildConfig.USE_MOCK_DATA]
 * mode. Values mirror the DNESTX dashboards design reference 1:1 so the built screens match
 * the mockup. Kept in its own object (not [MockData]) purely to avoid editing that 1300-line
 * file — the dashboard ViewModels read from here.
 */
object DashboardMockData {

    // ── Owner: Rajesh Kumar, 6 active listings in Chennai & Bangalore ────────────
    val owner = OwnerDashboardData(
        tiles = listOf(
            KpiTile("My Properties", "6", "1 new", DeltaType.NEUTRAL, "this month"),
            KpiTile("Total Views", "3,412", "+18%", DeltaType.GOOD, "vs last 30 days"),
            KpiTile("Leads", "27", "+9%", DeltaType.GOOD, "vs last 30 days"),
            KpiTile("Saved by Users", "112", "+4", DeltaType.NEUTRAL, "vs last 30 days"),
        ),
        viewsByProperty = BarChartData(
            bars = listOf(
                BarDatum("ECR Villa", 842.0),
                BarDatum("OMR 2BHK", 610.0),
                BarDatum("Adyar Flat", 505.0),
                BarDatum("Whitefield Plot", 388.0),
                BarDatum("T Nagar Shop", 340.0),
                BarDatum("Anna Nagar 3BHK", 727.0),
            ),
            valueLabel = "Views",
        ),
        viewsTrend = LineChartData(
            xLabels = listOf("D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10", "D11", "D12", "D13", "D14"),
            series = listOf(
                LineSeries("Views", ChartColors.BLUE,
                    listOf(180.0, 190.0, 175.0, 210.0, 230.0, 220.0, 245.0, 260.0, 255.0, 280.0, 300.0, 290.0, 320.0, 340.0)),
            ),
        ),
        properties = DashTable(
            headers = listOf("Property", "Type", "Status", "Views", "Leads"),
            rows = listOf(
                listOf("ECR Villa", "Villa", "Live", "842", "9"),
                listOf("OMR 2BHK", "Apartment", "Live", "610", "7"),
                listOf("Adyar Flat", "Apartment", "Pending", "505", "3"),
                listOf("Whitefield Plot", "Agricultural Land", "Live", "388", "4"),
                listOf("T Nagar Shop", "Shop", "Rejected", "340", "1"),
                listOf("Anna Nagar 3BHK", "Apartment", "Live", "727", "3"),
            ),
            statusColumnIndex = 2,
        ),
    )

    // ── Agent: Priya Sharma ──────────────────────────────────────────────────────
    val agent = AgentDashboardData(
        tiles = listOf(
            KpiTile("Active Listings", "34", "+3", DeltaType.GOOD, "this month"),
            KpiTile("Leads in Pipeline", "58", "+11", DeltaType.GOOD, "this month"),
            KpiTile("Site Visits Booked", "12", "2 today", DeltaType.NEUTRAL, ""),
            KpiTile("Commission MTD", "₹1.85L", "+22%", DeltaType.GOOD, "vs last month"),
        ),
        leadPipeline = BarChartData(
            bars = listOf(
                BarDatum("New", 24.0, ChartColors.ORD_250),
                BarDatum("Contacted", 16.0, ChartColors.ORD_350),
                BarDatum("Site Visit", 9.0, ChartColors.ORD_450),
                BarDatum("Negotiation", 6.0, ChartColors.ORD_550),
                BarDatum("Closed", 3.0, ChartColors.ORD_600),
            ),
            valueLabel = "Leads",
        ),
        commissionEarned = LineChartData(
            xLabels = listOf("Feb", "Mar", "Apr", "May", "Jun", "Jul"),
            series = listOf(
                LineSeries("Commission (₹L)", ChartColors.BLUE,
                    listOf(0.9, 1.1, 1.3, 1.5, 1.52, 1.85)),
            ),
        ),
        leadInbox = DashTable(
            headers = listOf("Lead", "Property", "Stage", "Last Contact", "Assigned"),
            rows = listOf(
                listOf("S. Venkatesh", "ECR Villa", "Site Visit", "2 days ago", "Priya Sharma"),
                listOf("Farhan Sheikh", "OMR 2BHK", "Contacted", "Today", "Priya Sharma"),
                listOf("Meena R.", "Anna Nagar 3BHK", "Negotiation", "Yesterday", "Priya Sharma"),
                listOf("Arjun Dev", "Whitefield Plot", "New", "1 hour ago", "Priya Sharma"),
                listOf("K. Lakshmi", "T Nagar Shop", "Closed", "5 days ago", "Priya Sharma"),
            ),
            statusColumnIndex = 2,
        ),
    )

    // ── Channel Partner ──────────────────────────────────────────────────────────
    val partner = PartnerDashboardData(
        tiles = listOf(
            KpiTile("Referrals Sent", "41", "+6", DeltaType.GOOD, "this month"),
            KpiTile("Conversions", "9", "+2", DeltaType.GOOD, "this month"),
            KpiTile("Conversion Rate", "22%", "+3pt", DeltaType.GOOD, "vs last month"),
            KpiTile("Commission Earned", "₹3.4L", "+15%", DeltaType.GOOD, "vs last month"),
        ),
        referralFunnel = BarChartData(
            bars = listOf(
                BarDatum("Sent", 41.0, ChartColors.ORD_250),
                BarDatum("Contacted", 28.0, ChartColors.ORD_350),
                BarDatum("Site Visit", 15.0, ChartColors.ORD_450),
                BarDatum("Converted", 9.0, ChartColors.ORD_600),
            ),
            valueLabel = "Referrals",
        ),
        payoutTrend = LineChartData(
            xLabels = listOf("Feb", "Mar", "Apr", "May", "Jun", "Jul"),
            series = listOf(
                LineSeries("Commission (₹L)", ChartColors.BLUE,
                    listOf(1.6, 2.0, 2.3, 2.6, 2.9, 3.4)),
            ),
        ),
        referralPipeline = DashTable(
            headers = listOf("Referral", "Stage", "Property Value", "Commission"),
            rows = listOf(
                listOf("Nikhil Rao", "Site Visit", "₹1.2Cr", "₹1.2L (pending)"),
                listOf("Ayesha Khan", "Converted", "₹68L", "₹68,000 (paid)"),
                listOf("Deepak Iyer", "Contacted", "—", "—"),
                listOf("Sowmya P.", "Converted", "₹95L", "₹95,000 (paid)"),
                listOf("Ramesh Babu", "Lost", "₹40L", "—"),
            ),
            statusColumnIndex = 1,
        ),
    )

    // ── Admin (platform-wide) ────────────────────────────────────────────────────
    val admin = AdminDashboardData(
        tiles = listOf(
            KpiTile("Active Users", "8,240", "+6%", DeltaType.GOOD, "vs last month"),
            KpiTile("Pending Approvals", "18", "+4", DeltaType.CRITICAL, "needs review"),
            KpiTile("Revenue MTD", "₹42.6L", "+12%", DeltaType.GOOD, "vs last month"),
            KpiTile("Fraud Alerts", "3", "open", DeltaType.CRITICAL, "unresolved"),
        ),
        revenueByStream = BarChartData(
            bars = listOf(
                BarDatum("Premium List.", 12.4, ChartColors.series[0]),
                BarDatum("Featured Proj.", 8.1, ChartColors.series[1]),
                BarDatum("Owner Sub", 6.7, ChartColors.series[2]),
                BarDatum("Agent Sub", 5.9, ChartColors.series[3]),
                BarDatum("Ads", 4.2, ChartColors.series[4]),
                BarDatum("Mortgage Ref.", 3.1, ChartColors.series[5]),
                BarDatum("Insurance Ref.", 1.6, ChartColors.series[6]),
                BarDatum("Other Svcs", 0.6, ChartColors.series[7]),
            ),
            valueLabel = "Revenue (₹L)",
            valueSuffix = "L",
        ),
        userGrowth = LineChartData(
            xLabels = listOf("Feb", "Mar", "Apr", "May", "Jun", "Jul"),
            series = listOf(
                LineSeries("New Signups", ChartColors.BLUE,
                    listOf(520.0, 610.0, 700.0, 760.0, 810.0, 905.0)),
                LineSeries("Verified Agents", ChartColors.GREEN,
                    listOf(40.0, 52.0, 61.0, 70.0, 78.0, 88.0)),
            ),
        ),
        approvalQueue = DashTable(
            headers = listOf("Item", "Type", "Submitted By", "Status"),
            rows = listOf(
                listOf("ECR Beachfront Villa", "Property", "Rajesh Kumar", "Pending"),
                listOf("Skyline Towers Project", "Builder", "Skyline Developers", "In Review"),
                listOf("Agent KYC — R. Nair", "Agent Verification", "R. Nair", "Pending"),
                listOf("Whitefield Plot", "Property", "Rajesh Kumar", "Approved"),
                listOf("T Nagar Shop Listing", "Property", "Rajesh Kumar", "Rejected"),
            ),
            statusColumnIndex = 3,
        ),
        fraudAlerts = DashTable(
            headers = listOf("Alert", "Details", "Severity"),
            rows = listOf(
                listOf("Duplicate listing detected", "Same photos across 2 owner accounts", "Flagged"),
                listOf("Price anomaly", "Listing 40% below locality average", "Pending"),
                listOf("Suspicious payout request", "Partner commission claim mismatch", "Flagged"),
            ),
            statusColumnIndex = 2,
        ),
    )
}
