package com.shanti.app
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class BlockActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); P.init(this)
        setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF0A1F22)).padding(32.dp),
                Arrangement.Center, Alignment.CenterHorizontally) {
                Text("আজকের রিল-সময় শেষ", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(if (P.blockAll) "রিল ও শর্টস পুরোপুরি বন্ধ আছে।" else "কাল সকালে আবার সীমা শুরু হবে।", color = Color(0xFF8FB0AB))
                Spacer(Modifier.height(24.dp))
                Button(onClick = { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); finish() },
                    colors = ButtonDefaults.buttonColors(Color(0xFF6FE0BF), Color(0xFF08302A))) { Text("ফিরে যান") }
            }
        }
    }
}
