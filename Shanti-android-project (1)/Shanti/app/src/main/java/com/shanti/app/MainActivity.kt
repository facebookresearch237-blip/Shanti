package com.shanti.app
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private val Teal = Color(0xFF0E8F78)
const val DNS_HOST = "family-filter-dns.cleanbrowsing.org" // অশালীন সাইট + সেফ সার্চ

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); P.init(this)
        val gate = intent.getBooleanExtra("gate", false)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Teal)) {
                Surface(Modifier.fillMaxSize()) {
                    var pinFor by remember { mutableStateOf<(() -> Unit)?>(if (gate) ({ P.unlockUntil = System.currentTimeMillis() + 120_000; finish() }) else null) }
                    if (!P.hasPin) PinDialog("নতুন পিন ঠিক করুন (৪ সংখ্যা)", { }, { P.setPin(it) }, null)
                    else if (pinFor != null) PinDialog("পিন দিন", { pinFor = null; if (gate) finish() }, { if (P.check(it)) { val f = pinFor; pinFor = null; f?.invoke() } }, null)
                    App { action -> if (P.hasPin) pinFor = action }
                }
            }
        }
    }
}

@Composable
fun PinDialog(title: String, onCancel: () -> Unit, onOk: (String) -> Unit, x: Nothing?) {
    var t by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onCancel, title = { Text(title) },
        text = { OutlinedTextField(t, { if (it.length <= 4 && it.all(Char::isDigit)) t = it }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)) },
        confirmButton = { TextButton({ if (t.length == 4) { onOk(t); t = "" } }) { Text("ঠিক আছে") } },
        dismissButton = { if (P.hasPin) TextButton(onCancel) { Text("বাতিল") } })
}

@Composable
fun App(needPin: ((() -> Unit)) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("আজ", "৩০ দিন", "সুরক্ষা")
    Scaffold(bottomBar = { NavigationBar { tabs.forEachIndexed { i, n -> NavigationBarItem(tab == i, { tab = i }, { }, label = { Text(n) }) } } }) { pad ->
        Column(Modifier.padding(pad).padding(18.dp).verticalScroll(rememberScrollState())) {
            when (tab) { 0 -> Today(); 1 -> Month(); else -> Guard(needPin) }
        }
    }
}

@Composable
fun Today() {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(2000); tick++ } }
    val used = P.reelSecs / 60; val lim = P.limitMin; tick.let { }
    Text("আজকের হিসাব", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth(), Alignment.Center) {
        CircularProgressIndicator(progress = { (used / lim.toFloat()).coerceIn(0f, 1f) }, Modifier.size(150.dp), strokeWidth = 12.dp)
        Text("$used / $lim মি.", style = MaterialTheme.typography.titleLarge)
    }
    Spacer(Modifier.height(16.dp))
    Text(if (P.blockAll) "রিল আজ বন্ধ আছে" else if (used < lim) "${lim - used} মিনিট বাকি" else "সীমা শেষ", style = MaterialTheme.typography.titleMedium)
}

@Composable
fun Month() {
    val c = LocalContext.current
    val d = remember { runCatching { last30Days(c) }.getOrDefault(List(30) { 0f }) }
    Text("গত ৩০ দিন", style = MaterialTheme.typography.headlineMedium)
    Text("প্রতিদিনের স্ক্রিনটাইম (ঘণ্টা)")
    Canvas(Modifier.fillMaxWidth().height(180.dp).padding(top = 12.dp)) {
        val mx = (d.maxOrNull() ?: 1f).coerceAtLeast(1f); val w = size.width / 30
        d.forEachIndexed { i, v -> val h = size.height * v / mx
            drawRect(if (v > 6) Color(0xFFD4492F) else Teal, Offset(i * w + 1, size.height - h), Size(w - 2, h)) }
    }
    Spacer(Modifier.height(8.dp))
    Text("গড়: ${"%.1f".format(d.average())} ঘণ্টা/দিন   মোট: ${d.sum().toInt()} ঘণ্টা")
    if (d.all { it == 0f }) Text("তথ্য দেখতে ‘সুরক্ষা’ ট্যাব থেকে ইউসেজ অ্যাক্সেস চালু করুন।", color = Color.Gray)
}

@Composable
fun Guard(needPin: ((() -> Unit)) -> Unit) {
    val c = LocalContext.current
    var all by remember { mutableStateOf(P.blockAll) }; var prot by remember { mutableStateOf(P.protect) }; var lim by remember { mutableIntStateOf(P.limitMin) }
    fun open(a: String) = c.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    Text("সুরক্ষা সেটিংস", style = MaterialTheme.typography.headlineMedium)
    Text("চালু করা সহজ, বন্ধ করতে পিন লাগবে")
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("রিল ও শর্টস পুরোপুরি বন্ধ")
        Switch(all, { v -> val f = { all = v; P.blockAll = v }; if (v) f() else needPin(f) })
    }
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("দৈনিক সীমা: $lim মি.")
        Row { TextButton({ if (lim > 10) { lim -= 5; P.limitMin = lim } }) { Text("−") }
              TextButton({ needPin { lim += 5; P.limitMin = lim } }) { Text("+") } }
    }
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("সেটিংস/আনইনস্টল পিন-সুরক্ষা")
        Switch(prot, { v -> val f = { prot = v; P.protect = v }; if (v) f() else needPin(f) })
    }
    Spacer(Modifier.height(16.dp)); Text("প্রথমবার সেটআপ (সব কটি দরকার)", style = MaterialTheme.typography.titleMedium)
    OutlinedButton({ open(Settings.ACTION_ACCESSIBILITY_SETTINGS) }, Modifier.fillMaxWidth()) { Text("১. অ্যাক্সেসিবিলিটি সেবা চালু করুন") }
    OutlinedButton({ open(Settings.ACTION_USAGE_ACCESS_SETTINGS) }, Modifier.fillMaxWidth()) { Text("২. ইউসেজ অ্যাক্সেস দিন") }
    OutlinedButton({ c.startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(c, AdminReceiver::class.java)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }, Modifier.fillMaxWidth()) { Text("৩. ডিভাইস অ্যাডমিন চালু করুন") }
    OutlinedButton({ open("android.settings.WIRELESS_SETTINGS") }, Modifier.fillMaxWidth()) { Text("৪. অশালীন কনটেন্ট ফিল্টার: প্রাইভেট DNS") }
    Text("প্রাইভেট DNS-এ এই হোস্টনেম বসান:\n$DNS_HOST", color = Color.Gray)
}
