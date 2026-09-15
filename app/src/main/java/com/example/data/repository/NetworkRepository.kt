package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.random.Random

class NetworkRepository(private val db: AppDatabase) {

    // Network Identity & Core Configuration
    val networkIdentity: Flow<NetworkIdentityEntity?> = db.networkIdentityDao().getNetworkIdentityFlow()

    suspend fun getNetworkIdentity(): NetworkIdentityEntity = withContext(Dispatchers.IO) {
        val existing = db.networkIdentityDao().getNetworkIdentity()
        if (existing != null) {
            existing
        } else {
            val defaultIdentity = NetworkIdentityEntity()
            db.networkIdentityDao().insertOrUpdate(defaultIdentity)
            defaultIdentity
        }
    }

    suspend fun saveNetworkIdentity(identity: NetworkIdentityEntity): Long = withContext(Dispatchers.IO) {
        db.networkIdentityDao().insertOrUpdate(identity.copy(id = 1L, updatedAt = System.currentTimeMillis()))
    }

    fun extractSubnetPrefix(subnetOrIp: String): String {
        val clean = subnetOrIp.substringBefore("/").trim()
        val parts = clean.split(".")
        return if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.${parts[2]}."
        } else {
            "192.168.88."
        }
    }

    fun isIpInApprovedSubnet(ip: String, subnet: String): Boolean {
        val cleanIp = ip.trim()
        if (cleanIp.isEmpty()) return true
        val prefix = extractSubnetPrefix(subnet)
        return cleanIp.startsWith(prefix)
    }

    // Devices
    val allDevices: Flow<List<NetworkDeviceEntity>> = db.networkDeviceDao().getAllDevices()
    val deviceCount: Flow<Int> = db.networkDeviceDao().getDeviceCount()

    suspend fun checkIpConflict(ip: String, excludeDeviceId: Long? = null): NetworkDeviceEntity? = withContext(Dispatchers.IO) {
        val cleanIp = ip.trim()
        if (cleanIp.isEmpty()) return@withContext null
        val devices = db.networkDeviceDao().findDevicesWithIp(cleanIp)
        devices.firstOrNull { it.id != (excludeDeviceId ?: -1L) }
    }

    suspend fun suggestNextAvailableIp(basePrefix: String? = null): String = withContext(Dispatchers.IO) {
        val identity = getNetworkIdentity()
        val prefix = basePrefix ?: extractSubnetPrefix(identity.approvedDeviceSubnet)
        val devices = db.networkDeviceDao().getAllDevices().first()
        val usedHostNumbers = mutableSetOf<Int>()
        for (device in devices) {
            val ip = device.ipAddress.trim()
            if (ip.startsWith(prefix)) {
                val lastPart = ip.removePrefix(prefix).toIntOrNull()
                if (lastPart != null) {
                    usedHostNumbers.add(lastPart)
                }
            }
        }
        // Exclude gateway IP host if in same subnet
        if (identity.gatewayIp.startsWith(prefix)) {
            val gwPart = identity.gatewayIp.removePrefix(prefix).toIntOrNull()
            if (gwPart != null) usedHostNumbers.add(gwPart)
        }

        val startHost = if (identity.ipRangeStart.startsWith(prefix)) {
            identity.ipRangeStart.removePrefix(prefix).toIntOrNull() ?: 2
        } else 2

        val endHost = if (identity.ipRangeEnd.startsWith(prefix)) {
            identity.ipRangeEnd.removePrefix(prefix).toIntOrNull() ?: 254
        } else 254

        // Find first unused between startHost and endHost
        for (i in startHost..endHost) {
            if (!usedHostNumbers.contains(i)) {
                return@withContext "$prefix$i"
            }
        }
        return@withContext "${prefix}100"
    }

    suspend fun saveDevice(device: NetworkDeviceEntity): Long = withContext(Dispatchers.IO) {
        if (device.id == 0L) {
            db.networkDeviceDao().insertDevice(device)
        } else {
            db.networkDeviceDao().updateDevice(device)
            device.id
        }
    }

    suspend fun deleteDevice(device: NetworkDeviceEntity) = withContext(Dispatchers.IO) {
        db.networkDeviceDao().deleteDevice(device)
    }

    // Cards & Batches
    val allBatches: Flow<List<CardBatchEntity>> = db.cardDao().getAllBatches()
    val allCards: Flow<List<CardEntity>> = db.cardDao().getAllCards()
    val availableCardsCount: Flow<Int> = db.cardDao().countAvailableCards()
    val distributedCardsCount: Flow<Int> = db.cardDao().countDistributedCards()
    val soldCardsCount: Flow<Int> = db.cardDao().countSoldCards()

    fun getCardsForBatch(batchId: Long): Flow<List<CardEntity>> = db.cardDao().getCardsByBatch(batchId)
    fun getCardsForRetailer(retailerId: Long): Flow<List<CardEntity>> = db.cardDao().getCardsByRetailer(retailerId)

    suspend fun createBatchAndGenerateCards(
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
        passwordPolicy: com.example.data.cards.PasswordPolicy = com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME
    ): Long = withContext(Dispatchers.IO) {
        val batch = CardBatchEntity(
            batchName = batchName,
            categoryName = categoryName,
            retailPrice = retailPrice,
            wholesalePrice = wholesalePrice,
            quotaMb = quotaMb,
            validityHours = validityHours,
            speedLimit = speedLimit,
            totalCount = count,
            mikrotikProfile = "prof-${retailPrice.toInt()}",
            prefix = prefix
        )
        val batchId = db.cardDao().insertBatch(batch)

        val cards = mutableListOf<CardEntity>()
        for (i in 1..count) {
            val code = com.example.data.cards.CardGenerationEngine.generateVoucherCode(codeLength, prefix, charSet)
            val pass = when (passwordPolicy) {
                com.example.data.cards.PasswordPolicy.SAME_AS_USERNAME -> code
                com.example.data.cards.PasswordPolicy.SEPARATE_PIN -> com.example.data.cards.CardGenerationEngine.generatePin(4)
                com.example.data.cards.PasswordPolicy.NO_PASSWORD -> ""
            }
            cards.add(
                CardEntity(
                    batchId = batchId,
                    username = code,
                    password = pass,
                    categoryName = categoryName,
                    retailPrice = retailPrice,
                    wholesalePrice = wholesalePrice,
                    status = "AVAILABLE"
                )
            )
        }
        db.cardDao().insertCards(cards)
        batchId
    }

    // Inventory Management (مخزن الكروت)
    val allInventoryItems: Flow<List<InventoryItemEntity>> = db.inventoryDao().getAllInventoryItems()

    suspend fun saveInventoryItem(item: InventoryItemEntity): Long = withContext(Dispatchers.IO) {
        if (item.id == 0L) {
            db.inventoryDao().insertItem(item)
        } else {
            db.inventoryDao().updateItem(item)
            item.id
        }
    }

    suspend fun deleteInventoryItem(id: Long) = withContext(Dispatchers.IO) {
        db.inventoryDao().deleteItem(id)
    }

    suspend fun distributeFromInventory(
        inventoryId: Long,
        retailerId: Long,
        quantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val retailer = db.retailerDao().getRetailerById(retailerId) ?: return@withContext false
        val item = db.inventoryDao().getItemById(inventoryId) ?: return@withContext false
        if (item.quantityAvailable < quantity) return@withContext false

        // Deduct from inventory
        val updatedItem = item.copy(quantityAvailable = item.quantityAvailable - quantity)
        db.inventoryDao().updateItem(updatedItem)

        // Add debt to retailer
        val totalDebt = item.wholesalePrice * quantity
        db.retailerDao().updateBalanceAndCards(retailerId, totalDebt, quantity)
        
        true
    }

    // Distribute Cards to Retailer (تسليم دفعة كروت للبقالة)
    suspend fun distributeCardsToRetailer(
        batchId: Long,
        retailerId: Long,
        quantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val retailer = db.retailerDao().getRetailerById(retailerId) ?: return@withContext false
        val availableCards = db.cardDao().getAvailableCardsForBatch(batchId, quantity)
        if (availableCards.isEmpty()) return@withContext false

        val countToDistribute = availableCards.size
        val cardIds = availableCards.map { it.id }
        val now = System.currentTimeMillis()

        db.cardDao().assignCardsToRetailer(cardIds, retailerId, retailer.name, now)

        val totalWholesaleDebt = availableCards.sumOf { it.wholesalePrice }
        db.retailerDao().updateBalanceAndCards(retailerId, totalWholesaleDebt, countToDistribute)
        true
    }

    suspend fun markCardSold(cardId: Long) = withContext(Dispatchers.IO) {
        db.cardDao().markCardSold(cardId, System.currentTimeMillis())
    }

    // Retailers
    val allRetailers: Flow<List<RetailerEntity>> = db.retailerDao().getAllRetailers()

    suspend fun saveRetailer(retailer: RetailerEntity): Long = withContext(Dispatchers.IO) {
        if (retailer.id == 0L) {
            db.retailerDao().insertRetailer(retailer)
        } else {
            db.retailerDao().updateRetailer(retailer)
            retailer.id
        }
    }

    suspend fun deleteRetailer(retailer: RetailerEntity) = withContext(Dispatchers.IO) {
        db.retailerDao().deleteRetailer(retailer)
    }

    // Financial Vouchers (سندات القبض والصرف)
    val allVouchers: Flow<List<FinancialVoucherEntity>> = db.financialVoucherDao().getAllVouchers()
    val totalReceipts: Flow<Double?> = db.financialVoucherDao().getTotalReceipts()
    val totalPayments: Flow<Double?> = db.financialVoucherDao().getTotalPayments()

    fun getVouchersForRetailer(retailerId: Long): Flow<List<FinancialVoucherEntity>> =
        db.financialVoucherDao().getVouchersForRetailer(retailerId)

    suspend fun createVoucher(
        voucherType: String, // "RECEIPT" or "PAYMENT"
        amount: Double,
        partyName: String,
        retailerId: Long?,
        category: String,
        paymentMethod: String,
        description: String,
        issuerName: String
    ): Long = withContext(Dispatchers.IO) {
        val prefix = if (voucherType == "RECEIPT") "REC" else "PAY"
        val randomNum = Random.nextInt(1000, 9999)
        val voucherNumber = "$prefix-2026-$randomNum"

        val voucher = FinancialVoucherEntity(
            voucherNumber = voucherNumber,
            voucherType = voucherType,
            amount = amount,
            partyName = partyName,
            retailerId = retailerId,
            category = category,
            paymentMethod = paymentMethod,
            description = description,
            issuerName = issuerName
        )

        val id = db.financialVoucherDao().insertVoucher(voucher)

        // If it's a receipt from a retailer, reduce their balance
        if (voucherType == "RECEIPT" && retailerId != null) {
            db.retailerDao().recordPayment(retailerId, amount)
        }

        id
    }

    suspend fun deleteVoucher(voucher: FinancialVoucherEntity) = withContext(Dispatchers.IO) {
        db.financialVoucherDao().deleteVoucher(voucher)
    }

    // Users & Roles
    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val allDistributors: Flow<List<UserEntity>> = db.userDao().getUsersByRole("DISTRIBUTOR")

    suspend fun saveUser(user: UserEntity): Long = withContext(Dispatchers.IO) {
        if (user.id == 0L) {
            db.userDao().insertUser(user)
        } else {
            db.userDao().updateUser(user)
            user.id
        }
    }

    suspend fun deleteUser(user: UserEntity) = withContext(Dispatchers.IO) {
        db.userDao().deleteUser(user)
    }

    suspend fun registerOrUpdateGoogleUser(
        email: String,
        displayName: String,
        photoUrl: String
    ): UserEntity = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val existing = db.userDao().getUserByEmail(normalizedEmail)
            ?: if (email != normalizedEmail) db.userDao().getUserByEmail(email) else null

        if (existing != null) {
            val updated = existing.copy(
                fullName = if (existing.fullName.isNotBlank()) existing.fullName else displayName,
                photoUrl = photoUrl,
                email = normalizedEmail,
                isGoogleUser = true
            )
            db.userDao().updateUser(updated)
            updated
        } else {
            // Check if user was registered in Firestore cloud by Administrator as a Distributor
            val cloudDistributor = try {
                com.example.data.firebase.FirebaseDbService().fetchAuthorizedUserByEmail(normalizedEmail)
            } catch (e: Exception) {
                null
            }

            if (cloudDistributor != null) {
                val newUser = cloudDistributor.copy(
                    fullName = if (cloudDistributor.fullName.isNotBlank()) cloudDistributor.fullName else displayName,
                    photoUrl = photoUrl,
                    email = normalizedEmail,
                    isGoogleUser = true
                )
                val newId = db.userDao().insertUser(newUser)
                newUser.copy(id = newId)
            } else {
                // If there is already an owner in local database, default new logins without pre-authorization
                // to DISTRIBUTOR role so they don't overwrite the network administrator
                val hasOwner = db.userDao().getUsersByRole("OWNER").first().isNotEmpty()
                val assignedRole = if (hasOwner) "DISTRIBUTOR" else "OWNER"

                val newUser = UserEntity(
                    username = normalizedEmail.substringBefore("@"),
                    fullName = displayName.ifBlank { if (assignedRole == "OWNER") "مالك الشبكة" else "موزع كروت جديد" },
                    role = assignedRole,
                    email = normalizedEmail,
                    photoUrl = photoUrl,
                    isGoogleUser = true
                )
                val newId = db.userDao().insertUser(newUser)
                newUser.copy(id = newId)
            }
        }
    }

    suspend fun resetToProductionEnvironment(activeGoogleUser: UserEntity? = null): UserEntity = withContext(Dispatchers.IO) {
        // 1. Purge all operational and demo data
        db.cardDao().deleteAllCards()
        db.cardDao().deleteAllBatches()
        db.financialVoucherDao().deleteAllVouchers()
        db.retailerDao().deleteAllRetailers()
        db.networkDeviceDao().deleteAllDevices()

        // 2. Clear previous demo users
        db.userDao().deleteAllUsers()

        // 3. Create or preserve the authentic production administrator
        val productionAdmin = if (activeGoogleUser != null && activeGoogleUser.email.isNotBlank()) {
            activeGoogleUser.copy(id = 0, role = "OWNER")
        } else {
            UserEntity(
                username = "admin_owner",
                fullName = "مالك الشبكة (المدير العام)",
                role = "OWNER",
                phone = "",
                email = "",
                isGoogleUser = false
            )
        }
        val adminId = db.userDao().insertUser(productionAdmin)
        productionAdmin.copy(id = adminId)
    }

    // ==========================================
    // Investment, Partners & Fixed Assets (CAPEX)
    // ==========================================
    val allPartners: Flow<List<PartnerEntity>> = db.partnerDao().getAllPartners()
    val totalInvestedCapital: Flow<Double?> = db.partnerDao().getTotalInvestedCapital()
    val totalDistributedProfits: Flow<Double?> = db.partnerDao().getTotalDistributedProfits()
    val allPartnerTransactions: Flow<List<PartnerTransactionEntity>> = db.partnerDao().getAllPartnerTransactions()

    suspend fun savePartner(partner: PartnerEntity): Long = withContext(Dispatchers.IO) {
        if (partner.id == 0L) {
            db.partnerDao().insertPartner(partner)
        } else {
            db.partnerDao().updatePartner(partner)
            partner.id
        }
    }

    suspend fun deletePartner(partner: PartnerEntity) = withContext(Dispatchers.IO) {
        db.partnerDao().deletePartner(partner)
    }

    suspend fun recordPartnerTransaction(tx: PartnerTransactionEntity) = withContext(Dispatchers.IO) {
        db.partnerDao().insertPartnerTransaction(tx)
        // If it's a dividend payout, increment partner's withdrawn profit
        if (tx.transactionType == "DIVIDEND_PAYOUT" || tx.transactionType == "DRAWING") {
            val partner = db.partnerDao().getPartnerById(tx.partnerId)
            if (partner != null) {
                db.partnerDao().updatePartner(
                    partner.copy(totalWithdrawnProfit = partner.totalWithdrawnProfit + tx.amount)
                )
            }
        } else if (tx.transactionType == "CAPITAL_ADDITION") {
            val partner = db.partnerDao().getPartnerById(tx.partnerId)
            if (partner != null) {
                db.partnerDao().updatePartner(
                    partner.copy(capitalInvested = partner.capitalInvested + tx.amount)
                )
            }
        }
    }

    suspend fun deletePartnerTransaction(tx: PartnerTransactionEntity) = withContext(Dispatchers.IO) {
        db.partnerDao().deletePartnerTransaction(tx)
    }

    // Fixed Assets (CAPEX)
    val allAssets: Flow<List<NetworkAssetEntity>> = db.networkAssetDao().getAllAssets()
    val totalAssetPurchaseCost: Flow<Double?> = db.networkAssetDao().getTotalPurchaseCost()
    val totalCurrentAssetValue: Flow<Double?> = db.networkAssetDao().getTotalCurrentAssetValue()
    val assetCount: Flow<Int> = db.networkAssetDao().getAssetsCount()

    suspend fun saveAsset(asset: NetworkAssetEntity): Long = withContext(Dispatchers.IO) {
        if (asset.id == 0L) {
            db.networkAssetDao().insertAsset(asset)
        } else {
            db.networkAssetDao().updateAsset(asset)
            asset.id
        }
    }

    suspend fun deleteAsset(asset: NetworkAssetEntity) = withContext(Dispatchers.IO) {
        db.networkAssetDao().deleteAsset(asset)
    }
}

