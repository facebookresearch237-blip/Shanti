package com.shanti.app
import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.time.LocalDate

/** সব সেটিংস ও আজকের রিল-সময় এখানে থাকে। পিন শুধু হ্যাশ হিসেবে জমা হয়। */
object P {
    private lateinit var sp: SharedPreferences
    fun init(c: Context) { if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("p", 0) }
    private fun h(s: String) = MessageDigest.getInstance("SHA-256").digest(("shanti$s").toByteArray()).joinToString("") { "%02x".format(it) }
    val hasPin get() = sp.contains("pin")
    fun setPin(p: String) = sp.edit().putString("pin", h(p)).apply()
    fun check(p: String) = sp.getString("pin", null) == h(p)
    var blockAll: Boolean get() = sp.getBoolean("all", false); set(v) = sp.edit().putBoolean("all", v).apply()
    var protect: Boolean get() = sp.getBoolean("protect", true); set(v) = sp.edit().putBoolean("protect", v).apply()
    var limitMin: Int get() = sp.getInt("limit", 30); set(v) = sp.edit().putInt("limit", v).apply()
    var unlockUntil: Long get() = sp.getLong("unlock", 0); set(v) = sp.edit().putLong("unlock", v).apply()
    val reelSecs: Int get() = if (sp.getString("day", "") == LocalDate.now().toString()) sp.getInt("secs", 0) else 0
    fun addSecs(n: Int) {
        val today = LocalDate.now().toString()
        val cur = if (sp.getString("day", "") == today) sp.getInt("secs", 0) else 0
        sp.edit().putString("day", today).putInt("secs", cur + n).apply()
    }
    fun overLimit() = blockAll || reelSecs >= limitMin * 60
}
