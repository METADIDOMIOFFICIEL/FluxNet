package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {

    fun formatSpeed(bytesPerSec: Long): String {
        val bps = bytesPerSec.coerceAtLeast(0L)
        return when {
            bps < 1_024L -> "$bps o/s"
            bps < 1_048_576L -> String.format(Locale.FRANCE, "%.1f Ko/s", bps / 1_024.0)
            bps < 1_073_741_824L -> String.format(Locale.FRANCE, "%.2f Mo/s", bps / 1_048_576.0)
            else -> String.format(Locale.FRANCE, "%.2f Go/s", bps / 1_073_741_824.0)
        }
    }

    fun formatBytes(bytes: Long): String {
        val b = bytes.coerceAtLeast(0L)
        return when {
            b < 1_024L -> "$b o"
            b < 1_048_576L -> String.format(Locale.FRANCE, "%.1f Ko", b / 1_024.0)
            b < 1_073_741_824L -> String.format(Locale.FRANCE, "%.1f Mo", b / 1_048_576.0)
            else -> String.format(Locale.FRANCE, "%.2f Go", b / 1_073_741_824.0)
        }
    }

    fun formatCurrency(amount: Double, symbol: String): String {
        return if (symbol == "FCFA" || symbol == "XOF" || symbol == "XAF") {
            "${amount.roundToInt()} $symbol"
        } else {
            String.format(Locale.FRANCE, "%.2f %s", amount, symbol)
        }
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        return sdf.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM", Locale.FRENCH)
        return sdf.format(Date(millis))
    }
}
