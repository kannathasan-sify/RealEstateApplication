"""
routers/dashboard.py — /api/v1/dashboard

Role-based analytics dashboards. Each endpoint aggregates real data from Supabase for the
signed-in user and returns presentation-neutral payloads (values + deltas + semantic
delta_type); the Android client applies colours/layout.

GET /dashboard/owner    — any signed-in user, scoped to the properties they own
GET /dashboard/admin    — admin only            (Phase 2)
GET /dashboard/agent    — agent only            (Phase 3)
GET /dashboard/partner  — channel_partner only  (Phase 4)
"""

from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from app.middleware.auth_middleware import get_current_user, require_admin
from app.services.supabase_client import get_supabase_admin as get_supabase

router = APIRouter()


# ── Shared presentation-neutral DTOs ─────────────────────────────────────────

class KpiDto(BaseModel):
    label: str
    value: str
    delta: str = ""
    delta_type: str = "NEUTRAL"        # GOOD | NEUTRAL | CRITICAL
    delta_caption: str = ""


class NamedValueDto(BaseModel):
    label: str
    value: float


class TableDto(BaseModel):
    headers: List[str]
    rows: List[List[str]]
    status_column_index: Optional[int] = None


class SeriesDto(BaseModel):
    name: str
    values: List[float]


class LineChartDto(BaseModel):
    x_labels: List[str]
    series: List[SeriesDto]


class OwnerDashboardResponse(BaseModel):
    tiles: List[KpiDto]
    views_by_property: List[NamedValueDto]
    views_trend: List[NamedValueDto]
    properties: TableDto


class AdminDashboardResponse(BaseModel):
    tiles: List[KpiDto]
    revenue_by_stream: List[NamedValueDto]
    user_growth: LineChartDto
    approval_queue: TableDto
    fraud_alerts: TableDto


class AgentDashboardResponse(BaseModel):
    tiles: List[KpiDto]
    lead_pipeline: List[NamedValueDto]
    commission_earned: List[NamedValueDto]
    lead_inbox: TableDto


class PartnerDashboardResponse(BaseModel):
    tiles: List[KpiDto]
    referral_funnel: List[NamedValueDto]
    payout_trend: List[NamedValueDto]
    referral_pipeline: TableDto


# ── Helpers ──────────────────────────────────────────────────────────────────

def _pct_delta(current: int, previous: int) -> tuple[str, str]:
    """Return (label, delta_type) comparing current vs previous period."""
    if previous == 0:
        if current == 0:
            return "0%", "NEUTRAL"
        return "+100%", "GOOD"
    change = round((current - previous) / previous * 100)
    if change > 0:
        return f"+{change}%", "GOOD"
    if change < 0:
        return f"{change}%", "NEUTRAL"
    return "0%", "NEUTRAL"


def _status_label(approval_status: Optional[str], status: Optional[str]) -> str:
    if approval_status == "pending":
        return "Pending"
    if approval_status == "rejected":
        return "Rejected"
    if approval_status == "approved" and status == "active":
        return "Live"
    return (status or "inactive").capitalize()


def _pretty(value: Optional[str]) -> str:
    return (value or "").replace("_", " ").title()


def _resolve_uid(current_user: dict, user_id: Optional[str]) -> str:
    """
    Resolve whose data to return. Normally the caller's own id; but an admin may pass
    ?user_id=<other> to view that user's dashboard (agent-wise / builder-wise filtering on
    the admin console). Non-admins asking for someone else get 403.
    """
    uid = current_user["id"]
    if user_id and user_id != uid:
        if current_user.get("role") != "admin":
            raise HTTPException(status_code=403, detail="Only admins can view another user's dashboard")
        return user_id
    return uid


# ── GET /dashboard/owner ──────────────────────────────────────────────────────

@router.get("/owner", response_model=OwnerDashboardResponse, summary="Owner analytics")
async def owner_dashboard(
    user_id: Optional[str] = None,
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """
    Real analytics for the signed-in owner, scoped to properties they own:
    property count, total views, leads and saves, per-property view breakdown, a 14-day
    view trend and a table of their listings. Any authenticated user can call this — it
    simply reflects whatever listings they own (empty state if none). Admins may pass
    ?user_id= to view a specific owner/builder (builder-wise filter on the admin console).
    """
    uid = _resolve_uid(current_user, user_id)
    now = datetime.now(timezone.utc)
    window_30 = now - timedelta(days=30)
    window_60 = now - timedelta(days=60)

    # 1) Owner's listings
    props_res = (
        supabase.table("properties")
        .select("id, title, property_type, status, approval_status, created_at")
        .eq("owner_id", uid)
        .order("created_at", desc=True)
        .execute()
    )
    props = props_res.data or []
    prop_ids = [p["id"] for p in props]
    title_by_id = {p["id"]: (p.get("title") or "Untitled") for p in props}

    # 2) Views for those listings (event rows: property_id + created_at)
    view_rows: List[dict] = []
    if prop_ids:
        views_res = (
            supabase.table("property_views")
            .select("property_id, created_at")
            .in_("property_id", prop_ids)
            .execute()
        )
        view_rows = views_res.data or []

    # 3) Leads on those listings (owner-scoped)
    leads_res = (
        supabase.table("property_leads")
        .select("property_id, created_at")
        .eq("owner_id", uid)
        .execute()
    )
    lead_rows = leads_res.data or []

    # 4) Saves of those listings
    saved_rows: List[dict] = []
    if prop_ids:
        saved_res = (
            supabase.table("saved_properties")
            .select("property_id, created_at")
            .in_("property_id", prop_ids)
            .execute()
        )
        saved_rows = saved_res.data or []

    # ── Aggregate ─────────────────────────────────────────────────────────────
    def _in_window(rows, start, end=None):
        out = 0
        for r in rows:
            ts = _parse(r.get("created_at"))
            if ts is None:
                continue
            if ts >= start and (end is None or ts < end):
                out += 1
        return out

    views_by_prop = Counter(r["property_id"] for r in view_rows)
    leads_by_prop = Counter(r["property_id"] for r in lead_rows if r.get("property_id"))

    total_views = len(view_rows)
    total_leads = len(lead_rows)
    total_saved = len(saved_rows)

    views_30 = _in_window(view_rows, window_30)
    views_prev = _in_window(view_rows, window_60, window_30)
    leads_30 = _in_window(lead_rows, window_30)
    leads_prev = _in_window(lead_rows, window_60, window_30)
    saved_30 = _in_window(saved_rows, window_30)
    new_props_month = sum(
        1 for p in props if (_parse(p.get("created_at")) or now) >= now.replace(day=1)
    )

    views_delta, views_dt = _pct_delta(views_30, views_prev)
    leads_delta, leads_dt = _pct_delta(leads_30, leads_prev)

    tiles = [
        KpiDto(label="My Properties", value=str(len(props)),
               delta=f"{new_props_month} new", delta_type="NEUTRAL", delta_caption="this month"),
        KpiDto(label="Total Views", value=f"{total_views:,}",
               delta=views_delta, delta_type=views_dt, delta_caption="vs last 30 days"),
        KpiDto(label="Leads", value=str(total_leads),
               delta=leads_delta, delta_type=leads_dt, delta_caption="vs last 30 days"),
        KpiDto(label="Saved by Users", value=str(total_saved),
               delta=f"+{saved_30}" if saved_30 else "0", delta_type="NEUTRAL",
               delta_caption="vs last 30 days"),
    ]

    # Views by property (top 8), newest listings first already ordered
    views_by_property = [
        NamedValueDto(label=title_by_id[pid], value=float(views_by_prop.get(pid, 0)))
        for pid in prop_ids[:8]
    ]

    # 14-day view trend (oldest → newest)
    trend = _daily_trend(view_rows, now, days=14)

    # Listings table
    rows = []
    for p in props:
        rows.append([
            p.get("title") or "Untitled",
            _pretty(p.get("property_type")),
            _status_label(p.get("approval_status"), p.get("status")),
            str(views_by_prop.get(p["id"], 0)),
            str(leads_by_prop.get(p["id"], 0)),
        ])

    return OwnerDashboardResponse(
        tiles=tiles,
        views_by_property=views_by_property,
        views_trend=trend,
        properties=TableDto(
            headers=["Property", "Type", "Status", "Views", "Leads"],
            rows=rows,
            status_column_index=2,
        ),
    )


# ── date helpers ──────────────────────────────────────────────────────────────

def _parse(ts: Optional[str]) -> Optional[datetime]:
    if not ts:
        return None
    try:
        # Supabase returns ISO 8601, sometimes with 'Z'
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except Exception:
        return None


def _daily_trend(rows: List[dict], now: datetime, days: int = 14) -> List[NamedValueDto]:
    """Count rows per day for the last `days` days, oldest → newest, labelled d/M."""
    buckets: dict = defaultdict(int)
    for r in rows:
        ts = _parse(r.get("created_at"))
        if ts is None:
            continue
        buckets[ts.date()] += 1
    out: List[NamedValueDto] = []
    today = now.date()
    for i in range(days - 1, -1, -1):
        d = today - timedelta(days=i)
        out.append(NamedValueDto(label=f"{d.day}/{d.month}", value=float(buckets.get(d, 0))))
    return out


_MONTH_NAMES = ["", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]


def _count_between(rows, start, end=None) -> int:
    c = 0
    for r in rows:
        ts = _parse(r.get("created_at"))
        if ts and ts >= start and (end is None or ts < end):
            c += 1
    return c


def _sum_amount_between(payments, start, end=None) -> float:
    s = 0.0
    for p in payments:
        ts = _parse(p.get("created_at"))
        if ts and ts >= start and (end is None or ts < end):
            s += (p.get("amount") or 0)
    return s


def _month_series(rows, now: datetime, months: int = 6) -> tuple[List[str], List[float]]:
    """Count rows per calendar month for the last `months` months (oldest → newest)."""
    buckets: dict = defaultdict(int)
    for r in rows:
        ts = _parse(r.get("created_at"))
        if ts:
            buckets[(ts.year, ts.month)] += 1
    seq = []
    y, m = now.year, now.month
    for _ in range(months):
        seq.append((y, m))
        m -= 1
        if m == 0:
            m = 12
            y -= 1
    seq.reverse()
    labels = [_MONTH_NAMES[mm] for (_, mm) in seq]
    values = [float(buckets.get((yy, mm), 0)) for (yy, mm) in seq]
    return labels, values


def _inr_short(amount: float) -> str:
    amount = amount or 0
    if amount >= 100000:
        return f"₹{amount / 100000:.1f}L"
    if amount >= 1000:
        return f"₹{amount / 1000:.0f}K"
    return f"₹{amount:.0f}"


def _month_start(now: datetime) -> datetime:
    return now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)


def _prev_month_start(month_start: datetime) -> datetime:
    if month_start.month == 1:
        return month_start.replace(year=month_start.year - 1, month=12)
    return month_start.replace(month=month_start.month - 1)


# ── GET /dashboard/admin ──────────────────────────────────────────────────────

@router.get("/admin", response_model=AdminDashboardResponse, summary="Admin platform analytics")
async def admin_dashboard(
    current_user: dict = Depends(require_admin),
    supabase           = Depends(get_supabase),
):
    """
    Platform-wide analytics (admin only): active users, pending approvals, monthly revenue and
    open fraud alerts, plus revenue-by-stream, a 6-month user-growth trend, the approval queue
    and the fraud/complaint feed. All figures are real aggregates over Supabase.
    """
    now = datetime.now(timezone.utc)
    month_start = _month_start(now)
    prev_start = _prev_month_start(month_start)

    # Users
    profiles = (supabase.table("profiles")
                .select("id, role, is_verified, created_at").execute().data) or []
    active_users = len(profiles)
    signups_month = _count_between(profiles, month_start)

    # Pending approvals
    pending = (supabase.table("properties")
               .select("id", count="exact").eq("approval_status", "pending").execute())
    pending_count = pending.count or 0

    # Payments / revenue
    payments = (supabase.table("payments")
                .select("amount, tier, created_at").execute().data) or []
    revenue_mtd = _sum_amount_between(payments, month_start)
    revenue_prev = _sum_amount_between(payments, prev_start, month_start)
    rev_delta, rev_dt = _pct_delta(int(revenue_mtd), int(revenue_prev))

    # Fraud alerts
    fraud = (supabase.table("fraud_alerts")
             .select("*").order("created_at", desc=True).execute().data) or []
    open_fraud = sum(1 for f in fraud if f.get("severity") != "resolved")

    tiles = [
        KpiDto(label="Active Users", value=f"{active_users:,}",
               delta=f"+{signups_month}", delta_type="GOOD" if signups_month else "NEUTRAL",
               delta_caption="this month"),
        KpiDto(label="Pending Approvals", value=str(pending_count),
               delta="needs review", delta_type="CRITICAL" if pending_count else "NEUTRAL",
               delta_caption=""),
        KpiDto(label="Revenue MTD", value=_inr_short(revenue_mtd),
               delta=rev_delta, delta_type=rev_dt, delta_caption="vs last month"),
        KpiDto(label="Fraud Alerts", value=str(open_fraud),
               delta="open" if open_fraud else "clear",
               delta_type="CRITICAL" if open_fraud else "GOOD", delta_caption="unresolved"),
    ]

    # Revenue by stream (grouped by subscription tier, in ₹ lakhs, largest first)
    by_tier: dict = defaultdict(float)
    for p in payments:
        by_tier[(p.get("tier") or "other")] += (p.get("amount") or 0)
    revenue_by_stream = [
        NamedValueDto(label=_pretty(t) or "Other", value=round(v / 100000, 2))
        for t, v in sorted(by_tier.items(), key=lambda kv: -kv[1])
    ]

    # User growth (6 months): new signups + newly-verified agents
    labels, signups = _month_series(profiles, now, 6)
    _, verified_agents = _month_series(
        [p for p in profiles if p.get("role") == "agent" and p.get("is_verified")], now, 6
    )
    user_growth = LineChartDto(
        x_labels=labels,
        series=[
            SeriesDto(name="New Signups", values=signups),
            SeriesDto(name="Verified Agents", values=verified_agents),
        ],
    )

    # Approval queue (latest 8 listings + submitter name)
    queue = (supabase.table("properties")
             .select("id, title, property_type, approval_status, owner_id, created_at")
             .order("created_at", desc=True).limit(8).execute().data) or []
    owner_ids = list({p["owner_id"] for p in queue if p.get("owner_id")})
    owners: dict = {}
    if owner_ids:
        o_rows = (supabase.table("profiles")
                  .select("id, full_name").in_("id", owner_ids).execute().data) or []
        owners = {o["id"]: (o.get("full_name") or "—") for o in o_rows}
    approval_rows = [
        [p.get("title") or "Untitled", "Property",
         owners.get(p.get("owner_id"), "—"),
         (p.get("approval_status") or "pending").capitalize()]
        for p in queue
    ]

    fraud_rows = [
        [f.get("title") or "Alert", f.get("details") or "",
         (f.get("severity") or "pending").capitalize()]
        for f in fraud[:10]
    ]

    return AdminDashboardResponse(
        tiles=tiles,
        revenue_by_stream=revenue_by_stream,
        user_growth=user_growth,
        approval_queue=TableDto(
            headers=["Item", "Type", "Submitted By", "Status"],
            rows=approval_rows, status_column_index=3,
        ),
        fraud_alerts=TableDto(
            headers=["Alert", "Details", "Severity"],
            rows=fraud_rows, status_column_index=2,
        ),
    )


# ── helpers for agent/partner ─────────────────────────────────────────────────

def _monthly_sum_series(rows, now: datetime, months: int, amount_key: str,
                        date_key: str = "created_at") -> List[NamedValueDto]:
    """Sum `amount_key` per month for the last `months` months, returned in ₹ lakhs."""
    buckets: dict = defaultdict(float)
    for r in rows:
        ts = _parse(r.get(date_key))
        if ts:
            buckets[(ts.year, ts.month)] += (r.get(amount_key) or 0)
    seq = []
    y, m = now.year, now.month
    for _ in range(months):
        seq.append((y, m))
        m -= 1
        if m == 0:
            m = 12
            y -= 1
    seq.reverse()
    return [
        NamedValueDto(label=_MONTH_NAMES[mm], value=round(buckets.get((yy, mm), 0) / 100000, 2))
        for (yy, mm) in seq
    ]


def _time_ago(ts_str: Optional[str]) -> str:
    ts = _parse(ts_str)
    if not ts:
        return "—"
    secs = (datetime.now(timezone.utc) - ts).total_seconds()
    if secs < 3600:
        return f"{max(1, int(secs // 60))}m ago"
    if secs < 86400:
        return f"{int(secs // 3600)}h ago"
    days = int(secs // 86400)
    return "Yesterday" if days == 1 else f"{days}d ago"


# ── GET /dashboard/agent ──────────────────────────────────────────────────────

@router.get("/agent", response_model=AgentDashboardResponse, summary="Agent analytics")
async def agent_dashboard(
    user_id: Optional[str] = None,
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """
    Agent CRM analytics: listings, lead pipeline by stage, commission trend, lead inbox.
    Self-scoped to the caller (their own listings/leads/commissions) — like the Owner
    dashboard — so any authenticated user may call it; a non-agent simply sees an empty state.
    Admins may pass ?user_id= to view a specific agent (agent-wise filter on the admin console).
    """
    uid = _resolve_uid(current_user, user_id)
    now = datetime.now(timezone.utc)
    month_start = _month_start(now)

    props = (supabase.table("properties").select("id, status")
             .eq("owner_id", uid).execute().data) or []
    prop_ids = [p["id"] for p in props]
    active_listings = sum(1 for p in props if p.get("status") == "active")

    leads = (supabase.table("property_leads")
             .select("buyer_name, property_title, status, updated_at, created_at")
             .eq("owner_id", uid).order("created_at", desc=True).execute().data) or []

    stage_map = {"pending": "New", "contacted": "Contacted", "visit_scheduled": "Site Visit",
                 "converted": "Converted", "closed": "Closed"}
    stage_order = ["New", "Contacted", "Site Visit", "Converted", "Closed"]
    counts = {s: 0 for s in stage_order}
    active_pipeline = 0
    for l in leads:
        label = stage_map.get(l.get("status"))
        if label:
            counts[label] += 1
        if l.get("status") not in ("converted", "closed", "rejected"):
            active_pipeline += 1
    lead_pipeline = [NamedValueDto(label=s, value=float(counts[s])) for s in stage_order]

    site_visits = 0
    if prop_ids:
        bookings = (supabase.table("bookings").select("id, status")
                    .in_("property_id", prop_ids).execute().data) or []
        site_visits = sum(1 for b in bookings if b.get("status") in ("pending", "confirmed"))

    comms = (supabase.table("commissions").select("amount, status, earned_at")
             .eq("user_id", uid).execute().data) or []
    commission_mtd = sum(
        (c.get("amount") or 0) for c in comms
        if (_parse(c.get("earned_at")) or now) >= month_start
    )
    commission_earned = _monthly_sum_series(comms, now, 6, "amount", "earned_at")

    tiles = [
        KpiDto(label="Active Listings", value=str(active_listings),
               delta="", delta_type="NEUTRAL", delta_caption="live"),
        KpiDto(label="Leads in Pipeline", value=str(active_pipeline),
               delta="", delta_type="GOOD" if active_pipeline else "NEUTRAL", delta_caption="open"),
        KpiDto(label="Site Visits Booked", value=str(site_visits),
               delta="", delta_type="NEUTRAL", delta_caption="booked"),
        KpiDto(label="Commission MTD", value=_inr_short(commission_mtd),
               delta="", delta_type="GOOD" if commission_mtd else "NEUTRAL",
               delta_caption="this month"),
    ]

    inbox_rows = [
        [l.get("buyer_name") or "—", l.get("property_title") or "—",
         stage_map.get(l.get("status"), (l.get("status") or "").capitalize()),
         _time_ago(l.get("updated_at") or l.get("created_at")),
         current_user.get("full_name") or "You"]
        for l in leads[:10]
    ]

    return AgentDashboardResponse(
        tiles=tiles,
        lead_pipeline=lead_pipeline,
        commission_earned=commission_earned,
        lead_inbox=TableDto(
            headers=["Lead", "Property", "Stage", "Last Contact", "Assigned"],
            rows=inbox_rows, status_column_index=2,
        ),
    )


# ── GET /dashboard/partner ────────────────────────────────────────────────────

@router.get("/partner", response_model=PartnerDashboardResponse, summary="Channel partner analytics")
async def partner_dashboard(
    user_id: Optional[str] = None,
    current_user: dict = Depends(get_current_user),
    supabase           = Depends(get_supabase),
):
    """
    Channel-partner analytics: referral funnel, conversion, commission payouts, pipeline.
    Self-scoped to the caller (their own referrals/commissions), so any authenticated user may
    call it; a non-partner simply sees an empty state. Admins may pass ?user_id= to view a
    specific channel partner (partner-wise filter on the admin console).
    """
    uid = _resolve_uid(current_user, user_id)
    now = datetime.now(timezone.utc)
    month_start = _month_start(now)

    refs = (supabase.table("referrals").select("*")
            .eq("partner_id", uid).order("created_at", desc=True).execute().data) or []

    total = len(refs)
    contacted = sum(1 for r in refs if r.get("stage") in ("contacted", "site_visit", "converted"))
    site_visit = sum(1 for r in refs if r.get("stage") in ("site_visit", "converted"))
    converted = sum(1 for r in refs if r.get("stage") == "converted")
    conv_rate = round(converted / total * 100) if total else 0
    commission_earned = sum(
        (r.get("commission_amount") or 0) for r in refs
        if r.get("commission_status") in ("pending", "paid")
    )

    sent_month = _count_between(refs, month_start)
    conv_month = sum(
        1 for r in refs
        if r.get("stage") == "converted" and (_parse(r.get("updated_at")) or now) >= month_start
    )

    tiles = [
        KpiDto(label="Referrals Sent", value=str(total),
               delta=f"+{sent_month}", delta_type="GOOD" if sent_month else "NEUTRAL",
               delta_caption="this month"),
        KpiDto(label="Conversions", value=str(converted),
               delta=f"+{conv_month}", delta_type="GOOD" if conv_month else "NEUTRAL",
               delta_caption="this month"),
        KpiDto(label="Conversion Rate", value=f"{conv_rate}%",
               delta="", delta_type="NEUTRAL", delta_caption="overall"),
        KpiDto(label="Commission Earned", value=_inr_short(commission_earned),
               delta="", delta_type="GOOD" if commission_earned else "NEUTRAL",
               delta_caption="pending + paid"),
    ]

    referral_funnel = [
        NamedValueDto(label="Sent", value=float(total)),
        NamedValueDto(label="Contacted", value=float(contacted)),
        NamedValueDto(label="Site Visit", value=float(site_visit)),
        NamedValueDto(label="Converted", value=float(converted)),
    ]
    payout_trend = _monthly_sum_series(refs, now, 6, "commission_amount", "created_at")

    stage_labels = {"sent": "Sent", "contacted": "Contacted", "site_visit": "Site Visit",
                    "converted": "Converted", "lost": "Lost"}
    pipeline_rows = []
    for r in refs[:10]:
        comm = r.get("commission_amount") or 0
        cstatus = r.get("commission_status")
        comm_str = "—" if not comm else f"{_inr_short(comm)} ({cstatus})"
        val = r.get("property_value")
        pipeline_rows.append([
            r.get("referred_name") or "—",
            stage_labels.get(r.get("stage"), (r.get("stage") or "").capitalize()),
            _inr_short(val) if val else "—",
            comm_str,
        ])

    return PartnerDashboardResponse(
        tiles=tiles,
        referral_funnel=referral_funnel,
        payout_trend=payout_trend,
        referral_pipeline=TableDto(
            headers=["Referral", "Stage", "Property Value", "Commission"],
            rows=pipeline_rows, status_column_index=1,
        ),
    )
