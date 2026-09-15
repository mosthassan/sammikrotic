package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.EquityBlue
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VouchersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFinanceTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // High-level Top Finance & Investment Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedFinanceTab,
            containerColor = MikroTikNavy,
            contentColor = Color.White,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                if (selectedFinanceTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFinanceTab]),
                        color = when (selectedFinanceTab) {
                            1 -> InvestmentGold
                            2 -> AssetPurple
                            3 -> ProfitEmerald
                            else -> MikroTikPrimary
                        },
                        height = 3.dp
                    )
                }
            }
        ) {
            val tabs = listOf(
                Triple("السندات والمصروفات", Icons.Default.Receipt, 0),
                Triple("الشركاء ورأس المال", Icons.Default.Group, 1),
                Triple("الأصول الثابتة (CAPEX)", Icons.Default.Devices, 2),
                Triple("الأرباح والخسائر (P&L)", Icons.Default.Assessment, 3)
            )

            tabs.forEach { (title, icon, idx) ->
                val isSelected = selectedFinanceTab == idx
                Tab(
                    selected = isSelected,
                    onClick = { selectedFinanceTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                when (idx) {
                                    1 -> InvestmentGold
                                    2 -> AssetPurple
                                    3 -> ProfitEmerald
                                    else -> Color.White
                                }
                            } else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Screen Body Based on Selected Tab
        when (selectedFinanceTab) {
            0 -> VouchersListSubScreen(viewModel = viewModel)
            1 -> PartnersScreen(viewModel = viewModel)
            2 -> AssetsScreen(viewModel = viewModel)
            3 -> ProfitLossScreen(
                viewModel = viewModel,
                onNavigateToAI = { prompt ->
                    viewModel.consultAi(prompt)
                }
            )
        }
    }
}

@Composable
fun VouchersListSubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val totalReceipts by viewModel.totalReceipts.collectAsState()
    val totalPayments by viewModel.totalPayments.collectAsState()
    val retailers by viewModel.retailers.collectAsState()
    val context = LocalContext.current

    var selectedTypeFilter by remember { mutableStateOf("الكل") }
    var showAddDialog by remember { mutableStateOf(false) }
    var voucherToPreview by remember { mutableStateOf<FinancialVoucherEntity?>(null) }
    var voucherToDelete by remember { mutableStateOf<FinancialVoucherEntity?>(null) }

    val filterTypes = listOf("الكل", "سندات قبض", "سندات صرف")

    val filteredVouchers = vouchers.filter { voucher ->
        when (selectedTypeFilter) {
            "سندات قبض" -> voucher.voucherType == "RECEIPT"
            "سندات صرف" -> voucher.voucherType == "PAYMENT"
            else -> true
        }
    }

    val receiptsVal = totalReceipts ?: 0.0
    val paymentsVal = totalPayments ?: 0.0
    val netProfit = receiptsVal - paymentsVal

    Box(modifier = modifier.fillMaxSize().testTag("vouchers_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "السندات المالية والمصروفات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "سندات القبض والصرف، أرباح الكروت، ومصاريف التشغيل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            Spacer(modifier = Modifier.height(14.dp))

            // Accounting KPI Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoucherSummaryCard(
                    title = "إجمالي المقبوضات",
                    amount = receiptsVal,
                    color = ReceiptGreen,
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
                VoucherSummaryCard(
                    title = "إجمالي المصروفات",
                    amount = paymentsVal,
                    color = PaymentRed,
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                VoucherSummaryCard(
                    title = "صافي الحركة",
                    amount = netProfit,
                    color = if (netProfit >= 0) ReceiptGreen else PaymentRed,
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterTypes) { filter ->
                    val isSelected = selectedTypeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { selectedTypeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vouchers List
            if (filteredVouchers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "لا توجد سندات مالية مسجلة في هذا القسم", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredVouchers, key = { it.id }) { voucher ->
                        VoucherItemCard(
                            voucher = voucher,
                            onPreview = { voucherToPreview = voucher },
                            onDelete = { voucherToDelete = voucher }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // FAB to Add Voucher
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MikroTikPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_voucher_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "إصدار سند")
                Spacer(modifier = Modifier.width(6.dp))
                Text("إصدار سند مالي", fontWeight = FontWeight.Bold)
            }
        }

        // Add Voucher Dialog
        if (showAddDialog) {
            AddVoucherDialog(
                retailers = retailers,
                onDismiss = { showAddDialog = false },
                onSave = { type, amount, party, retailerId, cat, method, desc ->
                    viewModel.createVoucher(
                        voucherType = type,
                        amount = amount,
                        partyName = party,
                        retailerId = retailerId,
                        category = cat,
                        paymentMethod = method,
                        description = desc
                    ) {
                        showAddDialog = false
                        Toast.makeText(context, "تم إصدار السند المالي بنجاح ✓", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Voucher Print & Share Slip Modal
        if (voucherToPreview != null) {
            VoucherSlipModal(
                voucher = voucherToPreview!!,
                onDismiss = { voucherToPreview = null },
                onShare = { shareText ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة السند المالي"))
                }
            )
        }

        // Delete Dialog
        if (voucherToDelete != null) {
            AlertDialog(
                onDismissRequest = { voucherToDelete = null },
                title = { Text("تأكيد حذف السند") },
                text = { Text("هل أنت متأكد من حذف السند (${voucherToDelete?.voucherNumber})؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            voucherToDelete?.let { viewModel.deleteVoucher(it) }
                            voucherToDelete = null
                            Toast.makeText(context, "تم حذف السند", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaymentRed)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { voucherToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun VoucherSummaryCard(
    title: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${amount.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(text = "ريال", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun VoucherItemCard(
    voucher: FinancialVoucherEntity,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val isReceipt = voucher.voucherType == "RECEIPT"
    val badgeColor = if (isReceipt) ReceiptGreen else PaymentRed
    val badgeText = if (isReceipt) "سند قبض" else "سند صرف"
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(voucher.dateMillis))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("voucher_card_${voucher.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Voucher Number, Type Badge, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = voucher.voucherNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "${voucher.amount.toInt()} ريال",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Party Name
            Text(
                text = if (isReceipt) "استلمنا من: ${voucher.partyName}" else "صرفنا إلى: ${voucher.partyName}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Category & Description
            if (voucher.description.isNotEmpty()) {
                Text(
                    text = "${voucher.category} • ${voucher.description}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer info and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateStr | ${voucher.issuerName}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("معاينة السند", color = MikroTikPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVoucherDialog(
    retailers: List<com.example.data.local.entity.RetailerEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Long?, String, String, String) -> Unit
) {
    var voucherType by remember { mutableStateOf("RECEIPT") } // RECEIPT or PAYMENT
    var amountText by remember { mutableStateOf("") }
    var partyName by remember { mutableStateOf("") }
    var selectedRetailerId by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf("توريد مبيعات كروت") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var description by remember { mutableStateOf("") }

    var isRetailerDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (voucherType == "RECEIPT") "إصدار سند قبض مالي (توريد)" else "إصدار سند صرف مالي (مصروفات)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Selector (Receipt vs Payment)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                voucherType = "RECEIPT"
                                category = "توريد مبيعات كروت"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (voucherType == "RECEIPT") ReceiptGreen else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سند قبض", color = if (voucherType == "RECEIPT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                voucherType = "PAYMENT"
                                category = "اشتراك نت رئيسي"
                                selectedRetailerId = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (voucherType == "PAYMENT") PaymentRed else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سند صرف", color = if (voucherType == "PAYMENT") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Amount
                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ بالريال *") },
                        placeholder = { Text("مثال: 50000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("voucher_amount_input")
                    )
                }

                // Select from Retailers (Optional for receipts)
                if (voucherType == "RECEIPT" && retailers.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = isRetailerDropdownExpanded,
                            onExpandedChange = { isRetailerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = retailers.firstOrNull { it.id == selectedRetailerId }?.name ?: "اختيار بقالة مسجلة (اختياري)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("تحديد البقالة لتخفيض رصيدها تلقائياً") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRetailerDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isRetailerDropdownExpanded,
                                onDismissRequest = { isRetailerDropdownExpanded = false }
                            ) {
                                retailers.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("${r.name} (دين: ${r.balanceOwed.toInt()} ريال)") },
                                        onClick = {
                                            selectedRetailerId = r.id
                                            partyName = r.name
                                            isRetailerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Party Name (Custom or autofilled)
                item {
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { partyName = it },
                        label = { Text(if (voucherType == "RECEIPT") "استلمنا من الأخ / الجهة *" else "صرفنا إلى الأخ / الجهة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("voucher_party_input")
                    )
                }

                // Category
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("بند الحساب / التصنيف") },
                        placeholder = { Text(if (voucherType == "RECEIPT") "توريد كروت، تأمين..." else "فاتورة يمن نت، ديزل، صيانة...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Payment Method
                item {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("طريقة الدفع (نقداً / حوالة / بنك)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("البيان والتفاصيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && partyName.isNotBlank()) {
                        onSave(voucherType, amt, partyName.trim(), selectedRetailerId, category.trim(), paymentMethod.trim(), description.trim())
                    }
                },
                enabled = amountText.toDoubleOrNull() != null && partyName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (voucherType == "RECEIPT") ReceiptGreen else PaymentRed)
            ) {
                Text("إصدار السند")
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
fun VoucherSlipModal(
    voucher: FinancialVoucherEntity,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    val isReceipt = voucher.voucherType == "RECEIPT"
    val title = if (isReceipt) "سند قبض مالي رسمي" else "سند صرف مالي رسمي"
    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(voucher.dateMillis))

    val shareText = """
        🧾 *سام ميكروتك - $title*
        رقم السند: ${voucher.voucherNumber}
        التاريخ: $dateStr
        المبلغ: ${voucher.amount.toInt()} ريال يمني
        ${if (isReceipt) "استلمنا من" else "صرفنا إلى"}: ${voucher.partyName}
        البيان: ${voucher.description}
        البند: ${voucher.category}
        طريقة الدفع: ${voucher.paymentMethod}
        المسؤول: ${voucher.issuerName}
        ------------------------------
        نظام سام ميكروتك لإدارة شبكات الوايرلس والمحاسبة
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "سام ميكروتك", fontWeight = FontWeight.Bold, color = MikroTikNavy, fontSize = 14.sp)
                        Text(text = voucher.voucherNumber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MikroTikPrimary, fontSize = 12.sp)
                    }
                    Text(text = "التاريخ: $dateStr", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Amount box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isReceipt) ReceiptGreen.copy(alpha = 0.1f) else PaymentRed.copy(alpha = 0.1f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "المبلغ: ${voucher.amount.toInt()} ريال يمني فقط لا غير",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isReceipt) ReceiptGreen else PaymentRed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isReceipt) "وصلنا من الأخ/السيد:" else "صرفنا للأخ/السيد:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(text = voucher.partyName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "وذلك عن:", fontSize = 11.sp, color = Color.Gray)
                    Text(text = "${voucher.category} - ${voucher.description}", fontSize = 13.sp, color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "طريقة الدفع: ${voucher.paymentMethod}", fontSize = 12.sp, color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Signatures
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "المستلم", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(text = ".......................", fontSize = 10.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "أمين الصندوق / الإدارة", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = voucher.issuerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MikroTikNavy)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShare(shareText) },
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مشاركة السند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
