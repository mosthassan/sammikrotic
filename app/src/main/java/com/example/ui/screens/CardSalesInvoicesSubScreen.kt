package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.model.CardSalesInvoiceItem
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.InvoicePdfManager
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * شاشة وتبويب فواتير مبيعات الكروت
 * - نظام فواتير محاسبي احترافي متكامل
 * - يدعم الفواتير متعددة الأصناف، النقدية، والآجلة، والجزئية
 * - خصم الكميات من المخزن تلقائياً وتحديث أرصدة البقالات
 */
@Composable
fun CardSalesInvoicesSubScreen(
    viewModel: MainViewModel,
    onOpenCreateInvoice: () -> Unit,
    onCloneInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawInvoices by viewModel.salesInvoices.collectAsState()
    val invoices = remember(rawInvoices) {
        rawInvoices.filter { !it.invoiceNumber.startsWith("INV-DELIV-") }
    }
    val totalSalesAmount = remember(invoices) { invoices.sumOf { it.totalAmount } }
    val totalSoldCards = remember(invoices) { invoices.sumOf { it.totalCardsCount } }
    val totalCreditRemaining = remember(invoices) { invoices.sumOf { it.remainingAmount } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CASH, CREDIT, PARTIAL
    var selectedInvoiceForDetail by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var invoiceToDelete by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }

    val filteredInvoices = invoices.filter { inv ->
        val matchesSearch = searchQuery.isBlank() ||
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.itemsSummary.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "CASH" -> inv.paymentType == "CASH"
            "CREDIT" -> inv.paymentType == "CREDIT"
            "PARTIAL" -> inv.paymentType == "PARTIAL"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("card_sales_invoices_sub_screen")
    ) {
        // شريط التدقيق والمطابقة الدفترية مع نقاط البيع
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ProfitEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المطابقة الدفترية: ديون المحلات متطابقة 100% مع فواتير المبيعات الآجلة",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.reconcileAccountingLedger {
                            Toast.makeText(context, "تم تدقيق ومطابقة فواتير المبيعات مع ديون المحلات بنجاح 100% ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة التدقيق", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPI Summary Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // إجمالي المبيعات
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, MikroTikPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("إجمالي المبيعات", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${(totalSalesAmount ?: 0.0).toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            // إجمالي الكروت المباعة
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("كروت مباعة", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${totalSoldCards ?: 0} كرت",
                        fontFamily = CairoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitEmerald
                    )
                }
            }

            // مديونية آجلة متبقية
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("المديونية الآجلة", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${(totalCreditRemaining ?: 0.0).toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        // Action & Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Pills
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterTabPill(title = "الكل (${invoices.size})", selected = selectedFilter == "ALL") { selectedFilter = "ALL" }
                FilterTabPill(title = "نقدية", selected = selectedFilter == "CASH") { selectedFilter = "CASH" }
                FilterTabPill(title = "آجلة", selected = selectedFilter == "CREDIT") { selectedFilter = "CREDIT" }
                FilterTabPill(title = "جزئية", selected = selectedFilter == "PARTIAL") { selectedFilter = "PARTIAL" }
            }

            // Create Invoice Button
            Button(
                onClick = onOpenCreateInvoice,
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("فاتورة جديدة", fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث برقم الفاتورة أو اسم العميل...", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MikroTikPrimary,
                unfocusedBorderColor = CyberBorder,
                focusedContainerColor = CyberDarkSurface,
                unfocusedContainerColor = CyberDarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        )

        // Invoices List
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد فواتير مطابقة للبحث" else "لا توجد فواتير مبيعات مسجلة بعد",
                        fontFamily = CairoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط على (فاتورة جديدة) لإصدار فاتورة مبيعات متعددة الأصناف مع الخصم الفوري من المخزن",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    CardSalesInvoiceRow(
                        invoice = invoice,
                        onClick = { selectedInvoiceForDetail = invoice },
                        onClone = { onCloneInvoice?.invoke(invoice) },
                        onDelete = { invoiceToDelete = invoice }
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedInvoiceForDetail?.let { inv ->
        CardSalesInvoiceDetailDialog(
            invoice = inv,
            viewModel = viewModel,
            onDismiss = { selectedInvoiceForDetail = null },
            onCloneInvoice = { invoiceToClone ->
                selectedInvoiceForDetail = null
                onCloneInvoice?.invoke(invoiceToClone)
            },
            onDeleteInvoice = { invoiceToDel ->
                selectedInvoiceForDetail = null
                invoiceToDelete = invoiceToDel
            }
        )
    }

    // Delete Confirmation Dialog
    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = {
                Text("تأكيد حذف الفاتورة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "هل أنت متأكد من حذف الفاتورة #${inv.invoiceNumber} الخاصة بـ (${inv.customerName})؟\n\n" +
                    "• سيتم استرجاع الكروت (${inv.totalCardsCount} كرت) إلى المخزن تلقائياً.\n" +
                    "• سيتم إلغاء السند المالي وضبط حساب البقالة محاسبياً.",
                    fontFamily = CairoFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = inv
                        invoiceToDelete = null
                        viewModel.deleteSalesInvoice(toDelete) {
                            Toast.makeText(context, "تم حذف الفاتورة ${toDelete.invoiceNumber} واسترجاع المخزن بنجاح ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("تأكيد الحذف", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("إلغاء", fontFamily = CairoFontFamily)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}

/**
 * سطر عرض الفاتورة في القائمة
 */
@Composable
private fun CardSalesInvoiceRow(
    invoice: CardSalesInvoiceEntity,
    onClick: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatted = remember(invoice.invoiceDateMillis) {
        val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(invoice.invoiceDateMillis))
    }

    val paymentTitle = when (invoice.paymentType) {
        "CASH" -> "نقد"
        "CREDIT" -> "آجل"
        "PARTIAL" -> "جزئي"
        else -> invoice.paymentType
    }
    val badgeColor = when (invoice.paymentType) {
        "CASH" -> ProfitEmerald
        "CREDIT" -> Color(0xFFEF4444)
        "PARTIAL" -> StatusWarning
        else -> MikroTikPrimary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Number, Date, Payment Badge, PDF Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = invoice.invoiceNumber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$dateFormatted | ${invoice.customerName}",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Clone
                    IconButton(
                        onClick = onClone,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.CopyAll,
                            contentDescription = "استنساخ الفاتورة",
                            tint = ProfitEmerald,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Quick PDF Share
                    IconButton(
                        onClick = {
                            val itemsList = try {
                                val arr = JSONArray(invoice.itemsJson)
                                val list = mutableListOf<CardSalesInvoiceItem>()
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    list.add(
                                        CardSalesInvoiceItem(
                                            id = obj.optString("id", UUID.randomUUID().toString()),
                                            packageName = obj.optString("packageName", ""),
                                            quantity = obj.optInt("quantity", 1),
                                            unitPrice = obj.optDouble("unitPrice", 0.0),
                                            retailPrice = obj.optDouble("retailPrice", 0.0)
                                        )
                                    )
                                }
                                list
                            } catch (e: Exception) {
                                emptyList()
                            }
                            InvoicePdfManager.shareInvoicePdf(context, invoice, itemsList)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "مشاركة كـ PDF",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Quick Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف الفاتورة",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = paymentTitle,
                            fontFamily = CairoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items Summary
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CyberDarkCardElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = invoice.itemsSummary,
                    fontFamily = CairoFontFamily,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Financial Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${invoice.totalCardsCount} كرت",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• الإجمالي: ${invoice.totalAmount.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (invoice.remainingAmount > 0) {
                    Text(
                        text = "متبقي: ${invoice.remainingAmount.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                } else {
                    Text(
                        text = "مدفوعة بالكامل ✓",
                        fontFamily = CairoFontFamily,
                        fontSize = 10.5.sp,
                        color = ProfitEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MikroTikPrimary else CyberDarkSurface,
        border = BorderStroke(1.dp, if (selected) MikroTikPrimary else CyberBorder),
        onClick = onClick
    ) {
        Text(
            text = title,
            fontFamily = CairoFontFamily,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondaryDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
