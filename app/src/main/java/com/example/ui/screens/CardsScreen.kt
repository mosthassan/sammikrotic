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
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.MikroTikNavy
import com.example.ui.theme.MikroTikPrimary
import com.example.ui.theme.StatusDistributed
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning

@Composable
fun CardsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val batches by viewModel.cardBatches.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val availableCount by viewModel.availableCardsCount.collectAsState()
    val distributedCount by viewModel.distributedCardsCount.collectAsState()
    val soldCount by viewModel.soldCardsCount.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedStatusFilter by remember { mutableStateOf("الكل") }
    var screenViewMode by remember { mutableIntStateOf(0) } // 0: استوديو وتصميم الكروت (مطابق للموقع), 1: جدول وسجل المخزن
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showStudioDialog by remember { mutableStateOf(false) }
    var selectedCardForStudio by remember { mutableStateOf<CardEntity?>(null) }

    val statusFilters = listOf("الكل", "المتاحة بالمستودع", "الموزعة للمحلات", "المباعة")

    val filteredCards = cards.filter { card ->
        when (selectedStatusFilter) {
            "المتاحة بالمستودع" -> card.status == "AVAILABLE"
            "الموزعة للمحلات" -> card.status == "DISTRIBUTED"
            "المباعة" -> card.status == "SOLD"
            else -> true
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("cards_screen")) {
        when (screenViewMode) {
            0 -> {
            // نمط استوديو وتصميم الكروت وطباعتها (مطابق تماماً لموقع سام ميكروتك sammikrotic.vercel.app)
            Column(modifier = Modifier.fillMaxSize()) {
                // شريط تبديل سريع بين الاستوديو وقائمة المخزن
                Surface(
                    color = Color(0xFF091424),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF0284C7),
                                onClick = { screenViewMode = 0 }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("استوديو وتصميم الكروت A4", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1E293B),
                                onClick = { screenViewMode = 1 }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("سجل الدفعات (${cards.size})", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }

                        // Status Info
                        Text("طاقة إنتاجية متقدمة", color = Color(0xFF38BDF8), fontSize = 10.sp)
                    }
                }

                // شاشة الاستوديو الكاملة
                CardStudioScreen(
                    viewModel = viewModel,
                    onNavigateToInventory = { screenViewMode = 1 }
                )
            }
        } 1 -> {
            // نمط جدول وسجل كروت المخزن
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // شريط التبديل العلوي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مخزن كروت ودفعات ميكروتك",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "إدارة وسجل الكروت بالمستودع والتوزيع",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { screenViewMode = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استوديو الكروت", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { screenViewMode = 2 },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المخزن الفعلي", color = MikroTikPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير", color = MikroTikPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Inventory Metric Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(title = "جاهزة بالمستودع", count = availableCount, color = StatusOnline, modifier = Modifier.weight(1f))
                    MetricPill(title = "بحوزة المحلات", count = distributedCount, color = StatusDistributed, modifier = Modifier.weight(1f))
                    MetricPill(title = "مباعة ومفعلة", count = soldCount, color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(statusFilters) { filter ->
                        val isSelected = selectedStatusFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) MikroTikPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedStatusFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
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

                // Cards List
                if (filteredCards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "لا توجد كروت في هذه الحالة حالياً", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredCards, key = { it.id }) { card ->
                            CardItemRow(
                                card = card,
                                onCopy = {
                                    clipboard.setText(AnnotatedString("User: ${card.username} | PIN: ${card.password}"))
                                    Toast.makeText(context, "تم نسخ بيانات الكرت: ${card.username}", Toast.LENGTH_SHORT).show()
                                },
                                onPreview = {
                                    selectedCardForStudio = card
                                    showStudioDialog = true
                                },
                                onMarkSold = {
                                    viewModel.markCardSold(card.id)
                                    Toast.makeText(context, "تم تحديد الكرت كمباع ومفعل ✓", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }

            // FAB to Generate New Batch
            FloatingActionButton(
                onClick = { showGenerateDialog = true },
                containerColor = MikroTikPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("generate_batch_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "توليد كروت")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("توليد كروت جديدة", fontWeight = FontWeight.Bold)
                }
            }
        }
        2 -> {
            InventoryScreen(
                viewModel = viewModel,
                onNavigateToStudio = { screenViewMode = 0 },
                onNavigateToBatches = { screenViewMode = 1 }
            )
        }
    }

        // Card Studio Dialog
        if (showStudioDialog) {
            CardStudioDialog(
                sampleCards = cards,
                initialCard = selectedCardForStudio,
                onDismiss = {
                    showStudioDialog = false
                    selectedCardForStudio = null
                }
            )
        }

        // Generate Cards Dialog
        if (showGenerateDialog) {
            GenerateBatchDialog(
                onDismiss = { showGenerateDialog = false },
                onGenerate = { name, cat, retail, wholesale, mb, hours, speed, count, prefix, len, charSet, pwdPolicy ->
                    viewModel.createBatchAndGenerateCards(
                        name, cat, retail, wholesale, mb, hours, speed, count, prefix, len, charSet, pwdPolicy
                    ) {
                        showGenerateDialog = false
                        Toast.makeText(context, "تم توليد $count كرت جديد بنجاح ✓", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Export Cards Dialog
        if (showExportDialog) {
            val exportText = cards.filter { it.status == "AVAILABLE" }.take(50).joinToString("\n") {
                "${it.username},${it.password},${it.categoryName},${it.retailPrice.toInt()} YER"
            }
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("تصدير الكروت للطباعة أو ميكروتك") },
                text = {
                    Column {
                        Text(
                            text = "صيغة CSV جاهزة للطباعة أو الاستيراد في سيرفر الميكروتك (أول 50 كرت متاح):",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = exportText.ifEmpty { "لا توجد كروت متاحة بالمستودع حالياً للتصدير." },
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(exportText))
                            Toast.makeText(context, "تم نسخ قائمة الكروت إلى الحافظة", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
                    ) {
                        Text("نسخ الكل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}

@Composable
fun MetricPill(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CardItemRow(
    card: CardEntity,
    onCopy: () -> Unit,
    onPreview: () -> Unit,
    onMarkSold: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("card_row_${card.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).clickable { onPreview() }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MikroTikPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MikroTikPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.username,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PIN: ${card.password}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = "${card.categoryName} • سعر البيع: ${card.retailPrice.toInt()} ريال",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (card.retailerName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "لدى: ${card.retailerName}",
                                fontSize = 10.sp,
                                color = MikroTikPrimary
                            )
                        }
                    }
                }
            }

            // Actions & Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreview, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = "معاينة وتصميم الكرت", tint = MikroTikPrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                if (card.status == "DISTRIBUTED") {
                    Button(
                        onClick = onMarkSold,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOnline),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("تم البيع", fontSize = 10.sp)
                    }
                } else {
                    val statusText = when (card.status) {
                        "AVAILABLE" -> "متاح"
                        "SOLD" -> "مباع"
                        else -> card.status
                    }
                    val statusColor = when (card.status) {
                        "AVAILABLE" -> StatusOnline
                        "SOLD" -> Color.Gray
                        else -> StatusDistributed
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GenerateBatchDialog(
    onDismiss: () -> Unit,
    onGenerate: (
        name: String,
        cat: String,
        retail: Double,
        wholesale: Double,
        mb: Long,
        hours: Int,
        speed: String,
        count: Int,
        prefix: String,
        codeLength: Int,
        charSet: com.example.data.cards.CodeCharacterSet,
        passwordPolicy: com.example.data.cards.PasswordPolicy
    ) -> Unit
) {
    var batchName by remember { mutableStateOf("دفعة كروت فئة 200 ريال") }
    var categoryName by remember { mutableStateOf("200 ريال - 1.5GB / 24H") }
    var retailPrice by remember { mutableStateOf("200") }
    var wholesalePrice by remember { mutableStateOf("180") }
    var countText by remember { mutableStateOf("50") }
    var prefix by remember { mutableStateOf("SAM") }
    var codeLength by remember { mutableStateOf("6") }
    var selectedCharSet by remember { mutableStateOf(com.example.data.cards.CodeCharacterSet.DIGITS_ONLY) }
    var selectedPasswordPolicy by remember { mutableStateOf(com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("توليد دفعة كروت هوتسبوت جديدة", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("اسم الدفعة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("الفئة / السعة") },
                        placeholder = { Text("مثال: 500 ريال - 4GB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = retailPrice,
                            onValueChange = { retailPrice = it },
                            label = { Text("سعر المستهلك") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wholesalePrice,
                            onValueChange = { wholesalePrice = it },
                            label = { Text("سعر البقالة") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = countText,
                            onValueChange = { countText = it },
                            label = { Text("الكمية (عدد الكروت)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text("بادئة الكرت") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = codeLength,
                        onValueChange = { codeLength = it },
                        label = { Text("طول كود المستخدم (أرقام/حروف)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("نمط الرموز ومجموعة الأحرف:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.example.data.cards.CodeCharacterSet.values().forEach { set ->
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCharSet == set) MikroTikPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCharSet = set }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = set.titleArabic,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = if (selectedCharSet == set) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countText.toIntOrNull() ?: 50
                    val retail = retailPrice.toDoubleOrNull() ?: 200.0
                    val wholesale = wholesalePrice.toDoubleOrNull() ?: 180.0
                    val len = codeLength.toIntOrNull() ?: 6
                    onGenerate(
                        batchName, categoryName, retail, wholesale,
                        1500L, 24, "4M/2M", count, prefix, len,
                        selectedCharSet, selectedPasswordPolicy
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Text("توليد الكروت الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

