package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.JarvisSettings
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonRed
import com.example.ui.theme.OutlineCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentSettings: JarvisSettings,
    onSaveSettings: (JarvisSettings) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentSettings.languageCode) }
    var voiceOutputEnabled by remember { mutableStateOf(currentSettings.voiceOutputEnabled) }
    var requireConfirmation by remember { mutableStateOf(currentSettings.requireConfirmationForSensitive) }
    var offlineOnly by remember { mutableStateOf(currentSettings.offlineModeOnly) }
    var speechSpeed by remember { mutableFloatStateOf(currentSettings.ttsSpeed) }
    var speechPitch by remember { mutableFloatStateOf(currentSettings.ttsPitch) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisSurface, RoundedCornerShape(16.dp))
                .border(1.dp, OutlineCyan, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CyanGlow,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "JARVIS CONFIGURATION",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Language Preference
            Text(
                text = "Primary Language & Speech Model",
                color = CyanBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Pair("en-IN", "English (IN)"),
                    Pair("hi-IN", "Hindi / Hinglish"),
                    Pair("en-US", "English (US)")
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = selectedLanguage == code,
                        onClick = { selectedLanguage = code },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanGlow,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Voice Output Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voice Feedback (TTS)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Speak assistant responses aloud", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = voiceOutputEnabled,
                    onCheckedChange = { voiceOutputEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanGlow)
                )
            }

            if (voiceOutputEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Speech Rate (${String.format("%.1f", speechSpeed)}x)", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = speechSpeed,
                    onValueChange = { speechSpeed = it },
                    valueRange = 0.6f..1.6f,
                    colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = CyanGlow)
                )

                Text("Pitch (${String.format("%.1f", speechPitch)})", color = TextSecondary, fontSize = 12.sp)
                Slider(
                    value = speechPitch,
                    onValueChange = { speechPitch = it },
                    valueRange = 0.6f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = CyanGlow)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Sensitive Action Confirmation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Require Authorization", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Prompt before setting alarms, dialer, or settings", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = requireConfirmation,
                    onCheckedChange = { requireConfirmation = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanGlow)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Offline Only Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Offline-Only Mode", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Disable cloud API calls; use local intent rule engine", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = offlineOnly,
                    onCheckedChange = { offlineOnly = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanGlow)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Honest Android Sandbox Disclosure
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Android System Sandbox Notice",
                        color = NeonAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "JARVIS operates under standard Android security sandboxing. It cannot bypass screen locks, perform silent phone calls without dialer review, or run arbitrary root/shell commands. All device actions use official Android Intents.",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Clear History
            OutlinedButton(
                onClick = { showClearConfirmation = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_history_button")
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Purge Local Conversation History", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save / Dismiss Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        onSaveSettings(
                            JarvisSettings(
                                languageCode = selectedLanguage,
                                ttsSpeed = speechSpeed,
                                ttsPitch = speechPitch,
                                voiceOutputEnabled = voiceOutputEnabled,
                                requireConfirmationForSensitive = requireConfirmation,
                                offlineModeOnly = offlineOnly
                            )
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    modifier = Modifier.testTag("save_settings_button")
                ) {
                    Text("Apply Settings", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Confirm Data Deletion", color = TextPrimary) },
            text = { Text("Are you sure you want to delete all local conversation history and logs? This action is irreversible.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) {
                    Text("Delete All", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
