package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.model.ParsedInvoiceData
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.StatusOnline
import com.example.util.CurrencyHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssetsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val assets by viewModel.assets.collectAsState()
    val totalPurchaseCost by viewModel.totalAssetPurchaseCost.collectAsState()
    val totalCurrentAssetValue by viewModel.totalCurrentAssetValue.collectAsState()
    val networkIdentity by viewModel.networkIdentity.collectAsState()
    val context = LocalContext.current

    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var showInvoiceScannerDialog by remember { mutableStateOf(false) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var parsedInvoiceForAssets by remember { mutableStateOf<ParsedInvoiceData?>(null) }
    var assetToEdit by remember { mutableStateOf<NetworkAssetEntity?>(null) }
    var assetToDelete by remember { mutableStateOf<NetworkAssetEntity?>(null) }

    val categories = listOf("الكل", "سيرفرات وراوترات", "أبراج وهوائيات", "طاقة شمسية وبطاريات", "كابلات وألياف", "أخرى")

    val filteredAssets = assets.filter { asset ->
        when (selectedCategoryFilter) {
            "سيرفرات وراوترات" -> asset.category == "SERVERS"
            "أبراج وهوائيات" -> asset.category == "TOWERS"
            "طاقة شمسية وبطاريات" -> asset.category == "SOLAR_POWER"
            "كابلات وألياف" -> asset.category == "FIBER_CABLES"
            "أخرى" -> asset.category == "OTHER"
            else -> true
        }
    }

    val costTotal = totalPurchaseCost ?: 0.0
    val currentValTotal = totalCurrentAssetValue ?: 0.0
    val totalDepreciation = (costTotal - currentValTotal).coerceAtLeast(0.0)

    Box(modifier = modifier.fillMaxSize().testTag("assets_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Hero Card
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
                            Column {
                                Text(
                                    text = "سجل الأصول الرأسمالية (CAPEX)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "حصر أجهزة ومعدات وأبراج ومنظومات طاقة الشبكة",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AssetPurple.copy(alpha = 0.25f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${assets.size} أصل ثابت",
                                    color = AssetPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total Purchase Cost
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("تكلفة الشراء الأصلية", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", costTotal)} ر.ي",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Current Estimated Value
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("القيمة السوقية الحالية", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", currentValTotal)} ر.ي",
                                        color = ProfitEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Depreciation
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("مجموع الإهلاك التقديري", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", totalDepreciation)} ر.ي",
                                        color = InvestmentGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI & JSON Invoice Scanning Quick Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AssetPurple.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AssetPurple.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AssetPurple.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "إدخال وتفريغ فواتير الأصول والمعدات",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontFamily = CairoFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = InvestmentGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "صور الفاتورة بالذكاء الاصطناعي أو استورد ملف JSON لتوفير رصيد التوكن",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = CairoFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    parsedInvoiceForAssets = null
                                    showInvoiceScannerDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AssetPurple),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("scan_asset_invoice_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسح بالذكاء 📸", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFontFamily)
                            }

                            Button(
                                onClick = { showJsonImportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(38.dp)
                                    .testTag("import_asset_json_button")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استيراد JSON (بدون توكن) ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = CairoFontFamily)
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) AssetPurple else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedCategoryFilter = cat }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Assets List
            if (filteredAssets.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد أصول مسجلة في هذا التصنيف", fontWeight = FontWeight.Bold)
                            Text(
                                text = "استخدم زر (+) لإضافة الأجهزة والمعدات لحساب أصول الشبكة بدقة.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredAssets, key = { it.id }) { asset ->
                    AssetCard(
                        asset = asset,
                        onClone = {
                            assetToEdit = asset.copy(
                                id = 0L,
                                assetName = "${asset.assetName} (نسخة)",
                                serialNumber = "",
                                purchaseDateMillis = System.currentTimeMillis()
                            )
                            showAddAssetDialog = true
                            Toast.makeText(context, "تم استنساخ الأصل. قم بتعديل التفاصيل ثم احفظ", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = {
                            assetToEdit = asset
                            showAddAssetDialog = true
                        },
                        onDelete = {
                            assetToDelete = asset
                        },
                        onShare = {
                            val shareText = """
                                🛠 كشف أصل رأسمالي لشبكة الوايرلس:
                                🏷 الاسم: ${asset.assetName}
                                🏢 التصنيف: ${asset.category}
                                💵 تكلفة الشراء: ${String.format(Locale.US, "%,.0f", asset.purchaseCost)} ريال
                                📉 القيمة الدفترية الحالية: ${String.format(Locale.US, "%,.0f", asset.estimatedCurrentValue)} ريال
                                📍 الموقع: ${asset.location.ifBlank { "غير محدد" }}
                                🔢 السيريال / الموديل: ${asset.serialNumber.ifBlank { "غير متوفر" }}
                                ⚡ الحالة: ${if (asset.status == "ACTIVE") "يعمل بكفاءة" else "صيانة / مستهلك"}
                                📝 ملاحظات: ${asset.notes.ifBlank { "أصل مسجل ومعتمد" }}
                            """.trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة بيانات الأصل"))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Dual Action Floating Buttons: Scan Invoice (AI) & Add Asset
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { showInvoiceScannerDialog = true },
                containerColor = MikroTikPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_scan_asset_invoice")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسح فاتورة (AI)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            FloatingActionButton(
                onClick = {
                    assetToEdit = null
                    showAddAssetDialog = true
                },
                containerColor = AssetPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_asset")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة أصل يدوي")
            }
        }
    }

    // Smart Invoice AI / JSON Scanner Dialog
    if (showInvoiceScannerDialog) {
        SmartInvoiceScannerDialog(
            viewModel = viewModel,
            initialTargetType = "ASSETS",
            initialParsedData = parsedInvoiceForAssets,
            onDismissRequest = {
                showInvoiceScannerDialog = false
                parsedInvoiceForAssets = null
            }
        )
    }

    // Direct JSON Invoice Import Dialog
    if (showJsonImportDialog) {
        JsonInvoiceImportDialog(
            onDismissRequest = { showJsonImportDialog = false },
            onInvoiceImported = { importedData ->
                parsedInvoiceForAssets = importedData
                showInvoiceScannerDialog = true
                showJsonImportDialog = false
            }
        )
    }

    // Add / Edit Asset Dialog
    if (showAddAssetDialog) {
        AddEditAssetDialog(
            assetToEdit = assetToEdit,
            sarToYerRate = if (networkIdentity.sarToYerRate > 0) networkIdentity.sarToYerRate else 430.0,
            usdToYerRate = if (networkIdentity.usdToYerRate > 0) networkIdentity.usdToYerRate else 1630.0,
            onDismiss = { showAddAssetDialog = false },
            onSave = { asset ->
                viewModel.saveAsset(asset) {
                    Toast.makeText(context, "تم حفظ الأصل الرأسمالي بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddAssetDialog = false
            }
        )
    }

    // Delete Asset Dialog
    if (assetToDelete != null) {
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            title = { Text("حذف الأصل من السجل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف الأصل (${assetToDelete?.assetName})؟") },
            confirmButton = {
                Button(
                    onClick = {
                        assetToDelete?.let { viewModel.deleteAsset(it) }
                        assetToDelete = null
                        Toast.makeText(context, "تم حذف الأصل بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                ) {
                    Text("تأكيد الحذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AssetCard(
    asset: NetworkAssetEntity,
    onClone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val categoryIcon = when (asset.category) {
        "SERVERS" -> Icons.Default.Router
        "TOWERS" -> Icons.Default.Devices
        "SOLAR_POWER" -> Icons.Default.SolarPower
        "FIBER_CABLES" -> Icons.Default.Cable
        else -> Icons.Default.Build
    }

    val isOnline = asset.status == "ACTIVE"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AssetPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon, contentDescription = null, tint = AssetPurple, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = asset.assetName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (asset.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = asset.location,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Status chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOnline) StatusOnline.copy(alpha = 0.15f) else PaymentRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isOnline) "يعمل بكفاءة" else "تحت الصيانة",
                        color = if (isOnline) StatusOnline else PaymentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("سعر الشراء", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", asset.purchaseCost)} ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (asset.currency.isNotBlank() && asset.currency != "YER" && asset.originalCost > 0) {
                        Text(
                            text = CurrencyHelper.formatAmount(asset.originalCost, asset.currency),
                            fontSize = 10.sp,
                            color = InvestmentGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("القيمة الحالية (بعد الإهلاك)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", asset.estimatedCurrentValue)} ر.ي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ProfitEmerald
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("تاريخ الشراء", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(asset.purchaseDateMillis)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (asset.notes.isNotBlank() || asset.serialNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${if (asset.serialNumber.isNotBlank()) "S/N: ${asset.serialNumber} • " else ""}${asset.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MikroTikCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClone, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "استنساخ الأصل", tint = InvestmentGold, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = PaymentRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetDialog(
    assetToEdit: NetworkAssetEntity?,
    sarToYerRate: Double,
    usdToYerRate: Double,
    onDismiss: () -> Unit,
    onSave: (NetworkAssetEntity) -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }
    var name by remember(assetToEdit) { mutableStateOf(assetToEdit?.assetName ?: "") }
    var category by remember(assetToEdit) { mutableStateOf(assetToEdit?.category ?: "SERVERS") }
    var selectedCurrency by remember(assetToEdit) { mutableStateOf(assetToEdit?.currency?.ifBlank { "YER" } ?: "YER") }

    val initialCost = if (assetToEdit != null && assetToEdit.currency != "YER" && assetToEdit.originalCost > 0) {
        assetToEdit.originalCost
    } else {
        assetToEdit?.purchaseCost ?: 0.0
    }
    var purchaseCostText by remember(assetToEdit) { mutableStateOf(if (initialCost > 0) initialCost.toInt().toString() else "") }
    var currentValueText by remember(assetToEdit) { mutableStateOf(assetToEdit?.estimatedCurrentValue?.toInt()?.toString() ?: "") }
    var location by remember(assetToEdit) { mutableStateOf(assetToEdit?.location ?: "") }
    var serialNumber by remember(assetToEdit) { mutableStateOf(assetToEdit?.serialNumber ?: "") }
    var notes by remember(assetToEdit) { mutableStateOf(assetToEdit?.notes ?: "") }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val categoryMap = listOf(
        "SERVERS" to "سيرفرات وراوترات ومفاتيح ميكروتك",
        "TOWERS" to "أبراج وهوائيات وسيكتورات",
        "SOLAR_POWER" to "منظومة طاقة شمسية وبطاريات ومولدات",
        "FIBER_CABLES" to "كابلات ألياف بصرية وفايبر",
        "OTHER" to "أدوات ومعدات أخرى"
    )

    // Calculate converted cost in YER
    val rawEnteredCost = purchaseCostText.toDoubleOrNull() ?: 0.0
    val equivalentYerCost = CurrencyHelper.convertToYer(rawEnteredCost, selectedCurrency, sarToYerRate, usdToYerRate)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AssetPurple.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AssetPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = AssetPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (assetToEdit == null || assetToEdit.id == 0L) {
                                    if (assetToEdit?.id == 0L) "استنساخ وتسجيل أصل جديد" else "إضافة أصل رأسمالي جديد"
                                } else "تعديل بيانات الأصل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تسجيل الأصول الثابتة وحساب الإهلاك والقيمة الرأسمالية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الأصل والموديل *") },
                        placeholder = { Text("مثال: راوتر CCR2004، بطارية 200A...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_asset_name")
                    )

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = categoryMap.firstOrNull { it.first == category }?.second ?: category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تصنيف الأصل") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            categoryMap.forEach { (catKey, catLabel) ->
                                DropdownMenuItem(
                                    text = { Text(catLabel) },
                                    onClick = {
                                        category = catKey
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Currency selector chips
                    Column {
                        Text("عملة الشراء والفاتورة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                CurrencyHelper.CURRENCY_YER to "ريال يمني",
                                CurrencyHelper.CURRENCY_SAR to "ريال سعودي",
                                CurrencyHelper.CURRENCY_USD to "دولار أمريكي"
                            ).forEach { (cKey, cLabel) ->
                                val isSelected = selectedCurrency == cKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AssetPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            selectedCurrency = cKey
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = purchaseCostText,
                            onValueChange = {
                                purchaseCostText = it
                                val entered = it.toDoubleOrNull() ?: 0.0
                                val inYer = CurrencyHelper.convertToYer(entered, selectedCurrency, sarToYerRate, usdToYerRate)
                                if (currentValueText.isEmpty()) currentValueText = inYer.toInt().toString()
                            },
                            label = { Text("سعر الشراء (${CurrencyHelper.getCurrencySymbol(selectedCurrency)}) *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_asset_cost")
                        )

                        OutlinedTextField(
                            value = currentValueText,
                            onValueChange = { currentValueText = it },
                            label = { Text("القيمة الحالية (ريال يمني)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Currency conversion notice card
                    if (selectedCurrency != CurrencyHelper.CURRENCY_YER && rawEnteredCost > 0) {
                        val rate = if (selectedCurrency == CurrencyHelper.CURRENCY_USD) usdToYerRate else sarToYerRate
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = ProfitEmerald.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المعادل بالريال اليمني (سعر الصرف: $rate):",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%,.0f", equivalentYerCost)} ر.ي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ProfitEmerald
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("موقع التركيب (البرج / الغرفة)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serialNumber,
                        onValueChange = { serialNumber = it },
                        label = { Text("الرقم التسلسلي S/N (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات والضمان") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sticky Bottom Action Bar (ثابت دائماً في أسفل الشاشة ولا يختفي أبداً)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (!isSaving && name.isNotBlank()) {
                                isSaving = true
                                val entered = purchaseCostText.toDoubleOrNull() ?: 0.0
                                val costInYer = CurrencyHelper.convertToYer(entered, selectedCurrency, sarToYerRate, usdToYerRate)
                                val currentVal = currentValueText.toDoubleOrNull() ?: costInYer
                                val originalCost = if (selectedCurrency != CurrencyHelper.CURRENCY_YER) entered else 0.0

                                val asset = assetToEdit?.copy(
                                    assetName = name,
                                    category = category,
                                    purchaseCost = costInYer,
                                    estimatedCurrentValue = currentVal,
                                    currency = selectedCurrency,
                                    originalCost = originalCost,
                                    location = location,
                                    serialNumber = serialNumber,
                                    notes = notes
                                ) ?: NetworkAssetEntity(
                                    assetName = name,
                                    category = category,
                                    purchaseCost = costInYer,
                                    estimatedCurrentValue = currentVal,
                                    currency = selectedCurrency,
                                    originalCost = originalCost,
                                    location = location,
                                    serialNumber = serialNumber,
                                    notes = notes
                                )
                                onSave(asset)
                            }
                        },
                        enabled = !isSaving && name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AssetPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btn_save_asset")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الحفظ...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (assetToEdit != null && assetToEdit.id != 0L) "تحديث بيانات الأصل" else "حفظ الأصل الرأسمالي ✓",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
