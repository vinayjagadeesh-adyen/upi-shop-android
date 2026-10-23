package com.demo.upishop.ui

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** 100 -> "₹1.00" */
fun formatInr(minorUnits: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    return format.format(minorUnits / 100.0)
}
