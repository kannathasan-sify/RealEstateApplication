package com.realestate.app.ui.utils

/**
 * FormatUtils — INR currency helpers.
 * All AED references have been removed; use CurrencyFormatter for ₹ formatting.
 */
import com.realestate.app.utils.CurrencyFormatter

/**
 * Format a price in ₹ with optional frequency suffix.
 * e.g. formatPrice(18000, "month") → "₹18,000/month"
 */
fun formatPrice(price: Double, frequency: String): String =
    CurrencyFormatter.format(price.toLong(), frequency)

/**
 * Short form for display in cards.
 * e.g. formatPriceShort(6500000) → "₹65L"
 */
fun formatPriceShort(price: Double): String =
    CurrencyFormatter.short(price.toLong())
