package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RetailerEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ReceiptGreen
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning

@Composable
fun DistributionScreen(
    viewModel: MainViewModel,
    onNavigateToVouchers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val retailers by viewModel.retailers.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()
    val context = LocalContext.current

    var showAddRetailerDialog by remember { mutableStateOf(false) }
    var showDistributeDialog by remember { mutableStateOf(false) }
    var selectedRetailerForDistribution by remember { mutableStateOf<RetailerEntity?>(null) }
    var showQuickPayDialog by remember { mutableStateOf<RetailerEntity?>(null) }
    var retailerToDelete by remember { mutableStateOf<RetailerEntity?>(null) }

    val totalDebt = retailers.sumOf { it.balanceOwed }
    val totalActiveCardsWithRetailers = retailers.sumOf { it.activeCardsCount }

    Box(modifier = modifier.fillMaxSize().testTag("distribution_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "توزيع الكروت للمحلات والبقالات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "متابعة نقاط البيع، تسليم الدفعات، وأرصدة المديونية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        selectedRetailerForDistribution = retailers.firstOrNull()
                        showDistributeDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = retailers.isNotEmpty() && inventoryItems.isNotEmpty()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسليم كروت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Debt & Cards Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "إجمالي ديون المحلات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${totalDebt.toInt()} ريال",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MikroTikPrimary.copy(alpha = 0.12f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "كروت بحوزة البقالات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalActiveCardsWithRetailers كرت",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MikroTikPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Retailers List
            if (retailers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "لا توجد بقالات مسجلة بعد. اضغط إضافة بقالة للبدء.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(retailers, key = { it.id }) { retailer ->
                        RetailerCard(
                            retailer = retailer,
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${retailer.phone}"))
                                context.startActivity(intent)
                            },
                            onDistribute = {
                                selectedRetailerForDistribution = retailer
                                showDistributeDialog = true
                            },
                            onQuickPay = {
                                showQuickPayDialog = retailer
                            },
                            onDelete = {
                                retailerToDelete = retailer
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // FAB to Add Retailer
        FloatingActionButton(
            onClick = { showAddRetailerDialog = true },
            containerColor = MikroTikPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_retailer_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة بقالة")
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة بقالة", fontWeight = FontWeight.Bold)
            }
        }

        // Add Retailer Dialog
        if (showAddRetailerDialog) {
            AddRetailerDialog(
                onDismiss = { showAddRetailerDialog = false },
                onSave = { newRetailer ->
                    viewModel.saveRetailer(newRetailer) {
                        showAddRetailerDialog = false
                        Toast.makeText(context, "تمت إضافة البقالة بنجاح ✓", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Distribute Cards Dialog
        if (showDistributeDialog) {
            DistributeCardsDialog(
                retailers = retailers,
                inventoryItems = inventoryItems,
                initialRetailer = selectedRetailerForDistribution,
                onDismiss = { showDistributeDialog = false },
                onConfirm = { inventoryId, retailerId, qty ->
                    viewModel.distributeFromInventory(inventoryId, retailerId, qty) { success ->
                        showDistributeDialog = false
                        if (success) {
                            Toast.makeText(context, "تم تسليم $qty كرت للبقالة وقيد المبلغ على الحساب ✓", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "الكمية المطلوبة غير متوفرة في المخزن!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Quick Payment / Receipt Voucher Dialog
        if (showQuickPayDialog != null) {
            val r = showQuickPayDialog!!
            QuickPaymentDialog(
                retailer = r,
                onDismiss = { showQuickPayDialog = null },
                onConfirm = { amount, method, desc ->
                    viewModel.createVoucher(
                        voucherType = "RECEIPT",
                        amount = amount,
                        partyName = r.name,
                        retailerId = r.id,
                        category = "توريد مبيعات كروت",
                        paymentMethod = method,
                        description = desc
                    ) {
                        showQuickPayDialog = null
                        Toast.makeText(context, "تم إنشاء سند قبض بمبلغ $amount ريال وتخفيض مديونية البقالة ✓", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (retailerToDelete != null) {
            AlertDialog(
                onDismissRequest = { retailerToDelete = null },
                title = { Text("تأكيد حذف البقالة") },
                text = { Text("هل أنت متأكد من حذف (${retailerToDelete?.name})؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            retailerToDelete?.let { viewModel.deleteRetailer(it) }
                            retailerToDelete = null
                            Toast.makeText(context, "تم حذف البقالة", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOffline)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { retailerToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun RetailerCard(
    retailer: RetailerEntity,
    onCall: () -> Unit,
    onDistribute: () -> Unit,
    onQuickPay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("retailer_card_${retailer.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top: Store Name & Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MikroTikPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = retailer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "المسؤول: ${retailer.ownerName} • عمولة ${retailer.commissionPercent.toInt()}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Balance Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الرصيد المستحق:",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${retailer.balanceOwed.toInt()} ريال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (retailer.balanceOwed > 0) StatusWarning else StatusOnline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location & Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = retailer.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (retailer.phone.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onCall() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "اتصال", tint = StatusOnline, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = retailer.phone,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row (Cards held, Total paid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الكروت بحوزته: ${retailer.activeCardsCount} كرت",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "إجمالي المسدد: ${retailer.totalPaid.toInt()} ريال",
                    fontSize = 11.sp,
                    color = ReceiptGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onQuickPay,
                        colors = ButtonDefaults.buttonColors(containerColor = ReceiptGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.LocalAtm, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سداد / قبض", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDistribute,
                        colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسليم كروت", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddRetailerDialog(
    onDismiss: () -> Unit,
    onSave: (RetailerEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var commission by remember { mutableStateOf("10") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة بقالة أو نقطة بيع جديدة", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم البقالة / المحل *") },
                        placeholder = { Text("مثال: بقالة النصر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("retailer_name_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("اسم المسؤول / التاجر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف / الواتساب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("الموقع / الشارع") },
                        placeholder = { Text("مثال: حي الصافية - جوار الجامع") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = commission,
                        onValueChange = { commission = it },
                        label = { Text("نسبة العمولة (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = RetailerEntity(
                        name = name.trim(),
                        ownerName = ownerName.trim(),
                        phone = phone.trim(),
                        location = location.trim(),
                        commissionPercent = commission.toDoubleOrNull() ?: 10.0,
                        notes = notes.trim()
                    )
                    onSave(entity)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Text("حفظ البقالة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributeCardsDialog(
    retailers: List<RetailerEntity>,
    inventoryItems: List<com.example.data.local.entity.InventoryItemEntity>,
    initialRetailer: RetailerEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Int) -> Unit
) {
    var selectedRetailer by remember { mutableStateOf(initialRetailer ?: retailers.firstOrNull()) }
    var selectedItem by remember { mutableStateOf(inventoryItems.firstOrNull()) }
    var quantityText by remember { mutableStateOf("20") }

    var isRetailerExpanded by remember { mutableStateOf(false) }
    var isItemExpanded by remember { mutableStateOf(false) }

    val qty = quantityText.toIntOrNull() ?: 0
    val wholesalePrice = selectedItem?.wholesalePrice ?: 0.0
    val totalAmount = qty * wholesalePrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسليم كروت للبقالة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Select Retailer Dropdown
                ExposedDropdownMenuBox(
                    expanded = isRetailerExpanded,
                    onExpandedChange = { isRetailerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRetailer?.name ?: "اختر البقالة",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("البقالة المستلمة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRetailerExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isRetailerExpanded,
                        onDismissRequest = { isRetailerExpanded = false }
                    ) {
                        retailers.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name) },
                                onClick = {
                                    selectedRetailer = r
                                    isRetailerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Select Inventory Item Dropdown
                ExposedDropdownMenuBox(
                    expanded = isItemExpanded,
                    onExpandedChange = { isItemExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.packageName ?: "اختر الباقة",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("باقة الكروت (المخزن)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isItemExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isItemExpanded,
                        onDismissRequest = { isItemExpanded = false }
                    ) {
                        inventoryItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.packageName} - جملة ${item.wholesalePrice.toInt()} ريال (متاح: ${item.quantityAvailable})") },
                                onClick = {
                                    selectedItem = item
                                    isItemExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المسلمة (عدد الكروت)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Summary calculation
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "الحسبة المالية للدين:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$qty كرت × ${wholesalePrice.toInt()} ريال = ${totalAmount.toInt()} ريال",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                        Text(
                            text = "سيتم إضافة المبلغ إلى حساب مديونية البقالة تلقائياً.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedItem != null && selectedRetailer != null && qty > 0) {
                        onConfirm(selectedItem!!.id, selectedRetailer!!.id, qty)
                    }
                },
                enabled = selectedItem != null && selectedRetailer != null && qty > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Text("تأكيد التسليم والقيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun QuickPaymentDialog(
    retailer: RetailerEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(retailer.balanceOwed.toInt().toString()) }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var description by remember { mutableStateOf("سداد قيمة كروت هوتسبوت مباعة") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سند قبض فوري من ${retailer.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الرصيد المستحق حالياً: ${retailer.balanceOwed.toInt()} ريال",
                    color = StatusWarning,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المقبوض (ريال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = { Text("طريقة الدفع (نقداً / حوالة / بنك)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("البيان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt, paymentMethod, description)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ReceiptGreen)
            ) {
                Text("إصدار سند القبض")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
