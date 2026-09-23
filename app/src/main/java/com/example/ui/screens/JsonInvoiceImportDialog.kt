package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ParsedInvoiceData
import com.example.ui.theme.AssetPurple
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.MikroTikDarkBg
import com.example.ui.theme.MikroTikDarkSurface
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikNavyLight
import com.example.ui.theme.PaymentRed
import com.example.ui.theme.ProfitEmerald
import com.example.util.JsonInvoiceParser
import java.util.Locale

@Composable
fun JsonInvoiceImportDialog(
    onDismissRequest: () -> Unit,
    onInvoiceImported: (ParsedInvoiceData) -> Unit
) {
    val context = LocalContext.current
    var jsonInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var previewData by remember { mutableStateOf<ParsedInvoiceData?>(null) }

    fun updateJson(content: String) {
        jsonInput = content
        if (content.isBlank()) {
            parseError = null
            previewData = null
            return
        }
        val result = JsonInvoiceParser.parse(content)
        result.onSuccess { data ->
            previewData = data
            parseError = null
        }.onFailure { err ->
            previewData = null
            parseError = err.localizedMessage ?: "صيغة JSON غير صحيحة"
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    updateJson(content)
                    Toast.makeText(context, "تم تحميل ملف JSON بنجاح ✓", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "الملف فارغ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MikroTikCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MikroTikCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "استيراد فاتورة من ملف JSON",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontFamily = CairoFontFamily
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ProfitEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "بدون استهلاك توكن ⚡",
                                        color = ProfitEmerald,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = CairoFontFamily
                                    )
                                }
                            }
                            Text(
                                text = "تفريغ وتوثيق الفاتورة فورياً وبدقة 100% كبديل للذكاء الاصطناعي",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = CairoFontFamily
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_json_import_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action Buttons Row: Pick File, Paste, Insert Sample, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .testTag("pick_json_file_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MikroTikCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار ملف JSON 📁", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.isNotBlank()) {
                                        updateJson(clipText)
                                        Toast.makeText(context, "تم لصق النص من الحافظة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر اللصق: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("لصق 📋", fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                        }

                        OutlinedButton(
                            onClick = {
                                updateJson(JsonInvoiceParser.SAMPLE_JSON)
                                Toast.makeText(context, "تم إدراج نموذج تجريبي جاهز", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InvestmentGold),
                            border = BorderStroke(1.dp, InvestmentGold.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = InvestmentGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نموذج جاهز 💡", fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                        }
                    }

                    // JSON Input Editor
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MikroTikDarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (parseError != null) PaymentRed.copy(alpha = 0.5f) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "محتوى أو كود JSON:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CairoFontFamily
                                )
                                if (jsonInput.isNotBlank()) {
                                    TextButton(
                                        onClick = { updateJson("") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تفريغ", color = PaymentRed, fontSize = 11.sp, fontFamily = CairoFontFamily)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = jsonInput,
                                onValueChange = { updateJson(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .testTag("json_invoice_input_field"),
                                placeholder = {
                                    Text(
                                        text = "قم بلصق محتوى ملف JSON هنا، أو انقر على 'اختيار ملف JSON' من الأعلى...",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.5.sp,
                                        fontFamily = CairoFontFamily
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFE2E8F0)
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MikroTikCyan,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = MikroTikNavyLight,
                                    unfocusedContainerColor = MikroTikNavyLight
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Parse Error State
                    if (parseError != null && jsonInput.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PaymentRed.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, PaymentRed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = PaymentRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("خطأ في قراءة ملف JSON", color = PaymentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = CairoFontFamily)
                                    Text(parseError ?: "", color = Color(0xFFFECACA), fontSize = 11.sp, fontFamily = CairoFontFamily)
                                }
                            }
                        }
                    }

                    // Parsed Preview Card
                    if (previewData != null) {
                        val data = previewData!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F243A)),
                            border = BorderStroke(1.5.dp, ProfitEmerald.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم تفريغ بيانات الفاتورة بنجاح ✓",
                                            color = ProfitEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            fontFamily = CairoFontFamily
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (data.invoiceType == "ASSETS") AssetPurple.copy(alpha = 0.25f) else InvestmentGold.copy(alpha = 0.25f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (data.invoiceType == "ASSETS") "أصول ثابتة (CAPEX)" else "مشتريات تشغيلية (OPEX)",
                                            color = if (data.invoiceType == "ASSETS") AssetPurple else InvestmentGold,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = CairoFontFamily
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Supplier & Invoice Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("المورد:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = CairoFontFamily)
                                        Text(data.supplierName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, fontFamily = CairoFontFamily)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("رقم الفاتورة:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = CairoFontFamily)
                                        Text(data.invoiceNumber, color = MikroTikCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = CairoFontFamily)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("التاريخ:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = CairoFontFamily)
                                        Text(data.invoiceDate, color = Color(0xFFCBD5E1), fontSize = 11.5.sp, fontFamily = CairoFontFamily)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Items List Preview
                                Text(
                                    text = "الأصناف المستخرجة (${data.items.size} أصناف):",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CairoFontFamily
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    data.items.take(4).forEachIndexed { idx, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Text("${idx + 1}. ", color = Color(0xFF64748B), fontSize = 11.sp)
                                                Text(item.name, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, fontFamily = CairoFontFamily)
                                            }
                                            Text(
                                                text = "${item.quantity.toInt()} × ${String.format(Locale.US, "%,.0f", item.unitPrice)} = ${String.format(Locale.US, "%,.0f", item.subtotal)} ${data.currency}",
                                                color = ProfitEmerald,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (data.items.size > 4) {
                                        Text(
                                            text = "... وباقي ${data.items.size - 4} أصناف أخرى",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Total Amount Box
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ProfitEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("إجمالي قيمة الفاتورة المستوردة:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = CairoFontFamily)
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", data.totalAmount)} ${data.currency}",
                                        color = ProfitEmerald,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء", fontFamily = CairoFontFamily)
                    }

                    Button(
                        onClick = {
                            val data = previewData
                            if (data != null) {
                                onInvoiceImported(data)
                                onDismissRequest()
                            } else {
                                Toast.makeText(context, "يرجى إدخال أو اختيار ملف JSON صالح أولاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = previewData != null,
                        modifier = Modifier
                            .weight(2f)
                            .height(46.dp)
                            .testTag("apply_json_import_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfitEmerald,
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اعتماد وتحميل الفاتورة للمراجعة والترحيل ✓",
                            fontWeight = FontWeight.Bold,
                            fontFamily = CairoFontFamily,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}
