package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل شريكاً في استثمار وتأسيس الشبكة اللاسلكية
 */
@Entity(tableName = "partners")
data class PartnerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val capitalInvested: Double = 0.0,      // المبلغ المدفوع في تأسيس وتطوير الشبكة
    val sharePercentage: Double = 0.0,     // النسبة المئوية المتفق عليها من الأرباح (مثلاً 50.0%)
    val joinDateMillis: Long = System.currentTimeMillis(),
    val totalWithdrawnProfit: Double = 0.0, // إجمالي الأرباح المستلمة حتى الآن
    val isActive: Boolean = true,
    val notes: String = ""
)
