package com.example.ui

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
import com.example.data.auth.GoogleAuthManager
import com.example.data.firebase.FirebaseDbService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.CardSalesInvoiceItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ParsedInvoiceData
import com.example.data.repository.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class IpValidationResult(
    val hasConflict: Boolean = false,
    val conflictingDeviceName: String? = null,
    val conflictingDeviceLocation: String? = null,
    val isOutsideSubnet: Boolean = false,
    val approvedSubnet: String = "192.168.88.0/24"
)

data class GoogleSignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isMissingClientId: Boolean = false,
    val activeFirebaseEmail: String? = null,
    val activeFirebaseUid: String? = null,
    val activeAuthProvider: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = NetworkRepository(db)
    private val aiService = GeminiAiService()
    private val firebaseService = FirebaseDbService()
    val authManager = GoogleAuthManager(application)

    // Google Sign-In state
    private val _googleSignInState = MutableStateFlow(
        GoogleSignInUiState(
            activeFirebaseEmail = authManager.getActiveEmail(),
            activeFirebaseUid = authManager.getActiveUid(),
            activeAuthProvider = if (authManager.isUserLoggedIn()) "Firebase" else null
        )
    )
    val googleSignInState: StateFlow<GoogleSignInUiState> = _googleSignInState.asStateFlow()

    // Active User Role
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distributors: StateFlow<List<UserEntity>> = repository.allDistributors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    fun selectUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun saveDistributor(
        distributor: UserEntity,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val normalizedUser = distributor.copy(
                role = "DISTRIBUTOR",
                email = distributor.email.trim().lowercase(),
                username = if (distributor.username.isBlank()) {
                    distributor.email.substringBefore("@").ifBlank { "dist_${System.currentTimeMillis()}" }
                } else distributor.username
            )
            val id = repository.saveUser(normalizedUser)
            val updated = if (distributor.id == 0L) normalizedUser.copy(id = id) else normalizedUser

            // Push to cloud authorized_users directory
            val adminEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushAuthorizedUser(updated, adminEmail)
            onComplete()
        }
    }

    fun deleteDistributor(distributor: UserEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteUser(distributor)
            if (distributor.email.isNotBlank()) {
                firebaseService.deleteAuthorizedUser(distributor.email)
            }
            onComplete()
        }
    }

    fun toggleDistributorActive(distributor: UserEntity) {
        viewModelScope.launch {
            val updated = distributor.copy(isActive = !distributor.isActive)
            repository.saveUser(updated)
            val adminEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushAuthorizedUser(updated, adminEmail)
        }
    }

    // Google One-Tap & Authentication Dialog state
    /**
     * Genuine Google Sign-In through Android Credential Manager and Firebase Authentication.
     */
    fun signInWithGoogle(activity: Activity? = null, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _googleSignInState.value = _googleSignInState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            val result = authManager.signInWithGoogle(activity = activity)
            if (result.success && !result.email.isNullOrBlank()) {
                val registeredUser = repository.registerOrUpdateGoogleUser(
                    email = result.email,
                    displayName = result.displayName ?: "مستخدم جوجل",
                    photoUrl = result.photoUrl ?: ""
                )
                _currentUser.value = registeredUser
                _googleSignInState.value = GoogleSignInUiState(
                    isLoading = false,
                    successMessage = "تم التحقق وتسجيل الدخول الرسمي بحساب ${result.email}",
                    activeFirebaseEmail = result.email,
                    activeFirebaseUid = result.uid,
                    activeAuthProvider = result.authProvider
                )
                // Automatically sync cloud partition with verified email
                triggerCloudSync()
                onComplete(true)
            } else if (result.isCancelled) {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                onComplete(false)
            } else if (result.isMissingClientId) {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    isMissingClientId = true,
                    errorMessage = result.errorMessage
                )
                onComplete(false)
            } else {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "تعذر إكمال تسجيل الدخول عبر Google"
                )
                onComplete(false)
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            authManager.signOut()
            _currentUser.value = null
            _googleSignInState.value = GoogleSignInUiState(
                successMessage = "تم تسجيل الخروج من حساب Google و Firebase"
            )
        }
    }

    fun signInWithEmail(email: String, pass: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _googleSignInState.value = _googleSignInState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            val result = authManager.signInWithEmailAndPassword(email, pass)
            if (result.success && !result.email.isNullOrBlank()) {
                val registeredUser = repository.registerOrUpdateGoogleUser(
                    email = result.email,
                    displayName = result.displayName ?: result.email.substringBefore("@"),
                    photoUrl = ""
                )
                _currentUser.value = registeredUser
                _googleSignInState.value = GoogleSignInUiState(
                    isLoading = false,
                    successMessage = "تم تسجيل الدخول بنجاح عبر Firebase (${result.email})",
                    activeFirebaseEmail = result.email,
                    activeFirebaseUid = result.uid,
                    activeAuthProvider = "password"
                )
                triggerCloudSync()
                onComplete(true)
            } else {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "تعذر تسجيل الدخول بالبريد"
                )
                onComplete(false)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _googleSignInState.value = _googleSignInState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            val result = authManager.signUpWithEmailAndPassword(email, pass, name)
            if (result.success && !result.email.isNullOrBlank()) {
                val registeredUser = repository.registerOrUpdateGoogleUser(
                    email = result.email,
                    displayName = result.displayName ?: name,
                    photoUrl = ""
                )
                _currentUser.value = registeredUser
                _googleSignInState.value = GoogleSignInUiState(
                    isLoading = false,
                    successMessage = "تم إنشاء الحساب وتسجيل الدخول في Firebase: ${result.email}",
                    activeFirebaseEmail = result.email,
                    activeFirebaseUid = result.uid,
                    activeAuthProvider = "password"
                )
                triggerCloudSync()
                onComplete(true)
            } else {
                _googleSignInState.value = _googleSignInState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "تعذر إنشاء الحساب"
                )
                onComplete(false)
            }
        }
    }

    fun dismissGoogleMessage() {
        _googleSignInState.value = _googleSignInState.value.copy(errorMessage = null, successMessage = null)
    }

    // Reset To Production Environment (حذف كافة البيانات التجريبية وتهيئة التطبيق للعمل الفعلي)
    private val prefs = application.getSharedPreferences("sam_mikrotik_prefs", android.content.Context.MODE_PRIVATE)

    private val _isProductionMode = MutableStateFlow(
        prefs.getBoolean("is_production_mode", true)
    )
    val isProductionMode: StateFlow<Boolean> = _isProductionMode.asStateFlow()

    private val _resetProductionState = MutableStateFlow<String?>(null)
    val resetProductionState: StateFlow<String?> = _resetProductionState.asStateFlow()

    fun resetToProductionEnvironment(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val activeUser = currentUser.value
            val productionAdmin = repository.resetToProductionEnvironment(
                activeGoogleUser = if (activeUser?.isGoogleUser == true) activeUser else null
            )
            _currentUser.value = productionAdmin
            // تثبيت حالة الوضع الفعلي بحيث يختفي زر التهيئة نهائياً
            prefs.edit().putBoolean("is_production_mode", true).apply()
            _isProductionMode.value = true
            _resetProductionState.value = "تمت تهيئة بيئة العمل الفعلية بنجاح وحذف كافة البيانات الافتراضية والتحول للوضع الفعلي."
            triggerCloudSync()
            onComplete()
        }
    }

    fun dismissResetMessage() {
        _resetProductionState.value = null
    }

    // Devices
    val devices: StateFlow<List<NetworkDeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceCount: StateFlow<Int> = repository.deviceCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // IP Conflict and Subnet check
    private val _ipValidation = MutableStateFlow(IpValidationResult())
    val ipValidation: StateFlow<IpValidationResult> = _ipValidation.asStateFlow()

    // Network Identity & Configuration State
    val networkIdentity: StateFlow<NetworkIdentityEntity> = repository.networkIdentity
        .map { it ?: NetworkIdentityEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NetworkIdentityEntity())

    val sarToYerRate: StateFlow<Double> = networkIdentity
        .map { if (it.sarToYerRate > 0) it.sarToYerRate else 430.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 430.0)

    val usdToYerRate: StateFlow<Double> = networkIdentity
        .map { if (it.usdToYerRate > 0) it.usdToYerRate else 1630.0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1630.0)

    fun convertToYer(amount: Double, fromCurrency: String): Double {
        val identity = networkIdentity.value
        return com.example.util.CurrencyHelper.convertToYer(
            amount,
            fromCurrency,
            if (identity.sarToYerRate > 0) identity.sarToYerRate else 430.0,
            if (identity.usdToYerRate > 0) identity.usdToYerRate else 1630.0
        )
    }

    fun reconcileAccountingLedger(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reconcileAccountingLedger()
            onComplete()
        }
    }

    fun saveNetworkIdentity(identity: NetworkIdentityEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveNetworkIdentity(identity)
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushNetworkIdentity(identity, userEmail)
            onComplete()
        }
    }

    fun resetNetworkIdentityToDefault(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val defaultIdentity = NetworkIdentityEntity(
                id = 1L,
                networkName = "شبكة سام ميكروتك الذكية",
                ownerName = _currentUser.value?.fullName ?: "المهندس سام",
                supportPhone = "770000001",
                supportWhatsapp = "967770000001",
                supportEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() } ?: "support@sam-mikrotic.ye",
                networkLocation = "اليمن - صنعاء - السبعين",
                routerModel = "MikroTik CCR2004-16G-2S+",
                routerOsVersion = "RouterOS v7.15",
                approvedDeviceSubnet = "192.168.88.0/24",
                gatewayIp = "192.168.88.1",
                ipRangeStart = "192.168.88.2",
                ipRangeEnd = "192.168.88.254",
                hotspotSubnet = "10.5.50.0/24",
                hotspotGatewayIp = "10.5.50.1",
                dnsServers = "8.8.8.8, 1.1.1.1",
                welcomeNotice = "أهلاً بكم في شبكة سام اللاسلكية - إنترنت فائق السرعة واستقرار دائم"
            )
            repository.saveNetworkIdentity(defaultIdentity)
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushNetworkIdentity(defaultIdentity, userEmail)
            onComplete()
        }
    }

    fun validateIp(ip: String, excludeDeviceId: Long? = null) {
        viewModelScope.launch {
            val cleanIp = ip.trim()
            val currentIdentity = networkIdentity.value
            if (cleanIp.isEmpty()) {
                _ipValidation.value = IpValidationResult(false, approvedSubnet = currentIdentity.approvedDeviceSubnet)
                return@launch
            }
            val isOutside = !repository.isIpInApprovedSubnet(cleanIp, currentIdentity.approvedDeviceSubnet)
            val conflict = repository.checkIpConflict(cleanIp, excludeDeviceId)
            if (conflict != null) {
                _ipValidation.value = IpValidationResult(
                    hasConflict = true,
                    conflictingDeviceName = conflict.name,
                    conflictingDeviceLocation = conflict.locationArea,
                    isOutsideSubnet = isOutside,
                    approvedSubnet = currentIdentity.approvedDeviceSubnet
                )
            } else {
                _ipValidation.value = IpValidationResult(
                    hasConflict = false,
                    isOutsideSubnet = isOutside,
                    approvedSubnet = currentIdentity.approvedDeviceSubnet
                )
            }
        }
    }

    fun clearIpValidation() {
        _ipValidation.value = IpValidationResult(false, approvedSubnet = networkIdentity.value.approvedDeviceSubnet)
    }

    suspend fun getSuggestedIp(): String {
        return repository.suggestNextAvailableIp()
    }

    fun saveDevice(device: NetworkDeviceEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveDevice(device)
            val updated = if (device.id == 0L) device.copy(id = id) else device
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushDevice(updated, userEmail)
            onComplete()
        }
    }

    fun updateDeviceCoordinates(
        deviceId: Long,
        latitude: Double,
        longitude: Double,
        coverageRadiusMeters: Int? = null,
        parentDeviceId: Long? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentDevice = devices.value.firstOrNull { it.id == deviceId } ?: return@launch
            val updated = currentDevice.copy(
                latitude = latitude,
                longitude = longitude,
                coverageRadiusMeters = coverageRadiusMeters ?: currentDevice.coverageRadiusMeters,
                parentDeviceId = parentDeviceId ?: currentDevice.parentDeviceId
            )
            repository.saveDevice(updated)
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushDevice(updated, userEmail)
            onComplete()
        }
    }

    fun deleteDevice(device: NetworkDeviceEntity) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.deleteDevice(device.id, userEmail)
        }
    }

    // Inventory Management (مخزن الكروت بالعدد والأصناف)
    val inventoryItems: StateFlow<List<InventoryItemEntity>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryMovements: StateFlow<List<InventoryMovementEntity>> = repository.allInventoryMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * إضافة أو توريد كروت للمخزن بالعدد فقط بدون إنشاء أو توليد أرقام كروت
     */
    fun addStockToInventory(
        packageName: String,
        quantity: Int,
        wholesalePrice: Double = 0.0,
        retailPrice: Double = 0.0,
        notes: String = "",
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val newBalance = repository.addStockToInventory(
                packageName = packageName,
                quantity = quantity,
                wholesalePrice = wholesalePrice,
                retailPrice = retailPrice,
                notes = notes
            )
            onComplete(newBalance)
        }
    }

    fun saveInventoryItem(
        packageName: String,
        quantity: Int,
        wholesalePrice: Double,
        retailPrice: Double
    ) {
        viewModelScope.launch {
            repository.saveInventoryItem(
                InventoryItemEntity(
                    packageName = packageName,
                    quantityAvailable = quantity,
                    wholesalePrice = wholesalePrice,
                    retailPrice = retailPrice
                )
            )
        }
    }

    fun deleteInventoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteInventoryItem(id)
        }
    }

    fun distributeFromInventory(
        inventoryId: Long,
        retailerId: Long,
        quantity: Int,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.distributeFromInventory(inventoryId, retailerId, quantity)
            onResult(success)
        }
    }

    // Card Sales Invoices (فواتير مبيعات الكروت متعددة الأصناف)
    val salesInvoices: StateFlow<List<CardSalesInvoiceEntity>> = repository.allSalesInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSalesAmount: StateFlow<Double?> = repository.totalSalesAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSoldCardsCount: StateFlow<Int?> = repository.totalSoldCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCreditRemaining: StateFlow<Double?> = repository.totalCreditRemaining
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * إصدار فاتورة مبيعات كروت متعددة الأصناف مع الخصم التلقائي من المخزن
     */
    fun issueMultiItemSalesInvoice(
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH",
        paidAmount: Double = 0.0,
        notes: String = "",
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val issuer = _currentUser.value?.fullName ?: "المهندس حسن"
            val invoiceId = repository.issueMultiItemSalesInvoice(
                customerName = customerName,
                customerPhone = customerPhone,
                retailerId = retailerId,
                items = items,
                paymentType = paymentType,
                paidAmount = paidAmount,
                notes = notes,
                issuerName = issuer
            )
            onComplete(invoiceId)
        }
    }

    fun deleteSalesInvoice(invoice: CardSalesInvoiceEntity) {
        viewModelScope.launch {
            repository.deleteSalesInvoice(invoice)
        }
    }

    /**
     * تدقيق وتحليل ذكي للفاتورة بواسطة Gemini AI
     */
    fun analyzeInvoiceWithAi(
        invoice: CardSalesInvoiceEntity,
        items: List<CardSalesInvoiceItem>,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val prompt = """
                    أنت مستشار مالي وخبير أنظمة شبكات ميكروتك ومحاسبة نقاط البيع.
                    قم بتحليل فاتورة مبيعات الكروت التالية وقدم ملخصاً احترافياً موجزاً (3-4 أسطر فقط باللغة العربية):
                    - رقم الفاتورة: ${invoice.invoiceNumber}
                    - العميل: ${invoice.customerName}
                    - نوع الفاتورة: ${invoice.paymentType}
                    - إجمالي المبلغ: ${invoice.totalAmount} ريال
                    - المبلغ المدفوع: ${invoice.paidAmount} ريال
                    - المتبقي (الآجل): ${invoice.remainingAmount} ريال
                    - إجمالي عدد الكروت المباعة: ${invoice.totalCardsCount} كرت
                    - تفاصيل الأصناف: ${items.joinToString { "${it.packageName}: ${it.quantity} كرت بسعر جملة ${it.unitPrice} ر.ي (تجزئة ${it.retailPrice} ر.ي)" }}
                    
                    اذكر باختصار:
                    1. هامش ربح البقالة/العميل الإجمالي المتوقع
                    2. سرعة دوران الأصناف الأكثر سحباً
                    3. نصيحة لإدارة الائتمان وسداد المبلغ المتبقي إن وجد
                """.trimIndent()
                val response = aiService.generateText(prompt)
                onResult(response)
            } catch (e: Exception) {
                onResult("تعذر الاتصال بالمساعد الذكي: ${e.message}")
            }
        }
    }

    // Cards & Batches
    val cardBatches: StateFlow<List<CardBatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cards: StateFlow<List<CardEntity>> = repository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCardsCount: StateFlow<Int> = repository.availableCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val distributedCardsCount: StateFlow<Int> = repository.distributedCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val soldCardsCount: StateFlow<Int> = repository.soldCardsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun createBatchAndGenerateCards(
        batchName: String,
        categoryName: String,
        retailPrice: Double,
        wholesalePrice: Double,
        quotaMb: Long,
        validityHours: Int,
        speedLimit: String,
        count: Int,
        prefix: String,
        codeLength: Int = 6,
        charSet: com.example.data.cards.CodeCharacterSet = com.example.data.cards.CodeCharacterSet.DIGITS_ONLY,
        passwordPolicy: com.example.data.cards.PasswordPolicy = com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createBatchAndGenerateCards(
                batchName, categoryName, retailPrice, wholesalePrice,
                quotaMb, validityHours, speedLimit, count, prefix,
                codeLength, charSet, passwordPolicy
            )
            onComplete(id)
        }
    }

    /**
     * استيراد دفعة كروت تم إنشاؤها في برامج خارجية (User Manager, Excel, SAS4, إلخ)
     */
    fun importExternalCardBatch(
        batchName: String,
        categoryName: String,
        retailPrice: Double,
        wholesalePrice: Double,
        quotaMb: Long,
        validityHours: Int,
        speedLimit: String,
        cardsList: List<Pair<String, String>>,
        sourceProgram: String = "برنامج خارجي",
        prefix: String = "EXT",
        targetRetailerId: Long? = null,
        onComplete: (batchId: Long, importedCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            val batchId = repository.importExternalCardBatch(
                batchName = batchName,
                categoryName = categoryName,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                quotaMb = quotaMb,
                validityHours = validityHours,
                speedLimit = speedLimit,
                cardsList = cardsList,
                sourceProgram = sourceProgram,
                prefix = prefix,
                targetRetailerId = targetRetailerId
            )
            onComplete(batchId, cardsList.size)
        }
    }

    /**
     * إضافة كروت يدوياً بالعدد فقط (مثلاً كروت فئة 200 ريال بعدد 1000 كرت)
     * دون اشتراط وجود رموز وأرقام الكروت مسبقاً.
     */
    fun addManualCardQuantity(
        categoryName: String,
        quantity: Int,
        retailPrice: Double,
        wholesalePrice: Double,
        batchName: String = "إضافة يدوية - $categoryName",
        quotaMb: Long = 0,
        validityHours: Int = 24,
        speedLimit: String = "4M/2M",
        targetRetailerId: Long? = null,
        onComplete: (batchId: Long, count: Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val batchId = repository.addManualCardQuantity(
                categoryName = categoryName,
                quantity = quantity,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                batchName = batchName,
                quotaMb = quotaMb,
                validityHours = validityHours,
                speedLimit = speedLimit,
                targetRetailerId = targetRetailerId
            )
            onComplete(batchId, quantity)
        }
    }

    // Retailers / Groceries
    val retailers: StateFlow<List<RetailerEntity>> = repository.allRetailers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRetailer(retailer: RetailerEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveRetailer(retailer)
            val updated = if (retailer.id == 0L) retailer.copy(id = id) else retailer
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushRetailer(updated, userEmail)
            onComplete()
        }
    }

    fun deleteRetailer(retailer: RetailerEntity) {
        viewModelScope.launch {
            repository.deleteRetailer(retailer)
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.deleteRetailer(retailer.id, userEmail)
        }
    }

    fun distributeCardsToRetailer(
        batchId: Long,
        retailerId: Long,
        quantity: Int,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.distributeCardsToRetailer(batchId, retailerId, quantity)
            onResult(success)
        }
    }

    fun markCardSold(cardId: Long) {
        viewModelScope.launch {
            repository.markCardSold(cardId)
        }
    }

    // Packages & Profiles (باقات وفئات الكروت والبروفايلات)
    val cardPackages: StateFlow<List<CardPackageEntity>> = repository.allPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePackage(pkg: CardPackageEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.savePackage(pkg)
            onDone?.invoke()
        }
    }

    fun deletePackage(pkg: CardPackageEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deletePackage(pkg)
            onDone?.invoke()
        }
    }

    fun issueCardSalesInvoice(
        pkg: CardPackageEntity,
        retailerId: Long,
        quantity: Int,
        paymentMethod: String = "نقداً",
        notes: String = "",
        onDone: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val issuer = _currentUser.value?.fullName ?: "المهندس حسن"
            val voucherId = repository.issueCardSalesInvoice(
                packageEntity = pkg,
                retailerId = retailerId,
                quantity = quantity,
                paymentMethod = paymentMethod,
                issuerName = issuer,
                notes = notes
            )
            onDone?.invoke(voucherId)
        }
    }

    // Financial Vouchers
    val vouchers: StateFlow<List<FinancialVoucherEntity>> = repository.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalReceipts: StateFlow<Double?> = repository.totalReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayments: StateFlow<Double?> = repository.totalPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun createVoucher(
        voucherType: String,
        amount: Double,
        partyName: String,
        retailerId: Long?,
        category: String,
        paymentMethod: String,
        description: String,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val issuer = _currentUser.value?.fullName ?: "المهندس سام"
            val id = repository.createVoucher(
                voucherType, amount, partyName, retailerId,
                category, paymentMethod, description, issuer
            )
            val voucherNumber = "${if (voucherType == "RECEIPT") "REC" else "PAY"}-2026-${id}"
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.pushVoucher(
                FinancialVoucherEntity(
                    id = id,
                    voucherNumber = voucherNumber,
                    voucherType = voucherType,
                    amount = amount,
                    partyName = partyName,
                    retailerId = retailerId,
                    category = category,
                    paymentMethod = paymentMethod,
                    description = description,
                    issuerName = issuer
                ),
                userEmail
            )
            onComplete(voucherNumber)
        }
    }

    fun deleteVoucher(voucher: FinancialVoucherEntity) {
        viewModelScope.launch {
            repository.deleteVoucher(voucher)
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
            firebaseService.deleteVoucher(voucher.voucherNumber, userEmail)
        }
    }

    // =========================================================
    // Investment, Partners & Fixed Assets (CAPEX & Equity System)
    // =========================================================
    val partners: StateFlow<List<PartnerEntity>> = repository.allPartners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInvestedCapital: StateFlow<Double?> = repository.totalInvestedCapital
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDistributedProfits: StateFlow<Double?> = repository.totalDistributedProfits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val partnerTransactions: StateFlow<List<PartnerTransactionEntity>> = repository.allPartnerTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePartner(partner: PartnerEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.savePartner(partner)
            onComplete()
        }
    }

    fun deletePartner(partner: PartnerEntity) {
        viewModelScope.launch {
            repository.deletePartner(partner)
        }
    }

    fun recordPartnerTransaction(tx: PartnerTransactionEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.recordPartnerTransaction(tx)
            onComplete()
        }
    }

    fun deletePartnerTransaction(tx: PartnerTransactionEntity) {
        viewModelScope.launch {
            repository.deletePartnerTransaction(tx)
        }
    }

    // Fixed Assets (CAPEX)
    val assets: StateFlow<List<NetworkAssetEntity>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAssetPurchaseCost: StateFlow<Double?> = repository.totalAssetPurchaseCost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCurrentAssetValue: StateFlow<Double?> = repository.totalCurrentAssetValue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val assetCount: StateFlow<Int> = repository.assetCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun saveAsset(asset: NetworkAssetEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveAsset(asset)
            onComplete()
        }
    }

    fun deleteAsset(asset: NetworkAssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    // =========================================================
    // Purchase Invoices & AI Vision Capture (فواتير المشتريات والأصول)
    // =========================================================
    val invoices: StateFlow<List<PurchaseInvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInvoicesAmount: StateFlow<Double> = repository.totalInvoicesAmount
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _isScanningInvoice = MutableStateFlow(false)
    val isScanningInvoice: StateFlow<Boolean> = _isScanningInvoice.asStateFlow()

    private val _lastScannedInvoice = MutableStateFlow<ParsedInvoiceData?>(null)
    val lastScannedInvoice: StateFlow<ParsedInvoiceData?> = _lastScannedInvoice.asStateFlow()

    fun scanInvoiceWithAi(
        bitmap: Bitmap,
        onResult: (ParsedInvoiceData) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isScanningInvoice.value = true
            try {
                val parsed = aiService.parseInvoiceImage(bitmap)
                _lastScannedInvoice.value = parsed
                _isScanningInvoice.value = false
                onResult(parsed)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error analyzing invoice with AI", e)
                _isScanningInvoice.value = false
                onError(e.localizedMessage ?: "فشل تحليل الفاتورة")
            }
        }
    }

    fun approveAndSaveInvoice(
        invoice: PurchaseInvoiceEntity,
        items: List<InvoiceItem>,
        saveAsAssets: Boolean,
        saveAsVoucher: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            // Build items JSON and summary
            val itemsArray = JSONArray()
            items.forEach { item ->
                val itemObj = JSONObject().apply {
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("subtotal", item.subtotal)
                    put("category", item.category)
                }
                itemsArray.put(itemObj)
            }
            val summaryText = items.joinToString("، ") { "${it.name} (${it.quantity.toInt()} × ${it.unitPrice.toInt()})" }
            val totalCalc = items.sumOf { it.subtotal }

            val invoiceToSave = invoice.copy(
                totalAmount = if (totalCalc > 0) totalCalc else invoice.totalAmount,
                itemsJson = itemsArray.toString(),
                itemsSummary = summaryText,
                status = "APPROVED"
            )
            repository.saveInvoice(invoiceToSave)

            // 1. If user designated as Assets or requested saving items to Fixed Assets
            if (saveAsAssets || invoice.targetType == "ASSETS") {
                items.forEach { item ->
                    val assetCat = when (item.category.uppercase()) {
                        "SERVERS", "ROUTERS" -> "SERVERS"
                        "TOWERS", "ANTENNAS" -> "TOWERS"
                        "SOLAR_POWER", "BATTERIES" -> "SOLAR_POWER"
                        "CABLES", "FIBER" -> "FIBER_CABLES"
                        else -> "OTHER"
                    }
                    val asset = NetworkAssetEntity(
                        assetName = item.name,
                        category = assetCat,
                        purchaseCost = item.subtotal,
                        estimatedCurrentValue = item.subtotal,
                        purchaseDateMillis = invoice.invoiceDateMillis,
                        location = "المركز الرئيسي والشبكة",
                        serialNumber = "",
                        status = "ACTIVE",
                        notes = "مستورد من فاتورة رقم: ${invoice.invoiceNumber} (المورد: ${invoice.supplierName})"
                    )
                    repository.saveAsset(asset)
                }
            }

            // 2. If user requested recording in financial accounting vouchers (سند صرف مشتريات)
            if (saveAsVoucher) {
                val voucherNum = "PAY-${System.currentTimeMillis() % 100000}"
                val voucherCat = if (invoice.targetType == "ASSETS") "أصول ومعدات شبكة" else "صيانة ومعدات"
                val desc = "سداد فاتورة مشتريات #${invoice.invoiceNumber} (${invoice.supplierName}): $summaryText"
                val voucher = FinancialVoucherEntity(
                    voucherNumber = voucherNum,
                    voucherType = "PAYMENT",
                    amount = invoiceToSave.totalAmount,
                    partyName = invoice.supplierName.ifBlank { "مورد معدات" },
                    category = voucherCat,
                    paymentMethod = invoice.paymentMethod,
                    description = desc,
                    issuerName = _currentUser.value?.fullName ?: "المهندس سام",
                    dateMillis = invoice.invoiceDateMillis,
                    notes = invoice.notes
                )
                repository.insertVoucher(voucher)
                val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() }
                firebaseService.pushVoucher(voucher, userEmail)
            }

            onComplete()
        }
    }

    fun deleteInvoice(invoice: PurchaseInvoiceEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            onComplete()
        }
    }


    // AI Consultant State
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun consultAi(prompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val identity = networkIdentity.value
            val currentDeviceList = devices.value.joinToString { "${it.name} (${it.ipAddress} - ${it.locationArea})" }
            val statsContext = "إجمالي الأجهزة: ${devices.value.size}, البقالات: ${retailers.value.size}, الكروت المتاحة: ${availableCardsCount.value}, الأجهزة الحالية: $currentDeviceList"
            val response = aiService.consultMikrotikAi(prompt, statsContext, identity)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    // Cloud Sync Status
    private val _syncStatus = MutableStateFlow("متزامن سحابياً ومحلياً مع Firebase (sam-mikrotic) ☁️⚡")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    /**
     * سحب واستعادة البيانات من Firebase إلى قاعدة البيانات المحلية
     */
    fun pullDataFromCloud(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() } ?: authManager.getActiveEmail()
            val targetLabel = if (!userEmail.isNullOrBlank()) "حساب $userEmail" else "المشروع السحابي"
            _syncStatus.value = "جارٍ استيراد وتحديث البيانات من السحابة ($targetLabel)..."

            try {
                val cloudData = firebaseService.pullFromCloud(userEmail)
                repository.restoreFromCloudData(cloudData)
                val totalItems = cloudData.devices.size + cloudData.retailers.size + cloudData.vouchers.size +
                        cloudData.cardPackages.size + cloudData.inventoryItems.size + cloudData.salesInvoices.size
                if (totalItems > 0) {
                    _syncStatus.value = "تم استيراد $totalItems سجل بنجاح من السحابة (${cloudData.devices.size} جهاز، ${cloudData.retailers.size} بقالة، ${cloudData.vouchers.size} سند، ${cloudData.cardPackages.size} باقة، ${cloudData.inventoryItems.size} صنف مخزن، ${cloudData.salesInvoices.size} فاتورة) ✓"
                } else {
                    _syncStatus.value = "لا توجد بيانات سابقة في السحابة لهذا الحساب ($targetLabel)"
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error pulling data from cloud", e)
                _syncStatus.value = "تعذر سحب البيانات السحابية: ${e.message}"
            }
            onDone()
        }
    }

    /**
     * مزامنة ثنائية كاملة: سحب أحدث بيانات من السحابة ودمجها محلياً، ثم رفع أي بيانات محلية جديدة للسحابة
     */
    fun triggerCloudSync(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val userEmail = _currentUser.value?.email?.takeIf { it.isNotBlank() } ?: authManager.getActiveEmail()
            val targetLabel = if (!userEmail.isNullOrBlank()) "حساب $userEmail" else "المشروع sam-mikrotic"
            _syncStatus.value = "جارٍ المزامنة التفاعلية مع السحابة ($targetLabel)..."

            try {
                // الخطوة 1: سحب أي بيانات مخزنة بالسحابة (مهم جداً للهواتف الجديدة أو الأجهزة المتعددة)
                val cloudData = firebaseService.pullFromCloud(userEmail)
                repository.restoreFromCloudData(cloudData)

                // الخطوة 2: رفع كافة البيانات المحلية إلى السحابة
                val devList = devices.value
                val retList = retailers.value
                val vouchList = vouchers.value
                val pkgList = cardPackages.value
                val invList = inventoryItems.value
                val invSalesList = salesInvoices.value
                val identity = networkIdentity.value

                val result = firebaseService.syncAllToCloud(
                    devices = devList,
                    retailers = retList,
                    vouchers = vouchList,
                    userEmail = userEmail,
                    networkIdentity = identity,
                    cardPackages = pkgList,
                    inventoryItems = invList,
                    salesInvoices = invSalesList
                )

                if (result.success) {
                    val pulledCount = cloudData.devices.size + cloudData.retailers.size + cloudData.vouchers.size +
                            cloudData.cardPackages.size + cloudData.inventoryItems.size + cloudData.salesInvoices.size
                    val msg = if (pulledCount > 0) {
                        "تمت المزامنة الثنائية بنجاح 🔄 (سحب $pulledCount سجل من السحابة ومزامنة كافة الأجهزة والبقالات والفواتير)"
                    } else {
                        "تمت المزامنة بنجاح ($targetLabel): ${result.syncedDevicesCount} جهاز، ${result.syncedRetailersCount} بقالة، ${result.syncedVouchersCount} سند، وهوية الشبكة ✓"
                    }
                    _syncStatus.value = msg
                } else {
                    _syncStatus.value = result.message
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error during cloud sync", e)
                _syncStatus.value = "خطأ في المزامنة: ${e.message}"
            }
            onDone()
        }
    }

    init {
        viewModelScope.launch {
            try {
                // إزالة البيانات الافتراضية التجريبية المؤقتة فقط
                repository.purgeDefaultDataOnly()
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Error purging default data: ${e.message}")
            }

            try {
                // Ensure mosthassan.ye@gmail.com is registered as OWNER in database (Role & Permission only, NOT active session)
                repository.ensureSuperAdminExists()
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Error ensuring super admin: ${e.message}")
            }

            try {
                // تسوية ومطابقة الحسابات والديون المحاسبية فورياً لضمان التزامن التام
                repository.reconcileAccountingLedger()
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Error reconciling ledger: ${e.message}")
            }

            // سحب تلقائي للبيانات السحابية إذا كان المستخدم مسجل دخوله مسبقاً
            val activeEmail = authManager.getActiveEmail()?.trim()?.lowercase()
            if (!activeEmail.isNullOrBlank()) {
                try {
                    val cloudData = firebaseService.pullFromCloud(activeEmail)
                    if (cloudData.devices.isNotEmpty() || cloudData.retailers.isNotEmpty() || cloudData.vouchers.isNotEmpty()) {
                        repository.restoreFromCloudData(cloudData)
                    }
                } catch (e: Throwable) {
                    Log.w("MainViewModel", "Auto cloud pull note: ${e.message}")
                }
            }

            repository.allUsers.collect { list ->
                if (list.isNotEmpty()) {
                    val currentActiveEmail = authManager.getActiveEmail()?.trim()?.lowercase()
                    val current = _currentUser.value
                    if (current == null) {
                        // Only log in if there is a genuinely active authenticated Google session
                        if (!currentActiveEmail.isNullOrBlank()) {
                            val matchedLoggedIn = list.find { it.email.trim().lowercase() == currentActiveEmail }
                            if (matchedLoggedIn != null) {
                                _currentUser.value = matchedLoggedIn
                            }
                        }
                    } else {
                        // Refresh current user if data was updated in database
                        val refreshed = list.find { it.id == current.id || (current.email.isNotBlank() && it.email.equals(current.email, ignoreCase = true)) }
                        if (refreshed != null && refreshed != current) {
                            _currentUser.value = refreshed
                        }
                    }
                }
            }
        }
    }
}
