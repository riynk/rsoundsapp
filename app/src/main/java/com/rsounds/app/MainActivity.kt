package com.rsounds.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RsoundsFoundationScreen()
                }
            }
        }
    }
}

@Composable
private fun RsoundsFoundationScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("RSounds", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Music, in every word.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "A unified listening space for your library, permitted music sources, and deeply synchronized lyrics.",
            style = MaterialTheme.typography.bodyLarge
        )
        HorizontalDivider()
        Text("Foundation status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        FoundationStatus("Playback foundation", "Media3 ExoPlayer and MediaSessionService are wired into the Android app.")
        FoundationStatus("Deep lyrics foundation", "Lyricsfile and enhanced-LRC parsing support line and word timestamps.")
        FoundationStatus("Provider architecture", "Provider adapters and source-specific policy checks are the next integration layer.")
        Spacer(Modifier.height(4.dp))
        Text(
            "This is an engineering skeleton, not a finished music player. Local scanning, playback controls, provider search, and the full lyrics interface are not implemented yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FoundationStatus(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
