package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChatMessage
import com.example.domain.model.JarvisActionType
import com.example.domain.model.MessageSender
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.OutlineCyan
import com.example.ui.theme.OutlineSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // JARVIS AI avatar badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(JarvisSurfaceVariant)
                    .border(1.dp, CyanGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "J",
                    color = CyanGlow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) JarvisSurfaceElevated else JarvisSurfaceVariant)
                .border(
                    width = 1.dp,
                    color = if (isUser) OutlineCyan else OutlineSlate,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Proposed Action Details Card
            val proposal = message.actionProposal
            if (proposal != null && proposal.type != JarvisActionType.SPEAK_ONLY) {
                Spacer(modifier = Modifier.height(8.dp))
                ActionBadge(
                    actionType = proposal.type,
                    explanation = proposal.explanation.ifBlank { proposal.target },
                    executionSuccess = message.executionResult?.success,
                    executionMessage = message.executionResult?.message
                )
            }

            // Footer with timestamp & TTS speak button for assistant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "You" else "JARVIS AI",
                    color = if (isUser) CyanBright else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )

                if (!isUser && message.text.isNotBlank()) {
                    IconButton(
                        onClick = { onSpeak(message.text) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("tts_replay_button_${message.id}")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak message",
                            tint = CyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionBadge(
    actionType: JarvisActionType,
    explanation: String,
    executionSuccess: Boolean?,
    executionMessage: String?
) {
    val icon: ImageVector = when (actionType) {
        JarvisActionType.OPEN_APP -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew
        JarvisActionType.SEARCH_WEB -> androidx.compose.material.icons.Icons.Default.Search
        JarvisActionType.SET_ALARM -> androidx.compose.material.icons.Icons.Default.Alarm
        JarvisActionType.CREATE_NOTE, JarvisActionType.LIST_NOTES -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.NoteAdd
        JarvisActionType.OPEN_SETTINGS -> androidx.compose.material.icons.Icons.Default.Settings
        else -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1B2A))
            .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanGlow,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = actionType.name,
                color = CyanGlow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))

            // Execution status indicator
            when (executionSuccess) {
                true -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Executed successfully",
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                false -> Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Execution failed",
                    tint = NeonRed,
                    modifier = Modifier.size(16.dp)
                )
                null -> Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = "Pending authorization",
                    tint = NeonAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (explanation.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = explanation,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2
            )
        }

        if (!executionMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = executionMessage,
                color = if (executionSuccess == true) NeonGreen else NeonAmber,
                fontSize = 10.sp
            )
        }
    }
}
