package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.data.repository.NetworkRepository
import com.example.ui.GoogleSignInUiState
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning

@Composable
fun TopNavBar(
    currentUser: UserEntity?,
    allUsers: List<UserEntity>,
    syncStatus: String,
    googleSignInState: GoogleSignInUiState,
    onSelectUser: (UserEntity) -> Unit,
    onTriggerSync: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onGoogleSignOutClick: () -> Unit,
    onResetToProductionClick: () -> Unit = {}
) {
    var showUserMenu by remember { mutableStateOf(false) }

    Surface(
        color = MikroTikNavy,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_nav_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Brand matching original site "سام تك - شبكة طلقة نت"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "سام تك",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "سام تك",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val isSuperAdmin = NetworkRepository.isSuperAdminEmail(currentUser?.email)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    isSuperAdmin -> Color(0xFF1E3A8A)
                                    currentUser?.role == "DISTRIBUTOR" -> Color(0xFF3B2D05)
                                    else -> Color(0xFF0F2D4A)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isSuperAdmin -> "المدير العام الأعلى 👑"
                                        currentUser?.role == "DISTRIBUTOR" -> "بوابة الموزع 🛵"
                                        currentUser?.role == "RETAILER" -> "نقطة بيع 🛒"
                                        currentUser?.role == "ENGINEER" -> "مهندس شبكة 🛠️"
                                        else -> "لوحة المالك 👑"
                                    },
                                    color = when {
                                        isSuperAdmin -> Color(0xFF67E8F9)
                                        currentUser?.role == "DISTRIBUTOR" -> Color(0xFFFBBF24)
                                        else -> Color(0xFF38BDF8)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (currentUser?.role == "DISTRIBUTOR") "بوابة توزيع الكروت وإدارة البقالات" else "شبكة طلقة نت • نظام إدارة الشبكات",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Actions: Cloud Sync & User Profile / Google Sign-In & Factory Reset
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Production Reset Icon Button (Only visible for Owner/Admin)
                    if (currentUser?.role != "DISTRIBUTOR") {
                        IconButton(
                            onClick = onResetToProductionClick,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D1219))
                                .testTag("reset_production_navbar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "تهيئة بيئة العمل الفعلية",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Sync Button
                    IconButton(
                        onClick = onTriggerSync,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "مزامنة سحابية",
                            tint = MikroTikCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick Google One-Tap Sign-In Button (Prominent when not connected to Google)
                    if (currentUser?.isGoogleUser != true) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onGoogleSignInClick() }
                                .padding(end = 6.dp)
                                .testTag("quick_google_signin_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "دخول جوجل",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // User Selector Pill with Google Account integration
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (currentUser?.isGoogleUser == true) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                )
                                .clickable { showUserMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("user_role_selector")
                        ) {
                            if (googleSignInState.isLoading) {
                                CircularProgressIndicator(
                                    color = MikroTikCyan,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (currentUser?.isGoogleUser == true) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "حساب جوجل",
                                    tint = MikroTikCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "المستخدم",
                                    tint = StatusOnline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (currentUser?.isGoogleUser == true) {
                                    currentUser.fullName.ifBlank { currentUser.email.substringBefore("@") }
                                } else {
                                    currentUser?.fullName?.split(" ")?.take(2)?.joinToString(" ") ?: "المستخدم"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF1A2744))
                                .testTag("user_dropdown_menu")
                        ) {
                            // Section 1: One-Tap Google Sign-In option (requested by user)
                            if (currentUser?.isGoogleUser == true) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(StatusOnline)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "متصل بحساب جوجل",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = currentUser.email,
                                                color = MikroTikCyan,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "معرّف المزامنة السحابية: ${currentUser.email}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                    },
                                    onClick = { }
                                )

                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ExitToApp,
                                            contentDescription = null,
                                            tint = StatusWarning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "تسجيل الخروج من جوجل",
                                            color = StatusWarning,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        onGoogleSignOutClick()
                                        showUserMenu = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = MikroTikCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = {
                                        Column {
                                            Text(
                                                text = "تسجيل الدخول عبر جوجل (نقرة واحدة)",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "جعل البريد هو معرّف المزامنة السحابية في Firebase",
                                                color = MikroTikCyan,
                                                fontSize = 10.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        onGoogleSignInClick()
                                        showUserMenu = false
                                    },
                                    modifier = Modifier.testTag("menu_google_signin_button")
                                )
                            }

                            HorizontalDivider(
                                color = Color(0xFF2E4064),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Section 2: Switch between local roles/users
                            Text(
                                text = "أو اختر دور محلي:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )

                            allUsers.forEach { user ->
                                val isSelected = user.id == currentUser?.id
                                DropdownMenuItem(
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MikroTikCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    text = {
                                        Column {
                                            Text(
                                                text = user.fullName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MikroTikCyan else Color.White,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = when {
                                                    NetworkRepository.isSuperAdminEmail(user.email) -> "👑 المدير العام والمسؤول الأعلى (Super Admin)"
                                                    user.role == "OWNER" -> "👑 مالك ومدير الشبكة"
                                                    user.role == "ENGINEER" -> "🛠️ مهندس الشبكة"
                                                    user.role == "DISTRIBUTOR" -> "🚚 موزع كروت وكاشير"
                                                    user.role == "RETAILER" -> "🏪 بقالة ونقطة بيع"
                                                    else -> user.role
                                                },
                                                fontSize = 11.sp,
                                                color = if (NetworkRepository.isSuperAdminEmail(user.email)) Color(0xFFFDE047) else Color(0xFF94A3B8),
                                                fontWeight = if (NetworkRepository.isSuperAdminEmail(user.email)) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (user.email.isNotBlank()) {
                                                Text(
                                                    text = user.email,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF38BDF8)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectUser(user)
                                        showUserMenu = false
                                    }
                                )
                            }

                            HorizontalDivider(
                                color = Color(0xFF2E4064),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Section 3: Reset to Production Environment
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = PaymentRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = "تهيئة بيئة العمل الفعلية 🚀",
                                            color = PaymentRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "حذف الكروت والسندات والأجهزة الافتراضية",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showUserMenu = false
                                    onResetToProductionClick()
                                },
                                modifier = Modifier.testTag("menu_reset_production_button")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sync status line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1E33))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusOnline)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = syncStatus,
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
