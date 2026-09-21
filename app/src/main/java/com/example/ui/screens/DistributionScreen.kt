package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.WhatsAppHelper

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
    var retailerToEdit by remember { mutableStateOf<RetailerEntity?>(null) }
    var showDistributeDialog by remember { mutableStateOf(false) }
    var selectedRetailerForDistribution by remember { mutableStateOf<RetailerEntity?>(null) }
    var showQuickPayDialog by remember { mutableStateOf<RetailerEntity?>(null) }
    var retailerToDelete by remember { mutableStateOf<RetailerEntity?>(null) }
    var retailerForWhatsAppMenu by remember { mutableStateOf<RetailerEntity?>(null) }

    var distributionSubTab by remember { mutableIntStateOf(0) } // 0: نقاط البيع والديون, 1: فواتير المبيعات, 2: مخزن الأصناف بالعدد
    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var selectedRetailerForInvoice by remember { mutableStateOf<RetailerEntity?>(null) }

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
                        text = "المبيعات وتوزيع الكروت للمحلات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "فواتير احترافية، متابعة نقاط البيع، وتسليم الدفعات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            selectedRetailerForInvoice = null
                            showCreateInvoiceDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("فاتورة مبيعات", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("تسليم كروت", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub Navigation Tabs Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (distributionSubTab == 0) MikroTikPrimary else CyberDarkSurface,
                    border = BorderStroke(1.dp, if (distributionSubTab == 0) MikroTikPrimary else CyberBorder),
                    onClick = { distributionSubTab = 0 },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (distributionSubTab == 0) Color.White else TextSecondaryDark)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("نقاط البيع (${retailers.size})", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (distributionSubTab == 0) Color.White else TextSecondaryDark)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (distributionSubTab == 1) MikroTikPrimary else CyberDarkSurface,
                    border = BorderStroke(1.dp, if (distributionSubTab == 1) MikroTikPrimary else CyberBorder),
                    onClick = { distributionSubTab = 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (distributionSubTab == 1) Color.White else TextSecondaryDark)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("فواتير المبيعات", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (distributionSubTab == 1) Color.White else TextSecondaryDark)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (distributionSubTab == 2) MikroTikPrimary else CyberDarkSurface,
                    border = BorderStroke(1.dp, if (distributionSubTab == 2) MikroTikPrimary else CyberBorder),
                    onClick = { distributionSubTab = 2 },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (distributionSubTab == 2) Color.White else TextSecondaryDark)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("المخزن بالعدد", fontFamily = CairoFontFamily, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (distributionSubTab == 2) Color.White else TextSecondaryDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (distributionSubTab) {
                1 -> {
                    // تبويب فواتير مبيعات الكروت
                    CardSalesInvoicesSubScreen(
                        viewModel = viewModel,
                        onOpenCreateInvoice = {
                            selectedRetailerForInvoice = null
                            showCreateInvoiceDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                2 -> {
                    // تبويب مخزن الأصناف بالعدد
                    CardStockCategoryTab(
                        viewModel = viewModel,
                        onIssueInvoiceForPackage = { pkgName ->
                            selectedRetailerForInvoice = null
                            showCreateInvoiceDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    // تبويب نقاط البيع والديون
                    // Debt & Cards Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "إجمالي ديون المحلات",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${totalDebt.toInt()} ريال",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusWarning
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "كروت بحوزة البقالات",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalActiveCardsWithRetailers كرت",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
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
                                    onWhatsApp = {
                                        if (retailer.phone.isBlank()) {
                                            Toast.makeText(context, "يرجى إضافة رقم الهاتف للبقالة أولاً لتفعيل الواتساب", Toast.LENGTH_SHORT).show()
                                        } else {
                                            retailerForWhatsAppMenu = retailer
                                        }
                                    },
                                    onClone = {
                                        retailerToEdit = retailer.copy(
                                            id = 0L,
                                            name = "${retailer.name} (فرع جديد)",
                                            balanceOwed = 0.0,
                                            totalPaid = 0.0,
                                            activeCardsCount = 0
                                        )
                                        Toast.makeText(context, "تم استنساخ بيانات البقالة. عدّل التفاصيل ثم احفظ", Toast.LENGTH_SHORT).show()
                                    },
                                    onEdit = {
                                        retailerToEdit = retailer
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
            }
        }

        // FAB to Add Retailer
        if (distributionSubTab == 0) {
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
        }

        // Add Retailer Dialog
        if (showAddRetailerDialog) {
            AddEditRetailerDialog(
                initialRetailer = null,
                onDismiss = { showAddRetailerDialog = false },
                onSave = { newRetailer ->
                    viewModel.saveRetailer(newRetailer) {
                        showAddRetailerDialog = false
                        Toast.makeText(context, "تمت إضافة البقالة وربط الحساب بالواتساب بنجاح ✓", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Edit Retailer Dialog
        retailerToEdit?.let { existingRetailer ->
            AddEditRetailerDialog(
                initialRetailer = existingRetailer,
                onDismiss = { retailerToEdit = null },
                onSave = { updatedRetailer ->
                    viewModel.saveRetailer(updatedRetailer) {
                        retailerToEdit = null
                        Toast.makeText(context, "تم تحديث بيانات البقالة بنجاح ✓", Toast.LENGTH_SHORT).show()
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
                onConfirm = { inventoryId, retailerId, qty, sendWhatsAppNotice ->
                    val chosenRetailer = retailers.find { it.id == retailerId }
                    val chosenItem = inventoryItems.find { it.id == inventoryId }

                    viewModel.distributeFromInventory(inventoryId, retailerId, qty) { success ->
                        showDistributeDialog = false
                        if (success) {
                            Toast.makeText(context, "تم تسليم $qty كرت للبقالة وقيد المبلغ على الحساب ✓", Toast.LENGTH_LONG).show()

                            if (sendWhatsAppNotice && chosenRetailer != null && chosenItem != null && chosenRetailer.phone.isNotBlank()) {
                                val totalWholesale = chosenItem.wholesalePrice * qty
                                val message = WhatsAppHelper.generateCardDeliveryReceiptMessage(
                                    retailer = chosenRetailer,
                                    batchName = chosenItem.packageName,
                                    quantity = qty,
                                    totalWholesale = totalWholesale
                                )
                                WhatsAppHelper.sendWhatsAppMessage(context, chosenRetailer.phone, message)
                            }
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
                onConfirm = { amount, method, desc, sendWhatsAppReceipt ->
                    viewModel.createVoucher(
                        voucherType = "RECEIPT",
                        amount = amount,
                        partyName = r.name,
                        retailerId = r.id,
                        category = "توريد مبيعات كروت",
                        paymentMethod = method,
                        description = desc
                    ) { voucherNumber ->
                        showQuickPayDialog = null
                        Toast.makeText(context, "تم إنشاء سند قبض بمبلغ $amount ريال وتخفيض مديونية البقالة ✓", Toast.LENGTH_LONG).show()

                        if (sendWhatsAppReceipt && r.phone.isNotBlank()) {
                            val savedReceiptVoucher = com.example.data.local.entity.FinancialVoucherEntity(
                                voucherNumber = voucherNumber,
                                voucherType = "RECEIPT",
                                amount = amount,
                                partyName = r.name,
                                retailerId = r.id,
                                category = "توريد مبيعات كروت",
                                paymentMethod = method,
                                description = desc,
                                issuerName = "المهندس حسن"
                            )
                            val message = WhatsAppHelper.generateVoucherMessage(savedReceiptVoucher)
                            WhatsAppHelper.sendWhatsAppMessage(context, r.phone, message)
                        }
                    }
                }
            )
        }

        // WhatsApp Options Dialog for Retailer
        retailerForWhatsAppMenu?.let { r ->
            AlertDialog(
                onDismissRequest = { retailerForWhatsAppMenu = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مراسلة ${r.name} عبر الواتساب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "رقم الواتساب المسجل: ${r.phone}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "اختر نوع المعاملة المراد إرسالها للبقالة عبر الواتساب بتذييل (${WhatsAppHelper.NETWORK_BRAND_NAME}):",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        // 1. كشف حساب ومطابقة رصيد
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WhatsAppGreen.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.3f)),
                            onClick = {
                                val msg = WhatsAppHelper.generateRetailerStatementMessage(r)
                                WhatsAppHelper.sendWhatsAppMessage(context, r.phone, msg)
                                retailerForWhatsAppMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = WhatsAppDarkGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("إرسال كشف حساب ومطابقة رصيد", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WhatsAppDarkGreen)
                                    Text("المستحق: ${r.balanceOwed.toInt()} ر.ي • الكروت: ${r.activeCardsCount} كرت", fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }
                        }

                        // 2. فتح محادثة مباشرة
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            onClick = {
                                val msg = "مرحباً ${r.name}، بخصوص حسابكم لدى ${WhatsAppHelper.NETWORK_BRAND_NAME}."
                                WhatsAppHelper.sendWhatsAppMessage(context, r.phone, msg)
                                retailerForWhatsAppMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("فتح محادثة واتساب مباشرة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("محادثة فورية مع مسؤول المحل", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { retailerForWhatsAppMenu = null }) {
                        Text("إلغاء")
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

        // نافذة إصدار فاتورة مبيعات كروت احترافية
        if (showCreateInvoiceDialog) {
            CreateCardSalesInvoiceDialog(
                inventoryItems = inventoryItems,
                retailers = retailers,
                preSelectedPackageName = null,
                onDismiss = {
                    showCreateInvoiceDialog = false
                    selectedRetailerForInvoice = null
                },
                onConfirmInvoice = { customerName, customerPhone, retailerId, items, paymentType, paidAmount, notes ->
                    viewModel.issueMultiItemSalesInvoice(
                        customerName = customerName,
                        customerPhone = customerPhone,
                        retailerId = retailerId,
                        items = items,
                        paymentType = paymentType,
                        paidAmount = paidAmount,
                        notes = notes
                    ) { invoiceId ->
                        showCreateInvoiceDialog = false
                        selectedRetailerForInvoice = null
                        Toast.makeText(context, "تم إصدار فاتورة المبيعات وخصم الكميات من المخزن بنجاح ✓", Toast.LENGTH_LONG).show()
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
    onWhatsApp: () -> Unit,
    onClone: () -> Unit,
    onEdit: () -> Unit,
    onDistribute: () -> Unit,
    onQuickPay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        border = BorderStroke(1.dp, CyberBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberDarkCardElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MikroTikCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = retailer.name,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "المسؤول: ${retailer.ownerName.ifBlank { "غير محدد" }} • عمولة ${retailer.commissionPercent.toInt()}%",
                            fontFamily = CairoFontFamily,
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Balance Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الرصيد المستحق:",
                        fontFamily = CairoFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "${retailer.balanceOwed.toInt()} ريال",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (retailer.balanceOwed > 0) StatusWarning else StatusOnline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location & Phone / WhatsApp Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = retailer.location.ifBlank { "لم يحدد الموقع" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (retailer.phone.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // WhatsApp Quick Action Button
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WhatsAppGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.4f)),
                            onClick = onWhatsApp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = WhatsAppDarkGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "واتساب",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppDarkGreen
                                )
                            }
                        }

                        // Direct Call Action
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            onClick = onCall
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "اتصال", tint = StatusOnline, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = retailer.phone,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row (Cards held, Total paid, WhatsApp status)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الكروت بحوزته: ${retailer.activeCardsCount} كرت",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (retailer.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("مربوط بالواتساب", fontSize = 10.sp, color = WhatsAppDarkGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onClone, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "استنساخ البقالة", tint = InvestmentGold, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                    }
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
fun AddEditRetailerDialog(
    initialRetailer: RetailerEntity? = null,
    onDismiss: () -> Unit,
    onSave: (RetailerEntity) -> Unit
) {
    val isEditing = initialRetailer != null
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

    var name by remember(initialRetailer) { mutableStateOf(initialRetailer?.name ?: "") }
    var ownerName by remember(initialRetailer) { mutableStateOf(initialRetailer?.ownerName ?: "") }
    var phone by remember(initialRetailer) { mutableStateOf(initialRetailer?.phone ?: "") }
    var location by remember(initialRetailer) { mutableStateOf(initialRetailer?.location ?: "") }
    var commission by remember(initialRetailer) { mutableStateOf((initialRetailer?.commissionPercent ?: 10.0).toInt().toString()) }
    var notes by remember(initialRetailer) { mutableStateOf(initialRetailer?.notes ?: "") }

    // Contact Picker
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                try {
                    context.contentResolver.query(
                        contactUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        ),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            if (numIdx != -1) {
                                val rawNum = cursor.getString(numIdx) ?: ""
                                phone = rawNum.replace("[^0-9+]".toRegex(), "")
                            }
                            if (nameIdx != -1) {
                                val contactName = cursor.getString(nameIdx) ?: ""
                                if (ownerName.isBlank() && contactName.isNotBlank()) {
                                    ownerName = contactName
                                }
                                if (name.isBlank() && contactName.isNotBlank()) {
                                    name = "بقالة $contactName"
                                }
                            }
                            Toast.makeText(context, "تم استيراد الرقم والاسم من جهات الاتصال بنجاح ✓", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "تعذر استيراد جهة الاتصال: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val openContactsBook: () -> Unit = {
        try {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        } catch (e: Exception) {
            try {
                val genericIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                contactPickerLauncher.launch(genericIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "تعذر فتح دفتر الهاتف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialRetailer == null || initialRetailer.id == 0L) {
                    if (initialRetailer?.id == 0L) "استنساخ وتسجيل بقالة جديدة" else "إضافة بقالة أو نقطة بيع جديدة"
                } else "تعديل بيانات البقالة / نقطة البيع",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                // Phone field with Contact Picker button
                item {
                    Column {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف / الواتساب *") },
                            placeholder = { Text("77XXXXXXX") },
                            trailingIcon = {
                                IconButton(onClick = openContactsBook) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = "اختيار من دفتر الهاتف",
                                        tint = MikroTikPrimary
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = openContactsBook,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار الرقم من دفتر الهاتف / جهات الاتصال", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // WhatsApp banner
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WhatsAppGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = WhatsAppDarkGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ربط الحساب بنظام واتساب شبكة طلقة نت ✓",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppDarkGreen
                                )
                                Text(
                                    text = "إرسال الفواتير وسندات القبض وكشوفات الحساب للبقالة بنقرة واحدة بتذييل (${WhatsAppHelper.NETWORK_BRAND_NAME})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF1F2937)
                                )
                            }
                        }
                    }
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

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات إضافية (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSaving && name.isNotBlank()) {
                        isSaving = true
                        val entity = (initialRetailer ?: RetailerEntity(
                            name = "",
                            ownerName = "",
                            phone = "",
                            location = ""
                        )).copy(
                            name = name.trim(),
                            ownerName = ownerName.trim(),
                            phone = phone.trim(),
                            location = location.trim(),
                            commissionPercent = commission.toDoubleOrNull() ?: 10.0,
                            notes = notes.trim()
                        )
                        onSave(entity)
                    }
                },
                enabled = !isSaving && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
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
                    Text(
                        if (isEditing && initialRetailer?.id != 0L) "تحديث البيانات" else "حفظ وربط البقالة",
                        fontWeight = FontWeight.Bold
                    )
                }
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
    onConfirm: (inventoryId: Long, retailerId: Long, quantity: Int, sendWhatsAppNotice: Boolean) -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }
    var selectedRetailer by remember { mutableStateOf(initialRetailer ?: retailers.firstOrNull()) }
    var selectedItem by remember { mutableStateOf(inventoryItems.firstOrNull()) }
    var quantityText by remember { mutableStateOf("20") }
    var sendWhatsAppNotice by remember { mutableStateOf(true) }

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
                                text = { Text("${r.name} (${r.location})") },
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

                // WhatsApp Receipt Notification Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhatsAppGreen.copy(alpha = 0.1f))
                        .clickable { sendWhatsAppNotice = !sendWhatsAppNotice }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sendWhatsAppNotice,
                        onCheckedChange = { sendWhatsAppNotice = it },
                        colors = CheckboxDefaults.colors(checkedColor = WhatsAppDarkGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مشاركة إشعار الاستلام للبقالة عبر الواتساب (${WhatsAppHelper.NETWORK_BRAND_NAME})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WhatsAppDarkGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSaving && selectedItem != null && selectedRetailer != null && qty > 0) {
                        isSaving = true
                        onConfirm(selectedItem!!.id, selectedRetailer!!.id, qty, sendWhatsAppNotice)
                    }
                },
                enabled = !isSaving && selectedItem != null && selectedRetailer != null && qty > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري التسليم...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text("تأكيد التسليم والقيد", fontWeight = FontWeight.Bold)
                }
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
    onConfirm: (amount: Double, method: String, desc: String, sendWhatsAppReceipt: Boolean) -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf(retailer.balanceOwed.toInt().toString()) }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var description by remember { mutableStateOf("سداد قيمة كروت هوتسبوت مباعة") }
    var sendWhatsAppReceipt by remember { mutableStateOf(retailer.phone.isNotBlank()) }

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

                // WhatsApp checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WhatsAppGreen.copy(alpha = 0.1f))
                        .clickable { sendWhatsAppReceipt = !sendWhatsAppReceipt }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sendWhatsAppReceipt,
                        onCheckedChange = { sendWhatsAppReceipt = it },
                        colors = CheckboxDefaults.colors(checkedColor = WhatsAppDarkGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مشاركة سند القبض عبر الواتساب فوراً (${WhatsAppHelper.NETWORK_BRAND_NAME})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WhatsAppDarkGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (!isSaving && amt > 0) {
                        isSaving = true
                        onConfirm(amt, paymentMethod, description, sendWhatsAppReceipt)
                    }
                },
                enabled = !isSaving && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = ReceiptGreen)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الإصدار...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text("إصدار سند القبض", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
