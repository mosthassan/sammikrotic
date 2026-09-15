package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.DistributorAccessRestrictedScreen
import com.example.ui.components.ResetProductionDialog
import com.example.ui.components.TopNavBar
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.CardsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.DistributionScreen
import com.example.ui.screens.VouchersScreen
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for Arabic interface
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SamMikrotikApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SamMikrotikApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.users.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val googleSignInState by viewModel.googleSignInState.collectAsState()
    val resetProductionState by viewModel.resetProductionState.collectAsState()

    var showResetProductionDialog by remember { mutableStateOf(false) }

    // Show toast for Google Sign-In events
    androidx.compose.runtime.LaunchedEffect(googleSignInState.successMessage) {
        googleSignInState.successMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.dismissGoogleMessage()
        }
    }

    androidx.compose.runtime.LaunchedEffect(googleSignInState.errorMessage) {
        googleSignInState.errorMessage?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.dismissGoogleMessage()
        }
    }

    // Show toast for Production Reset
    androidx.compose.runtime.LaunchedEffect(resetProductionState) {
        resetProductionState?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.dismissResetMessage()
        }
    }

    // Production Reset Confirmation Dialog
    if (showResetProductionDialog) {
        ResetProductionDialog(
            onDismissRequest = { showResetProductionDialog = false },
            onConfirmReset = {
                viewModel.resetToProductionEnvironment {
                    showResetProductionDialog = false
                }
            }
        )
    }

    val isDistributor = currentUser?.role == "DISTRIBUTOR"

    val navItems = if (isDistributor) {
        listOf(
            NavigationItem("لوحة الموزع", Icons.Default.Dashboard),
            NavigationItem("الأجهزة (مقيد)", Icons.Default.Lock),
            NavigationItem("توزيع الكروت", Icons.Default.ConfirmationNumber),
            NavigationItem("إضافة بقالات", Icons.Default.Store),
            NavigationItem("تحصيل السندات", Icons.Default.Receipt),
            NavigationItem("مستشار AI", Icons.Default.AutoAwesome)
        )
    } else {
        listOf(
            NavigationItem("لوحة التحكم", Icons.Default.Dashboard),
            NavigationItem("الأجهزة والآيبي", Icons.Default.Router),
            NavigationItem("استوديو الكروت", Icons.Default.ConfirmationNumber),
            NavigationItem("نقاط البيع", Icons.Default.Store),
            NavigationItem("المالية والاستثمار", Icons.Default.AccountBalance),
            NavigationItem("مستشار AI", Icons.Default.AutoAwesome)
        )
    }

    Scaffold(
        topBar = {
            TopNavBar(
                currentUser = currentUser,
                allUsers = allUsers,
                syncStatus = syncStatus,
                googleSignInState = googleSignInState,
                onSelectUser = { viewModel.selectUser(it) },
                onTriggerSync = {
                    viewModel.triggerCloudSync()
                },
                onGoogleSignInClick = {
                    viewModel.signInWithGoogle(activity)
                },
                onGoogleSignOutClick = {
                    viewModel.signOutGoogle()
                },
                onResetToProductionClick = {
                    showResetProductionDialog = true
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MikroTikNavy,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = MikroTikPrimary
                        ),
                        modifier = Modifier.testTag("nav_item_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { selectedTab = it },
                    onSignInWithGoogle = { viewModel.signInWithGoogle(activity) },
                    onSignOutGoogle = { viewModel.signOutGoogle() },
                    onResetToProduction = { showResetProductionDialog = true }
                )
                1 -> {
                    if (isDistributor) {
                        DistributorAccessRestrictedScreen(
                            onGoToDashboard = { selectedTab = 0 },
                            onGoToCards = { selectedTab = 2 },
                            onGoToRetailers = { selectedTab = 3 },
                            onGoToVouchers = { selectedTab = 4 }
                        )
                    } else {
                        DevicesScreen(viewModel = viewModel)
                    }
                }
                2 -> CardsScreen(viewModel = viewModel)
                3 -> DistributionScreen(
                    viewModel = viewModel,
                    onNavigateToVouchers = { selectedTab = 4 }
                )
                4 -> VouchersScreen(viewModel = viewModel)
                5 -> AiStudioScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
