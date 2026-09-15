package com.example.data.firebase

import android.util.Log
import com.example.data.local.entity.FinancialVoucherEntity
import com.example.data.local.entity.NetworkDeviceEntity
import com.example.data.local.entity.NetworkIdentityEntity
import com.example.data.local.entity.RetailerEntity
import com.example.data.local.entity.UserEntity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class CloudSyncResult(
    val success: Boolean,
    val message: String,
    val userPartition: String = "",
    val syncedDevicesCount: Int = 0,
    val syncedRetailersCount: Int = 0,
    val syncedVouchersCount: Int = 0
)

class FirebaseDbService {

    private val tag = "FirebaseDbService"

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Sanitizes email to be used as a document or collection path safely.
     * E.g. "mosthassan.ye2@gmail.com" -> "mosthassan_ye2_at_gmail_com"
     */
    private fun sanitizeEmail(email: String): String {
        return email.trim().lowercase()
            .replace("@", "_at_")
            .replace(".", "_")
            .replace("+", "_plus_")
    }

    private fun getDevicesRef(userEmail: String?): CollectionReference {
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            firestore.collection("users").document(userKey).collection("devices")
        } else {
            firestore.collection("devices")
        }
    }

    private fun getRetailersRef(userEmail: String?): CollectionReference {
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            firestore.collection("users").document(userKey).collection("retailers")
        } else {
            firestore.collection("retailers")
        }
    }

    private fun getVouchersRef(userEmail: String?): CollectionReference {
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            firestore.collection("users").document(userKey).collection("vouchers")
        } else {
            firestore.collection("vouchers")
        }
    }

    private fun getNetworkIdentityDocRef(userEmail: String?): DocumentReference {
        return if (!userEmail.isNullOrBlank()) {
            val userKey = sanitizeEmail(userEmail)
            firestore.collection("users").document(userKey).collection("settings").document("network_identity")
        } else {
            firestore.collection("settings").document("network_identity")
        }
    }

    suspend fun pushNetworkIdentity(identity: NetworkIdentityEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getNetworkIdentityDocRef(userEmail)
            val data = mapOf(
                "networkName" to identity.networkName,
                "ownerName" to identity.ownerName,
                "supportPhone" to identity.supportPhone,
                "supportWhatsapp" to identity.supportWhatsapp,
                "supportEmail" to identity.supportEmail,
                "networkLocation" to identity.networkLocation,
                "routerModel" to identity.routerModel,
                "routerOsVersion" to identity.routerOsVersion,
                "approvedDeviceSubnet" to identity.approvedDeviceSubnet,
                "gatewayIp" to identity.gatewayIp,
                "ipRangeStart" to identity.ipRangeStart,
                "ipRangeEnd" to identity.ipRangeEnd,
                "hotspotSubnet" to identity.hotspotSubnet,
                "hotspotGatewayIp" to identity.hotspotGatewayIp,
                "dnsServers" to identity.dnsServers,
                "welcomeNotice" to identity.welcomeNotice,
                "updatedAt" to identity.updatedAt,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing network identity to Firestore", e)
            false
        }
    }

    suspend fun syncAllToCloud(
        devices: List<NetworkDeviceEntity>,
        retailers: List<RetailerEntity>,
        vouchers: List<FinancialVoucherEntity>,
        userEmail: String? = null,
        networkIdentity: NetworkIdentityEntity? = null
    ): CloudSyncResult = withContext(Dispatchers.IO) {
        try {
            var syncedDevices = 0
            var syncedRetailers = 0
            var syncedVouchers = 0

            // Upload network identity if provided
            if (networkIdentity != null) {
                pushNetworkIdentity(networkIdentity, userEmail)
            }

            val devRef = getDevicesRef(userEmail)
            val retRef = getRetailersRef(userEmail)
            val vouchRef = getVouchersRef(userEmail)

            // Upload user profile meta if email exists
            if (!userEmail.isNullOrBlank()) {
                val userKey = sanitizeEmail(userEmail)
                val userDocRef = firestore.collection("users").document(userKey)
                setDocAsync(
                    userDocRef,
                    mapOf(
                        "email" to userEmail,
                        "updatedAt" to System.currentTimeMillis(),
                        "devicesCount" to devices.size,
                        "retailersCount" to retailers.size,
                        "vouchersCount" to vouchers.size
                    )
                )
            }

            // Upload devices
            for (device in devices) {
                val docRef = devRef.document(device.id.toString())
                val data = mapOf(
                    "id" to device.id,
                    "name" to device.name,
                    "ipAddress" to device.ipAddress,
                    "macAddress" to device.macAddress,
                    "deviceType" to device.deviceType,
                    "locationArea" to device.locationArea,
                    "portOrInterface" to device.portOrInterface,
                    "frequencyOrSsid" to device.frequencyOrSsid,
                    "model" to device.model,
                    "username" to device.username,
                    "status" to device.status,
                    "signalDbm" to device.signalDbm,
                    "uptimeHours" to device.uptimeHours,
                    "notes" to device.notes,
                    "latitude" to device.latitude,
                    "longitude" to device.longitude,
                    "coverageRadiusMeters" to device.coverageRadiusMeters,
                    "parentDeviceId" to (device.parentDeviceId ?: 0L),
                    "createdAt" to device.createdAt,
                    "ownerEmail" to (userEmail ?: "public")
                )
                setDocAsync(docRef, data)
                syncedDevices++
            }

            // Upload retailers
            for (retailer in retailers) {
                val docRef = retRef.document(retailer.id.toString())
                val data = mapOf(
                    "id" to retailer.id,
                    "name" to retailer.name,
                    "ownerName" to retailer.ownerName,
                    "phone" to retailer.phone,
                    "location" to retailer.location,
                    "balanceOwed" to retailer.balanceOwed,
                    "totalPaid" to retailer.totalPaid,
                    "activeCardsCount" to retailer.activeCardsCount,
                    "commissionPercent" to retailer.commissionPercent,
                    "notes" to retailer.notes,
                    "createdAt" to retailer.createdAt,
                    "ownerEmail" to (userEmail ?: "public")
                )
                setDocAsync(docRef, data)
                syncedRetailers++
            }

            // Upload vouchers
            for (voucher in vouchers) {
                val docRef = vouchRef.document(voucher.voucherNumber)
                val data = mapOf(
                    "id" to voucher.id,
                    "voucherNumber" to voucher.voucherNumber,
                    "voucherType" to voucher.voucherType,
                    "amount" to voucher.amount,
                    "partyName" to voucher.partyName,
                    "retailerId" to (voucher.retailerId ?: 0L),
                    "category" to voucher.category,
                    "paymentMethod" to voucher.paymentMethod,
                    "description" to voucher.description,
                    "dateMillis" to voucher.dateMillis,
                    "issuerName" to voucher.issuerName,
                    "notes" to voucher.notes,
                    "ownerEmail" to (userEmail ?: "public")
                )
                setDocAsync(docRef, data)
                syncedVouchers++
            }

            val partitionLabel = if (!userEmail.isNullOrBlank()) "حساب $userEmail" else "المستودع العام"

            CloudSyncResult(
                success = true,
                message = "تمت المزامنة بنجاح مع Firebase ($partitionLabel)",
                userPartition = userEmail ?: "",
                syncedDevicesCount = syncedDevices,
                syncedRetailersCount = syncedRetailers,
                syncedVouchersCount = syncedVouchers
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync with Firestore", e)
            CloudSyncResult(
                success = false,
                message = "خطأ بالمزامنة السحابية: ${e.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}"
            )
        }
    }

    suspend fun pushDevice(device: NetworkDeviceEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getDevicesRef(userEmail).document(device.id.toString())
            val data = mapOf(
                "id" to device.id,
                "name" to device.name,
                "ipAddress" to device.ipAddress,
                "macAddress" to device.macAddress,
                "deviceType" to device.deviceType,
                "locationArea" to device.locationArea,
                "portOrInterface" to device.portOrInterface,
                "frequencyOrSsid" to device.frequencyOrSsid,
                "model" to device.model,
                "username" to device.username,
                "status" to device.status,
                "signalDbm" to device.signalDbm,
                "uptimeHours" to device.uptimeHours,
                "notes" to device.notes,
                "latitude" to device.latitude,
                "longitude" to device.longitude,
                "coverageRadiusMeters" to device.coverageRadiusMeters,
                "parentDeviceId" to (device.parentDeviceId ?: 0L),
                "createdAt" to device.createdAt,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing device to Firestore", e)
            false
        }
    }

    suspend fun deleteDevice(deviceId: Long, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getDevicesRef(userEmail).document(deviceId.toString())
            deleteDocAsync(docRef)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting device from Firestore", e)
            false
        }
    }

    suspend fun pushRetailer(retailer: RetailerEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getRetailersRef(userEmail).document(retailer.id.toString())
            val data = mapOf(
                "id" to retailer.id,
                "name" to retailer.name,
                "ownerName" to retailer.ownerName,
                "phone" to retailer.phone,
                "location" to retailer.location,
                "balanceOwed" to retailer.balanceOwed,
                "totalPaid" to retailer.totalPaid,
                "activeCardsCount" to retailer.activeCardsCount,
                "commissionPercent" to retailer.commissionPercent,
                "notes" to retailer.notes,
                "createdAt" to retailer.createdAt,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing retailer to Firestore", e)
            false
        }
    }

    suspend fun deleteRetailer(retailerId: Long, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getRetailersRef(userEmail).document(retailerId.toString())
            deleteDocAsync(docRef)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting retailer from Firestore", e)
            false
        }
    }

    suspend fun pushVoucher(voucher: FinancialVoucherEntity, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getVouchersRef(userEmail).document(voucher.voucherNumber)
            val data = mapOf(
                "id" to voucher.id,
                "voucherNumber" to voucher.voucherNumber,
                "voucherType" to voucher.voucherType,
                "amount" to voucher.amount,
                "partyName" to voucher.partyName,
                "retailerId" to (voucher.retailerId ?: 0L),
                "category" to voucher.category,
                "paymentMethod" to voucher.paymentMethod,
                "description" to voucher.description,
                "dateMillis" to voucher.dateMillis,
                "issuerName" to voucher.issuerName,
                "notes" to voucher.notes,
                "ownerEmail" to (userEmail ?: "public")
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing voucher to Firestore", e)
            false
        }
    }

    suspend fun deleteVoucher(voucherNumber: String, userEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = getVouchersRef(userEmail).document(voucherNumber)
            deleteDocAsync(docRef)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting voucher from Firestore", e)
            false
        }
    }

    // ==========================================
    // Authorized Users & Distributors Cloud Sync
    // ==========================================

    private fun getAuthorizedUsersRef(): CollectionReference {
        return firestore.collection("authorized_users")
    }

    suspend fun pushAuthorizedUser(user: UserEntity, adminEmail: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val emailKey = if (user.email.isNotBlank()) sanitizeEmail(user.email) else "user_${user.id}"
            val docRef = getAuthorizedUsersRef().document(emailKey)
            val data = mapOf(
                "id" to user.id,
                "username" to user.username,
                "fullName" to user.fullName,
                "role" to user.role,
                "email" to user.email.trim().lowercase(),
                "phone" to user.phone,
                "assignedArea" to user.assignedArea,
                "commissionRate" to user.commissionRate,
                "notes" to user.notes,
                "isActive" to user.isActive,
                "registeredByAdmin" to (adminEmail ?: "admin"),
                "createdAt" to user.createdAt
            )
            setDocAsync(docRef, data)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error pushing authorized distributor to Firestore", e)
            false
        }
    }

    suspend fun deleteAuthorizedUser(userEmail: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (userEmail.isBlank()) return@withContext false
            val emailKey = sanitizeEmail(userEmail)
            val docRef = getAuthorizedUsersRef().document(emailKey)
            deleteDocAsync(docRef)
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting authorized user from Firestore", e)
            false
        }
    }

    suspend fun fetchAuthorizedUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext null
        try {
            val emailKey = sanitizeEmail(email)
            val snap = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { cont ->
                getAuthorizedUsersRef().document(emailKey).get()
                    .addOnSuccessListener { snapshot ->
                        if (cont.isActive) cont.resume(snapshot)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }

            if (snap != null && snap.exists()) {
                val fullName = snap.getString("fullName") ?: ""
                val role = snap.getString("role") ?: "DISTRIBUTOR"
                val phone = snap.getString("phone") ?: ""
                val assignedArea = snap.getString("assignedArea") ?: ""
                val commissionRate = snap.getDouble("commissionRate") ?: 0.0
                val notes = snap.getString("notes") ?: ""
                val isActive = snap.getBoolean("isActive") ?: true

                return@withContext UserEntity(
                    username = email.substringBefore("@"),
                    fullName = fullName,
                    role = role,
                    email = email.trim().lowercase(),
                    phone = phone,
                    assignedArea = assignedArea,
                    commissionRate = commissionRate,
                    notes = notes,
                    isActive = isActive,
                    isGoogleUser = true
                )
            }
            null
        } catch (e: Exception) {
            Log.e(tag, "Error fetching authorized user from Firestore", e)
            null
        }
    }

    private suspend fun setDocAsync(
        docRef: com.google.firebase.firestore.DocumentReference,
        data: Map<String, Any>
    ): Unit = suspendCancellableCoroutine { continuation ->
        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Unit)
            }
            .addOnFailureListener { exception ->
                Log.w(tag, "Firestore write note: ${exception.message}")
                if (continuation.isActive) continuation.resume(Unit)
            }
    }

    private suspend fun deleteDocAsync(
        docRef: com.google.firebase.firestore.DocumentReference
    ): Unit = suspendCancellableCoroutine { continuation ->
        docRef.delete()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Unit)
            }
            .addOnFailureListener { exception ->
                Log.w(tag, "Firestore delete note: ${exception.message}")
                if (continuation.isActive) continuation.resume(Unit)
            }
    }
}
