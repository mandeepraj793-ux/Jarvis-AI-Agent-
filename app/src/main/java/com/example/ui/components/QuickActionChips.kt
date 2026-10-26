package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanBright
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.OutlineSlate

@Composable
fun QuickActionChips(
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "Open YouTube",
        "WhatsApp kholo",
        "Set alarm for 7 AM",
        "Kal 6 baje ka alarm lagao",
        "Note: Buy groceries & milk",
        "Read my saved notes",
        "Open Wi-Fi Settings",
        "Search Gemini AI features"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEachIndexed { index, prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(JarvisSurfaceVariant)
                    .border(1.dp, OutlineSlate, RoundedCornerShape(20.dp))
                    .clickable { onSelect(prompt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("quick_chip_$index")
            ) {
                Text(
                    text = prompt,
                    color = CyanBright,
                    fontSize = 12.sp
                )
            }
        }
    }
}
