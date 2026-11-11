package com.shanti.app
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelService : AccessibilityService() {
    private var last = 0L

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        P.init(this)
        val pkg = e.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return
        val now = System.currentTimeMillis()

        // ১) সেটিংস/আনইনস্টল স্ক্রিনে অ্যাপের নাম দেখা গেলে পিন চাওয়া হবে
        if (P.protect && now > P.unlockUntil && (pkg == "com.android.settings" || pkg.contains("packageinstaller"))
            && root.findAccessibilityNodeInfosByText("শান্তি").isNotEmpty()) {
            startActivity(Intent(this, MainActivity::class.java).putExtra("gate", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            performGlobalAction(GLOBAL_ACTION_HOME); return
        }

        // ২) রিল/শর্টস চলছে কি না
        if (!isReel(pkg, root)) { last = 0; return }
        if (last > 0) P.addSecs(((now - last).coerceAtMost(3000) / 1000f).let { Math.round(it) })
        last = now
        if (P.overLimit()) {
            startActivity(Intent(this, BlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            performGlobalAction(GLOBAL_ACTION_HOME); last = 0
        }
    }

    // দ্রষ্টব্য: অ্যাপের আপডেটে view id বদলাতে পারে, তাই বাস্তব ফোনে পরীক্ষা করে আপডেট রাখতে হবে।
    private fun isReel(pkg: String, r: AccessibilityNodeInfo) = when (pkg) {
        "com.instagram.android" -> has(r, "com.instagram.android:id/clips_viewer_view_pager")
        "com.google.android.youtube" -> has(r, "com.google.android.youtube:id/reel_recycler") || has(r, "com.google.android.youtube:id/reel_player_page_container")
        "com.facebook.katana" -> r.findAccessibilityNodeInfosByText("Reels").any { it.isSelected }
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> true
        else -> false
    }
    private fun has(r: AccessibilityNodeInfo, id: String) = r.findAccessibilityNodeInfosByViewId(id).isNotEmpty()
    override fun onInterrupt() {}
}
