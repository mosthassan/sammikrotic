package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.model.CardSalesInvoiceItem
import com.example.ui.theme.*
import java.util.UUID

/**
 * نافذة إصدار فاتورة مبيعات كروت احترافية متعددة الأصناف
 * - تدعم إضافة عدة أصناف في الفاتورة الواحدة
 * - تحسب عدد الكروت وسعر الكرت والإجمالي لكل صنف وللفاتورة ككل تلقائياً
 * - تتيح تحديد نوع السداد: نقد، آجل، أو مبلغ مقدم ومتبقي
 * - تخصم الكميات تلقائياً من مخزن الكروت وتسجل حركة بيع
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCardSalesInvoiceDialog(
    inventoryItems: List<InventoryItemEntity>,
    retailers: List<RetailerEntity>,
    preSelectedPackageName: String? = null,
    onDismiss: () -> Unit,
    onConfirmInvoice: (
        customerName: String,
        customerPhone: String,
        retailerId: Long?,
        items: List<CardSalesInvoiceItem>,
        paymentType: String,
        paidAmount: Double,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current

    // Customer Info
    var selectedRetailer by remember { mutableStateOf<RetailerEntity?>(retailers.firstOrNull()) }
    var isDirectCustomer by remember { mutableStateOf(false) }
    var customerNameText by remember { mutableStateOf(retailers.firstOrNull()?.name ?: "عميل مباشر") }
    var customerPhoneText by remember { mutableStateOf(retailers.firstOrNull()?.phone ?: "") }

    // Multi-Item Invoice Rows
    // Initial row
    val initialPackage = inventoryItems.find { it.packageName == preSelectedPackageName } ?: inventoryItems.firstOrNull()
    val initialItem = CardSalesInvoiceItem(
        id = UUID.randomUUID().toString(),
        packageName = initialPackage?.packageName ?: "باقة 200 ريال يومية",
        quantity = 50,
        unitPrice = initialPackage?.wholesalePrice ?: 180.0,
        retailPrice = initialPackage?.retailPrice ?: 200.0
    )

    var invoiceItems by remember { mutableStateOf(listOf(initialItem)) }

    // Payment Type: CASH (نقد), CREDIT (آجل), PARTIAL (مقدم ومتبقي)
    var paymentType by remember { mutableStateOf("CASH") }
    var customPaidAmountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    // Computed totals
    val totalInvoiceCards = invoiceItems.sumOf { it.quantity }
    val grandTotalAmount = invoiceItems.sumOf { it.lineTotal }

    val calculatedPaidAmount = when (paymentType) {
        "CASH" -> grandTotalAmount
        "CREDIT" -> 0.0
        "PARTIAL" -> (customPaidAmountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, grandTotalAmount)
        else -> grandTotalAmount
    }
    val calculatedRemainingAmount = (grandTotalAmount - calculatedPaidAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.96f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CyberDarkSurface,
            border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                                .background(MikroTikPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "فاتورة مبيعات كروت جديدة",
                                fontFamily = CairoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "خصم تلقائي من المخزن وحسابات محاسبية دقيقة",
                                fontFamily = CairoFontFamily,
                                fontSize = 10.5.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Customer Selection
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("العميل / نقطة البيع:", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (isDirectCustomer) "عميل كاش مباشر" else "من البقالات المسجلة",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 10.5.sp,
                                            color = TextSecondaryDark
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Switch(
                                            checked = isDirectCustomer,
                                            onCheckedChange = {
                                                isDirectCustomer = it
                                                if (it) {
                                                    customerNameText = "عميل نقدي مباشر"
                                                    customerPhoneText = ""
                                                    selectedRetailer = null
                                                } else {
                                                    selectedRetailer = retailers.firstOrNull()
                                                    customerNameText = selectedRetailer?.name ?: ""
                                                    customerPhoneText = selectedRetailer?.phone ?: ""
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (!isDirectCustomer && retailers.isNotEmpty()) {
                                    // Choose registered retailer
                                    LazyColumn(modifier = Modifier.heightIn(max = 90.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(retailers.size) { idx ->
                                            val ret = retailers[idx]
                                            val isSel = selectedRetailer?.id == ret.id
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) MikroTikPrimary.copy(alpha = 0.25f) else CyberDarkSurface,
                                                border = BorderStroke(1.dp, if (isSel) MikroTikPrimary else CyberBorder),
                                                onClick = {
                                                    selectedRetailer = ret
                                                    customerNameText = ret.name
                                                    customerPhoneText = ret.phone
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(ret.name, fontFamily = CairoFontFamily, fontSize = 11.5.sp, color = Color.White)
                                                    Text("رصيد آجل: ${ret.balanceOwed.toInt()} ر.ي", fontFamily = CairoFontFamily, fontSize = 10.sp, color = if (ret.balanceOwed > 0) StatusWarning else ProfitEmerald)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = customerNameText,
                                            onValueChange = { customerNameText = it },
                                            label = { Text("اسم العميل أو المحل") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        OutlinedTextField(
                                            value = customerPhoneText,
                                            onValueChange = { customerPhoneText = it },
                                            label = { Text("رقم الهاتف") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Multi-Item Table Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أصناف الفاتورة (${invoiceItems.size} أصناف):",
                                fontFamily = CairoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Button(
                                onClick = {
                                    val nextPkg = inventoryItems.firstOrNull { inv -> invoiceItems.none { it.packageName == inv.packageName } }
                                        ?: inventoryItems.firstOrNull()
                                    val newItem = CardSalesInvoiceItem(
                                        id = UUID.randomUUID().toString(),
                                        packageName = nextPkg?.packageName ?: "باقة 500 ريال فايبر",
                                        quantity = 20,
                                        unitPrice = nextPkg?.wholesalePrice ?: 450.0,
                                        retailPrice = nextPkg?.retailPrice ?: 500.0
                                    )
                                    invoiceItems = invoiceItems + newItem
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+ إضافة صنف آخر", fontFamily = CairoFontFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    // 3. Multi-Item Line Rows
                    itemsIndexed(invoiceItems) { index, item ->
                        InvoiceLineItemCard(
                            index = index + 1,
                            item = item,
                            inventoryItems = inventoryItems,
                            canDelete = invoiceItems.size > 1,
                            onUpdateItem = { updated ->
                                invoiceItems = invoiceItems.map { if (it.id == item.id) updated else it }
                            },
                            onDeleteItem = {
                                invoiceItems = invoiceItems.filterNot { it.id == item.id }
                            }
                        )
                    }

                    // 4. Payment Terms Selector
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
                            border = BorderStroke(1.dp, CyberBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "نوع الفاتورة وطريقة السداد:",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // نقد
                                    PaymentTypePill(
                                        title = "نقد (كاش)",
                                        sub = "سداد فوري",
                                        selected = paymentType == "CASH",
                                        color = ProfitEmerald,
                                        modifier = Modifier.weight(1f),
                                        onClick = { paymentType = "CASH" }
                                    )
                                    // آجل
                                    PaymentTypePill(
                                        title = "آجل (دين)",
                                        sub = "قيد على الحساب",
                                        selected = paymentType == "CREDIT",
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f),
                                        onClick = { paymentType = "CREDIT" }
                                    )
                                    // جزئي
                                    PaymentTypePill(
                                        title = "مقدم ومتبقي",
                                        sub = "دفع جزئي",
                                        selected = paymentType == "PARTIAL",
                                        color = StatusWarning,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            paymentType = "PARTIAL"
                                            if (customPaidAmountText.isEmpty()) {
                                                customPaidAmountText = (grandTotalAmount / 2).toInt().toString()
                                            }
                                        }
                                    )
                                }

                                if (paymentType == "PARTIAL") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customPaidAmountText,
                                            onValueChange = { customPaidAmountText = it },
                                            label = { Text("المبلغ المدفوع مقدماً (ر.ي)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("المبلغ المتبقي (آجل):", fontFamily = CairoFontFamily, fontSize = 9.5.sp, color = TextSecondaryDark)
                                                Text("${calculatedRemainingAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Notes
                    item {
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("ملاحظات الفاتورة") },
                            placeholder = { Text("مثال: تم التسليم لمندوب البقالة - يُسدد الباقي يوم الخميس") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 6. Grand Calculation Summary Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F1E33),
                            border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي عدد الكروت المباعة:", fontFamily = CairoFontFamily, fontSize = 11.5.sp, color = TextSecondaryDark)
                                    Text("$totalInvoiceCards كرت", fontFamily = CairoFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("إجمالي قيمة الفاتورة:", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${grandTotalAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Divider(modifier = Modifier.padding(vertical = 6.dp), color = CyberBorder)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("المدفوع نقداً:", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                                        Text("${calculatedPaidAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("المتبقي (آجل):", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                                        Text("${calculatedRemainingAmount.toInt()} ريال", fontFamily = CairoFontFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (calculatedRemainingAmount > 0) Color(0xFFEF4444) else ProfitEmerald)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text("إلغاء", fontFamily = CairoFontFamily)
                    }

                    Button(
                        onClick = {
                            if (customerNameText.isBlank()) {
                                Toast.makeText(context, "يرجى تحديد أو إدخال اسم العميل", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (invoiceItems.isEmpty() || totalInvoiceCards <= 0) {
                                Toast.makeText(context, "يجب إضافة صنف واحد على الأقل بكمية صحيحة", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Check stock availability
                            val stockIssue = invoiceItems.firstOrNull { item ->
                                val available = inventoryItems.find { it.packageName == item.packageName }?.quantityAvailable ?: 0
                                item.quantity > available
                            }
                            if (stockIssue != null) {
                                val available = inventoryItems.find { it.packageName == stockIssue.packageName }?.quantityAvailable ?: 0
                                Toast.makeText(context, "الكمية المطلوبة من (${stockIssue.packageName}) هي ${stockIssue.quantity} والرصيد المتاح فقط $available كرت!", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            onConfirmInvoice(
                                customerNameText.trim(),
                                customerPhoneText.trim(),
                                selectedRetailer?.id,
                                invoiceItems,
                                paymentType,
                                calculatedPaidAmount,
                                notesText.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إصدار الفاتورة وخصم المخزن", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * سطر الصنف الواحد داخل جدول الفاتورة
 */
@Composable
private fun InvoiceLineItemCard(
    index: Int,
    item: CardSalesInvoiceItem,
    inventoryItems: List<InventoryItemEntity>,
    canDelete: Boolean,
    onUpdateItem: (CardSalesInvoiceItem) -> Unit,
    onDeleteItem: () -> Unit
) {
    val currentStock = inventoryItems.find { it.packageName == item.packageName }?.quantityAvailable ?: 0
    val isExceedingStock = item.quantity > currentStock

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, if (isExceedingStock) Color(0xFFEF4444) else CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header of item row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$index", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.packageName,
                        fontFamily = CairoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "المخزن: $currentStock كرت",
                        fontFamily = CairoFontFamily,
                        fontSize = 10.sp,
                        color = if (currentStock > 0) ProfitEmerald else Color(0xFFEF4444)
                    )
                    if (canDelete) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDeleteItem, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الصنف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Package Selector Dropdown/Row if multiple exist
            if (inventoryItems.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    inventoryItems.take(4).forEach { inv ->
                        val isSel = inv.packageName == item.packageName
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSel) MikroTikPrimary else CyberDarkCardElevated,
                            onClick = {
                                onUpdateItem(
                                    item.copy(
                                        packageName = inv.packageName,
                                        unitPrice = inv.wholesalePrice,
                                        retailPrice = inv.retailPrice
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = inv.packageName.replace("باقة ", ""),
                                fontFamily = CairoFontFamily,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (isSel) Color.White else TextSecondaryDark,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inputs: Quantity, Unit Wholesale Price, Line Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity with Stepper
                Column(modifier = Modifier.weight(1.3f)) {
                    Text("العدد (كرت)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberDarkCardElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                    ) {
                        IconButton(
                            onClick = {
                                if (item.quantity > 1) {
                                    val newQ = item.quantity - 10.coerceAtMost(item.quantity - 1)
                                    onUpdateItem(item.copy(quantity = newQ))
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = "${item.quantity}",
                            fontFamily = CairoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f),
                            color = Color.White
                        )
                        IconButton(
                            onClick = {
                                val newQ = item.quantity + 10
                                onUpdateItem(item.copy(quantity = newQ))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Unit Price (Wholesale)
                Column(modifier = Modifier.weight(1.1f)) {
                    Text("سعر الكرت (جملة)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberDarkCardElevated,
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${item.unitPrice.toInt()} ر.ي",
                            fontFamily = CairoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                // Subtotal for line (العدد × السعر)
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("الإجمالي (ر.ي)", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MikroTikPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${item.lineTotal.toInt()} ر.ي",
                            fontFamily = CairoFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitEmerald,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            if (isExceedingStock) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تنبيه: الكمية المطلوبة (${item.quantity}) تتجاوز الرصيد المتوفر في المخزن ($currentStock)!",
                    fontFamily = CairoFontFamily,
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

/**
 * زر تحديد نوع السداد
 */
@Composable
private fun PaymentTypePill(
    title: String,
    sub: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.2f) else CyberDarkSurface,
        border = BorderStroke(1.dp, if (selected) color else CyberBorder),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) color else Color.White
            )
            Text(
                text = sub,
                fontFamily = CairoFontFamily,
                fontSize = 9.sp,
                color = TextSecondaryDark
            )
        }
    }
}
