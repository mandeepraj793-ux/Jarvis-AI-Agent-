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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ActionProposal
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.OutlineCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfirmationDialog(
    proposal: ActionProposal,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("action_confirmation_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisSurface, RoundedCornerShape(16.dp))
                .border(1.dp, OutlineCyan, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            // Header with Security Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Confirmation",
                    tint = NeonAmber,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "ACTION AUTHORIZATION",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "JARVIS is requesting permission to execute the following Android operation:",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Details Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisSurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row {
                    Text(
                        text = "Protocol: ",
                        color = CyanGlow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = proposal.type.name,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
                if (proposal.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = proposal.explanation,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (proposal.arguments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    proposal.arguments.forEach { (k, v) ->
                        Text(
                            text = "• $k: $v",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("cancel_action_button")
                ) {
                    Text("Dismiss", color = TextSecondary)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    modifier = Modifier.testTag("confirm_action_button")
                ) {
                    Text("Authorize", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
