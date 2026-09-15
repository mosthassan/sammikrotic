package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import com.example.ui.theme.StatusDistributed
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.local.entity.UserEntity
import com.example.ui.GoogleSignInUiState

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSignInWithGoogle: () -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    onResetToProduction: () -> Unit = {}
) {
    val devices by viewModel.devices.collectAsState()
    val retailers by viewModel.retailers.collectAsState()
    val availableCards by viewModel.availableCardsCount.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val networkIdentity by viewModel.networkIdentity.collectAsState()
    val partners by viewModel.partners.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val totalInvestedCapital by viewModel.totalInvestedCapital.collectAsState()
    val totalAssetPurchaseCost by viewModel.totalAssetPurchaseCost.collectAsState()
    val totalReceipts by viewModel.totalReceipts.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val distributors by viewModel.distributors.collectAsState()

    var selectedDashboardTab by remember { mutableIntStateOf(0) }

    val totalDebt = retailers.sumOf { it.balanceOwed }
    val onlineDevicesCount = devices.count { it.status == "ONLINE" }
    val receiptsVal = totalReceipts ?: 0.0
    val paymentsVal = totalPayments ?: 0.0
    val netProfitVal = receiptsVal - paymentsVal

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        // Modern Dashboard Sub-Tabs Header
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedDashboardTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MikroTikPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedDashboardTab]),
                        color = MikroTikPrimary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedDashboardTab == 0,
                    onClick = { selectedDashboardTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مؤشرات وتشغيل الشبكة",
                                fontWeight = if (selectedDashboardTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_dashboard_overview")
                )
                Tab(
                    selected = selectedDashboardTab == 1,
                    onClick = { selectedDashboardTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إدارة الموزعين (${distributors.size})",
                                fontWeight = if (selectedDashboardTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_dashboard_distributors")
                )
                Tab(
                    selected = selectedDashboardTab == 2,
                    onClick = { selectedDashboardTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "هوية وإعدادات الشبكة",
                                fontWeight = if (selectedDashboardTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_dashboard_identity")
                )
            }
        }

        if (selectedDashboardTab == 1) {
            DistributorsManagementScreen(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        } else if (selectedDashboardTab == 2) {
            NetworkIdentityScreen(
                viewModel = viewModel,
                onNavigateToAi = { onNavigateToTab(5) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Welcome Hero Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MikroTikNavy),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "مرحباً بك، ${currentUser?.fullName ?: "مدير الشبكة"}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "${networkIdentity.networkName} • رينج ${networkIdentity.approvedDeviceSubnet}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MikroTikPrimary.copy(alpha = 0.25f))
                                            .clickable { selectedDashboardTab = 1 }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Settings, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "تعديل الهوية",
                                                color = MikroTikCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StatusOnline.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "مستقر 100%",
                                            color = StatusOnline,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                    Spacer(modifier = Modifier.height(14.dp))

                    // IP Anti-Collision Security Widget
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF172A45))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MikroTikCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "نظام الحماية من تعارض الآي بي (IP Conflict Guard)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "جميع عناوين الـ IP (${devices.size} جهاز) مفحوصة ومؤمنة دون أي تكرار",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Firebase Cloud Database Status Banner
                    val syncStatus by viewModel.syncStatus.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D2137))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (currentUser?.email?.isNotBlank() == true)
                                            "سحابة Firebase: ${currentUser?.email}"
                                        else
                                            "قاعدة بيانات Firebase (sam-mikrotic)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(StatusOnline.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (currentUser?.isGoogleUser == true) "Google Auth" else "مفعلة",
                                            color = StatusOnline,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = syncStatus,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.triggerCloudSync() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "مزامنة سحابية",
                                tint = MikroTikCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Google Cloud Account & Remote Backup Card (Matching the modern native security & backup card)
        item {
            val googleSignInState by viewModel.googleSignInState.collectAsState()
            val isConnected = currentUser?.isGoogleUser == true || !googleSignInState.activeFirebaseEmail.isNullOrBlank()
            val activeEmail = currentUser?.email ?: googleSignInState.activeFirebaseEmail ?: ""

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, if (isConnected) Color(0xFF10B981) else Color(0xFF1E3A8A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_google_cloud_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row: Title + Subtitle + Google Logo Circle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحساب السحابي والنسخ الاحتياطي",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "اربط جهازك بحساب Google لتأمين ونسخ بياناتك وسجلاتك سحابياً",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isConnected) {
                        // Warning Notice Box (Modeled directly after the reference screenshot)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF450A0A).copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تنبيه هام ⚠️: إذا لم تقم بتسجيل الدخول بحساب Google، فلن تتمكن من مزامنة كروت الشبكة، البقالات، وسجلات المحاسبة سحابياً ومتابعتها عن بُعد!",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full-Width Google Sign-In Button (Primary 1-Tap Trigger)
                        Button(
                            onClick = onSignInWithGoogle,
                            enabled = !googleSignInState.isLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_google_one_tap_dashboard")
                        ) {
                            if (googleSignInState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جاري الاتصال بخدمات Google...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول بحساب Google",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // Connected State: Real-Time Synced Google Account View
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "متصل بحساب Google ومزامن سحابياً ✅",
                                        color = Color(0xFFA7F3D0),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = activeEmail,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerCloudSync() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مزامنة سحابية الآن",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onSignOutGoogle,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                                border = BorderStroke(1.dp, Color(0xFFF87171)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تسجيل الخروج", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // Production Environment Initialization & Factory Reset Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1917)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PaymentRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("production_reset_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PaymentRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = PaymentRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تهيئة بيئة العمل الفعلية",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = PaymentRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (devices.isEmpty() && availableCards == 0 && retailers.isEmpty()) "مهيأ للعمل الفعلي ✅" else "بيانات تجريبية نشطة ⚠️",
                                        color = if (devices.isEmpty() && availableCards == 0 && retailers.isEmpty()) StatusOnline else PaymentRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "حذف كل البيانات الافتراضية وتصفير الكروت والسندات لبدء تشغيل شبكتك الخاصة",
                                color = Color(0xFFA8A29E),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onResetToProduction,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("dashboard_reset_production_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تهيئة التطبيق وحذف البيانات الافتراضية للعمل الفعلي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 4 KPI Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "أجهزة الشبكة",
                        value = "${devices.size} أجهزة",
                        subtitle = "$onlineDevicesCount متصل أونلاين",
                        color = MikroTikPrimary,
                        icon = Icons.Default.Router,
                        onClick = { onNavigateToTab(1) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "كروت المستودع",
                        value = "$availableCards كرت",
                        subtitle = "جاهزة للتوزيع",
                        color = StatusOnline,
                        icon = Icons.Default.ConfirmationNumber,
                        onClick = { onNavigateToTab(2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        title = "نقاط التوزيع (البقالات)",
                        value = "${retailers.size} بقالة",
                        subtitle = "ديون: ${totalDebt.toInt()} ريال",
                        color = StatusWarning,
                        icon = Icons.Default.Store,
                        onClick = { onNavigateToTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "السندات المالية",
                        value = "${vouchers.size} سندات",
                        subtitle = "حركات قبض وصرف",
                        color = ReceiptGreen,
                        icon = Icons.Default.Receipt,
                        onClick = { onNavigateToTab(4) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Authorized Distributors & Field Sales Team Overview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D223A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedDashboardTab = 1 }
                    .testTag("dashboard_distributors_overview_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "فريق الموزعين ونقاط التوزيع الميدانية",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "إضافة وتفويض الموزعين عبر البريد الإلكتروني لتوزيع الكروت والبقالات",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${distributors.size} موزع معتمد",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("الموزعين النشطين", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = "${distributors.count { it.isActive }} موزع",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitEmerald
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("نقاط البيع (البقالات)", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = "${retailers.size} بقالة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusWarning
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("الصلاحية الممنوحة", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = "توزيع + بقالات + سندات",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "إدارة فريق الموزعين وإضافة موزع جديد ←",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Investment, Partners & CAPEX Project Overview Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1929)),
                border = androidx.compose.foundation.BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(4) }
                    .testTag("dashboard_investment_overview_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(InvestmentGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = InvestmentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "نظام الاستثمار والشركاء والأصول",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "إدارة حصص الشركاء، أصول الشبكة (CAPEX)، والأرباح",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ProfitEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "مشروع استثماري",
                                color = ProfitEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Partner Capital
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("رأس المال", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%,.0f", totalInvestedCapital ?: 0.0)} ر.ي",
                                    color = InvestmentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Fixed Assets (CAPEX)
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, contentDescription = null, tint = AssetPurple, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("الأصول (${assets.size})", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%,.0f", totalAssetPurchaseCost ?: 0.0)} ر.ي",
                                    color = AssetPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Net Profit
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("صافي الربح", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%,.0f", netProfitVal)} ر.ي",
                                    color = if (netProfitVal >= 0) ProfitEmerald else PaymentRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }


        // Network GIS Map Card
        item {
            val mappedDevicesCount = devices.count { it.latitude != 0.0 && it.longitude != 0.0 }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F243A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(1) }
                    .testTag("dashboard_gis_map_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D9488).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CellTower,
                            contentDescription = null,
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "خريطة الأبراج والتغطية اللاسلكية GIS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0D9488).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "خريطة تفاعلية",
                                    color = Color(0xFF2DD4BF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$mappedDevicesCount أبراج وسواري محددة على الخريطة من أصل ${devices.size} جهاز",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D9488),
                        modifier = Modifier.clickable { onNavigateToTab(1) }
                    ) {
                        Text(
                            text = "عرض 🗺️",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = "الإجراءات والعمليات السريعة",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "إضافة جهاز وفحص IP",
                    icon = Icons.Default.Lan,
                    color = MikroTikPrimary,
                    onClick = { onNavigateToTab(1) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "توليد كروت جديدة",
                    icon = Icons.Default.ConfirmationNumber,
                    color = StatusOnline,
                    onClick = { onNavigateToTab(2) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "تسليم كروت لبقالة",
                    icon = Icons.Default.Send,
                    color = StatusDistributed,
                    onClick = { onNavigateToTab(3) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "مستشار سام AI",
                    icon = Icons.Default.AutoAwesome,
                    color = Color(0xFF8B5CF6),
                    onClick = { onNavigateToTab(5) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Vouchers Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر السندات المالية المقيدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "عرض الكل",
                    fontSize = 12.sp,
                    color = MikroTikPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToTab(4) }
                )
            }
        }

        if (vouchers.isEmpty()) {
            item {
                Text(text = "لا توجد حركات مالية مسجلة بعد", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            items(vouchers.take(4), key = { it.id }) { voucher ->
                val isReceipt = voucher.voucherType == "RECEIPT"
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTab(4) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isReceipt) ReceiptGreen.copy(alpha = 0.15f) else PaymentRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isReceipt) Icons.Default.LocalAtm else Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (isReceipt) ReceiptGreen else PaymentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = voucher.partyName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${voucher.voucherNumber} • ${voucher.category}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = "${if (isReceipt) "+" else "-"}${voucher.amount.toInt()} ريال",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isReceipt) ReceiptGreen else PaymentRed
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
    }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}
