package com.vx7.marketing

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val UNITS = listOf("Box", "Packet", "Piece", "Carton", "Bag", "Kg", "Gram", "Litre", "Dozen", "Bundle", "Set", "Roll", "Other")

/** Formats a quantity exactly as typed: 2.0 -> "2", 1.50 -> "1.5". */
fun fmtQty(q: Double): String = BigDecimal(q.toString()).stripTrailingZeros().toPlainString()

/** Returns a valid positive quantity or null. */
fun parseQty(s: String): Double? {
    val v = s.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return if (v > 0.0 && v.isFinite() && v <= 1_000_000_000.0) v else null
}

fun normName(s: String): String = s.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

fun editDistance(a: String, b: String): Int {
    var prev = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val cur = IntArray(b.length + 1)
        cur[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
        }
        prev = cur
    }
    return prev[b.length]
}

/** True when two product names are equal or very close (case, spacing, punctuation, 1-letter typo). */
fun similarNames(a: String, b: String): Boolean {
    val x = normName(a)
    val y = normName(b)
    if (x.isEmpty() || y.isEmpty()) return false
    if (x == y) return true
    if (minOf(x.length, y.length) >= 4 && (x.contains(y) || y.contains(x))) return true
    return x.length >= 4 && y.length >= 4 && editDistance(x, y) <= 1
}

fun dayStart(t: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = t
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun dayEnd(t: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = dayStart(t)
    c.add(Calendar.DAY_OF_YEAR, 1)
    return c.timeInMillis - 1
}

fun monthStart(t: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = dayStart(t)
    c.set(Calendar.DAY_OF_MONTH, 1)
    return c.timeInMillis
}

fun daysAgoStart(t: Long, days: Int): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = dayStart(t)
    c.add(Calendar.DAY_OF_YEAR, -days)
    return c.timeInMillis
}

fun fmtDate(t: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(t))
fun fmtTime(t: Long): String = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(t))
fun fmtLongDate(t: Long): String = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(Date(t))
fun fileStamp(t: Long): String = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.ENGLISH).format(Date(t))
fun fileDay(t: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(t))

/** Material date pickers return UTC midnight; convert to local midnight of the same calendar day. */
fun utcPickerToLocal(ms: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    u.timeInMillis = ms
    val c = Calendar.getInstance()
    c.clear()
    c.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    return c.timeInMillis
}

fun localToUtcPicker(t: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = t
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    u.clear()
    u.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    return u.timeInMillis
}

fun withDate(base: Long, localDayMillis: Long): Long {
    val b = Calendar.getInstance(); b.timeInMillis = base
    val d = Calendar.getInstance(); d.timeInMillis = localDayMillis
    b.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH))
    return b.timeInMillis
}

fun withTime(base: Long, hour: Int, minute: Int): Long {
    val b = Calendar.getInstance(); b.timeInMillis = base
    b.set(Calendar.HOUR_OF_DAY, hour); b.set(Calendar.MINUTE, minute); b.set(Calendar.SECOND, 0); b.set(Calendar.MILLISECOND, 0)
    return b.timeInMillis
}
