package com.realestate.app.utils

object CurrencyFormatter {

    /** Full display: ₹18,000/month or ₹65 L or ₹1.2 Cr */
    fun format(amount: Long, frequency: String? = null): String {
        val base = when {
            amount >= 10_000_000L -> "₹${"%.1f".format(amount / 10_000_000.0)} Cr"
            amount >= 100_000L   -> "₹${"%.1f".format(amount / 100_000.0)} L"
            amount >= 1_000L     -> "₹${"%,d".format(amount)}"
            else                 -> "₹$amount"
        }
        return if (frequency != null) "$base/$frequency" else base
    }

    /** Short form for cards: ₹18K, ₹65L, ₹1.2Cr */
    fun short(amount: Long): String = when {
        amount >= 10_000_000L -> "₹${"%.1f".format(amount / 10_000_000.0)}Cr"
        amount >= 100_000L   -> "₹${"%.0f".format(amount / 100_000.0)}L"
        amount >= 1_000L     -> "₹${"%.0f".format(amount / 1_000.0)}K"
        else                 -> "₹$amount"
    }
}
