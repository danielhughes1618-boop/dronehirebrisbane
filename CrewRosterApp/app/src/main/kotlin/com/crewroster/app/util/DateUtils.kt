package com.crewroster.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun todayIso(): String = LocalDate.now().toString()

fun formatDateLong(iso: String): String {
    val date = LocalDate.parse(iso)
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    return date.format(formatter)
}

fun formatDateShort(iso: String): String {
    val date = LocalDate.parse(iso)
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    return date.format(formatter)
}
