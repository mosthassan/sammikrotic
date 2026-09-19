package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.ParsedInvoiceData
import com.example.ui.MainViewModel
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikDarkBg
import com.example.ui.theme.MikroTikDarkSurface
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikNavyLight
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.ReceiptGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartInvoiceScannerDialog(
    viewModel: MainViewModel,
    initialTargetType: String = "ASSETS", // "ASSETS" or "EXPENSES"
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanningInvoice.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasParsedInvoice by remember { mutableStateOf(false) }

    // Invoice Header Fields
    var supplierName by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var invoiceDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())) }
    var targetType by remember { mutableStateOf(initialTargetType) }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }
    var saveAsAssets by remember { mutableStateOf(initialTargetType == "ASSETS") }
    var saveAsVoucher by remember { mutableStateOf(true) }

    // Dynamic Items List
    val itemsList = remember { mutableStateListOf<InvoiceItem>() }

    // Launchers for Camera & Gallery
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            viewModel.scanInvoiceWithAi(
                bitmap = bitmap,
                onResult = { parsed ->
                    populateFieldsFromParsed(
                        parsed = parsed,
                        setSupplier = { supplierName = it },
                        setNumber = { invoiceNumber = it },
                        setDate = { if (it.isNotBlank()) invoiceDate = it },
                        setTarget = { targetType = it },
                        setNotes = { notes = it },
                        itemsList = itemsList
                    )
                    hasParsedInvoice = true
                    Toast.makeText(context, "تم تحليل الفاتورة بنجاح بواسطة الذكاء الاصطناعي!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, "تنبيه: $err (تم توليد بيانات نموذجية للمراجعة)", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = uriToBitmap(context, uri)
            if (bitmap != null) {
                capturedBitmap = bitmap
                viewModel.scanInvoiceWithAi(
                    bitmap = bitmap,
                    onResult = { parsed ->
                        populateFieldsFromParsed(
                            parsed = parsed,
                            setSupplier = { supplierName = it },
                            setNumber = { invoiceNumber = it },
                            setDate = { if (it.isNotBlank()) invoiceDate = it },
                            setTarget = { targetType = it },
                            setNotes = { notes = it },
                            itemsList = itemsList
                        )
                        hasParsedInvoice = true
                        Toast.makeText(context, "تم تحليل الفاتورة بنجاح بواسطة الذكاء الاصطناعي!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "تنبيه: $err (تم توليد بيانات نموذجية للمراجعة)", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val totalCalculated = itemsList.sumOf { it.subtotal }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MikroTikDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MikroTikCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تحويل صورة الفاتورة بالذكاء الاصطناعي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "تصوير الفاتورة واستخراج الأصناف والأسعار والاعتماد",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_invoice_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // If currently scanning with AI: show animated spinner
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MikroTikCyan,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "جارٍ قراءة وفحص صورة الفاتورة بواسطة الذكاء الاصطناعي (Gemini Vision)...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "استخراج اسم المورد، التاريخ، قائمة الأصناف، الكميات، والأسعار بدقة",
                                color = MikroTikCyan,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (!hasParsedInvoice && capturedBitmap == null) {
                    // Phase 1: Capture or Pick Image
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MikroTikNavyLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "صور فاتورة الشراء أو الأصول",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "التقط صورة واضحة لفاتورة المشتريات الورقية أو اخترها من المعرض ليقوم الذكاء الاصطناعي بتحويلها تلقائياً إلى أصناف وجدول جاهز للمراجعة والاعتماد.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Capture Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_camera_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تصوير بالكاميرا", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_gallery_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MikroTikCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MikroTikCyan)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اختيار من المعرض")
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Test / Demo button
                        TextButton(
                            onClick = {
                                // Provide instant sample invoice for rapid testing
                                val sample = ParsedInvoiceData(
                                    supplierName = "مؤسسة الرواد للشبكات والأجهزة",
                                    invoiceNumber = "INV-${(1000..9999).random()}",
                                    invoiceDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()),
                                    invoiceType = initialTargetType,
                                    totalAmount = 264000.0,
                                    currency = "YER",
                                    notes = "فاتورة مشتريات وتجهيزات محطة السبعين",
                                    items = listOf(
                                        InvoiceItem(name = "راوتر MikroTik CCR2004-16G-2S+", quantity = 1.0, unitPrice = 165000.0, subtotal = 165000.0, category = "SERVERS"),
                                        InvoiceItem(name = "لفة كابل شبكة Cat6 خارجي 305 متر", quantity = 2.0, unitPrice = 32000.0, subtotal = 64000.0, category = "CABLES"),
                                        InvoiceItem(name = "بطارية جل 150 أمبير للطاقة البديلة", quantity = 1.0, unitPrice = 35000.0, subtotal = 35000.0, category = "SOLAR_POWER")
                                    )
                                )
                                populateFieldsFromParsed(
                                    parsed = sample,
                                    setSupplier = { supplierName = it },
                                    setNumber = { invoiceNumber = it },
                                    setDate = { if (it.isNotBlank()) invoiceDate = it },
                                    setTarget = { targetType = it },
                                    setNotes = { notes = it },
                                    itemsList = itemsList
                                )
                                hasParsedInvoice = true
                                Toast.makeText(context, "تم تحميل فاتورة تجريبية ذكية للمراجعة والتعديل", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InvestmentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("أو جرب فاتورة جاهزة للتجربة والتقييم السريع", color = InvestmentGold, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Phase 2: Review, Edit, Add & Approve Structured Invoice
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Image Thumbnail & Retake Bar
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MikroTikNavyLight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (capturedBitmap != null) {
                                            Image(
                                                bitmap = capturedBitmap!!.asImageBitmap(),
                                                contentDescription = "صورة الفاتورة",
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = ProfitEmerald,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "تم استخراج البيانات بالذكاء الاصطناعي",
                                                    color = ProfitEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Text(
                                                text = "يمكنك تعديل أي صنف أو كمية أو سعر قبل الاعتماد",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { cameraLauncher.launch() }) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = "إعادة التصوير", tint = MikroTikCyan)
                                        }
                                        IconButton(onClick = {
                                            galleryLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = "اختيار صورة أخرى", tint = MikroTikCyan)
                                        }
                                    }
                                }
                            }
                        }

                        // Invoice General Info
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "بيانات الفاتورة والمورد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MikroTikCyan
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = supplierName,
                                        onValueChange = { supplierName = it },
                                        label = { Text("اسم المورد / المتجر") },
                                        modifier = Modifier.fillMaxWidth().testTag("supplier_name_input"),
                                        colors = darkTextFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = invoiceNumber,
                                            onValueChange = { invoiceNumber = it },
                                            label = { Text("رقم الفاتورة") },
                                            modifier = Modifier.weight(1f).testTag("invoice_number_input"),
                                            colors = darkTextFieldColors()
                                        )

                                        OutlinedTextField(
                                            value = invoiceDate,
                                            onValueChange = { invoiceDate = it },
                                            label = { Text("تاريخ الفاتورة") },
                                            modifier = Modifier.weight(1f).testTag("invoice_date_input"),
                                            colors = darkTextFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Target Type selector: ASSETS vs EXPENSES
                                    Text(
                                        text = "نوع الفاتورة والوجهة:",
                                        fontSize = 12.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        InvoiceTargetOption(
                                            title = "أصول رأسمالية (CAPEX)",
                                            subtitle = "راوترات، أبراج، بطاريات...",
                                            isSelected = targetType == "ASSETS",
                                            selectedColor = AssetPurple,
                                            onClick = {
                                                targetType = "ASSETS"
                                                saveAsAssets = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        InvoiceTargetOption(
                                            title = "مصروفات مشتريات (OPEX)",
                                            subtitle = "ديزل، صيانة، كابلات...",
                                            isSelected = targetType == "EXPENSES",
                                            selectedColor = PaymentRed,
                                            onClick = {
                                                targetType = "EXPENSES"
                                                saveAsAssets = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Checkboxes for saving
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = saveAsVoucher,
                                            onCheckedChange = { saveAsVoucher = it },
                                            colors = CheckboxDefaults.colors(checkedColor = ProfitEmerald)
                                        )
                                        Text(
                                            text = "تسجيل سند صرف مالي تلقائي في المحاسبة",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = saveAsAssets,
                                            onCheckedChange = { saveAsAssets = it },
                                            colors = CheckboxDefaults.colors(checkedColor = AssetPurple)
                                        )
                                        Text(
                                            text = "إضافة الأصناف إلى سجل الأصول الثابتة (CAPEX)",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Items Section Header with "+ إضافة صنف"
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "الأصناف المستخرجة (${itemsList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        itemsList.add(
                                            InvoiceItem(
                                                name = "صنف جديد",
                                                quantity = 1.0,
                                                unitPrice = 0.0,
                                                subtotal = 0.0,
                                                category = if (targetType == "ASSETS") "SERVERS" else "MAINTENANCE"
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp).testTag("add_item_to_invoice_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة صنف", fontSize = 12.sp)
                                }
                            }
                        }

                        // List of items
                        itemsIndexed(itemsList) { index, item ->
                            InvoiceItemCard(
                                item = item,
                                index = index,
                                onUpdate = { updatedItem ->
                                    itemsList[index] = updatedItem
                                },
                                onDelete = {
                                    itemsList.removeAt(index)
                                }
                            )
                        }

                        // Grand Total Box
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MikroTikNavyLight),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MikroTikCyan.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "إجمالي الفاتورة المعتمد:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${formatMoney(totalCalculated)} ريال",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = ProfitEmerald
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("ملاحظات الفاتورة") },
                                        modifier = Modifier.fillMaxWidth().testTag("invoice_notes_input"),
                                        colors = darkTextFieldColors()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Actions: Approve & Save vs Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (itemsList.isEmpty()) {
                                    Toast.makeText(context, "يرجى إضافة صنف واحد على الأقل للفاتورة", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val finalInvoice = PurchaseInvoiceEntity(
                                    invoiceNumber = invoiceNumber.ifBlank { "INV-${System.currentTimeMillis() % 100000}" },
                                    supplierName = supplierName.ifBlank { "مورد أجهزة ومعدات" },
                                    invoiceDateMillis = try {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(invoiceDate)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        System.currentTimeMillis()
                                    },
                                    targetType = targetType,
                                    totalAmount = totalCalculated,
                                    paidAmount = totalCalculated,
                                    paymentMethod = paymentMethod,
                                    notes = notes
                                )

                                viewModel.approveAndSaveInvoice(
                                    invoice = finalInvoice,
                                    items = itemsList.toList(),
                                    saveAsAssets = saveAsAssets,
                                    saveAsVoucher = saveAsVoucher,
                                    onComplete = {
                                        Toast.makeText(context, "تم اعتماد وحفظ الفاتورة والأصناف بنجاح! ✓", Toast.LENGTH_LONG).show()
                                        onDismissRequest()
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp)
                                .testTag("approve_and_save_invoice_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اعتماد وحفظ الفاتورة والأصناف", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceTargetOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedColor.copy(alpha = 0.2f) else MikroTikNavyLight
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) selectedColor else Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) selectedColor else Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InvoiceItemCard(
    item: InvoiceItem,
    index: Int,
    onUpdate: (InvoiceItem) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var qtyStr by remember(item.id) { mutableStateOf(item.quantity.toString()) }
    var priceStr by remember(item.id) { mutableStateOf(item.unitPrice.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صنف #${index + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MikroTikCyan
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_item_${index}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الصنف",
                        tint = PaymentRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onUpdate(item.copy(name = it))
                },
                label = { Text("اسم الصنف أو المعدة") },
                modifier = Modifier.fillMaxWidth().testTag("item_name_${index}"),
                colors = darkTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = {
                        qtyStr = it
                        val q = it.toDoubleOrNull() ?: 1.0
                        val p = priceStr.toDoubleOrNull() ?: item.unitPrice
                        onUpdate(item.copy(quantity = q, subtotal = q * p))
                    },
                    label = { Text("الكمية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("item_qty_${index}"),
                    colors = darkTextFieldColors()
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = {
                        priceStr = it
                        val p = it.toDoubleOrNull() ?: 0.0
                        val q = qtyStr.toDoubleOrNull() ?: item.quantity
                        onUpdate(item.copy(unitPrice = p, subtotal = q * p))
                    },
                    label = { Text("سعر الوحدة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.5f).testTag("item_price_${index}"),
                    colors = darkTextFieldColors()
                )

                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "الإجمالي:",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${formatMoney(item.subtotal)} ريال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ProfitEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MikroTikCyan,
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = MikroTikCyan,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = MikroTikCyan
)

private fun populateFieldsFromParsed(
    parsed: ParsedInvoiceData,
    setSupplier: (String) -> Unit,
    setNumber: (String) -> Unit,
    setDate: (String) -> Unit,
    setTarget: (String) -> Unit,
    setNotes: (String) -> Unit,
    itemsList: MutableList<InvoiceItem>
) {
    setSupplier(parsed.supplierName)
    setNumber(parsed.invoiceNumber)
    setDate(parsed.invoiceDate)
    setTarget(parsed.invoiceType)
    setNotes(parsed.notes)
    itemsList.clear()
    itemsList.addAll(parsed.items)
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }
}

private fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "%,.0f", amount)
}
