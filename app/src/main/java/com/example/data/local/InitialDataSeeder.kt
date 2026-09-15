package com.example.data.local

import com.example.data.local.entity.CardBatchEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.NetworkAssetEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.PartnerEntity
import com.example.data.local.entity.PartnerTransactionEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import kotlin.random.Random

object InitialDataSeeder {
    suspend fun seedDatabase(db: AppDatabase) {
        try {
            // 0. Seed Network Identity & IP Configuration
        val defaultIdentity = NetworkIdentityEntity(
            id = 1L,
            networkName = "شبكة سام ميكروتك الذكية",
            ownerName = "المهندس سام",
            supportPhone = "770000001",
            supportWhatsapp = "967770000001",
            supportEmail = "support@sam-mikrotic.ye",
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
        db.networkIdentityDao().insertOrUpdate(defaultIdentity)

        // 1. Seed Users
        val users = listOf(
            UserEntity(username = "owner_sam", fullName = "المهندس سام (مالك الشبكة)", role = "OWNER", phone = "770000001"),
            UserEntity(username = "eng_ayman", fullName = "م. أيمن الشبواني (مهندس الشبكة)", role = "ENGINEER", phone = "770000002"),
            UserEntity(username = "dist_fahad", fullName = "فهد القدسي (مسؤول التوزيع)", role = "DISTRIBUTOR", phone = "770000003"),
            UserEntity(username = "pos_baraka", fullName = "أبو محمد (بقالة البركة)", role = "RETAILER", phone = "771234567", retailerId = 1L)
        )
        db.userDao().insertUsers(users)

        // 2. Seed Network Devices
        val devices = listOf(
            NetworkDeviceEntity(
                name = "سيرفر MikroTik CCR2004 الرئيسي",
                deviceType = "MikroTik RouterBOARD",
                ipAddress = "192.168.88.1",
                macAddress = "D4:CA:6D:11:22:33",
                locationArea = "غرفة السيرفرات - المركز الرئيسي",
                portOrInterface = "sfp-sfpplus1 (WAN)",
                frequencyOrSsid = "Core Gateway",
                model = "CCR2004-16G-2S+",
                status = "ONLINE",
                signalDbm = 0,
                uptimeHours = 340,
                latitude = 15.3547,
                longitude = 44.2066,
                coverageRadiusMeters = 800,
                parentDeviceId = null,
                notes = "الراوتر الرئيسي لتوزيع الهوتسبوت وإدارة الباندويث"
            ),
            NetworkDeviceEntity(
                name = "سويتش ميكروتك التوزيعي CRS328",
                deviceType = "Switch",
                ipAddress = "192.168.88.2",
                macAddress = "D4:CA:6D:44:55:66",
                locationArea = "كابينة السيرفرات الرئيسية",
                portOrInterface = "sfp-plus1",
                frequencyOrSsid = "Core Switch",
                model = "CRS328-24P-4S+RM",
                status = "ONLINE",
                signalDbm = 0,
                uptimeHours = 340,
                latitude = 15.3549,
                longitude = 44.2068,
                coverageRadiusMeters = 150,
                parentDeviceId = 1L,
                notes = "سويتش PoE لتغذية الأبراج والأكسسات"
            ),
            NetworkDeviceEntity(
                name = "سيكتور شمالي برج التحرير 5G",
                deviceType = "Sector Antenna",
                ipAddress = "192.168.88.10",
                macAddress = "F0:9F:C2:77:88:99",
                locationArea = "برج التحرير - أعلى سارية",
                portOrInterface = "ether1 (PoE 24V)",
                frequencyOrSsid = "5240 MHz / SAM-TOW1-NORTH",
                model = "Ubiquiti Rocket Prism 5AC",
                status = "ONLINE",
                signalDbm = -56,
                uptimeHours = 128,
                latitude = 15.3582,
                longitude = 44.2031,
                coverageRadiusMeters = 650,
                parentDeviceId = 1L,
                notes = "سيكتور 120 درجة يغطي الحارات الشمالية"
            ),
            NetworkDeviceEntity(
                name = "أكسس بوينت حي الروضة",
                deviceType = "Access Point",
                ipAddress = "192.168.88.20",
                macAddress = "68:D7:9A:12:34:56",
                locationArea = "حي الروضة - عمود الإضاءة 4",
                portOrInterface = "ether1",
                frequencyOrSsid = "2437 MHz / SAM-HOTSPOT-RAWDA",
                model = "TP-Link EAP225-Outdoor",
                status = "ONLINE",
                signalDbm = -61,
                uptimeHours = 72,
                latitude = 15.3625,
                longitude = 44.2115,
                coverageRadiusMeters = 320,
                parentDeviceId = 1L,
                notes = "تغطية ممتازة لمنطقة المحلات والشارع التجاري"
            ),
            NetworkDeviceEntity(
                name = "أكسس بوينت شارع الجامعة",
                deviceType = "Access Point",
                ipAddress = "192.168.88.25",
                macAddress = "CC:2D:E0:98:76:54",
                locationArea = "جولة الجامعة - عمارة النصر",
                portOrInterface = "wlan1-Hotspot",
                frequencyOrSsid = "2.4 GHz / SAM-HOTSPOT-UNIV",
                model = "MikroTik Metal 52 ac",
                status = "ONLINE",
                signalDbm = -64,
                uptimeHours = 195,
                latitude = 15.3501,
                longitude = 44.1950,
                coverageRadiusMeters = 400,
                parentDeviceId = 1L,
                notes = "ضغط مستخدمين عالي في الفترة المسائية"
            ),
            NetworkDeviceEntity(
                name = "نانوستيشن ربط برج الستين",
                deviceType = "CPE",
                ipAddress = "192.168.88.35",
                macAddress = "B4:FB:E4:AA:BB:CC",
                locationArea = "برج الستين - قطاع جنوبي",
                portOrInterface = "ether1",
                frequencyOrSsid = "5500 MHz / PtP-Backhaul",
                model = "NanoStation 5AC Loco",
                status = "WARNING",
                signalDbm = -74,
                uptimeHours = 16,
                latitude = 15.3420,
                longitude = 44.1880,
                coverageRadiusMeters = 500,
                parentDeviceId = 1L,
                notes = "الإشارة ضعيفة نسبياً بحاجة لضبط التوجيه والمحاذاة"
            )
        )
        devices.forEach { db.networkDeviceDao().insertDevice(it) }

        // 3. Seed Retailers
        val r1 = db.retailerDao().insertRetailer(
            RetailerEntity(
                name = "بقالة البركة والخير",
                ownerName = "أبو محمد اليافعي",
                phone = "771234567",
                location = "حي التحرير - جوار مدرسة الفتح",
                commissionPercent = 10.0,
                balanceOwed = 45000.0,
                totalPaid = 120000.0,
                activeCardsCount = 50,
                notes = "نقطة توزيع رئيسية، ملتزم بالسداد الدوري"
            )
        )
        val r2 = db.retailerDao().insertRetailer(
            RetailerEntity(
                name = "سوبرماركت النخبة",
                ownerName = "عمر السعدي",
                phone = "777654321",
                location = "حي الروضة - الشارع العام",
                commissionPercent = 10.0,
                balanceOwed = 68000.0,
                totalPaid = 240000.0,
                activeCardsCount = 80,
                notes = "سحب عالي لكروت فئة 500 و1000 ريال"
            )
        )
        val r3 = db.retailerDao().insertRetailer(
            RetailerEntity(
                name = "كشك الأمل للاتصالات",
                ownerName = "ماجد العماري",
                phone = "733889900",
                location = "جولة الجامعة - أمام البوابة",
                commissionPercent = 10.0,
                balanceOwed = 18500.0,
                totalPaid = 75000.0,
                activeCardsCount = 35,
                notes = "أغلب زبائنه طلاب جامعة"
            )
        )

        // 4. Seed Card Batches & Cards
        val b1Id = db.cardDao().insertBatch(
            CardBatchEntity(
                batchName = "دفعة الربيع فئة 200 ريال",
                categoryName = "200 ريال - 1.5GB",
                retailPrice = 200.0,
                wholesalePrice = 180.0,
                quotaMb = 1536,
                validityHours = 24,
                speedLimit = "4M/2M",
                totalCount = 100,
                mikrotikProfile = "profile-200r",
                prefix = "SAM2"
            )
        )
        val b2Id = db.cardDao().insertBatch(
            CardBatchEntity(
                batchName = "دفعة VIP فئة 500 ريال",
                categoryName = "500 ريال - 4.5GB",
                retailPrice = 500.0,
                wholesalePrice = 450.0,
                quotaMb = 4608,
                validityHours = 72,
                speedLimit = "6M/3M",
                totalCount = 60,
                mikrotikProfile = "profile-500r",
                prefix = "SAM5"
            )
        )

        // Generate sample cards for batch 1
        val sampleCards = mutableListOf<CardEntity>()
        for (i in 1..40) {
            val userCode = "sam200${1000 + i}"
            val passCode = "${Random.nextInt(10000, 99999)}"
            val status = when {
                i <= 15 -> "SOLD"
                i <= 30 -> "DISTRIBUTED"
                else -> "AVAILABLE"
            }
            val retId = if (status != "AVAILABLE") r1 else null
            val retName = if (status != "AVAILABLE") "بقالة البركة والخير" else null
            sampleCards.add(
                CardEntity(
                    batchId = b1Id,
                    username = userCode,
                    password = passCode,
                    categoryName = "200 ريال - 1.5GB",
                    retailPrice = 200.0,
                    wholesalePrice = 180.0,
                    status = status,
                    retailerId = retId,
                    retailerName = retName,
                    distributedAt = if (retId != null) System.currentTimeMillis() - 86400000L * 2 else null,
                    soldAt = if (status == "SOLD") System.currentTimeMillis() - 3600000L * 5 else null
                )
            )
        }

        // Generate sample cards for batch 2
        for (i in 1..25) {
            val userCode = "sam500${2000 + i}"
            val passCode = "${Random.nextInt(10000, 99999)}"
            val status = when {
                i <= 8 -> "SOLD"
                i <= 18 -> "DISTRIBUTED"
                else -> "AVAILABLE"
            }
            val retId = if (status != "AVAILABLE") r2 else null
            val retName = if (status != "AVAILABLE") "سوبرماركت النخبة" else null
            sampleCards.add(
                CardEntity(
                    batchId = b2Id,
                    username = userCode,
                    password = passCode,
                    categoryName = "500 ريال - 4.5GB",
                    retailPrice = 500.0,
                    wholesalePrice = 450.0,
                    status = status,
                    retailerId = retId,
                    retailerName = retName,
                    distributedAt = if (retId != null) System.currentTimeMillis() - 86400000L else null,
                    soldAt = if (status == "SOLD") System.currentTimeMillis() - 3600000L * 2 else null
                )
            )
        }
        db.cardDao().insertCards(sampleCards)

        // 5. Seed Financial Vouchers (سندات قبض وصرف)
        val vouchers = listOf(
            FinancialVoucherEntity(
                voucherNumber = "REC-2026-001",
                voucherType = "RECEIPT",
                amount = 40000.0,
                partyName = "بقالة البركة والخير",
                retailerId = r1,
                category = "توريد مبيعات كروت",
                paymentMethod = "نقداً",
                description = "استلام دفعة سداد كروت هوتسبوت مباعة فئة 200 ريال",
                issuerName = "فهد القدسي",
                dateMillis = System.currentTimeMillis() - 86400000L * 2
            ),
            FinancialVoucherEntity(
                voucherNumber = "REC-2026-002",
                voucherType = "RECEIPT",
                amount = 55000.0,
                partyName = "سوبرماركت النخبة",
                retailerId = r2,
                category = "توريد مبيعات كروت",
                paymentMethod = "حوالة مصرفية",
                description = "تسديد جزئي لحساب كروت فئة 500 وفئة 1000 ريال",
                issuerName = "المهندس سام",
                dateMillis = System.currentTimeMillis() - 86400000L
            ),
            FinancialVoucherEntity(
                voucherNumber = "PAY-2026-001",
                voucherType = "PAYMENT",
                amount = 130000.0,
                partyName = "شركة الاتصالات اليمنية (يمن نت)",
                category = "اشتراك نت رئيسي",
                paymentMethod = "تحويل بنكي",
                description = "سداد اشتراك الفايبر الرئيسي للشبكة سرعة 250 ميجا",
                issuerName = "المهندس سام",
                dateMillis = System.currentTimeMillis() - 86400000L * 3
            ),
            FinancialVoucherEntity(
                voucherNumber = "PAY-2026-002",
                voucherType = "PAYMENT",
                amount = 35000.0,
                partyName = "محطة المحروقات",
                category = "ديزل وطاقة شمسية",
                paymentMethod = "نقداً",
                description = "شراء ديزل لمولد برج التحرير أثناء انقطاع الكهرباء العمومية",
                issuerName = "م. أيمن الشبواني",
                dateMillis = System.currentTimeMillis() - 86400000L * 1
            ),
            FinancialVoucherEntity(
                voucherNumber = "PAY-2026-003",
                voucherType = "PAYMENT",
                amount = 22000.0,
                partyName = "محل إلكترونيات ومستلزمات شبكات",
                category = "صيانة ومعدات",
                paymentMethod = "نقداً",
                description = "شراء رول كيبل كات 6 خارجي ومحولات PoE 24V",
                issuerName = "م. أيمن الشبواني",
                dateMillis = System.currentTimeMillis() - 3600000L * 10
            )
        )
        vouchers.forEach { db.financialVoucherDao().insertVoucher(it) }

        // 6. Seed Investment Partners & Equity
        val partners = listOf(
            PartnerEntity(
                id = 1L,
                name = "المهندس سام الشبواني",
                phone = "770000001",
                capitalInvested = 1500000.0,
                sharePercentage = 50.0,
                joinDateMillis = System.currentTimeMillis() - 86400000L * 180,
                totalWithdrawnProfit = 120000.0,
                isActive = true,
                notes = "المؤسس والمدير التنفيذي - إدارة البنية التحتية والبرمجة"
            ),
            PartnerEntity(
                id = 2L,
                name = "الحاج عبد الرحمن القدسي",
                phone = "771122334",
                capitalInvested = 900000.0,
                sharePercentage = 30.0,
                joinDateMillis = System.currentTimeMillis() - 86400000L * 180,
                totalWithdrawnProfit = 72000.0,
                isActive = true,
                notes = "شريك ممول - تمويل الطاقة الشمسية وتوسعة الأبراج"
            ),
            PartnerEntity(
                id = 3L,
                name = "أ. نبيل صالح الحميري",
                phone = "772233445",
                capitalInvested = 600000.0,
                sharePercentage = 20.0,
                joinDateMillis = System.currentTimeMillis() - 86400000L * 120,
                totalWithdrawnProfit = 48000.0,
                isActive = true,
                notes = "شريك تشغيلي - الإشراف الميداني والتسويق لنقاط البيع"
            )
        )
        partners.forEach { db.partnerDao().insertPartner(it) }

        // 7. Seed Network Fixed Assets (CAPEX)
        val assets = listOf(
            NetworkAssetEntity(
                assetName = "سيرفر رئيسي MikroTik Cloud Core CCR2004",
                category = "SERVERS",
                purchaseCost = 380000.0,
                estimatedCurrentValue = 350000.0,
                purchaseDateMillis = System.currentTimeMillis() - 86400000L * 150,
                location = "غرفة السيرفرات المركزية - السبعين",
                serialNumber = "CCR2004-16G-SN8821",
                status = "ACTIVE",
                notes = "الراوتر الرئيسي لإدارة شبكة الهوتسبوت والمشتركين والكروت"
            ),
            NetworkAssetEntity(
                assetName = "برج حديدي ثلاثي الأرجل مجلفن 24 متر",
                category = "TOWERS",
                purchaseCost = 550000.0,
                estimatedCurrentValue = 520000.0,
                purchaseDateMillis = System.currentTimeMillis() - 86400000L * 160,
                location = "موقع برج التحرير - أعلى المبنى",
                serialNumber = "TWR-24M-TH",
                status = "ACTIVE",
                notes = "يشمل كوابل التثبيت وقاعدة خرسانية مسلحة ومانعة صواعق"
            ),
            NetworkAssetEntity(
                assetName = "منظومة طاقة شمسية (4 ألواح 650W + انفرتر Growatt 3.5KW)",
                category = "SOLAR_POWER",
                purchaseCost = 420000.0,
                estimatedCurrentValue = 390000.0,
                purchaseDateMillis = System.currentTimeMillis() - 86400000L * 140,
                location = "برج التحرير - سطح المبنى",
                serialNumber = "GW-SPF3500ES",
                status = "ACTIVE",
                notes = "تغذية مستمرة 24 ساعة لتشغيل أبراج التغطية بدون انقطاع"
            ),
            NetworkAssetEntity(
                assetName = "بنك بطاريات ليثيوم LiFePO4 48V 100Ah",
                category = "SOLAR_POWER",
                purchaseCost = 650000.0,
                estimatedCurrentValue = 620000.0,
                purchaseDateMillis = System.currentTimeMillis() - 86400000L * 130,
                location = "كابينة الطاقة - برج التحرير",
                serialNumber = "LITH-48100-PRO",
                status = "ACTIVE",
                notes = "بطاريات ذكية مع نظام BMS تدعم 6000 دورة شحن"
            ),
            NetworkAssetEntity(
                assetName = "محطات بث لاسلكية Mimosa A5c + 4 سيكتورات RF Elements",
                category = "TOWERS",
                purchaseCost = 480000.0,
                estimatedCurrentValue = 440000.0,
                purchaseDateMillis = System.currentTimeMillis() - 86400000L * 120,
                location = "قمة برج التحرير",
                serialNumber = "MIMO-A5C-4SEC",
                status = "ACTIVE",
                notes = "تغطية 360 درجة لمربع السبعين وحدة السكني"
            ),
            NetworkAssetEntity(
                assetName = "رول كابلات فايبر بصري مسلحة 4 كور 1000 متر",
                category = "FIBER_CABLES",
                purchaseCost = 140000.0,
                estimatedCurrentValue = 130000.0,
                purchaseDateMillis = System.currentTimeMillis() - 86400000L * 90,
                location = "مسار التمديد من السيرفر إلى برج النصر",
                serialNumber = "FIBER-4C-ARM-1KM",
                status = "ACTIVE",
                notes = "ربط رئيسي بين البرجين بسرعة 10 جيجابت"
            ),
            NetworkAssetEntity(
                assetName = "مولد كهربائي كوماتسو احتياطي 5KVA ديزل",
                category = "SOLAR_POWER",
                purchaseCost = 280000.0,
                estimatedCurrentValue = 250000.0,
                purchaseDateMillis = System.currentTimeMillis() - 86400000L * 170,
                location = "غرفة طوارئ برج التحرير",
                serialNumber = "GEN-KM-5KVA",
                status = "ACTIVE",
                notes = "تشغيل طوارئ في حال المنخفضات الجوية وغياب الشمس"
            )
        )
        assets.forEach { db.networkAssetDao().insertAsset(it) }

        // 8. Seed Partner Transactions (Dividends Distribution sample)
        val partnerTxs = listOf(
            PartnerTransactionEntity(
                partnerId = 1L,
                partnerName = "المهندس سام الشبواني",
                transactionType = "DIVIDEND_PAYOUT",
                amount = 70000.0,
                dateMillis = System.currentTimeMillis() - 86400000L * 30,
                notes = "توزيع أرباح الربع المالي الماضي (حصة 50%)"
            ),
            PartnerTransactionEntity(
                partnerId = 2L,
                partnerName = "الحاج عبد الرحمن القدسي",
                transactionType = "DIVIDEND_PAYOUT",
                amount = 42000.0,
                dateMillis = System.currentTimeMillis() - 86400000L * 30,
                notes = "توزيع أرباح الربع المالي الماضي (حصة 30%)"
            ),
            PartnerTransactionEntity(
                partnerId = 3L,
                partnerName = "أ. نبيل صالح الحميري",
                transactionType = "DIVIDEND_PAYOUT",
                amount = 28000.0,
                dateMillis = System.currentTimeMillis() - 86400000L * 30,
                notes = "توزيع أرباح الربع المالي الماضي (حصة 20%)"
            )
        )
        partnerTxs.forEach { db.partnerDao().insertPartnerTransaction(it) }
        } catch (e: Throwable) {
            android.util.Log.e("InitialDataSeeder", "Error while seeding initial data: ${e.message}", e)
        }
    }
}

