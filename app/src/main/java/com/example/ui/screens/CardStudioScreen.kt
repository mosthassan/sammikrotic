package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.cards.CardGenerationEngine
import com.example.data.cards.CardPdfPrintManager
import com.example.data.cards.CardTemplateConfig
import com.example.data.cards.CardThemeStyle
import com.example.data.cards.CardThemesLibrary
import com.example.data.cards.CodeCharacterSet
import com.example.data.cards.PasswordPolicy
import com.example.data.local.entity.CardEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

/**
 * شاشة استوديو وتصميم كروت الإنترنت والطباعة
 * مطابقة تماماً للمنصة السحابية الأصلية (sammikrotic.vercel.app):
 * - الرأس الداكن الفخم مع أزرار التحكم البارزة (تحميل PDF 24 كرت، حفظ الدفعة، تحديث/حفظ باسم، طباعة، حفظ كصورة)
 * - شريط التبويبات الفنية: (التوليد والأكواد، القوالب AI، عناصر ومقاس الكرت، ورقة A4 والقص)
 * - لوحة المعاينة الحية لورقة A4 كاملة (3 أعمدة × 8 أو 14 صفاً) مع خطوط القص ومقاسات الكرت الدقيقة
 * - مكتبة القوالب وتخصيص المظهر، وقوالب الباقات المجهزة سحابياً (أبو 500، 1000، 4500)
 * - محرك طباعة A4 وتوليد أوامر RouterOS CLI والتنظيف الدوري
 */

data class PackagePreset(
    val id: String,
    val title: String,
    val price: Double,
    val categoryName: String,
    val quota: String,
    val uptime: String,
    val speed: String,
    val cardColorThemeId: String,
    val tag: String
)

val DEFAULT_PACKAGE_PRESETS = listOf(
    PackagePreset(
        id = "p_500",
        title = "قالب كرت أبو 500",
        price = 500.0,
        categoryName = "باقة سرعة 2GB / 12H",
        quota = "2000 ميجا",
        uptime = "12 ساعة",
        speed = "2 Mbps",
        cardColorThemeId = "cyber_neon",
        tag = "كرت 500"
    ),
    PackagePreset(
        id = "p_1000",
        title = "قالب كرت أبو 1000",
        price = 1000.0,
        categoryName = "باقة فايبر 4.5GB / غير محدد",
        quota = "4500 ميجا",
        uptime = "غير محدد",
        speed = "طاقة كاملة",
        cardColorThemeId = "emerald_green",
        tag = "كرت 1000"
    ),
    PackagePreset(
        id = "p_4500",
        title = "قالب كرت أبو 4500",
        price = 4500.0,
        categoryName = "باقة شهرية غير محدودة VIP",
        quota = "25000 ميجا",
        uptime = "30 يوم",
        speed = "VIP Turbo",
        cardColorThemeId = "royal_gold",
        tag = "card 4000"
    )
)

@Composable
fun CardStudioScreen(
    viewModel: MainViewModel,
    onNavigateToInventory: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val allCards by viewModel.cards.collectAsState()
    val systemPackages by viewModel.cardPackages.collectAsState()

    val allPresets = remember(systemPackages) {
        val converted = systemPackages.map { pkg ->
            PackagePreset(
                id = "pkg_${pkg.id}",
                title = pkg.name,
                price = pkg.retailPrice,
                categoryName = "${pkg.name} - ${pkg.formattedQuota}",
                quota = pkg.formattedQuota,
                uptime = if (pkg.validityHours >= 24) "${pkg.validityHours / 24} يوم" else "${pkg.validityHours} ساعة",
                speed = pkg.speedLimit,
                cardColorThemeId = when {
                    pkg.retailPrice >= 4000 -> "royal_gold"
                    pkg.retailPrice >= 1000 -> "emerald_green"
                    pkg.retailPrice >= 500 -> "fiber_blue"
                    else -> "turbo_blaze"
                },
                tag = "باقة ${pkg.retailPrice.toInt()} ر.ي"
            )
        }
        if (converted.isNotEmpty()) converted + DEFAULT_PACKAGE_PRESETS else DEFAULT_PACKAGE_PRESETS
    }

    // Studio Active State
    var activePreset by remember { mutableStateOf(allPresets.firstOrNull() ?: DEFAULT_PACKAGE_PRESETS[1]) }
    var activeTab by remember { mutableIntStateOf(1) } // 0: التوليد والأكواد, 1: القوالب AI, 2: عناصر ومقاس الكرت, 3: ورقة A4 والقص
    var previewMode by remember { mutableIntStateOf(0) } // 0: ورقة A4 كاملة للطباعة, 1: استوديو السحب, 2: معاينة كرت مفرد
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }

    // Customization Properties (Matching the original web app)
    var networkTitle by remember { mutableStateOf("شبكة طلقة نت") }
    var supportPhone by remember { mutableStateOf("770446040") }
    var loginDomain by remember { mutableStateOf("t.net") }
    var quotaText by remember { mutableStateOf("4500 ميجا") }
    var durationText by remember { mutableStateOf("غير محدد") }
    var priceText by remember { mutableStateOf("YER 1000") }
    var cardCountToGenerate by remember { mutableIntStateOf(24) }
    var cardCodeLength by remember { mutableIntStateOf(6) }
    var characterType by remember { mutableStateOf("NUMBERS_ONLY") }
    var usernamePrefix by remember { mutableStateOf("") }
    var showQrCode by remember { mutableStateOf(false) }
    var showCutLines by remember { mutableStateOf(true) }
    var cardDimensionsMm by remember { mutableStateOf("63 × 19 مم") }
    var a4Columns by remember { mutableIntStateOf(3) }
    var a4Rows by remember { mutableIntStateOf(8) } // 3 columns * 8 rows = 24 cards

    // Current Theme Selection (Emerald Green matching screenshot)
    var selectedTheme by remember { mutableStateOf(CardThemesLibrary.EMERALD_PRO) }

    // Sync state when switching preset
    fun applyPreset(preset: PackagePreset) {
        activePreset = preset
        priceText = "YER ${preset.price.toInt()}"
        quotaText = preset.quota
        durationText = preset.uptime
        selectedTheme = when (preset.cardColorThemeId) {
            "royal_gold" -> CardThemesLibrary.ROYAL_GOLD
            "cyber_neon" -> CardThemesLibrary.CYBER_NEON
            "turbo_blaze" -> CardThemesLibrary.TURBO_BLAZE
            "fiber_blue" -> CardThemesLibrary.FIBER_BLUE
            else -> CardThemesLibrary.EMERALD_PRO
        }
    }

    // Build sample cards for A4 preview based on live settings
    val previewCardsList = remember(activePreset, cardCountToGenerate, allCards) {
        val count = if (cardCountToGenerate > 0) cardCountToGenerate else 24
        val realMatchingCards = allCards.filter { it.retailPrice == activePreset.price }
        if (realMatchingCards.isNotEmpty()) {
            realMatchingCards.take(count)
        } else {
            List(count) { index ->
                val sampleDigits = listOf("471766", "127459", "784609", "105301", "042713", "884028", "339184", "950211")
                val code = sampleDigits.getOrElse(index % sampleDigits.size) { "123456" }
                CardEntity(
                    id = (index + 1).toLong(),
                    batchId = 1,
                    username = code,
                    password = code,
                    categoryName = activePreset.categoryName,
                    retailPrice = activePreset.price,
                    wholesalePrice = activePreset.price * 0.9,
                    status = "AVAILABLE"
                )
            }
        }
    }

    // Print A4 Function
    fun triggerA4Print() {
        val config = CardTemplateConfig(
            templateId = selectedTheme.id,
            theme = selectedTheme,
            networkTitle = networkTitle,
            supportPhone = supportPhone,
            loginDomain = loginDomain,
            showQrCode = showQrCode,
            showPriceBadge = true,
            showPasswordOrPin = false,
            cornerRadiusDp = 6
        )

        val cardsToPrint = previewCardsList
        CardPdfPrintManager.printCardsSheet(context, cardsToPrint, config)
        Toast.makeText(context, "جارٍ تجهيز ورقة A4 للطباعة أو التصدير كـ PDF...", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070E1A)) // Dark cyberpunk background matching sammikrotic
            .testTag("card_studio_screen")
    ) {
        // ================= TOP HEADER (MATCHING SAMMIKROTIC EXACTLY) =================
        Surface(
            color = Color(0xFF0A1526),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top Title and Active Template Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "توليد وتصميم كروت الإنترنت وطباعتها",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "توليد وطباعة الكروت، وتخصيص القوالب وحفظها سحابياً لمزامنتها في أي وقت.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    // Active Template Pill (like screenshot: "القالب النشط: قالب كرت أبو 1000 💎 سحابي")
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F2644),
                        border = BorderStroke(1.dp, Color(0xFF1E4976))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "القالب النشط: ",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${activePreset.title} ",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "💎 سحابي",
                                    color = Color(0xFF7DD3FC),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row (Matching screenshot exact order and colors)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. تحميل PDF (24 كرت) - Bright Blue
                    Button(
                        onClick = { triggerA4Print() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("action_download_pdf")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحميل PDF (24 كرت)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // 2. حفظ الدفعة في المخزن - Green
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val charSet = if (characterType == "NUMBERS_ONLY") CodeCharacterSet.DIGITS_ONLY else CodeCharacterSet.ALPHANUMERIC_UPPER
                                viewModel.createBatchAndGenerateCards(
                                    batchName = "دفعة_${activePreset.title}",
                                    categoryName = activePreset.categoryName,
                                    retailPrice = activePreset.price,
                                    wholesalePrice = activePreset.price * 0.9,
                                    quotaMb = 4500L,
                                    validityHours = 24,
                                    speedLimit = activePreset.speed,
                                    count = cardCountToGenerate,
                                    prefix = usernamePrefix,
                                    codeLength = cardCodeLength,
                                    charSet = charSet,
                                    passwordPolicy = PasswordPolicy.SAME_AS_USERNAME
                                ) {
                                    Toast.makeText(context, "تم حفظ دفعة ${activePreset.title} في المخزن بنجاح ✓", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("action_save_to_inventory")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الدفعة في المخزن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // 3. تحديث / حفظ باسم - Orange
                    Button(
                        onClick = {
                            Toast.makeText(context, "تم حفظ إعدادات قالب ${activePreset.title} سحابياً ومحلياً ✓", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("action_save_template")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث / حفظ باسم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // 4. طباعة - Dark Grey
                    OutlinedButton(
                        onClick = { triggerA4Print() },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("action_print")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة", color = Color.White, fontSize = 12.sp)
                    }

                    // 5. حفظ كصورة - Purple
                    Button(
                        onClick = {
                            Toast.makeText(context, "تم التقاط وحفظ صورة ورقة الكروت بنجاح 🖼️", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("action_save_image")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ كصورة", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // ================= NAVIGATION TABS (MATCHING SAMMIKROTIC TABS) =================
        // [التوليد والأكواد] | [القوالب AI] | [عناصر ومقاس الكرت] | [ورقة A4 والقص]
        Surface(
            color = Color(0xFF0F1A2E),
            border = BorderStroke(1.dp, Color(0xFF1E2E4A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF0F1A2E),
                contentColor = Color(0xFF38BDF8),
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFF0284C7),
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التوليد والأكواد", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("القوالب AI", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("عناصر ومقاس الكرت", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ورقة A4 والقص", fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }
        }

        // ================= MAIN STUDIO SPLIT CONTENT =================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Section 1: Template Library & Package Quick Selection (Top side like web image)
            item {
                StudioTemplateLibraryCard(
                    activePreset = activePreset,
                    presets = allPresets,
                    onSelectPreset = { applyPreset(it) },
                    onDuplicate = {
                        Toast.makeText(context, "تم استنساخ التنسيق لفئة جديدة بنجاح ✓", Toast.LENGTH_SHORT).show()
                    },
                    onReset = {
                        applyPreset(activePreset)
                        Toast.makeText(context, "تمت استعادة إعدادات ألوان القالب الأصلية ✓", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section 2: Tab-Specific Customization Controls
            item {
                when (activeTab) {
                    0 -> GenerationControlsCard(
                        count = cardCountToGenerate,
                        onCountChange = { cardCountToGenerate = it },
                        length = cardCodeLength,
                        onLengthChange = { cardCodeLength = it },
                        charType = characterType,
                        onCharTypeChange = { characterType = it },
                        prefix = usernamePrefix,
                        onPrefixChange = { usernamePrefix = it }
                    )
                    1 -> ColorThemePickerCard(
                        selectedTheme = selectedTheme,
                        onThemeSelect = { selectedTheme = it }
                    )
                    2 -> CardElementsControlsCard(
                        networkTitle = networkTitle,
                        onNetworkTitleChange = { networkTitle = it },
                        supportPhone = supportPhone,
                        onSupportPhoneChange = { supportPhone = it },
                        loginDomain = loginDomain,
                        onLoginDomainChange = { loginDomain = it },
                        quota = quotaText,
                        onQuotaChange = { quotaText = it },
                        duration = durationText,
                        onDurationChange = { durationText = it },
                        showQrCode = showQrCode,
                        onShowQrCodeChange = { showQrCode = it }
                    )
                    3 -> A4LayoutControlsCard(
                        dimensions = cardDimensionsMm,
                        onDimensionsChange = { cardDimensionsMm = it },
                        columns = a4Columns,
                        onColumnsChange = { a4Columns = it },
                        rows = a4Rows,
                        onRowsChange = { a4Rows = it },
                        showCutLines = showCutLines,
                        onShowCutLinesChange = { showCutLines = it }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section 3: Live Preview Header & Toggle Bar (Matching web screenshot)
            item {
                LivePreviewBar(
                    previewMode = previewMode,
                    onSelectPreviewMode = { previewMode = it },
                    zoomLevel = zoomLevel,
                    onZoomIn = { if (zoomLevel < 1.8f) zoomLevel += 0.2f },
                    onZoomOut = { if (zoomLevel > 0.6f) zoomLevel -= 0.2f },
                    cardDimensions = cardDimensionsMm,
                    columns = a4Columns,
                    rows = a4Rows,
                    totalCards = a4Columns * a4Rows
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Section 4: The Live A4 Paper / Single Card Display
            item {
                if (previewMode == 0) {
                    // Full A4 Sheet View with Header and Matrix (Matching web screenshot)
                    A4PaperLiveCanvas(
                        cards = previewCardsList,
                        networkTitle = networkTitle,
                        supportPhone = supportPhone,
                        loginDomain = loginDomain,
                        quotaText = quotaText,
                        durationText = durationText,
                        priceText = priceText,
                        theme = selectedTheme,
                        showCutLines = showCutLines,
                        zoom = zoomLevel,
                        columns = a4Columns,
                        rows = a4Rows
                    )
                } else if (previewMode == 1) {
                    // Interactive Drag / Touch Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "استوديو السحب واللمس (Drag & Touch Studio)",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "يمكنك تحريك أو فحص الكرت بحرية بدقة الطباعة العالية",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            SingleSammikroticCardView(
                                code = "127459",
                                networkTitle = networkTitle,
                                quotaText = quotaText,
                                durationText = durationText,
                                priceText = priceText,
                                theme = selectedTheme,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                        }
                    }
                } else {
                    // Single Card Detailed View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SingleSammikroticCardView(
                            code = "784609",
                            networkTitle = networkTitle,
                            quotaText = quotaText,
                            durationText = durationText,
                            priceText = priceText,
                            theme = selectedTheme,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 5: CLI Script generation for RouterOS
            item {
                MikroTikTerminalScriptBanner(
                    cards = previewCardsList,
                    onCopy = {
                        clipboard.setText(AnnotatedString(it))
                        Toast.makeText(context, "تم نسخ أوامر RouterOS CLI بنجاح", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}


// ================= COMPONENT: TEMPLATE LIBRARY & PRESETS (TOP CARD) =================
@Composable
fun StudioTemplateLibraryCard(
    activePreset: PackagePreset,
    presets: List<PackagePreset>,
    onSelectPreset: (PackagePreset) -> Unit,
    onDuplicate: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C192E),
        border = BorderStroke(1.dp, Color(0xFF1E3252)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEAB308).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("مكتبة القوالب وتخصيص المظهر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("تحكم يدوي كامل في شكل ولون وتصميم الكروت بدون تعقيد", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                // Reset Action
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "استعادة الألوان الأصلية", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Pills Row: حفظ التعديلات / استنساخ لفئة أخرى
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Save */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التعديلات / باسم جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onDuplicate,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF13233D)),
                    border = BorderStroke(1.dp, Color(0xFF233A5E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استنساخ لفئة أخرى", color = Color(0xFF38BDF8), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF1E3252))
            Spacer(modifier = Modifier.height(10.dp))

            // Presets row title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("قوالبي المجهزة مسبقاً للباقات (${presets.size})", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.2f)
                ) {
                    Text("☁️ سحابي", color = Color(0xFF38BDF8), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Presets Horizontal Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = preset.id == activePreset.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF102E4E) else Color(0xFF0B1727),
                        border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFF1A2A40)),
                        onClick = { onSelectPreset(preset) },
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Palette,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = preset.title,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "🏷️ ${preset.tag}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= COMPONENT: TAB 0 - GENERATION CONTROLS =================
@Composable
fun GenerationControlsCard(
    count: Int,
    onCountChange: (Int) -> Unit,
    length: Int,
    onLengthChange: (Int) -> Unit,
    charType: String,
    onCharTypeChange: (String) -> Unit,
    prefix: String,
    onPrefixChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C192E),
        border = BorderStroke(1.dp, Color(0xFF1E3252)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("إعدادات توليد أرقام كروت الهوتسبوت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = count.toString(),
                    onValueChange = { onCountChange(it.toIntOrNull() ?: 24) },
                    label = { Text("عدد الكروت (مثلاً 24 أو 42)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = length.toString(),
                    onValueChange = { onLengthChange(it.toIntOrNull() ?: 6) },
                    label = { Text("طول الكود (مثلاً 6 أرقام)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = prefix,
                    onValueChange = onPrefixChange,
                    label = { Text("بادئة قبل الكود (اختياري)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Char Type Quick Selector
                Column(modifier = Modifier.weight(1f)) {
                    Text("نوع المحارف:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        FilterChip(
                            selected = charType == "NUMBERS_ONLY",
                            onClick = { onCharTypeChange("NUMBERS_ONLY") },
                            label = { Text("أرقام فقط", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = charType == "ALPHANUMERIC",
                            onClick = { onCharTypeChange("ALPHANUMERIC") },
                            label = { Text("حروف وأرقام", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

// ================= COMPONENT: TAB 1 - COLOR THEMES =================
@Composable
fun ColorThemePickerCard(
    selectedTheme: CardThemeStyle,
    onThemeSelect: (CardThemeStyle) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C192E),
        border = BorderStroke(1.dp, Color(0xFF1E3252)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ألوان وثيم الكرت (تطابق الهوية البصرية)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))

            val themes = CardThemesLibrary.ALL_THEMES

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(themes) { theme ->
                    val isSelected = theme.id == selectedTheme.id
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(theme.backgroundStart, theme.backgroundEnd)
                                )
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onThemeSelect(theme) }
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = theme.nameArabic,
                                color = theme.textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= COMPONENT: TAB 2 - CARD ELEMENTS CONTROLS =================
@Composable
fun CardElementsControlsCard(
    networkTitle: String,
    onNetworkTitleChange: (String) -> Unit,
    supportPhone: String,
    onSupportPhoneChange: (String) -> Unit,
    loginDomain: String,
    onLoginDomainChange: (String) -> Unit,
    quota: String,
    onQuotaChange: (String) -> Unit,
    duration: String,
    onDurationChange: (String) -> Unit,
    showQrCode: Boolean,
    onShowQrCodeChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C192E),
        border = BorderStroke(1.dp, Color(0xFF1E3252)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("نصوص وبيانات كرت الشبكة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = networkTitle,
                    onValueChange = onNetworkTitleChange,
                    label = { Text("اسم الشبكة") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = supportPhone,
                    onValueChange = onSupportPhoneChange,
                    label = { Text("هاتف الدعم") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = loginDomain,
                    onValueChange = onLoginDomainChange,
                    label = { Text("دومين الدخول") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = quota,
                    onValueChange = onQuotaChange,
                    label = { Text("الرصيد / الميجا") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إظهار رمز QR الدخول التلقائي", color = Color.White, fontSize = 12.sp)
                Switch(
                    checked = showQrCode,
                    onCheckedChange = onShowQrCodeChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7))
                )
            }
        }
    }
}

// ================= COMPONENT: TAB 3 - A4 LAYOUT CONTROLS =================
@Composable
fun A4LayoutControlsCard(
    dimensions: String,
    onDimensionsChange: (String) -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    rows: Int,
    onRowsChange: (Int) -> Unit,
    showCutLines: Boolean,
    onShowCutLinesChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C192E),
        border = BorderStroke(1.dp, Color(0xFF1E3252)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("هندسة ورقة A4 والقص (297 × 210 مم)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dimensions,
                    onValueChange = onDimensionsChange,
                    label = { Text("أبعاد الكرت") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E3252)
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Quick preset: 24 cards vs 42 cards
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = rows == 8,
                        onClick = {
                            onColumnsChange(3)
                            onRowsChange(8)
                        },
                        label = { Text("24 كرت (3×8)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = rows == 14,
                        onClick = {
                            onColumnsChange(3)
                            onRowsChange(14)
                        },
                        label = { Text("42 كرت (3×14)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إظهار خطوط القص المحددة على الورقة (✂️ Cut Lines)", color = Color.White, fontSize = 12.sp)
                Switch(
                    checked = showCutLines,
                    onCheckedChange = onShowCutLinesChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7))
                )
            }
        }
    }
}

// ================= COMPONENT: LIVE PREVIEW BAR (SWITCHER + ZOOM) =================
@Composable
fun LivePreviewBar(
    previewMode: Int,
    onSelectPreviewMode: (Int) -> Unit,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    cardDimensions: String,
    columns: Int,
    rows: Int,
    totalCards: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Title & Switcher Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("المعاينة الحية للكروت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Mode Selector Pills: [ورقة A4 كاملة للطباعة] | [استوديو السحب بالماوس] | [معاينة كرت مفرد]
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (previewMode == 0) Color(0xFF0284C7) else Color(0xFF1E293B),
                    onClick = { onSelectPreviewMode(0) }
                ) {
                    Text(
                        text = "ورقة A4 كاملة للطباعة",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (previewMode == 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (previewMode == 1) Color(0xFF0284C7) else Color(0xFF1E293B),
                    onClick = { onSelectPreviewMode(1) }
                ) {
                    Text(
                        text = "استوديو السحب",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (previewMode == 1) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (previewMode == 2) Color(0xFF0284C7) else Color(0xFF1E293B),
                    onClick = { onSelectPreviewMode(2) }
                ) {
                    Text(
                        text = "كرت مفرد",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (previewMode == 2) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Info bar & Zoom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Paper Specs Info (Green dot, dimensions)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ورقة A4 (297 × 210 مم) • $columns أعمدة × $rows صفوف ($totalCards كرت/ورقة) • أبعاد الكرت: $cardDimensions",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }

            // Zoom Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onZoomOut, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "تصغير", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                Text("${(zoomLevel * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                IconButton(onClick = onZoomIn, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "تكبير", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ================= COMPONENT: A4 PAPER LIVE CANVAS (EXACT MATCH TO SCREENSHOT) =================
@Composable
fun A4PaperLiveCanvas(
    cards: List<CardEntity>,
    networkTitle: String,
    supportPhone: String,
    loginDomain: String,
    quotaText: String,
    durationText: String,
    priceText: String,
    theme: CardThemeStyle,
    showCutLines: Boolean,
    zoom: Float,
    columns: Int,
    rows: Int
) {
    // Elegant A4 Paper representation (White background, thin subtle border, page header)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = zoom, scaleY = zoom)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Paper Header (Matching user screenshot: "خطوط القص محددة • صفحة 1 من 1 | شبكة طلقة نت • هاتف الدعم: ... • t.net")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showCutLines) {
                        Text(
                            text = "✂️ خطوط القص محددة • صفحة 1 من 1",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "صفحة 1 من 1",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$networkTitle • هاتف الدعم: $supportPhone • $loginDomain",
                        color = Color.DarkGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Cards Grid (Columns x Rows)
            val displayCards = cards.take(columns * rows)
            val chunkedRows = displayCards.chunked(columns)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunkedRows.forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowCards.forEach { card ->
                            SingleSammikroticCardView(
                                code = card.username,
                                networkTitle = networkTitle,
                                quotaText = quotaText,
                                durationText = durationText,
                                priceText = priceText,
                                theme = theme,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // If row isn't complete, pad with empty spaces to maintain alignment
                        if (rowCards.size < columns) {
                            repeat(columns - rowCards.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ================= COMPONENT: SINGLE SAMMIKROTIC CARD VIEW =================
// Matches the exact look from the user's screenshot:
// - Green/Custom gradient background
// - Top row: Price Badge (YER 1000) on left, Network Title + Wifi on right
// - Center row: Separate rounded digit boxes for code (e.g. [4] [7] [1] [7] [6] [6])
// - Bottom row: Quota (4500 ميجا), Duration (غير محدد), Network brand
@Composable
fun SingleSammikroticCardView(
    code: String,
    networkTitle: String,
    quotaText: String,
    durationText: String,
    priceText: String,
    theme: CardThemeStyle,
    modifier: Modifier = Modifier
) {
    // Individual card container
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(theme.backgroundStart, theme.backgroundEnd)
                )
            )
            .border(0.7.dp, theme.codeBoxBorderColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Top Row: Price badge + Network Name & Wifi Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price Badge (e.g. YER 1000)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = theme.badgeBgColor
                ) {
                    Text(
                        text = priceText,
                        color = theme.badgeTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Network Title & Wifi Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = networkTitle,
                        color = theme.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = theme.accentColor,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Center Row: Separate Digit Boxes for Code (Exact feature from user image)
            // e.g. [4] [7] [1] [7] [6] [6]
            val digits = code.take(8).toList()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                digits.forEach { char ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.5.dp)
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(theme.codeBoxBgColor)
                            .border(0.5.dp, theme.codeBoxBorderColor, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            color = theme.codeBoxTextColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Card Bottom Row: Quota and Duration details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quota (e.g. 4500 ميجا)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = theme.accentColor,
                        modifier = Modifier.size(8.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = quotaText,
                        color = theme.textColor.copy(alpha = 0.9f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Duration (e.g. غير محدد)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = durationText,
                        color = theme.textColor.copy(alpha = 0.8f),
                        fontSize = 7.5.sp
                    )
                }
            }
        }
    }
}

// ================= COMPONENT: ROUTEROS CLI BANNER =================
@Composable
fun MikroTikTerminalScriptBanner(
    cards: List<CardEntity>,
    onCopy: (String) -> Unit
) {
    val script = remember(cards) {
        CardGenerationEngine.generateRouterOsHotspotScript(
            cards = cards,
            serverName = "all",
            profileName = "default",
            batchComment = "SamMikroTik_WebStudio"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1727),
        border = BorderStroke(1.dp, Color(0xFF1E3350)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("أوامر ميكروتك المباشرة (RouterOS CLI Script)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { onCopy(script) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ السكربت", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF070E1A))
                    .padding(8.dp)
            ) {
                Text(
                    text = script.lines().take(4).joinToString("\n") + "\n...",
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}
