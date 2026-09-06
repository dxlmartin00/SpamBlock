package com.spamblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spamblock.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isRoleHeld: Boolean,
    hasContactsPermission: Boolean,
    totalBlockedCount: Int,
    onRequestRole: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    prefs: PreferencesManager
) {
    var blockUnknown by remember { mutableStateOf(prefs.blockUnknownNumbers) }
    var blockPrivate by remember { mutableStateOf(prefs.blockPrivateNumbers) }
    var rejectCall by remember { mutableStateOf(prefs.rejectCall) }
    var notifyOnBlocked by remember { mutableStateOf(prefs.notifyOnBlocked) }

    val isFullyProtected = isRoleHeld && hasContactsPermission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isFullyProtected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFullyProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFullyProtected) Icons.Default.Security else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isFullyProtected) "SpamBlock is Active" else "Setup Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isFullyProtected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = if (isFullyProtected)
                        "Unknown & spam callers are automatically blocked with zero battery drain."
                    else
                        "Complete permissions below to enable hardware-efficient call screening.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFullyProtected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                if (!isRoleHeld) {
                    Button(
                        onClick = onRequestRole,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PhoneDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Caller ID & Spam App")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!hasContactsPermission) {
                    OutlinedButton(
                        onClick = onRequestContactsPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Contacts Access")
                    }
                }
            }
        }

        // Metrics Overview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Blocked",
                value = "$totalBlockedCount",
                icon = Icons.Default.Block,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Screened",
                value = "${prefs.totalScreened}",
                icon = Icons.Default.Phone,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Allowed",
                value = "${prefs.getWhitelist().size}",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }

        // Blocking Rules
        Text(
            text = "Blocking Rules",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                RuleSwitchRow(
                    title = "Block Non-Contacts",
                    subtitle = "Block all incoming callers not saved in your contacts book",
                    checked = blockUnknown,
                    onCheckedChange = {
                        blockUnknown = it
                        prefs.blockUnknownNumbers = it
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                RuleSwitchRow(
                    title = "Block Private / Hidden",
                    subtitle = "Block callers with anonymous or restricted numbers",
                    checked = blockPrivate,
                    onCheckedChange = {
                        blockPrivate = it
                        prefs.blockPrivateNumbers = it
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                RuleSwitchRow(
                    title = "Hang Up Immediately",
                    subtitle = if (rejectCall) "Disconnects spam call right away" else "Mutes ringer without hanging up",
                    checked = rejectCall,
                    onCheckedChange = {
                        rejectCall = it
                        prefs.rejectCall = it
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                RuleSwitchRow(
                    title = "Blocked Notifications",
                    subtitle = "Show a silent notice when a call is blocked",
                    checked = notifyOnBlocked,
                    onCheckedChange = {
                        notifyOnBlocked = it
                        prefs.notifyOnBlocked = it
                    }
                )
            }
        }

        // Zero-Battery Architecture Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Zero Battery",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Zero Battery / Hardware Drain",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "SpamBlock uses Android Telecom's native CallScreeningService. No background services run while idle, using 0% CPU and 0 MB RAM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RuleSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
