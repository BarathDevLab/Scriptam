package com.scriptam.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SnippetTemplate(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val code: String
)

val DEFAULT_SNIPPETS = listOf(
    SnippetTemplate(
        title = "HTTP Fetch & Widget",
        description = "Fetch JSON from REST API and display in a widget",
        icon = Icons.Default.Http,
        code = """// Fetch data and render to home-screen widget
const res = await Native.fetch("https://api.github.com/zen");
const quote = await res.text();

Native.showToast("Quote loaded!");

Widget.render(
  Widget.column({
    padding: 16,
    children: [
      Widget.text("GitHub Zen", { size: 12, color: "#8E8E93" }),
      Widget.spacer(4),
      Widget.text(quote, { size: 16, color: "#FFFFFF", bold: true })
    ]
  })
);
"""
    ),
    SnippetTemplate(
        title = "Battery Alert",
        description = "Check battery level and trigger native notification",
        icon = Icons.Default.BatteryChargingFull,
        code = """// Read battery state and notify if low
const level = Native.getBatteryLevel();
console.log("Current battery level:", level + "%");

if (level <= 20) {
  Native.sendNotification("Low Battery Alert", "Battery is at " + level + "%. Please plug in!");
} else {
  Native.showToast("Battery healthy: " + level + "%");
}
"""
    ),
    SnippetTemplate(
        title = "Crypto Price Tracker",
        description = "Query CoinGecko API and build a live dashboard card",
        icon = Icons.Default.CurrencyBitcoin,
        code = """// Fetch Bitcoin price and build styled widget
const res = await Native.fetch("https://api.coindesk.com/v1/bpi/currentprice.json");
const data = await res.json();
const price = data.bpi.USD.rate;

Widget.render(
  Widget.column({
    padding: 16,
    backgroundColor: "#1A1A24",
    children: [
      Widget.row({
        children: [
          Widget.text("BTC / USD", { size: 14, color: "#F7931A", bold: true }),
          Widget.spacer(8),
          Widget.text(price, { size: 20, color: "#FFFFFF", bold: true })
        ]
      }),
      Widget.spacer(8),
      Widget.text("Updated: " + new Date().toLocaleTimeString(), { size: 10, color: "#6C63FF" })
    ]
  })
);
"""
    ),
    SnippetTemplate(
        title = "Persistent Storage Counter",
        description = "Store and increment execution counter across runs",
        icon = Icons.Default.Timer,
        code = """// Persistent counter via Native.storage
const count = parseInt(Native.getStorage("run_count") || "0", 10) + 1;
Native.setStorage("run_count", count.toString());

console.log("This script has executed " + count + " time(s).");
Native.showToast("Run #" + count);
"""
    ),
    SnippetTemplate(
        title = "Encrypted Secret Vault",
        description = "Securely save and retrieve API keys via Android Keystore",
        icon = Icons.Default.Lock,
        code = """// Store & retrieve encrypted secrets
Native.setSecret("api_key", "sk_live_secret_token_123");
const key = Native.getSecret("api_key");

console.log("Vault loaded secret safely: " + (key ? key.substring(0, 7) + "..." : "none"));
"""
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetSheet(
    onDismiss: () -> Unit,
    onSelectSnippet: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161622),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Insert Snippet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Tap a template to insert at cursor",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(DEFAULT_SNIPPETS) { snippet ->
                    SnippetCard(
                        snippet = snippet,
                        onClick = {
                            onSelectSnippet(snippet.code)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SnippetCard(
    snippet: SnippetTemplate,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF222232))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF6C63FF).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = snippet.icon,
                contentDescription = null,
                tint = Color(0xFF8C82FF),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.size(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snippet.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = snippet.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93),
                maxLines = 2
            )
        }
    }
}
