package com.spamblock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spamblock.data.PreferencesManager
import com.spamblock.util.SimCardInfo

@Composable
fun HomeScreen(
    isRoleHeld: Boolean,
    hasContactsPermission: Boolean,
    hasPhoneStatePermission: Boolean,
    activeSims: List<SimCardInfo>,
    totalBlockedCount: Int,
    onRequestRole: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onRequestPhoneStatePermission: () -> Unit,
    prefs: PreferencesManager
) {
    var blockUnknown by remember { mutableStateOf(prefs.blockUnknownNumbers) }
    var blockPrivate by remember { mutableStateOf(prefs.blockPrivateNumbers) }
    var rejectCall by remember { mutableStateOf(prefs.rejectCall) }
    var notifyOnBlocked by remember { mutableStateOf(prefs.notifyOnBlocked) }

    var sim1Protected by remember { mutableStateOf(prefs.sim1Protected) }
    var sim2Protected by remember { mutableStateOf(prefs.sim2Protected) }

    val isFullyProtected = isRoleHeld && hasContactsPermission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Header with Live Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SpamBlock",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "v2.0",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Hardware-efficient call screening",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isFullyProtected)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                border = BorderStroke(
                    1.dp,
                    if (isFullyProtected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFullyProtected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFullyProtected) "Protected" else "Needs Setup",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isFullyProtected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Action required banner (if permissions missing)
        if (!isFullyProtected) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Action Required",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To enable background blocking without battery drain, authorize the required system roles below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isRoleHeld) {
                        FilledTonalButton(
                            onClick = onRequestRole,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set as Caller ID & Spam App")
                        }
                    }

                    if (!hasContactsPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRequestContactsPermission,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Allow Contacts Access")
                        }
                    }
                }
            }
        }

        // SIM Card Slot Protection Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SIM Card Slots",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!hasPhoneStatePermission) {
                    TextButton(
                        onClick = onRequestPhoneStatePermission,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Detect SIMs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val sim1Info = activeSims.find { it.slotIndex == 0 }
            val sim2Info = activeSims.find { it.slotIndex == 1 }

            // SIM 1 Card
            SimSlotCard(
                slotLabel = "SIM 1",
                carrierName = sim1Info?.carrierName?.takeIf { it.isNotBlank() } ?: sim1Info?.displayName ?: "Primary Slot",
                isProtected = sim1Protected,
                accentColor = MaterialTheme.colorScheme.secondary,
                onToggle = {
                    sim1Protected = it
                    prefs.sim1Protected = it
                }
            )

            // SIM 2 Card
            SimSlotCard(
                slotLabel = "SIM 2",
                carrierName = sim2Info?.carrierName?.takeIf { it.isNotBlank() } ?: sim2Info?.displayName ?: if (activeSims.size > 1) "Secondary Slot" else "Slot 2 (Empty)",
                isProtected = sim2Protected,
                accentColor = MaterialTheme.colorScheme.tertiary,
                enabled = activeSims.isEmpty() || activeSims.size > 1 || sim2Info != null,
                onToggle = {
                    sim2Protected = it
                    prefs.sim2Protected = it
                }
            )
        }

        // Overview Metrics Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "Blocked", value = "$totalBlockedCount")
                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                StatItem(label = "Screened", value = "${prefs.totalScreened}")
                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                StatItem(label = "Allowed", value = "${prefs.getWhitelist().size}")
            }
        }

        // Blocking Rules
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Screening Policies",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column {
                    ModernRuleItem(
                        icon = Icons.Outlined.Contacts,
                        title = "Block Non-Contacts",
                        subtitle = "Screen numbers not saved in address book",
                        checked = blockUnknown,
                        onCheckedChange = {
                            blockUnknown = it
                            prefs.blockUnknownNumbers = it
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    ModernRuleItem(
                        icon = Icons.Outlined.VisibilityOff,
                        title = "Block Private / Hidden",
                        subtitle = "Drop anonymous or restricted callers",
                        checked = blockPrivate,
                        onCheckedChange = {
                            blockPrivate = it
                            prefs.blockPrivateNumbers = it
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    ModernRuleItem(
                        icon = Icons.Outlined.PhoneDisabled,
                        title = "Instant Disconnect",
                        subtitle = if (rejectCall) "Drops call before ringing starts" else "Mutes ringer silently without hangup",
                        checked = rejectCall,
                        onCheckedChange = {
                            rejectCall = it
                            prefs.rejectCall = it
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    ModernRuleItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Silent Notifications",
                        subtitle = "Notify quietly when a call is intercepted",
                        checked = notifyOnBlocked,
                        onCheckedChange = {
                            notifyOnBlocked = it
                            prefs.notifyOnBlocked = it
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SimSlotCard(
    slotLabel: String,
    carrierName: String,
    isProtected: Boolean,
    accentColor: Color,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isProtected && enabled) accentColor.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SimCard,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = slotLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isProtected && enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isProtected && enabled) "PROTECTED" else "OPEN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = if (isProtected && enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = carrierName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isProtected,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModernRuleItem(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
