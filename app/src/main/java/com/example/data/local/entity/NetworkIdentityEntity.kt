package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_identity")
data class NetworkIdentityEntity(
    @PrimaryKey
    val id: Long = 1L,
    val networkName: String = "شبكة سام ميكروتك الذكية",
    val ownerName: String = "المهندس سام",
    val supportPhone: String = "770000001",
    val supportWhatsapp: String = "967770000001",
    val supportEmail: String = "support@sam-mikrotic.ye",
    val networkLocation: String = "اليمن - صنعاء",
    val routerModel: String = "MikroTik CCR2004-16G-2S+",
    val routerOsVersion: String = "RouterOS v7.15",
    val approvedDeviceSubnet: String = "192.168.88.0/24",
    val gatewayIp: String = "192.168.88.1",
    val ipRangeStart: String = "192.168.88.2",
    val ipRangeEnd: String = "192.168.88.254",
    val hotspotSubnet: String = "10.5.50.0/24",
    val hotspotGatewayIp: String = "10.5.50.1",
    val dnsServers: String = "8.8.8.8, 1.1.1.1",
    val welcomeNotice: String = "أهلاً بكم في شبكة سام اللاسلكية - إنترنت فائق السرعة واستقرار دائم",
    val updatedAt: Long = System.currentTimeMillis()
)
