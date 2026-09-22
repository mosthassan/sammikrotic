package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.model.CardSalesInvoiceItem
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.util.InvoicePdfManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * شاشة وتبويب فواتير مبيعات الكروت المحاسبية الاحترافية
 * - تصميم مالي وتنفيذي عالي الوضوح والدقة
 * - دعم كامل للاستنساخ الفوري أو الاستنساخ مع التعديل
 * - إبراز وتمييز الفواتير الجديدة والمستنسخة في أعلى القائمة
 * - إمكانية تعديل الفواتير الصادرة وتحديث المخزن والدفاتر تلقائياً
 * - بطاقات غنية بالتفاصيل، واضحة الأرقام، وأزرار عمليات رحبة وسهلة الاستخدام
 */
@Composable
fun CardSalesInvoicesSubScreen(
    viewModel: MainViewModel,
    onOpenCreateInvoice: () -> Unit,
    onEditInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null,
    onCloneInvoice: ((CardSalesInvoiceEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val rawInvoices by viewModel.salesInvoices.collectAsState()

    // استبعاد الفواتير التلقائية للتسليم الداخلي
    val invoices = remember(rawInvoices) {
        rawInvoices.filter { !it.invoiceNumber.startsWith("INV-DELIV-") }
    }

    // مؤشرات KPI المالية العامة
    val totalSalesAmount = remember(invoices) { invoices.sumOf { it.totalAmount } }
    val totalPaidAmount = remember(invoices) { invoices.sumOf { it.paidAmount } }
    val totalCreditRemaining = remember(invoices) { invoices.sumOf { it.remainingAmount } }
    val totalSoldCards = remember(invoices) { invoices.sumOf { it.totalCardsCount } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CREDIT, CASH, PARTIAL
    var selectedInvoiceForDetail by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var invoiceToDelete by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var invoiceForCloneChoice by remember { mutableStateOf<CardSalesInvoiceEntity?>(null) }
    var isCloningInstant by remember { mutableStateOf(false) }

    // تتبع الفاتورة المنشأة أو المستنسخة حديثاً لتمييزها وتمرير الشاشة إليها
    var newlyCreatedInvoiceId by remember { mutableStateOf<Long?>(null) }
    var newlyClonedNumberBanner by remember { mutableStateOf<String?>(null) }

    // إحصاءات الفلاتر
    val creditCount = remember(invoices) { invoices.count { it.paymentType == "CREDIT" || it.remainingAmount > 0 } }
    val cashCount = remember(invoices) { invoices.count { it.paymentType == "CASH" && it.remainingAmount <= 0 } }
    val partialCount = remember(invoices) { invoices.count { it.paymentType == "PARTIAL" } }

    val filteredInvoices = remember(invoices, searchQuery, selectedFilter) {
        invoices.filter { inv ->
            val matchesSearch = searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.customerName.contains(searchQuery, ignoreCase = true) ||
                    inv.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    inv.itemsSummary.contains(searchQuery, ignoreCase = true) ||
                    inv.notes.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "CREDIT" -> inv.paymentType == "CREDIT" || inv.remainingAmount > 0
                "CASH" -> inv.paymentType == "CASH" && inv.remainingAmount <= 0
                "PARTIAL" -> inv.paymentType == "PARTIAL"
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    // تقسيم الفواتير زمنياً (اليوم، أمس، سابقة)
    val groupedInvoices = remember(filteredInvoices) {
        val todayList = mutableListOf<CardSalesInvoiceEntity>()
        val yesterdayList = mutableListOf<CardSalesInvoiceEntity>()
        val earlierList = mutableListOf<CardSalesInvoiceEntity>()

        filteredInvoices.forEach { inv ->
            when {
                isToday(inv.invoiceDateMillis) -> todayList.add(inv)
                isYesterday(inv.invoiceDateMillis) -> yesterdayList.add(inv)
                else -> earlierList.add(inv)
            }
        }
        listOf(
            "فواتير اليوم" to todayList,
            "فواتير أمس" to yesterdayList,
            "فواتير سابقة" to earlierList
        ).filter { it.second.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("card_sales_invoices_sub_screen")
    ) {
        // 1. شريط التدقيق والمطابقة الدفترية مع نقاط البيع
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0B132B),
            border = BorderStroke(1.dp, ProfitEmerald.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = ProfitEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المطابقة الدفترية: ترابط فواتير المبيعات وسندات القبض 100%",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.reconcileAccountingLedger {
                            Toast.makeText(context, "تم تدقيق ومطابقة فواتير المبيعات مع دفاتر البقالات بنجاح 100% ✓", Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تدقيق", fontFamily = CairoFontFamily, fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. إشعار النجاح عند استنساخ الفاتورة فوراً
        newlyClonedNumberBanner?.let { bannerMessage ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF064E3B),
                border = BorderStroke(1.dp, ProfitEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerMessage,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = { newlyClonedNumberBanner = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 3. لوحة المؤشرات المالية التنفيذية (KPIs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // إجمالي المبيعات
            KpiMetricBox(
                title = "إجمالي المبيعات",
                value = "${totalSalesAmount.toInt()} ر.ي",
                subValue = "${invoices.size} فاتورة",
                color = Color(0xFF38BDF8),
                icon = Icons.Default.ReceiptLong,
                modifier = Modifier.weight(1f)
            )

            // المحصل نقداً
            KpiMetricBox(
                title = "المحصل نقداً",
                value = "${totalPaidAmount.toInt()} ر.ي",
                subValue = "تم إيداعه",
                color = ProfitEmerald,
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )

            // المتبقي الآجل
            KpiMetricBox(
                title = "المتبقي الآجل",
                value = "${totalCreditRemaining.toInt()} ر.ي",
                subValue = if (totalCreditRemaining > 0) "ديون مستحقة" else "مصفى بالكامل",
                color = if (totalCreditRemaining > 0) Color(0xFFEF4444) else ProfitEmerald,
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )

            // الكروت المباعة
            KpiMetricBox(
                title = "الكروت المباعة",
                value = "$totalSoldCards كرت",
                subValue = "من المخزن",
                color = InvestmentGold,
                icon = Icons.Default.ConfirmationNumber,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. شريط زر الفاتورة الجديدة مع الفلاتر
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // كبسولات الفلاتر
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterTabChip(
                    title = "الكل",
                    count = invoices.size,
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                FilterTabChip(
                    title = "آجلة ⏳",
                    count = creditCount,
                    color = Color(0xFFEF4444),
                    selected = selectedFilter == "CREDIT",
                    onClick = { selectedFilter = "CREDIT" }
                )
                FilterTabChip(
                    title = "نقدية ✓",
                    count = cashCount,
                    color = ProfitEmerald,
                    selected = selectedFilter == "CASH",
                    onClick = { selectedFilter = "CASH" }
                )
                FilterTabChip(
                    title = "جزئية",
                    count = partialCount,
                    color = StatusWarning,
                    selected = selectedFilter == "PARTIAL",
                    onClick = { selectedFilter = "PARTIAL" }
                )
            }

            // زر إصدار فاتورة جديدة
            Button(
                onClick = onOpenCreateInvoice,
                colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("فاتورة جديدة", fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        // 5. حقل البحث الذكي الفوري
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text("بحث برقم الفاتورة، اسم المحل، رقم الهاتف، أو الصنف...", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondaryDark)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
            },
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
                focusedBorderColor = Color(0xFF38BDF8),
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

        // 6. قائمة الفواتير المصممة هندسياً وتنفيذياً
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد فواتير مطابقة للبحث" else "لا توجد فواتير مبيعات مسجلة حتى الآن",
                        fontFamily = CairoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "جرب البحث برقم الفاتورة أو اسم المحل" else "اضغط على (فاتورة جديدة) لإصدار فاتورة مبيعات متعددة الأصناف مع الخصم التلقائي من المخزن",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedInvoices.forEach { (sectionTitle, sectionInvoices) ->
                    item(key = "header_$sectionTitle") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$sectionTitle (${sectionInvoices.size})",
                                fontFamily = CairoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = CyberBorder.copy(alpha = 0.5f),
                                thickness = 0.8.dp
                            )
                        }
                    }

                    items(sectionInvoices, key = { it.id }) { invoice ->
                        val isRecent = invoice.id == newlyCreatedInvoiceId ||
                                (System.currentTimeMillis() - invoice.invoiceDateMillis < 90_000L)

                        CardSalesInvoiceExecutiveCard(
                            invoice = invoice,
                            isHighlighted = isRecent,
                            onClick = { selectedInvoiceForDetail = invoice },
                            onEdit = { onEditInvoice?.invoke(invoice) },
                            onClone = { invoiceForCloneChoice = invoice },
                            onDelete = { invoiceToDelete = invoice }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }

    // نافذة خيارات الاستنساخ (استنساخ فوري مباشر أو استنساخ مع تعديل البيانات)
    invoiceForCloneChoice?.let { sourceInvoice ->
        AlertDialog(
            onDismissRequest = {
                if (!isCloningInstant) invoiceForCloneChoice = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ProfitEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CopyAll, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Text(
                    text = "استنساخ الفاتورة #${sourceInvoice.invoiceNumber}",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "العميل: ${sourceInvoice.customerName} • ${sourceInvoice.totalCardsCount} كرت • ${sourceInvoice.totalAmount.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // الخيار 1: استنساخ فوري مباشر برقم جديد وتاريخ اللحظة
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F2942),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        onClick = {
                            if (!isCloningInstant) {
                                isCloningInstant = true
                                viewModel.instantCloneSalesInvoice(sourceInvoice) { newInvoiceId ->
                                    isCloningInstant = false
                                    invoiceForCloneChoice = null
                                    if (newInvoiceId > 0) {
                                        newlyCreatedInvoiceId = newInvoiceId
                                        selectedFilter = "ALL"
                                        searchQuery = ""
                                        newlyClonedNumberBanner = "تم استنساخ الفاتورة فوراً بنجاح وحفظها في أعلى القائمة وتحديث المخزن والحسابات ✓"
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                        Toast.makeText(context, "تم استنساخ الفاتورة بنجاح في أعلى القائمة ✓", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "تعذر استنساخ الفاتورة!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCloningInstant) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("استنساخ فوري مباشر برقم جديد", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("إصدار نسخة مطابقة فوراً برقم جديد وتاريخ اللحظة وتحديث المخزن والدفاتر", fontFamily = CairoFontFamily, fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    // الخيار 2: استنساخ مع التعديل أولاً
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberDarkCardElevated,
                        border = BorderStroke(1.dp, CyberBorder),
                        onClick = {
                            val inv = sourceInvoice
                            invoiceForCloneChoice = null
                            onCloneInvoice?.invoke(inv)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("تعديل البيانات قبل الاستنساخ", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("فتح نافذة الفاتورة لتغيير الكميات، الأسعار، العميل أو طريقة الدفع قبل الحفظ", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextSecondaryDark)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { invoiceForCloneChoice = null },
                    enabled = !isCloningInstant
                ) {
                    Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkSurface
        )
    }

    // نافذة تفاصيل الفاتورة الكاملة
    selectedInvoiceForDetail?.let { inv ->
        CardSalesInvoiceDetailDialog(
            invoice = inv,
            viewModel = viewModel,
            onDismiss = { selectedInvoiceForDetail = null },
            onEditInvoice = { invoiceToEdit ->
                selectedInvoiceForDetail = null
                onEditInvoice?.invoke(invoiceToEdit)
            },
            onCloneInvoice = { invoiceToClone ->
                selectedInvoiceForDetail = null
                invoiceForCloneChoice = invoiceToClone
            },
            onDeleteInvoice = { invoiceToDel ->
                selectedInvoiceForDetail = null
                invoiceToDelete = invoiceToDel
            }
        )
    }

    // نافذة تأكيد حذف الفاتورة واسترجاع المخزن
    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد حذف الفاتورة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك بحذف الفاتورة #${inv.invoiceNumber} الخاصة بـ (${inv.customerName})؟\n\n" +
                            "• سيتم إرجاع كميات الكروت (${inv.totalCardsCount} كرت) إلى المخزن تلقائياً.\n" +
                            "• سيتم إلغاء السند المالي وضبط حساب العميل محاسبياً 100%.",
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
                    Text("تأكيد الحذف واسترجاع المخزن", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondaryDark)
                }
            },
            containerColor = CyberDarkSurface
        )
    }
}

/**
 * بطاقة عرض الفاتورة التنفيذية الاحترافية
 * - رؤية متكاملة لبيانات العميل والأصناف
 * - أرقام واضحة وعالية التباين
 * - أزرار عمليات واضحة ومباشرة (تعديل، استنساخ، PDF، واتساب، حذف)
 */
@Composable
private fun CardSalesInvoiceExecutiveCard(
    invoice: CardSalesInvoiceEntity,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val dateFormatted = remember(invoice.invoiceDateMillis) {
        val sdf = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
        sdf.format(Date(invoice.invoiceDateMillis))
    }

    val (badgeText, badgeColor, badgeIcon) = remember(invoice.paymentType, invoice.remainingAmount) {
        when {
            invoice.remainingAmount <= 0 -> Triple("نقدية مدفوعة بالكامل ✓", ProfitEmerald, Icons.Default.CheckCircle)
            invoice.paidAmount <= 0 -> Triple("آجلة على الحساب ⏳", Color(0xFFEF4444), Icons.Default.Schedule)
            else -> Triple("سداد جزئي ⚡", StatusWarning, Icons.Default.PriceCheck)
        }
    }

    // استخراج الأصناف من JSON لعرض تفصيلي نظيف
    val parsedItems = remember(invoice.itemsJson) {
        val list = mutableListOf<CardSalesInvoiceItem>()
        if (invoice.itemsJson.isNotBlank()) {
            try {
                val arr = JSONArray(invoice.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CardSalesInvoiceItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            packageName = obj.optString("packageName", "صنف"),
                            quantity = obj.optInt("quantity", 0),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            retailPrice = obj.optDouble("retailPrice", 0.0)
                        )
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        list
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(
            if (isHighlighted) 1.8.dp else 1.dp,
            if (isHighlighted) ProfitEmerald else CyberBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 6.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // شريط تمييز الفاتورة الجديدة أو المستنسخة للتو
            if (isHighlighted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ProfitEmerald.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فاتورة جديدة / مستنسخة حديثاً ✨",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitEmerald
                    )
                }
            }

            // الصف 1: رأس البطاقة (رقم الفاتورة + شارة حالة السداد)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // رقم الفاتورة مع زر نسخ سريع
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = invoice.invoiceNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(invoice.invoiceNumber))
                            Toast.makeText(context, "تم نسخ رقم الفاتورة: ${invoice.invoiceNumber}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ رقم الفاتورة", tint = TextSecondaryDark, modifier = Modifier.size(13.dp))
                    }
                }

                // شارة حالة السداد
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            fontFamily = CairoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الصف 2: اسم المحل أو العميل، الهاتف والتاريخ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = invoice.customerName,
                        fontFamily = CairoFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (invoice.customerPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${invoice.customerPhone})",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Text(
                    text = dateFormatted,
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الصف 3: جدول الأصناف المصممة بهندسة بصرية واضحة
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyberDarkCardElevated,
                border = BorderStroke(0.5.dp, CyberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (parsedItems.isNotEmpty()) {
                        parsedItems.take(3).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF38BDF8))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.packageName,
                                        fontFamily = CairoFontFamily,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${item.quantity} كرت × ${item.unitPrice.toInt()} = ${item.lineTotal.toInt()} ر.ي",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                        if (parsedItems.size > 3) {
                            Text(
                                text = "+ ${parsedItems.size - 3} أصناف إضافية أخرى...",
                                fontFamily = CairoFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = invoice.itemsSummary.ifBlank { "${invoice.totalCardsCount} كرت شبكة" },
                            fontFamily = CairoFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الصف 4: الأعمدة المالية الثلاثة ذات التباين العالي (الإجمالي، المدفوع، المتبقي)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1E33))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // الكمية
                Column(horizontalAlignment = Alignment.Start) {
                    Text("الكروت", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = "${invoice.totalCardsCount} كرت",
                        fontFamily = CairoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                // الإجمالي
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المبلغ الإجمالي", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = "${invoice.totalAmount.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // المدفوع
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المدفوع نقداً", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = "${invoice.paidAmount.toInt()} ر.ي",
                        fontFamily = CairoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitEmerald
                    )
                }

                // المتبقي
                Column(horizontalAlignment = Alignment.End) {
                    Text("المتبقي الآجل", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = if (invoice.remainingAmount > 0) "${invoice.remainingAmount.toInt()} ر.ي" else "0 ر.ي (مصفى)",
                        fontFamily = CairoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.remainingAmount > 0) Color(0xFFEF4444) else ProfitEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // الصف 5: شريط أزرار العمليات الأساسية الواضحة والرحبة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. زر تعديل الفاتورة
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // 2. زر استنساخ الفاتورة
                Button(
                    onClick = onClone,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استنساخ", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                // 3. زر مشاركة PDF
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkCardElevated,
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                    onClick = {
                        InvoicePdfManager.shareInvoicePdf(context, invoice, parsedItems)
                    },
                    modifier = Modifier.weight(0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                }

                // 4. زر مشاركة عبر واتساب
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkCardElevated,
                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.6f)),
                    onClick = {
                        shareInvoiceViaWhatsApp(context, invoice)
                    },
                    modifier = Modifier.weight(0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة واتساب", tint = Color(0xFF22C55E), modifier = Modifier.size(15.dp))
                    }
                }

                // 5. زر حذف الفاتورة
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberDarkCardElevated,
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                    onClick = onDelete,
                    modifier = Modifier.weight(0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الفاتورة", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/**
 * بطاقة إحصائية مصغرة في لوحة المؤشرات العلوية
 */
@Composable
private fun KpiMetricBox(
    title: String,
    value: String,
    subValue: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = CairoFontFamily,
                    fontSize = 9.5.sp,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = CairoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subValue,
                fontFamily = CairoFontFamily,
                fontSize = 8.5.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * كبسولة فلترة حسب حالة الفاتورة
 */
@Composable
private fun FilterTabChip(
    title: String,
    count: Int,
    selected: Boolean,
    color: Color = Color(0xFF38BDF8),
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color else CyberDarkSurface,
        border = BorderStroke(1.dp, if (selected) color else CyberBorder),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title ($count)",
                fontFamily = CairoFontFamily,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) (if (color == InvestmentGold || color == ProfitEmerald) Color.Black else Color.White) else TextSecondaryDark
            )
        }
    }
}

/**
 * مشاركة الفاتورة عبر واتساب أو التطبيقات الأخرى بصيغة نصية منسقة
 */
private fun shareInvoiceViaWhatsApp(context: Context, invoice: CardSalesInvoiceEntity) {
    try {
        val message = buildString {
            append("🧾 *فاتورة مبيعات كروت شبكة*\n")
            append("رقم الفاتورة: #${invoice.invoiceNumber}\n")
            append("العميل: ${invoice.customerName}\n")
            if (invoice.customerPhone.isNotBlank()) append("الهاتف: ${invoice.customerPhone}\n")
            append("الأصناف: ${invoice.itemsSummary}\n")
            append("إجمالي الكروت: ${invoice.totalCardsCount} كرت\n")
            append("------------------------\n")
            append("💰 المبلغ الإجمالي: ${invoice.totalAmount.toInt()} ر.ي\n")
            append("💵 المدفوع نقداً: ${invoice.paidAmount.toInt()} ر.ي\n")
            if (invoice.remainingAmount > 0) {
                append("⚠️ المتبقي (آجل): ${invoice.remainingAmount.toInt()} ر.ي\n")
            } else {
                append("✅ الحالة: مسددة بالكامل\n")
            }
            if (invoice.notes.isNotBlank()) append("ملاحظات: ${invoice.notes}\n")
            append("------------------------\n")
            append("شبكة الاتصالات والإنترنت")
        }

        val cleanPhone = invoice.customerPhone.replace(Regex("[^0-9]"), "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = if (cleanPhone.isNotBlank()) {
                val formattedPhone = if (cleanPhone.startsWith("967")) cleanPhone else if (cleanPhone.length == 9) "967$cleanPhone" else cleanPhone
                Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "فاتورة مبيعات #${invoice.invoiceNumber} للعميل ${invoice.customerName} بمبلغ ${invoice.totalAmount.toInt()} ر.ي")
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة"))
    }
}

private fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
