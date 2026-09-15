package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.StatusOnline
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
    val context = LocalContext.current

    var selectedCategoryFilter by remember { mutableStateOf("الكل") }
    var showAddAssetDialog by remember { mutableStateOf(false) }
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

        // Add Asset FAB
        FloatingActionButton(
            onClick = {
                assetToEdit = null
                showAddAssetDialog = true
            },
            containerColor = AssetPurple,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_asset")
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة أصل جديد")
        }
    }

    // Add / Edit Asset Dialog
    if (showAddAssetDialog) {
        AddEditAssetDialog(
            assetToEdit = assetToEdit,
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
    onDismiss: () -> Unit,
    onSave: (NetworkAssetEntity) -> Unit
) {
    var name by remember { mutableStateOf(assetToEdit?.assetName ?: "") }
    var category by remember { mutableStateOf(assetToEdit?.category ?: "SERVERS") }
    var purchaseCostText by remember { mutableStateOf(assetToEdit?.purchaseCost?.toInt()?.toString() ?: "") }
    var currentValueText by remember { mutableStateOf(assetToEdit?.estimatedCurrentValue?.toInt()?.toString() ?: "") }
    var location by remember { mutableStateOf(assetToEdit?.location ?: "") }
    var serialNumber by remember { mutableStateOf(assetToEdit?.serialNumber ?: "") }
    var status by remember { mutableStateOf(assetToEdit?.status ?: "ACTIVE") }
    var notes by remember { mutableStateOf(assetToEdit?.notes ?: "") }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val categoryMap = listOf(
        "SERVERS" to "سيرفرات وراوترات ومفاتيح ميكروتك",
        "TOWERS" to "أبراج وهوائيات وسيكتورات",
        "SOLAR_POWER" to "منظومة طاقة شمسية وبطاريات ومولدات",
        "FIBER_CABLES" to "كابلات ألياف بصرية وفايبر",
        "OTHER" to "أدوات ومعدات أخرى"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (assetToEdit == null) "إضافة أصل رأسمالي جديد" else "تعديل بيانات الأصل",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchaseCostText,
                        onValueChange = {
                            purchaseCostText = it
                            if (currentValueText.isEmpty()) currentValueText = it
                        },
                        label = { Text("سعر الشراء (ريال) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_asset_cost")
                    )

                    OutlinedTextField(
                        value = currentValueText,
                        onValueChange = { currentValueText = it },
                        label = { Text("القيمة الحالية (ريال)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cost = purchaseCostText.toDoubleOrNull() ?: 0.0
                        val currentVal = currentValueText.toDoubleOrNull() ?: cost
                        val asset = assetToEdit?.copy(
                            assetName = name,
                            category = category,
                            purchaseCost = cost,
                            estimatedCurrentValue = currentVal,
                            location = location,
                            serialNumber = serialNumber,
                            notes = notes
                        ) ?: NetworkAssetEntity(
                            assetName = name,
                            category = category,
                            purchaseCost = cost,
                            estimatedCurrentValue = currentVal,
                            location = location,
                            serialNumber = serialNumber,
                            notes = notes
                        )
                        onSave(asset)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AssetPurple),
                modifier = Modifier.testTag("btn_save_asset")
            ) {
                Text("حفظ الأصل", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
