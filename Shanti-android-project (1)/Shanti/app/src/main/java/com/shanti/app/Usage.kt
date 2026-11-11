package com.shanti.app
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/** গত ৩০ দিনের প্রতিদিনের মোট স্ক্রিনটাইম (ঘণ্টা), পুরনো থেকে নতুন। */
fun last30Days(c: Context): List<Float> {
    val m = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val z = ZoneId.systemDefault()
    return (29 downTo 0).map { i ->
        val d = LocalDate.now().minusDays(i.toLong())
        val s = d.atStartOfDay(z).toInstant().toEpochMilli()
        val ms = m.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, s, s + 86_400_000L - 1)
            .filter { it.firstTimeStamp >= s - 1000 }.sumOf { it.totalTimeInForeground }
        ms / 3_600_000f
    }
}
