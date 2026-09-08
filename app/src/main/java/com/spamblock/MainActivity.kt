package com.spamblock

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.spamblock.data.BlockedCallsDbHelper
import com.spamblock.data.PreferencesManager
import com.spamblock.ui.screens.HistoryScreen
import com.spamblock.ui.screens.HomeScreen
import com.spamblock.ui.screens.WhitelistScreen
import com.spamblock.ui.theme.SpamBlockTheme
import com.spamblock.util.SimCardInfo
import com.spamblock.util.SimHelper

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var db: BlockedCallsDbHelper

    private var isRoleHeldState = mutableStateOf(false)
    private var hasContactsPermissionState = mutableStateOf(false)
    private var hasPhoneStatePermissionState = mutableStateOf(false)
    private var activeSimsState = mutableStateOf<List<SimCardInfo>>(emptyList())

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndRole()
    }

    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermissionState.value = granted
        if (!granted) {
            Toast.makeText(this, "Contacts access is needed to detect unknown numbers", Toast.LENGTH_LONG).show()
        }
    }

    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhoneStatePermissionState.value = granted
        if (granted) {
            activeSimsState.value = SimHelper.getActiveSims(this)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Optional notifications permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = PreferencesManager.getInstance(this)
        db = BlockedCallsDbHelper.getInstance(this)

        checkPermissionsAndRole()
        requestNotificationPermissionIfNeeded()

        setContent {
            SpamBlockTheme {
                MainApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndRole()
    }

    private fun checkPermissionsAndRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
            isRoleHeldState.value = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            isRoleHeldState.value = true
        }

        hasContactsPermissionState.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasPhoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        hasPhoneStatePermissionState.value = hasPhoneState
        if (hasPhoneState) {
            activeSimsState.value = SimHelper.getActiveSims(this)
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleRequestLauncher.launch(intent)
                } else {
                    val settingsIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    startActivity(settingsIntent)
                }
            } catch (e: Exception) {
                try {
                    val settingsIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    startActivity(settingsIntent)
                } catch (ex: Exception) {
                    Toast.makeText(this, "Please set SpamBlock as default Caller ID & Spam app in Settings -> Default Apps", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun requestContactsPermission() {
        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun requestPhoneStatePermission() {
        phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Composable
    fun MainApp() {
        var selectedTab by remember { mutableIntStateOf(0) }
        val calls by db.callsFlow.collectAsState()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Outlined.Shield, contentDescription = "Protection") },
                        label = { Text("Protection", style = MaterialTheme.typography.labelMedium) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (calls.isNotEmpty()) {
                                        Badge {
                                            Text(
                                                "${calls.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.History, contentDescription = "Activity")
                            }
                        },
                        label = { Text("Activity", style = MaterialTheme.typography.labelMedium) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = "Allowed") },
                        label = { Text("Allowed", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        isRoleHeld = isRoleHeldState.value,
                        hasContactsPermission = hasContactsPermissionState.value,
                        hasPhoneStatePermission = hasPhoneStatePermissionState.value,
                        activeSims = activeSimsState.value,
                        totalBlockedCount = calls.size,
                        onRequestRole = { requestCallScreeningRole() },
                        onRequestContactsPermission = { requestContactsPermission() },
                        onRequestPhoneStatePermission = { requestPhoneStatePermission() },
                        prefs = prefs
                    )
                    1 -> HistoryScreen(
                        db = db,
                        prefs = prefs
                    )
                    2 -> WhitelistScreen(
                        prefs = prefs
                    )
                }
            }
        }
    }
}
