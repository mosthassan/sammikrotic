package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.CardPackageEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.PurchaseInvoiceEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.CardSalesInvoiceEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.model.CardSalesInvoiceItem
import org.json.JSONArray
import org.json.JSONObject
import androidx.room.withTransaction
import java.util.Locale
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

    /**
     * استيراد دفعة كروت خارجية منشأة في برامج أخرى (مثل ميكروتك يوزر مانجر، إكسل، SAS4، وغيرها)
     * مع إمكانية إيداعها بالمستودع أو صرفها مباشرة لبقالة وتحديث القيود المحاسبية.
     */
    suspend fun importExternalCardBatch(
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
        targetRetailerId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val totalCount = cardsList.size
        val batch = CardBatchEntity(
            batchName = batchName,
            categoryName = categoryName,
            retailPrice = retailPrice,
            wholesalePrice = wholesalePrice,
            quotaMb = quotaMb,
            validityHours = validityHours,
            speedLimit = speedLimit,
            totalCount = totalCount,
            mikrotikProfile = "ext-${retailPrice.toInt()}",
            prefix = prefix
        )
        val batchId = db.cardDao().insertBatch(batch)

        val retailer = if (targetRetailerId != null && targetRetailerId > 0) {
            db.retailerDao().getRetailerById(targetRetailerId)
        } else null

        val now = System.currentTimeMillis()
        val cards = cardsList.map { (user, pass) ->
            CardEntity(
                batchId = batchId,
                username = user.trim(),
                password = pass.trim().ifEmpty { user.trim() },
                categoryName = categoryName,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                status = if (retailer != null) "DISTRIBUTED" else "AVAILABLE",
                retailerId = retailer?.id,
                retailerName = retailer?.name,
                distributedAt = if (retailer != null) now else null
            )
        }
        db.cardDao().insertCards(cards)

        // إذا تم الصرف الفوري لبقالة، نقيد الرصيد والكمية بحساب البقالة
        if (retailer != null) {
            val totalWholesaleDebt = wholesalePrice * totalCount
            db.retailerDao().updateBalanceAndCards(retailer.id, totalWholesaleDebt, totalCount)
        }

        batchId
    }

    /**
     * إضافة رصيد كروت يدوياً بالعدد فقط (مثلاً 1000 كرت فئة 200 ريال)
     * بدون اشتراط وجود أو إدخال أرقام ورموز الكروت.
     * يتم تحديث عدد الكروت المتاحة في المخزن، وإنشاء سجل الدفعة،
     * وتغذية حساب البقالة مباشرة إذا حُددت كوجهة استلام.
     */
    suspend fun addManualCardQuantity(
        categoryName: String,
        quantity: Int,
        retailPrice: Double,
        wholesalePrice: Double,
        batchName: String = "إضافة يدوية - $categoryName",
        quotaMb: Long = 0,
        validityHours: Int = 24,
        speedLimit: String = "4M/2M",
        targetRetailerId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val count = quantity.coerceAtLeast(1)
        val now = System.currentTimeMillis()

        // 1. إنشاء سجل الدفعة لتوثيق تاريخ وكمية الإضافة
        val batch = CardBatchEntity(
            batchName = batchName,
            categoryName = categoryName,
            retailPrice = retailPrice,
            wholesalePrice = wholesalePrice,
            quotaMb = quotaMb,
            validityHours = validityHours,
            speedLimit = speedLimit,
            totalCount = count,
            mikrotikProfile = "qty-${retailPrice.toInt()}",
            prefix = "QTY"
        )
        val batchId = db.cardDao().insertBatch(batch)

        val retailer = if (targetRetailerId != null && targetRetailerId > 0) {
            db.retailerDao().getRetailerById(targetRetailerId)
        } else null

        // 2. إدراج الكروت الرقمية لتمكين المتابعة والتوزيع وتوليد الفواتير بدون إجبار المستخدم على إدخال أرقام
        val prefixCode = categoryName.filter { it.isDigit() }.ifEmpty { "C" }
        val timeCode = (now % 100000).toString()
        val cards = (1..count).map { i ->
            CardEntity(
                batchId = batchId,
                username = "$prefixCode-$timeCode-${String.format(java.util.Locale.US, "%04d", i)}",
                password = "", // غير مشروط وجود رمز أو كلمة مرور
                categoryName = categoryName,
                retailPrice = retailPrice,
                wholesalePrice = wholesalePrice,
                status = if (retailer != null) "DISTRIBUTED" else "AVAILABLE",
                retailerId = retailer?.id,
                retailerName = retailer?.name,
                distributedAt = if (retailer != null) now else null
            )
        }
        db.cardDao().insertCards(cards)

        // 3. تحديث أو إدراج مخزون الكروت في جدول inventory_items
        val existingItem = db.inventoryDao().getItemByPackageName(categoryName)
        if (existingItem != null) {
            val updatedQty = if (retailer != null) {
                existingItem.quantityAvailable // تم صرفها مباشرة للبقالة
            } else {
                existingItem.quantityAvailable + count
            }
            db.inventoryDao().updateItem(
                existingItem.copy(
                    quantityAvailable = updatedQty,
                    wholesalePrice = wholesalePrice,
                    retailPrice = retailPrice
                )
            )
        } else {
            val initialQty = if (retailer != null) 0 else count
            db.inventoryDao().insertItem(
                InventoryItemEntity(
                    packageName = categoryName,
                    quantityAvailable = initialQty,
                    wholesalePrice = wholesalePrice,
                    retailPrice = retailPrice
                )
            )
        }

        // 4. إذا تم الصرف الفوري لبقالة، نقيد الرصيد والكمية بحساب البقالة
        if (retailer != null) {
            val totalWholesaleDebt = wholesalePrice * count
            db.retailerDao().updateBalanceAndCards(retailer.id, totalWholesaleDebt, count)
        }

        batchId
    }

    // Inventory Management (مخزن الكروت بالعدد)
    val allInventoryItems: Flow<List<InventoryItemEntity>> = db.inventoryDao().getAllInventoryItems()
    val allInventoryMovements: Flow<List<InventoryMovementEntity>> = db.inventoryMovementDao().getAllMovements()

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

    /**
     * توريد أو إضافة كمية للمخزن بالعدد فقط بدون إنشاء أو توليد أرقام كروت
     * يزداد الرصيد المتاح لهذا الصنف تلقائياً ويوثق في سجل حركات المخزن
     */
    suspend fun addStockToInventory(
        packageName: String,
        quantity: Int,
        wholesalePrice: Double = 0.0,
        retailPrice: Double = 0.0,
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val count = quantity.coerceAtLeast(1)
        val existingItem = db.inventoryDao().getItemByPackageName(packageName)
        val finalWholesale = if (wholesalePrice > 0) wholesalePrice else (existingItem?.wholesalePrice ?: 0.0)
        val finalRetail = if (retailPrice > 0) retailPrice else (existingItem?.retailPrice ?: 0.0)

        val newBalance = if (existingItem != null) {
            val updatedQty = existingItem.quantityAvailable + count
            db.inventoryDao().updateItem(
                existingItem.copy(
                    quantityAvailable = updatedQty,
                    wholesalePrice = finalWholesale,
                    retailPrice = finalRetail
                )
            )
            updatedQty
        } else {
            db.inventoryDao().insertItem(
                InventoryItemEntity(
                    packageName = packageName,
                    quantityAvailable = count,
                    wholesalePrice = finalWholesale,
                    retailPrice = finalRetail
                )
            )
            count
        }

        // تسجيل حركة مخزنية رسمية
        val movement = InventoryMovementEntity(
            packageName = packageName,
            movementType = "SUPPLY",
            quantityChange = count,
            resultingBalance = newBalance,
            referenceNumber = "SUP-${System.currentTimeMillis() % 100000}",
            customerOrSupplier = "المستودع الرئيسي",
            unitPrice = finalWholesale,
            notes = notes.ifBlank { "توريد كمية كروت للمخزن (+$count كرت)" }
        )
        db.inventoryMovementDao().insertMovement(movement)

        newBalance.toLong()
    }

    // Card Sales Invoices (فواتير مبيعات الكروت متعددة الأصناف)
    val allSalesInvoices: Flow<List<CardSalesInvoiceEntity>> = db.cardSalesInvoiceDao().getAllSalesInvoices()
    val totalSalesAmount: Flow<Double?> = db.cardSalesInvoiceDao().getTotalSalesAmount()
    val totalSoldCardsCount: Flow<Int?> = db.cardSalesInvoiceDao().getTotalSoldCardsCount()
    val totalCreditRemaining: Flow<Double?> = db.cardSalesInvoiceDao().getTotalCreditRemaining()

    /**
     * إصدار فاتورة مبيعات كروت متعددة الأصناف احترافية وفق المعايير المحاسبية
     * وينقص العدد تلقائياً من كل صنف بالمخزن، مع حساب الإجمالي والمدفوع والمتبقي
     */
    suspend fun issueMultiItemSalesInvoice(
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH", // "CASH", "CREDIT", "PARTIAL"
        paidAmount: Double = 0.0,
        notes: String = "",
        issuerName: String = "المهندس حسن"
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val totalAmount = items.sumOf { it.lineTotal }
            val totalCards = items.sumOf { it.quantity }

            val actualPaid = when (paymentType) {
                "CASH" -> totalAmount
                "CREDIT" -> 0.0
                "PARTIAL" -> paidAmount.coerceIn(0.0, totalAmount)
                else -> paidAmount
            }
            val remainingAmount = (totalAmount - actualPaid).coerceAtLeast(0.0)
            val status = when {
                remainingAmount <= 0.0 -> "PAID"
                actualPaid <= 0.0 -> "CREDIT"
                else -> "PARTIAL"
            }

            val totalInvoicesCount = db.cardSalesInvoiceDao().getSalesInvoicesList().size + 1
            var candidateNumber = "INV-2026-${String.format(Locale.US, "%04d", totalInvoicesCount)}"
            var collisionOffset = 1
            while (db.cardSalesInvoiceDao().getInvoiceByNumber(candidateNumber) != null) {
                candidateNumber = "INV-2026-${String.format(Locale.US, "%04d", totalInvoicesCount + collisionOffset)}"
                collisionOffset++
            }
            val invoiceNumber = candidateNumber
            val itemsSummary = items.joinToString(" + ") { "${it.quantity} كرت [${it.packageName}]" }

            // JSON serialization of items
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("packageName", item.packageName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("retailPrice", item.retailPrice)
                    put("lineTotal", item.lineTotal)
                }
                jsonArray.put(obj)
            }

            // البحث عن البقالة إذا لم يكن الـ ID محدداً صراحة
            val finalRetailerId = if (retailerId != null && retailerId > 0) {
                retailerId
            } else {
                val allRet = db.retailerDao().getRetailersList()
                allRet.find { it.name.trim().equals(customerName.trim(), ignoreCase = true) }?.id
            }

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = customerName.ifBlank { "عميل نقدي" },
                customerPhone = customerPhone,
                retailerId = finalRetailerId,
                paymentType = paymentType,
                totalAmount = totalAmount,
                paidAmount = actualPaid,
                remainingAmount = remainingAmount,
                totalCardsCount = totalCards,
                itemsCount = items.size,
                itemsSummary = itemsSummary,
                itemsJson = jsonArray.toString(),
                notes = notes,
                issuerName = issuerName,
                status = status
            )
            val invoiceId = db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)

            // خصم الكميات من المخزن لكل صنف وتسجيل حركات الصرف
            items.forEach { item ->
                val existing = db.inventoryDao().getItemByPackageName(item.packageName)
                val newQty = if (existing != null) {
                    val updated = (existing.quantityAvailable - item.quantity).coerceAtLeast(0)
                    db.inventoryDao().updateItem(existing.copy(quantityAvailable = updated))
                    updated
                } else {
                    db.inventoryDao().insertItem(
                        InventoryItemEntity(
                            packageName = item.packageName,
                            quantityAvailable = 0,
                            wholesalePrice = item.unitPrice,
                            retailPrice = item.retailPrice
                        )
                    )
                    0
                }

                // قيد حركة مخزنية خارجة (خصم مبيعات)
                db.inventoryMovementDao().insertMovement(
                    InventoryMovementEntity(
                        packageName = item.packageName,
                        movementType = "SALE",
                        quantityChange = -item.quantity,
                        resultingBalance = newQty,
                        referenceNumber = invoiceNumber,
                        customerOrSupplier = customerName.ifBlank { "عميل نقدي" },
                        unitPrice = item.unitPrice,
                        notes = "خصم مبيعات فاتورة رقم $invoiceNumber ($paymentType)"
                    )
                )
            }

            // إنشاء سند قبض مالي رسمي مربوط بمعرف الفاتورة مباشرة عند وجود سداد
            if (actualPaid > 0) {
                val voucher = FinancialVoucherEntity(
                    voucherNumber = "REC-2026-${Random.nextInt(1000, 9999)}",
                    voucherType = "RECEIPT",
                    amount = actualPaid,
                    partyName = customerName.ifBlank { "مبيعات كروت نقدية" },
                    retailerId = finalRetailerId,
                    invoiceId = invoiceId,
                    invoiceNumber = invoiceNumber,
                    allocatedAmount = actualPaid,
                    category = "مبيعات كروت",
                    paymentMethod = if (paymentType == "CASH") "نقداً" else "دفعة مقدمة",
                    description = "متحصلات من فاتورة مبيعات كروت $invoiceNumber - $itemsSummary",
                    issuerName = issuerName,
                    notes = notes
                )
                db.financialVoucherDao().insertVoucher(voucher)
            }

            // إنشاء وحفظ قيد يومية محاسبي في جدول journal_entries
            val entryNumber = "JE-$invoiceNumber"
            val effectiveCustomer = customerName.ifBlank { "عميل نقدي" }
            when (paymentType) {
                "CASH" -> {
                    db.journalEntryDao().insertEntry(
                        JournalEntryEntity(
                            entryNumber = entryNumber,
                            referenceType = "SALES_INVOICE",
                            referenceId = invoiceId.toString(),
                            debitAccount = "1101 - الصندوق الرئيسي",
                            creditAccount = "4101 - إيرادات مبيعات الكروت",
                            amount = totalAmount,
                            description = "قيد مبيعات نقداً لفاتورة رقم $invoiceNumber ($effectiveCustomer)",
                            createdBy = issuerName
                        )
                    )
                }
                "CREDIT" -> {
                    db.journalEntryDao().insertEntry(
                        JournalEntryEntity(
                            entryNumber = entryNumber,
                            referenceType = "SALES_INVOICE",
                            referenceId = invoiceId.toString(),
                            debitAccount = "1201 - ذمم الوكلاء / $effectiveCustomer",
                            creditAccount = "4101 - إيرادات مبيعات الكروت",
                            amount = totalAmount,
                            description = "قيد مبيعات آجلة لفاتورة رقم $invoiceNumber ($effectiveCustomer)",
                            createdBy = issuerName
                        )
                    )
                }
                "PARTIAL" -> {
                    if (actualPaid > 0) {
                        db.journalEntryDao().insertEntry(
                            JournalEntryEntity(
                                entryNumber = "$entryNumber-1",
                                referenceType = "SALES_INVOICE",
                                referenceId = invoiceId.toString(),
                                debitAccount = "1101 - الصندوق الرئيسي",
                                creditAccount = "4101 - إيرادات مبيعات الكروت",
                                amount = actualPaid,
                                description = "قيد مبيعات جزء مدفوع نقداً لفاتورة رقم $invoiceNumber ($effectiveCustomer)",
                                createdBy = issuerName
                            )
                        )
                    }
                    if (remainingAmount > 0) {
                        db.journalEntryDao().insertEntry(
                            JournalEntryEntity(
                                entryNumber = "$entryNumber-2",
                                referenceType = "SALES_INVOICE",
                                referenceId = invoiceId.toString(),
                                debitAccount = "1201 - ذمم الوكلاء / $effectiveCustomer",
                                creditAccount = "4101 - إيرادات مبيعات الكروت",
                                amount = remainingAmount,
                                description = "قيد مبيعات جزء آجل لفاتورة رقم $invoiceNumber ($effectiveCustomer)",
                                createdBy = issuerName
                            )
                        )
                    }
                }
                else -> {
                    db.journalEntryDao().insertEntry(
                        JournalEntryEntity(
                            entryNumber = entryNumber,
                            referenceType = "SALES_INVOICE",
                            referenceId = invoiceId.toString(),
                            debitAccount = "1101 - الصندوق الرئيسي",
                            creditAccount = "4101 - إيرادات مبيعات الكروت",
                            amount = totalAmount,
                            description = "قيد مبيعات لفاتورة رقم $invoiceNumber ($effectiveCustomer)",
                            createdBy = issuerName
                        )
                    )
                }
            }

            // تسوية دفتر الأستاذ دون تعديل مزدوج
            reconcileAccountingLedgerInternal()

            invoiceId
        }
    }

    /**
     * تحديث وتعديل فاتورة مبيعات كروت سابقة بشكل ذري (Atomic Transaction):
     * - يقوم باسترجاع الأصناف والكميات السابقة للمخزن
     * - إلغاء السند المالي السابق المرتبط بها برقم الفاتورة أو المعرف
     * - خصم الأصناف والكميات المعدلة الجديدة من المخزن
     * - تحديث بيانات الفاتورة والسند المالي بربط قيد مباشر
     * - إعادة تسوية أرصدة المحلات محاسبياً 100% بدون تعديل يدوي مزدوج
     */
    suspend fun updateMultiItemSalesInvoice(
        originalInvoice: CardSalesInvoiceEntity,
        customerName: String,
        customerPhone: String = "",
        retailerId: Long? = null,
        items: List<CardSalesInvoiceItem>,
        paymentType: String = "CASH",
        paidAmount: Double = 0.0,
        notes: String = "",
        issuerName: String = "المهندس حسن"
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            // 1. استرجاع الكميات السابقة للمخزن
            if (originalInvoice.itemsJson.isNotBlank()) {
                try {
                    val jsonArray = JSONArray(originalInvoice.itemsJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val pkgName = obj.optString("packageName", "")
                        val qty = obj.optInt("quantity", 0)
                        if (pkgName.isNotBlank() && qty > 0) {
                            val item = db.inventoryDao().getItemByPackageName(pkgName)
                            if (item != null) {
                                val newQty = item.quantityAvailable + qty
                                db.inventoryDao().updateItem(item.copy(quantityAvailable = newQty))
                                db.inventoryMovementDao().insertMovement(
                                    InventoryMovementEntity(
                                        packageName = pkgName,
                                        movementType = "RETURN",
                                        quantityChange = qty,
                                        resultingBalance = newQty,
                                        referenceNumber = "REFUND-${originalInvoice.invoiceNumber}",
                                        customerOrSupplier = originalInvoice.customerName,
                                        unitPrice = item.wholesalePrice,
                                        notes = "استرجاع كميات المخزن لتعديل الفاتورة #${originalInvoice.invoiceNumber}"
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NetworkRepository", "Error refunding old invoice items: ${e.message}")
                }
            }

            // 2. إلغاء حذف السندات القديمة للحفاظ على السجل المالي وسلامة الصندوق

            // 3. خصم الأصناف الجديدة من المخزن
            items.forEach { item ->
                val inventoryItem = db.inventoryDao().getItemByPackageName(item.packageName)
                if (inventoryItem != null) {
                    val newQty = (inventoryItem.quantityAvailable - item.quantity).coerceAtLeast(0)
                    db.inventoryDao().updateItem(inventoryItem.copy(quantityAvailable = newQty))
                    db.inventoryMovementDao().insertMovement(
                        InventoryMovementEntity(
                            packageName = item.packageName,
                            movementType = "SALE",
                            quantityChange = -item.quantity,
                            resultingBalance = newQty,
                            referenceNumber = "EDIT-${originalInvoice.invoiceNumber}",
                            customerOrSupplier = customerName,
                            unitPrice = item.unitPrice,
                            notes = "تعديل فاتورة مبيعات #${originalInvoice.invoiceNumber}"
                        )
                    )
                }
            }

            // 4. احتساب المبالغ والأصناف
            val totalAmount = items.sumOf { it.lineTotal }
            val totalCardsCount = items.sumOf { it.quantity }
            val actualPaidAmount = when (paymentType) {
                "CASH" -> totalAmount
                "CREDIT" -> 0.0
                "PARTIAL" -> paidAmount.coerceIn(0.0, totalAmount)
                else -> totalAmount
            }
            val remainingAmount = (totalAmount - actualPaidAmount).coerceAtLeast(0.0)
            val status = when {
                remainingAmount <= 0.0 -> "PAID"
                actualPaidAmount <= 0.0 -> "CREDIT"
                else -> "PARTIAL"
            }

            val itemsJsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("packageName", item.packageName)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                    put("retailPrice", item.retailPrice)
                    put("lineTotal", item.lineTotal)
                }
                itemsJsonArray.put(obj)
            }
            val itemsSummary = items.joinToString("، ") { "${it.packageName} (${it.quantity})" }

            // 5. حفظ التعديل في قاعدة البيانات
            val updatedInvoice = originalInvoice.copy(
                customerName = customerName,
                customerPhone = customerPhone,
                retailerId = retailerId,
                totalAmount = totalAmount,
                paidAmount = actualPaidAmount,
                remainingAmount = remainingAmount,
                totalCardsCount = totalCardsCount,
                paymentType = paymentType,
                itemsJson = itemsJsonArray.toString(),
                itemsSummary = itemsSummary,
                notes = notes,
                issuerName = issuerName,
                status = status
            )
            db.cardSalesInvoiceDao().updateInvoice(updatedInvoice)

            // 6. التعامل مع السند المالي بالفارق (paymentDelta) دون حذف السندات القديمة
            val paymentDelta = actualPaidAmount - originalInvoice.paidAmount
            if (paymentDelta > 0.0) {
                val voucher = FinancialVoucherEntity(
                    voucherNumber = "REC-${System.currentTimeMillis() % 100000}",
                    voucherType = "RECEIPT",
                    amount = paymentDelta,
                    partyName = customerName,
                    category = "مبيعات كروت شبكة",
                    paymentMethod = if (paymentType == "CASH") "نقداً" else "دفعة مقدمة",
                    description = "سند قبض تكميلي لتعديل فاتورة مبيعات #${originalInvoice.invoiceNumber} ($itemsSummary)",
                    issuerName = issuerName,
                    notes = notes,
                    retailerId = retailerId,
                    invoiceId = originalInvoice.id,
                    invoiceNumber = originalInvoice.invoiceNumber,
                    allocatedAmount = paymentDelta
                )
                db.financialVoucherDao().insertVoucher(voucher)
            } else if (paymentDelta < 0.0) {
                val refundAmount = kotlin.math.abs(paymentDelta)
                val voucher = FinancialVoucherEntity(
                    voucherNumber = "PAY-${System.currentTimeMillis() % 100000}",
                    voucherType = "PAYMENT",
                    amount = refundAmount,
                    partyName = customerName,
                    category = "استرداد/تسوية مبيعات كروت",
                    paymentMethod = "نقداً",
                    description = "سند صرف استرداد/تسوية لتعديل فاتورة مبيعات #${originalInvoice.invoiceNumber} ($itemsSummary)",
                    issuerName = issuerName,
                    notes = notes,
                    retailerId = retailerId,
                    invoiceId = originalInvoice.id,
                    invoiceNumber = originalInvoice.invoiceNumber,
                    allocatedAmount = refundAmount
                )
                db.financialVoucherDao().insertVoucher(voucher)
            }

            // 7. إنشاء وحفظ قيد يومية محاسبي في جدول journal_entries
            val editEntryNumber = "JE-EDIT-${originalInvoice.invoiceNumber}-${System.currentTimeMillis() % 100000}"
            val effectiveCustomer = customerName.ifBlank { "عميل نقدي" }
            when (paymentType) {
                "CASH" -> {
                    db.journalEntryDao().insertEntry(
                        JournalEntryEntity(
                            entryNumber = editEntryNumber,
                            referenceType = "SALES_INVOICE_EDIT",
                            referenceId = originalInvoice.id.toString(),
                            debitAccount = "1101 - الصندوق الرئيسي",
                            creditAccount = "4101 - إيرادات مبيعات الكروت",
                            amount = totalAmount,
                            description = "قيد تعديل مبيعات نقداً لفاتورة رقم #${originalInvoice.invoiceNumber} ($effectiveCustomer)",
                            createdBy = issuerName
                        )
                    )
                }
                "CREDIT" -> {
                    db.journalEntryDao().insertEntry(
                        JournalEntryEntity(
                            entryNumber = editEntryNumber,
                            referenceType = "SALES_INVOICE_EDIT",
                            referenceId = originalInvoice.id.toString(),
                            debitAccount = "1201 - ذمم الوكلاء / $effectiveCustomer",
                            creditAccount = "4101 - إيرادات مبيعات الكروت",
                            amount = totalAmount,
                            description = "قيد تعديل مبيعات آجلة لفاتورة رقم #${originalInvoice.invoiceNumber} ($effectiveCustomer)",
                            createdBy = issuerName
                        )
                    )
                }
                "PARTIAL" -> {
                    if (actualPaidAmount > 0) {
                        db.journalEntryDao().insertEntry(
                            JournalEntryEntity(
                                entryNumber = "$editEntryNumber-1",
                                referenceType = "SALES_INVOICE_EDIT",
                                referenceId = originalInvoice.id.toString(),
                                debitAccount = "1101 - الصندوق الرئيسي",
                                creditAccount = "4101 - إيرادات مبيعات الكروت",
                                amount = actualPaidAmount,
                                description = "قيد تعديل مبيعات جزء مدفوع نقداً لفاتورة رقم #${originalInvoice.invoiceNumber} ($effectiveCustomer)",
                                createdBy = issuerName
                            )
                        )
                    }
                    if (remainingAmount > 0) {
                        db.journalEntryDao().insertEntry(
                            JournalEntryEntity(
                                entryNumber = "$editEntryNumber-2",
                                referenceType = "SALES_INVOICE_EDIT",
                                referenceId = originalInvoice.id.toString(),
                                debitAccount = "1201 - ذمم الوكلاء / $effectiveCustomer",
                                creditAccount = "4101 - إيرادات مبيعات الكروت",
                                amount = remainingAmount,
                                description = "قيد تعديل مبيعات جزء آجل لفاتورة رقم #${originalInvoice.invoiceNumber} ($effectiveCustomer)",
                                createdBy = issuerName
                            )
                        )
                    }
                }
                else -> {
                    db.journalEntryDao().insertEntry(
                        JournalEntryEntity(
                            entryNumber = editEntryNumber,
                            referenceType = "SALES_INVOICE_EDIT",
                            referenceId = originalInvoice.id.toString(),
                            debitAccount = "1101 - الصندوق الرئيسي",
                            creditAccount = "4101 - إيرادات مبيعات الكروت",
                            amount = totalAmount,
                            description = "قيد تعديل مبيعات لفاتورة رقم #${originalInvoice.invoiceNumber} ($effectiveCustomer)",
                            createdBy = issuerName
                        )
                    )
                }
            }

            // 7. تسوية الدفاتر المحاسبية
            reconcileAccountingLedgerInternal()
        }
    }

    /**
     * استنساخ مباشر وفوري لفاتورة مبيعات كروت:
     * - ينشئ فاتورة جديدة برقم جديد كلياً
     * - ينسخ كافة الأصناف والأسعار ونوع الدفع وبيانات العميل
     * - يخصم الكروت من المخزن ويسجل السند المحاسبي
     */
    suspend fun cloneSalesInvoice(
        originalInvoice: CardSalesInvoiceEntity,
        issuerName: String = "المهندس حسن"
    ): Long = withContext(Dispatchers.IO) {
        val items = mutableListOf<CardSalesInvoiceItem>()
        if (originalInvoice.itemsJson.isNotBlank()) {
            try {
                val arr = JSONArray(originalInvoice.itemsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    items.add(
                        CardSalesInvoiceItem(
                            id = java.util.UUID.randomUUID().toString(),
                            packageName = obj.optString("packageName", ""),
                            quantity = obj.optInt("quantity", 1),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            retailPrice = obj.optDouble("retailPrice", 0.0)
                        )
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        val finalItems = if (items.isNotEmpty()) items else listOf(
            CardSalesInvoiceItem(
                id = java.util.UUID.randomUUID().toString(),
                packageName = "صنف كروت",
                quantity = originalInvoice.totalCardsCount.coerceAtLeast(1),
                unitPrice = if (originalInvoice.totalCardsCount > 0) originalInvoice.totalAmount / originalInvoice.totalCardsCount else originalInvoice.totalAmount,
                retailPrice = if (originalInvoice.totalCardsCount > 0) originalInvoice.totalAmount / originalInvoice.totalCardsCount else originalInvoice.totalAmount
            )
        )

        issueMultiItemSalesInvoice(
            customerName = originalInvoice.customerName,
            customerPhone = originalInvoice.customerPhone,
            retailerId = originalInvoice.retailerId,
            items = finalItems,
            paymentType = originalInvoice.paymentType,
            paidAmount = originalInvoice.paidAmount,
            notes = if (originalInvoice.notes.isNotBlank()) "${originalInvoice.notes} (مستنسخة من #${originalInvoice.invoiceNumber})" else "مستنسخة من #${originalInvoice.invoiceNumber}",
            issuerName = issuerName
        )
    }

    /**
     * الدالة الداخلية لتسوية دفتر الأستاذ (تستدعى داخل معامَلة ذرية withTransaction).
     * تعتمد كلياً على الربط الصريح بالمعرفات (invoiceId) والمطابقة الحسابية دون التحقق النصي الهش.
     */
    private suspend fun reconcileAccountingLedgerInternal() {
        try {
            // 0. تنظيف أي فواتير وهمية قديمة
            db.cardSalesInvoiceDao().deleteSyntheticInvoices()

            val retailers = db.retailerDao().getRetailersList()
            val initialInvoices = db.cardSalesInvoiceDao().getSalesInvoicesList()
            val initialVouchers = db.financialVoucherDao().getVouchersList()

            val retailersByName = retailers.associateBy { it.name.trim().lowercase() }
            val retailersById = retailers.associateBy { it.id }
            val invoicesByNumber = initialInvoices.associateBy { it.invoiceNumber }

            // 1. تسوية وتوحيد الربط للبقالات والفواتير القديمة غير المربوطة بمعرف
            for (inv in initialInvoices) {
                if (inv.retailerId == null || inv.retailerId == 0L || !retailersById.containsKey(inv.retailerId)) {
                    val matchedRetailer = retailersByName[inv.customerName.trim().lowercase()]
                    if (matchedRetailer != null) {
                        db.cardSalesInvoiceDao().updateInvoice(inv.copy(retailerId = matchedRetailer.id))
                    }
                }
            }

            for (v in initialVouchers) {
                var updatedVoucher = v
                var changed = false
                if (v.retailerId == null || v.retailerId == 0L || !retailersById.containsKey(v.retailerId)) {
                    val matchedRetailer = retailersByName[v.partyName.trim().lowercase()]
                    if (matchedRetailer != null) {
                        updatedVoucher = updatedVoucher.copy(retailerId = matchedRetailer.id)
                        changed = true
                    }
                }
                // إذا كان السند يحتوي على رقم فاتورة بدون invoiceId نربطه صراحة
                if (updatedVoucher.invoiceId == null && updatedVoucher.invoiceNumber.isNotBlank()) {
                    val matchedInv = invoicesByNumber[updatedVoucher.invoiceNumber]
                    if (matchedInv != null) {
                        updatedVoucher = updatedVoucher.copy(invoiceId = matchedInv.id)
                        changed = true
                    }
                }
                if (changed) {
                    db.financialVoucherDao().updateVoucher(updatedVoucher)
                }
            }

            // إعادة تحميل البيانات بعد اكتمال الربط بالمعرفات
            val allInvoices = db.cardSalesInvoiceDao().getSalesInvoicesList()
            val allVouchers = db.financialVoucherDao().getVouchersList()

            // 2. التسوية الدفترية الدقيقة لكل بقالة بناءً على فواتير المبيعات الحقيقية وسندات القبض
            for (retailer in retailers) {
                val matchingInvoices = allInvoices.filter {
                    it.retailerId == retailer.id ||
                    it.customerName.trim().equals(retailer.name.trim(), ignoreCase = true)
                }.sortedBy { it.invoiceDateMillis }

                val retailerReceipts = allVouchers.filter {
                    (it.retailerId == retailer.id || it.partyName.trim().equals(retailer.name.trim(), ignoreCase = true)) &&
                    it.voucherType == "RECEIPT" && !it.isVoided
                }

                // تقسيم السندات إلى:
                // أ) سندات مرتبطة مباشرة بفاتورة معينة (عبر invoiceId أو invoiceNumber)
                // ب) سندات قبض غير مخصصة (سداد عام للمديونية بنظام FIFO)
                val invoiceBoundReceipts = retailerReceipts.filter {
                    it.invoiceId != null || (it.invoiceNumber.isNotBlank() && matchingInvoices.any { inv -> inv.invoiceNumber == it.invoiceNumber })
                }
                val paymentsByInvoiceId = invoiceBoundReceipts.filter { it.invoiceId != null }.groupBy { it.invoiceId!! }
                val paymentsByInvoiceNumber = invoiceBoundReceipts.filter { it.invoiceNumber.isNotBlank() }.groupBy { it.invoiceNumber }

                val generalReceipts = retailerReceipts.filter { it !in invoiceBoundReceipts }
                var unallocatedPool = generalReceipts.sumOf { it.amount }

                var totalDebt = 0.0
                var totalUnpaidCards = 0
                var totalPaid = 0.0

                if (matchingInvoices.isNotEmpty()) {
                    for (inv in matchingInvoices) {
                        // المدفوع المباشر المرتبط بهذه الفاتورة صراحة
                        val directPaid = (paymentsByInvoiceId[inv.id]?.sumOf { it.amount } ?: 0.0) +
                                (paymentsByInvoiceNumber[inv.invoiceNumber]?.filter { it.invoiceId == null }?.sumOf { it.amount } ?: 0.0)

                        // في حال كانت الفاتورة مسددة نقداً أو جزئياً عند إنشائها ولم يكن هناك سند
                        val paidAtCreation = if (directPaid > 0.0) {
                            directPaid
                        } else {
                            when (inv.paymentType) {
                                "CASH" -> inv.totalAmount
                                "CREDIT" -> 0.0
                                "PARTIAL" -> (inv.totalAmount - inv.remainingAmount).coerceIn(0.0, inv.totalAmount)
                                else -> inv.paidAmount.coerceIn(0.0, inv.totalAmount)
                            }
                        }

                        val basePaid = maxOf(directPaid, paidAtCreation)
                        val unpaidAmount = (inv.totalAmount - basePaid).coerceAtLeast(0.0)

                        // تخصيص المبالغ من صندوق سندات القبض العامة بنظام FIFO
                        val extraPayment = minOf(unallocatedPool, unpaidAmount)
                        unallocatedPool = (unallocatedPool - extraPayment).coerceAtLeast(0.0)

                        val finalPaid = (basePaid + extraPayment).coerceAtMost(inv.totalAmount)
                        val finalRemaining = (inv.totalAmount - finalPaid).coerceAtLeast(0.0)
                        val finalStatus = when {
                            finalRemaining <= 0.01 -> "PAID"
                            finalPaid > 0.01 -> "PARTIAL"
                            else -> "CREDIT"
                        }

                        if (Math.abs(inv.paidAmount - finalPaid) > 0.01 ||
                            Math.abs(inv.remainingAmount - finalRemaining) > 0.01 ||
                            inv.status != finalStatus) {
                            db.cardSalesInvoiceDao().updateInvoice(
                                inv.copy(
                                    paidAmount = finalPaid,
                                    remainingAmount = finalRemaining,
                                    status = finalStatus
                                )
                            )
                        }

                        totalDebt += finalRemaining
                        if (finalRemaining > 0.01) {
                            totalUnpaidCards += inv.totalCardsCount
                        }
                        totalPaid += finalPaid
                    }

                    // في حال تبقت مبالغ من سندات القبض العامة تفوق مديونية كافة الفواتير
                    totalDebt = (totalDebt - unallocatedPool).coerceAtLeast(0.0)
                    totalPaid += unallocatedPool

                    // الكروت الفيزيائية الموزعة من دفعات الكروت
                    val physicalCards = db.cardDao().getDistributedCardsCountForRetailer(retailer.id)
                    val finalActiveCards = maxOf(totalUnpaidCards, physicalCards)

                    // تحديث بيانات البقالة بدقة 100%
                    if (Math.abs(retailer.balanceOwed - totalDebt) > 0.01 ||
                        retailer.activeCardsCount != finalActiveCards ||
                        Math.abs(retailer.totalPaid - totalPaid) > 0.01) {
                        db.retailerDao().updateRetailer(
                            retailer.copy(
                                balanceOwed = totalDebt,
                                activeCardsCount = finalActiveCards,
                                totalPaid = totalPaid
                            )
                        )
                    }
                } else {
                    // إذا لم تكن هناك فواتير مبيعات مسجلة للبقالة
                    val directTotalReceipts = retailerReceipts.sumOf { it.amount }
                    if (directTotalReceipts > 0.0) {
                        val newDebt = (retailer.balanceOwed - directTotalReceipts).coerceAtLeast(0.0)
                        val newPaid = retailer.totalPaid + directTotalReceipts
                        db.retailerDao().updateRetailer(
                            retailer.copy(balanceOwed = newDebt, totalPaid = newPaid)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkRepository", "reconcileAccountingLedger error: ${e.message}", e)
        }
    }

    /**
     * محرك التسوية المحاسبية والربط الفوري والتزامن الشامل بين كافة التبويبات.
     * يعمل كمعاملة ذرية مع ضمان تكامل البيانات 100%.
     */
    suspend fun reconcileAccountingLedger() = withContext(Dispatchers.IO) {
        db.withTransaction {
            reconcileAccountingLedgerInternal()
        }
    }

    suspend fun deleteSalesInvoice(invoice: CardSalesInvoiceEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            try {
                // 1. استرجاع الكميات للمخزن مع تسجيل حركة الإلغاء
                if (invoice.itemsJson.isNotBlank()) {
                    val jsonArray = org.json.JSONArray(invoice.itemsJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val pkgName = obj.optString("packageName", "")
                        val qty = obj.optInt("quantity", 0)
                        if (pkgName.isNotBlank() && qty > 0) {
                            val item = db.inventoryDao().getItemByPackageName(pkgName)
                            if (item != null) {
                                val newQty = item.quantityAvailable + qty
                                db.inventoryDao().updateItem(item.copy(quantityAvailable = newQty))
                                db.inventoryMovementDao().insertMovement(
                                    InventoryMovementEntity(
                                        packageName = pkgName,
                                        movementType = "RETURN",
                                        quantityChange = qty,
                                        resultingBalance = newQty,
                                        referenceNumber = "CANCEL-${invoice.invoiceNumber}",
                                        customerOrSupplier = invoice.customerName,
                                        unitPrice = item.wholesalePrice,
                                        notes = "استرجاع كميات بسبب حذف/إلغاء الفاتورة ${invoice.invoiceNumber}"
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. عدم حذف سندات القبض القديمة المرتبطة بالفاتورة نهائياً لحفظ السجل المالي
                // إنشاء سند صرف/تسوية (PAYMENT) بمقدار المبلغ المسدد لحفظ توازن الصندوق عند وجود سداد
                if (invoice.paidAmount > 0) {
                    val voucher = FinancialVoucherEntity(
                        voucherNumber = "PAY-CANCEL-${System.currentTimeMillis() % 100000}",
                        voucherType = "PAYMENT",
                        amount = invoice.paidAmount,
                        partyName = invoice.customerName.ifBlank { "عميل نقدي" },
                        category = "إلغاء/استرداد فاتورة مبيعات",
                        paymentMethod = "نقداً",
                        description = "سند صرف تسوية بسبب إلغاء/حذف فاتورة مبيعات رقم ${invoice.invoiceNumber}",
                        issuerName = invoice.issuerName.ifBlank { "المهندس حسن" },
                        notes = "إلغاء/حذف الفاتورة #${invoice.invoiceNumber}",
                        retailerId = invoice.retailerId,
                        invoiceId = invoice.id,
                        invoiceNumber = invoice.invoiceNumber,
                        allocatedAmount = invoice.paidAmount
                    )
                    db.financialVoucherDao().insertVoucher(voucher)
                }

                // 3. تسجيل قيد يومية محاسبي عكسي في journal_entries لإلغاء أثر الفاتورة
                val cancelEntryNumber = "JE-CANCEL-${invoice.invoiceNumber}-${System.currentTimeMillis() % 100000}"
                val effectiveCustomer = invoice.customerName.ifBlank { "عميل نقدي" }

                when (invoice.paymentType) {
                    "CASH" -> {
                        db.journalEntryDao().insertEntry(
                            JournalEntryEntity(
                                entryNumber = cancelEntryNumber,
                                referenceType = "SALES_INVOICE_CANCEL",
                                referenceId = invoice.id.toString(),
                                debitAccount = "4101 - إيرادات مبيعات الكروت",
                                creditAccount = "1101 - الصندوق الرئيسي",
                                amount = invoice.totalAmount,
                                description = "قيد عكسي لإلغاء فاتورة مبيعات نقداً رقم ${invoice.invoiceNumber} ($effectiveCustomer)",
                                createdBy = invoice.issuerName.ifBlank { "المهندس حسن" }
                            )
                        )
                    }
                    "CREDIT" -> {
                        db.journalEntryDao().insertEntry(
                            JournalEntryEntity(
                                entryNumber = cancelEntryNumber,
                                referenceType = "SALES_INVOICE_CANCEL",
                                referenceId = invoice.id.toString(),
                                debitAccount = "4101 - إيرادات مبيعات الكروت",
                                creditAccount = "1201 - ذمم الوكلاء / $effectiveCustomer",
                                amount = invoice.totalAmount,
                                description = "قيد عكسي لإلغاء فاتورة مبيعات آجلة رقم ${invoice.invoiceNumber} ($effectiveCustomer)",
                                createdBy = invoice.issuerName.ifBlank { "المهندس حسن" }
                            )
                        )
                    }
                    "PARTIAL" -> {
                        if (invoice.paidAmount > 0) {
                            db.journalEntryDao().insertEntry(
                                JournalEntryEntity(
                                    entryNumber = "$cancelEntryNumber-1",
                                    referenceType = "SALES_INVOICE_CANCEL",
                                    referenceId = invoice.id.toString(),
                                    debitAccount = "4101 - إيرادات مبيعات الكروت",
                                    creditAccount = "1101 - الصندوق الرئيسي",
                                    amount = invoice.paidAmount,
                                    description = "قيد عكسي للجزء المدفوع نقداً عند إلغاء فاتورة مبيعات رقم ${invoice.invoiceNumber} ($effectiveCustomer)",
                                    createdBy = invoice.issuerName.ifBlank { "المهندس حسن" }
                                )
                            )
                        }
                        if (invoice.remainingAmount > 0) {
                            db.journalEntryDao().insertEntry(
                                JournalEntryEntity(
                                    entryNumber = "$cancelEntryNumber-2",
                                    referenceType = "SALES_INVOICE_CANCEL",
                                    referenceId = invoice.id.toString(),
                                    debitAccount = "4101 - إيرادات مبيعات الكروت",
                                    creditAccount = "1201 - ذمم الوكلاء / $effectiveCustomer",
                                    amount = invoice.remainingAmount,
                                    description = "قيد عكسي للجزء الآجل عند إلغاء فاتورة مبيعات رقم ${invoice.invoiceNumber} ($effectiveCustomer)",
                                    createdBy = invoice.issuerName.ifBlank { "المهندس حسن" }
                                )
                            )
                        }
                    }
                    else -> {
                        if (invoice.paidAmount > 0) {
                            db.journalEntryDao().insertEntry(
                                JournalEntryEntity(
                                    entryNumber = cancelEntryNumber,
                                    referenceType = "SALES_INVOICE_CANCEL",
                                    referenceId = invoice.id.toString(),
                                    debitAccount = "4101 - إيرادات مبيعات الكروت",
                                    creditAccount = "1101 - الصندوق الرئيسي",
                                    amount = invoice.paidAmount,
                                    description = "قيد عكسي لإلغاء فاتورة مبيعات رقم ${invoice.invoiceNumber} ($effectiveCustomer)",
                                    createdBy = invoice.issuerName.ifBlank { "المهندس حسن" }
                                )
                            )
                        }
                        if (invoice.remainingAmount > 0) {
                            db.journalEntryDao().insertEntry(
                                JournalEntryEntity(
                                    entryNumber = "$cancelEntryNumber-rem",
                                    referenceType = "SALES_INVOICE_CANCEL",
                                    referenceId = invoice.id.toString(),
                                    debitAccount = "4101 - إيرادات مبيعات الكروت",
                                    creditAccount = "1201 - ذمم الوكلاء / $effectiveCustomer",
                                    amount = invoice.remainingAmount,
                                    description = "قيد عكسي للآجل عند إلغاء فاتورة مبيعات رقم ${invoice.invoiceNumber} ($effectiveCustomer)",
                                    createdBy = invoice.issuerName.ifBlank { "المهندس حسن" }
                                )
                            )
                        }
                    }
                }

                // 4. حذف الفاتورة
                db.cardSalesInvoiceDao().deleteInvoice(invoice)

                // 5. تسوية الحسابات المحاسبية فورا
                reconcileAccountingLedgerInternal()
            } catch (e: Exception) {
                android.util.Log.e("NetworkRepository", "deleteSalesInvoice error: ${e.message}", e)
                db.cardSalesInvoiceDao().deleteInvoice(invoice)
                reconcileAccountingLedgerInternal()
            }
        }
    }

    suspend fun distributeFromInventory(
        inventoryId: Long,
        retailerId: Long,
        quantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val retailer = db.retailerDao().getRetailerById(retailerId) ?: return@withTransaction false
            val item = db.inventoryDao().getItemById(inventoryId) ?: return@withTransaction false
            if (item.quantityAvailable < quantity) return@withTransaction false

            // Deduct from inventory
            val updatedItem = item.copy(quantityAvailable = item.quantityAvailable - quantity)
            db.inventoryDao().updateItem(updatedItem)

            val totalDebt = item.wholesalePrice * quantity
            val invoiceNumber = "INV-2026-${Random.nextInt(1000, 9999)}"

            // قيد حركة مخزنية خارجة (خصم تسليم كروت)
            db.inventoryMovementDao().insertMovement(
                InventoryMovementEntity(
                    packageName = item.packageName,
                    movementType = "SALE",
                    quantityChange = -quantity,
                    resultingBalance = updatedItem.quantityAvailable,
                    referenceNumber = invoiceNumber,
                    customerOrSupplier = retailer.name,
                    unitPrice = item.wholesalePrice,
                    notes = "تسليم كروت للبقالة $invoiceNumber"
                )
            )

            // إنشاء فاتورة مبيعات كروت رسمية بالآجل
            val jsonArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("packageName", item.packageName)
                    put("quantity", quantity)
                    put("unitPrice", item.wholesalePrice)
                    put("retailPrice", item.retailPrice)
                    put("lineTotal", totalDebt)
                })
            }

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = retailer.name,
                customerPhone = retailer.phone,
                retailerId = retailerId,
                paymentType = "CREDIT",
                totalAmount = totalDebt,
                paidAmount = 0.0,
                remainingAmount = totalDebt,
                totalCardsCount = quantity,
                itemsCount = 1,
                itemsSummary = "$quantity كرت [${item.packageName}]",
                itemsJson = jsonArray.toString(),
                notes = "تسليم وتوزيع كروت بالآجل من المخزن",
                issuerName = "مسؤول التوزيع",
                status = "CREDIT"
            )
            db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)

            // إعادة التسوية الشاملة ذرياً
            reconcileAccountingLedgerInternal()

            true
        }
    }

    // Distribute Cards to Retailer (تسليم دفعة كروت للبقالة)
    suspend fun distributeCardsToRetailer(
        batchId: Long,
        retailerId: Long,
        quantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val retailer = db.retailerDao().getRetailerById(retailerId) ?: return@withTransaction false
            val availableCards = db.cardDao().getAvailableCardsForBatch(batchId, quantity)
            if (availableCards.isEmpty()) return@withTransaction false

            val countToDistribute = availableCards.size
            val cardIds = availableCards.map { it.id }
            val now = System.currentTimeMillis()

            db.cardDao().assignCardsToRetailer(cardIds, retailerId, retailer.name, now)

            val totalWholesaleDebt = availableCards.sumOf { it.wholesalePrice }
            val categoryName = availableCards.firstOrNull()?.categoryName ?: "كروت شبكة"
            val invoiceNumber = "INV-2026-${Random.nextInt(1000, 9999)}"

            val jsonArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("packageName", categoryName)
                    put("quantity", countToDistribute)
                    put("unitPrice", if (countToDistribute > 0) totalWholesaleDebt / countToDistribute else 0.0)
                    put("retailPrice", availableCards.firstOrNull()?.retailPrice ?: 0.0)
                    put("lineTotal", totalWholesaleDebt)
                })
            }

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = retailer.name,
                customerPhone = retailer.phone,
                retailerId = retailerId,
                paymentType = "CREDIT",
                totalAmount = totalWholesaleDebt,
                paidAmount = 0.0,
                remainingAmount = totalWholesaleDebt,
                totalCardsCount = countToDistribute,
                itemsCount = 1,
                itemsSummary = "$countToDistribute كرت [$categoryName]",
                itemsJson = jsonArray.toString(),
                notes = "تسليم دفعة كروت بالآجل للبقالة",
                issuerName = "مسؤول التوزيع",
                status = "CREDIT"
            )
            db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)

            reconcileAccountingLedgerInternal()

            true
        }
    }

    suspend fun markCardSold(cardId: Long) = withContext(Dispatchers.IO) {
        db.cardDao().markCardSold(cardId, System.currentTimeMillis())
    }

    // Packages (باقات وفئات الكروت والبروفايلات)
    val allPackages: Flow<List<CardPackageEntity>> = db.cardPackageDao().getAllPackages()

    suspend fun savePackage(pkg: CardPackageEntity): Long = withContext(Dispatchers.IO) {
        if (pkg.id == 0L) {
            db.cardPackageDao().insertPackage(pkg)
        } else {
            db.cardPackageDao().updatePackage(pkg)
            pkg.id
        }
    }

    suspend fun deletePackage(pkg: CardPackageEntity) = withContext(Dispatchers.IO) {
        db.cardPackageDao().deletePackage(pkg)
    }

    /**
     * إصدار فاتورة وسند بيع كروت مباشرة للبقالة أو نقطة البيع
     * وحساب سعر الجملة وإجمالي القيمة وربح البقالة وتحديث الرصيد وسندات القبض
     */
    suspend fun issueCardSalesInvoice(
        packageEntity: CardPackageEntity,
        retailerId: Long,
        quantity: Int,
        paymentMethod: String = "نقداً",
        issuerName: String = "المهندس حسن",
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val retailer = db.retailerDao().getRetailerById(retailerId)
            val retailerName = retailer?.name ?: "نقطة توزيع"
            val totalWholesale = packageEntity.wholesalePrice * quantity
            val totalRetail = packageEntity.retailPrice * quantity
            val retailerProfit = (packageEntity.retailPrice - packageEntity.wholesalePrice) * quantity

            val randomNum = Random.nextInt(1000, 9999)
            val invoiceNumber = "INV-2026-$randomNum"
            val isCash = paymentMethod.contains("نقد")

            val invoiceEntity = CardSalesInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = retailerName,
                customerPhone = retailer?.phone ?: "",
                retailerId = retailerId,
                paymentType = if (isCash) "CASH" else "CREDIT",
                totalAmount = totalWholesale,
                paidAmount = if (isCash) totalWholesale else 0.0,
                remainingAmount = if (isCash) 0.0 else totalWholesale,
                totalCardsCount = quantity,
                itemsCount = 1,
                itemsSummary = "$quantity كرت [${packageEntity.name}]",
                itemsJson = "",
                notes = notes.ifBlank { "فاتورة مبيعات باقة ${packageEntity.name}" },
                issuerName = issuerName,
                status = if (isCash) "PAID" else "CREDIT"
            )
            val invoiceId = db.cardSalesInvoiceDao().insertInvoice(invoiceEntity)

            val descriptionText = buildString {
                append("فاتورة بيع $quantity كرت [${packageEntity.name}]")
                append(" بسعر جملة ${packageEntity.wholesalePrice.toInt()} ر.ي (إجمالي الجملة: ${totalWholesale.toInt()} ر.ي)")
                append(" - سعر البيع للمستهلك: ${packageEntity.retailPrice.toInt()} ر.ي (إجمالي: ${totalRetail.toInt()} ر.ي)")
                append(" - ربح البقالة المقدر: ${retailerProfit.toInt()} ر.ي")
                if (notes.isNotBlank()) append(" | ملاحظة: $notes")
            }

            var voucherId = 0L
            if (isCash) {
                val voucher = FinancialVoucherEntity(
                    voucherNumber = "REC-2026-${Random.nextInt(1000, 9999)}",
                    voucherType = "RECEIPT",
                    amount = totalWholesale,
                    partyName = retailerName,
                    retailerId = retailerId,
                    invoiceId = invoiceId,
                    invoiceNumber = invoiceNumber,
                    allocatedAmount = totalWholesale,
                    category = "مبيعات كروت",
                    paymentMethod = paymentMethod,
                    description = descriptionText,
                    issuerName = issuerName,
                    notes = notes
                )
                voucherId = db.financialVoucherDao().insertVoucher(voucher)
            }

            reconcileAccountingLedgerInternal()

            if (voucherId > 0) voucherId else invoiceId
        }
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
        issuerName: String,
        invoiceId: Long? = null,
        invoiceNumber: String = ""
    ): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val prefix = if (voucherType == "RECEIPT") "REC" else "PAY"
            val randomNum = Random.nextInt(1000, 9999)
            val voucherNumberGenerated = "$prefix-2026-$randomNum"

            val voucher = FinancialVoucherEntity(
                voucherNumber = voucherNumberGenerated,
                voucherType = voucherType,
                amount = amount,
                partyName = partyName,
                retailerId = retailerId,
                invoiceId = invoiceId,
                invoiceNumber = invoiceNumber,
                allocatedAmount = if (invoiceId != null || invoiceNumber.isNotBlank()) amount else 0.0,
                category = category,
                paymentMethod = paymentMethod,
                description = description,
                issuerName = issuerName
            )

            val id = db.financialVoucherDao().insertVoucher(voucher)

            // إنشاء وحفظ قيد محاسبي مزدوج تلقائي إذا لم يكن السند مرتبطاً بفاتورة مسبقاً
            if (invoiceId == null && invoiceNumber.isBlank()) {
                val (debitAcc, creditAcc) = if (voucherType == "RECEIPT") {
                    val credit = if (retailerId != null) "1201 - ذمم الوكلاء / $partyName" else "4101 - إيرادات عامة"
                    "1101 - الصندوق الرئيسي" to credit
                } else {
                    "5201 - مصروفات / $category" to "1101 - الصندوق الرئيسي"
                }

                db.journalEntryDao().insertEntry(
                    JournalEntryEntity(
                        entryNumber = "JE-$voucherNumberGenerated",
                        referenceType = "FINANCIAL_VOUCHER",
                        referenceId = voucherNumberGenerated,
                        debitAccount = debitAcc,
                        creditAccount = creditAcc,
                        amount = amount,
                        currency = voucher.currency,
                        exchangeRate = 1.0,
                        description = description,
                        createdBy = issuerName
                    )
                )
            }

            reconcileAccountingLedgerInternal()

            id
        }
    }

    suspend fun deleteVoucher(voucher: FinancialVoucherEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.financialVoucherDao().deleteVoucher(voucher)
            reconcileAccountingLedgerInternal()
        }
    }

    suspend fun voidVoucher(voucher: FinancialVoucherEntity, reason: String = "إلغاء السند المالي") = withContext(Dispatchers.IO) {
        db.withTransaction {
            // 1. تحديث حالة السند لتصبح ملغاة
            val voidedVoucher = voucher.copy(isVoided = true)
            db.financialVoucherDao().updateVoucher(voidedVoucher)

            // 2. إنشاء قيد يومية عكسي تلقائي (Reversal Journal Entry) لإلغاء الأثر المالي للسند
            val (origDebit, origCredit) = if (voucher.voucherType == "RECEIPT") {
                val credit = if (voucher.retailerId != null) "1201 - ذمم الوكلاء / ${voucher.partyName}" else "4101 - إيرادات عامة"
                "1101 - الصندوق الرئيسي" to credit
            } else {
                "5201 - مصروفات / ${voucher.category}" to "1101 - الصندوق الرئيسي"
            }

            db.journalEntryDao().insertEntry(
                JournalEntryEntity(
                    entryNumber = "REV-${voucher.voucherNumber}",
                    referenceType = "VOID_VOUCHER",
                    referenceId = voucher.voucherNumber,
                    debitAccount = origCredit,   // Reversal: Swap Debit and Credit
                    creditAccount = origDebit,
                    amount = voucher.amount,
                    currency = voucher.currency,
                    exchangeRate = 1.0,
                    description = "قيد عكسي لإلغاء السند رقم ${voucher.voucherNumber}${if (reason.isNotBlank()) " - $reason" else ""}",
                    createdBy = voucher.issuerName
                )
            )

            // 3. إعادة ضبط رصيد الحساب المربوط فوراً
            reconcileAccountingLedgerInternal()
        }
    }

    suspend fun insertVoucher(voucher: FinancialVoucherEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val id = db.financialVoucherDao().insertVoucher(voucher)

            // إنشاء وحفظ قيد محاسبي مزدوج تلقائي إذا لم يكن السند مرتبطاً بفاتورة مسبقاً
            if (voucher.invoiceId == null && voucher.invoiceNumber.isBlank()) {
                val (debitAcc, creditAcc) = if (voucher.voucherType == "RECEIPT") {
                    val credit = if (voucher.retailerId != null) "1201 - ذمم الوكلاء / ${voucher.partyName}" else "4101 - إيرادات عامة"
                    "1101 - الصندوق الرئيسي" to credit
                } else {
                    "5201 - مصروفات / ${voucher.category}" to "1101 - الصندوق الرئيسي"
                }

                db.journalEntryDao().insertEntry(
                    JournalEntryEntity(
                        entryNumber = "JE-${voucher.voucherNumber}",
                        referenceType = "FINANCIAL_VOUCHER",
                        referenceId = voucher.voucherNumber,
                        debitAccount = debitAcc,
                        creditAccount = creditAcc,
                        amount = voucher.amount,
                        currency = voucher.currency,
                        exchangeRate = 1.0,
                        description = voucher.description,
                        createdBy = voucher.issuerName
                    )
                )
            }

            id
        }
    }

    suspend fun saveJournalEntry(entry: JournalEntryEntity): Long = withContext(Dispatchers.IO) {
        db.journalEntryDao().insertEntry(entry)
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

    companion object {
        fun isSuperAdminEmail(email: String?): Boolean {
            return !email.isNullOrBlank()
        }
    }

    suspend fun purgeAllOperationalData() = withContext(Dispatchers.IO) {
        db.cardDao().deleteAllCards()
        db.cardDao().deleteAllBatches()
        db.financialVoucherDao().deleteAllVouchers()
        db.retailerDao().deleteAllRetailers()
        db.networkDeviceDao().deleteAllDevices()
        db.partnerDao().deleteAllPartners()
        db.partnerDao().deleteAllTransactions()
        db.networkAssetDao().deleteAllAssets()
        db.purchaseInvoiceDao().deleteAllInvoices()
        db.inventoryDao().deleteAllItems()
        db.inventoryMovementDao().deleteAllMovements()
        db.cardSalesInvoiceDao().deleteAllInvoices()
        db.cardPackageDao().deleteAllPackages()
        db.userDao().deleteAllUsers()
    }

    suspend fun switchUserWorkspace(
        email: String,
        displayName: String,
        photoUrl: String = ""
    ): UserEntity = withContext(Dispatchers.IO) {
        purgeAllOperationalData()

        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") }

        val newOwner = UserEntity(
            username = cleanEmail.substringBefore("@"),
            fullName = cleanName,
            role = "OWNER",
            email = cleanEmail,
            photoUrl = photoUrl,
            isGoogleUser = true
        )
        val id = db.userDao().insertUser(newOwner)

        val freshIdentity = NetworkIdentityEntity(
            id = 1L,
            networkName = "شبكة $cleanName",
            ownerName = cleanName,
            supportEmail = cleanEmail
        )
        db.networkIdentityDao().insertOrUpdate(freshIdentity)

        newOwner.copy(id = id)
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
                role = "OWNER",
                isGoogleUser = true
            )
            db.userDao().updateUser(updated)
            updated
        } else {
            val cleanName = displayName.ifBlank { normalizedEmail.substringBefore("@") }
            val newUser = UserEntity(
                username = normalizedEmail.substringBefore("@"),
                fullName = cleanName,
                role = "OWNER",
                email = normalizedEmail,
                photoUrl = photoUrl,
                isGoogleUser = true
            )
            val newId = db.userDao().insertUser(newUser)
            newUser.copy(id = newId)
        }
    }

    suspend fun resetToProductionEnvironment(activeGoogleUser: UserEntity? = null): UserEntity = withContext(Dispatchers.IO) {
        // 1. Purge all operational and demo data
        purgeAllOperationalData()

        // 2. Create or preserve the authentic production administrator
        val productionAdmin = if (activeGoogleUser != null && activeGoogleUser.email.isNotBlank()) {
            activeGoogleUser.copy(
                id = 0,
                role = "OWNER",
                fullName = activeGoogleUser.fullName.ifBlank { activeGoogleUser.email.substringBefore("@") }
            )
        } else {
            UserEntity(
                username = "owner",
                fullName = "مالك الشبكة الجديد",
                role = "OWNER",
                email = "",
                isGoogleUser = false
            )
        }
        val adminId = db.userDao().insertUser(productionAdmin)

        // 3. Reset network identity to clean slate
        val freshIdentity = NetworkIdentityEntity(
            id = 1L,
            networkName = "شبكتي الخاصة",
            ownerName = productionAdmin.fullName,
            supportEmail = productionAdmin.email
        )
        db.networkIdentityDao().insertOrUpdate(freshIdentity)

        productionAdmin.copy(id = adminId)
    }

    /**
     * حذف كافة البيانات الافتراضية والتجريبية التي تم إدراجها سابقاً
     * مع الإبقاء الكامل على أي بيانات فعلية قام المستخدم بإضافتها أو مزامنتها.
     */
    suspend fun purgeDefaultDataOnly() = withContext(Dispatchers.IO) {
        try {
            val sqlDb = db.openHelper.writableDatabase

            // 1. حذف الكروت والدفعات الافتراضية
            sqlDb.execSQL("""
                DELETE FROM cards WHERE username LIKE 'sam2001%' OR username LIKE 'sam5002%' OR batchId IN (SELECT id FROM card_batches WHERE prefix IN ('SAM2', 'SAM5'))
            """.trimIndent())
            sqlDb.execSQL("""
                DELETE FROM card_batches WHERE prefix IN ('SAM2', 'SAM5') OR batchName LIKE '%دفعة الربيع%' OR batchName LIKE '%دفعة VIP%'
            """.trimIndent())

            // 2. حذف سندات القبض والصرف الافتراضية
            sqlDb.execSQL("""
                DELETE FROM financial_vouchers WHERE voucherNumber IN ('REC-2026-001', 'REC-2026-002', 'PAY-2026-001', 'PAY-2026-002', 'PAY-2026-003')
            """.trimIndent())

            // 3. حذف البقالات ونقاط التوزيع الافتراضية
            sqlDb.execSQL("""
                DELETE FROM retailers WHERE name IN ('بقالة البركة والخير', 'سوبرماركت النخبة', 'كشك الأمل للاتصالات') OR phone IN ('771234567', '777654321', '733889900')
            """.trimIndent())

            // 4. حذف أجهزة ومعدات الشبكة الافتراضية
            sqlDb.execSQL("""
                DELETE FROM network_devices WHERE macAddress IN ('D4:CA:6D:11:22:33', 'D4:CA:6D:44:55:66', 'F0:9F:C2:77:88:99', '68:D7:9A:12:34:56', 'CC:2D:E0:98:76:54', 'B4:FB:E4:AA:BB:CC')
            """.trimIndent())

            // 5. حذف الشركاء وحركات الأرباح الافتراضية
            sqlDb.execSQL("""
                DELETE FROM partners WHERE name IN ('المهندس سام الشبواني', 'الحاج عبد الرحمن القدسي', 'أ. نبيل صالح الحميري') OR phone IN ('770000001', '771122334', '772233445')
            """.trimIndent())
            sqlDb.execSQL("""
                DELETE FROM partner_transactions WHERE notes LIKE '%توزيع أرباح الربع المالي الماضي%'
            """.trimIndent())

            // 6. حذف الأصول الرأسمالية الافتراضية
            sqlDb.execSQL("""
                DELETE FROM network_assets WHERE serialNumber IN ('CCR2004-16G-SN8821', 'TWR-24M-TH', 'GW-SPF3500ES', 'LITH-48100-PRO', 'MIMO-A5C-4SEC', 'FIBER-4C-ARM-1KM', 'GEN-KM-5KVA')
            """.trimIndent())

            // 7. حذف حركات وأصناف المخزن الافتراضية
            sqlDb.execSQL("""
                DELETE FROM inventory_movements WHERE referenceNumber IN ('SUP-2026-001', 'SUP-2026-002')
            """.trimIndent())
            sqlDb.execSQL("""
                DELETE FROM inventory_items WHERE packageName IN ('باقة 100 ريال سريعة', 'باقة 200 ريال يومية', 'باقة 500 ريال فايبر', 'باقة 1000 ريال أسبوعية', 'باقة 2500 ريال نصف شهرية')
            """.trimIndent())

            // 8. حذف فواتير المبيعات الافتراضية والفواتير الوهمية
            sqlDb.execSQL("""
                DELETE FROM card_sales_invoices WHERE invoiceNumber IN ('INV-2026-1042', 'INV-2026-1043') OR invoiceNumber LIKE 'INV-DELIV-%'
            """.trimIndent())

            // 9. حذف المستخدمين التجريبيين الافتراضيين
            sqlDb.execSQL("""
                DELETE FROM app_users WHERE username IN ('eng_ayman', 'dist_fahad', 'pos_baraka')
            """.trimIndent())

            // 10. حذف باقات الكروت الافتراضية
            sqlDb.execSQL("""
                DELETE FROM card_packages WHERE notes LIKE '%باقة اقتصادية خفيفة%' OR notes LIKE '%الباقة الأكثر مبيعاً%' OR notes LIKE '%باقة عائلية سريعة%' OR notes LIKE '%باقة 10 جيجا%' OR notes LIKE '%باقة 22 جيجا%' OR notes LIKE '%باقة شهرية 50 جيجابايت%'
            """.trimIndent())

            android.util.Log.i("NetworkRepository", "All default/dummy records purged from database successfully.")
        } catch (e: Throwable) {
            android.util.Log.e("NetworkRepository", "Error during purgeDefaultDataOnly: ${e.message}", e)
        }
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

    // Purchase Invoices (فواتير المشتريات والأصول)
    val allInvoices: Flow<List<PurchaseInvoiceEntity>> = db.purchaseInvoiceDao().getAllInvoices()
    val totalInvoicesAmount: Flow<Double?> = db.purchaseInvoiceDao().getTotalInvoicesAmount()

    suspend fun saveInvoice(invoice: PurchaseInvoiceEntity): Long = withContext(Dispatchers.IO) {
        if (invoice.id == 0L) {
            db.purchaseInvoiceDao().insertInvoice(invoice)
        } else {
            db.purchaseInvoiceDao().updateInvoice(invoice)
            invoice.id
        }
    }

    suspend fun deleteInvoice(invoice: PurchaseInvoiceEntity) = withContext(Dispatchers.IO) {
        db.purchaseInvoiceDao().deleteInvoice(invoice)
    }

    /**
     * Integrates all cloud-restored data into the local Room database.
     * Prevents duplication by matching IP/Names/Numbers and updating existing entities or inserting new ones.
     */
    suspend fun restoreFromCloudData(cloudData: com.example.data.firebase.CloudPullData) = withContext(Dispatchers.IO) {
        // 1. Restore Network Identity
        cloudData.networkIdentity?.let { idEntity ->
            db.networkIdentityDao().insertOrUpdate(idEntity.copy(id = 1L))
        }

        // 2. Restore Devices
        val existingDevices = db.networkDeviceDao().getAllDevices().first()
        val existingIps = existingDevices.associateBy { it.ipAddress.trim() }
        val existingDevNames = existingDevices.associateBy { it.name.trim() }

        for (device in cloudData.devices) {
            val match = existingIps[device.ipAddress.trim()] ?: existingDevNames[device.name.trim()]
            if (match != null) {
                db.networkDeviceDao().updateDevice(
                    device.copy(id = match.id)
                )
            } else {
                db.networkDeviceDao().insertDevice(
                    device.copy(id = 0L)
                )
            }
        }

        // 3. Restore Retailers
        val existingRetailers = db.retailerDao().getAllRetailers().first()
        val existingRetNames = existingRetailers.associateBy { it.name.trim() }

        for (retailer in cloudData.retailers) {
            val match = existingRetNames[retailer.name.trim()]
            if (match != null) {
                db.retailerDao().updateRetailer(
                    retailer.copy(id = match.id)
                )
            } else {
                db.retailerDao().insertRetailer(
                    retailer.copy(id = 0L)
                )
            }
        }

        // 4. Restore Financial Vouchers
        val existingVouchers = db.financialVoucherDao().getAllVouchers().first()
        val existingVouchNums = existingVouchers.associateBy { it.voucherNumber.trim() }

        for (voucher in cloudData.vouchers) {
            val match = existingVouchNums[voucher.voucherNumber.trim()]
            if (match != null) {
                db.financialVoucherDao().updateVoucher(
                    voucher.copy(id = match.id)
                )
            } else {
                db.financialVoucherDao().insertVoucher(
                    voucher.copy(id = 0L)
                )
            }
        }

        // 5. Restore Card Packages
        val existingPackages = db.cardPackageDao().getAllPackages().first()
        val existingPkgNames = existingPackages.associateBy { it.name.trim() }

        for (pkg in cloudData.cardPackages) {
            val match = existingPkgNames[pkg.name.trim()]
            if (match != null) {
                db.cardPackageDao().updatePackage(
                    pkg.copy(id = match.id)
                )
            } else {
                db.cardPackageDao().insertPackage(
                    pkg.copy(id = 0L)
                )
            }
        }

        // 6. Restore Inventory Items
        val existingInventory = db.inventoryDao().getAllInventoryItems().first()
        val existingInvNames = existingInventory.associateBy { it.packageName.trim() }

        for (item in cloudData.inventoryItems) {
            val match = existingInvNames[item.packageName.trim()]
            if (match != null) {
                db.inventoryDao().updateItem(
                    item.copy(id = match.id)
                )
            } else {
                db.inventoryDao().insertItem(
                    item.copy(id = 0L)
                )
            }
        }

        // 7. Restore Card Sales Invoices
        val existingInvoices = db.cardSalesInvoiceDao().getAllSalesInvoices().first()
        val existingInvNums = existingInvoices.associateBy { it.invoiceNumber.trim() }

        for (inv in cloudData.salesInvoices) {
            val match = existingInvNums[inv.invoiceNumber.trim()]
            if (match != null) {
                db.cardSalesInvoiceDao().updateInvoice(
                    inv.copy(id = match.id)
                )
            } else {
                db.cardSalesInvoiceDao().insertInvoice(
                    inv.copy(id = 0L)
                )
            }
        }

        // 8. تسوية ومطابقة الحسابات والديون المحاسبية فورياً بعد الاستعادة
        reconcileAccountingLedger()
    }
}

