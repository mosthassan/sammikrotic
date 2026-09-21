package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * مساعد إدارة العملات وأسعار الصرف المرنة
 * يدعم: الريال اليمني (YER)، الريال السعودي (SAR)، الدولار الأمريكي (USD)
 */
object CurrencyHelper {

    const val CURRENCY_YER = "YER"
    const val CURRENCY_SAR = "SAR"
    const val CURRENCY_USD = "USD"

    val SUPPORTED_CURRENCIES = listOf(CURRENCY_YER, CURRENCY_SAR, CURRENCY_USD)

    fun getCurrencyLabel(currency: String): String {
        return when (currency.uppercase()) {
            CURRENCY_SAR -> "ريال سعودي (SAR)"
            CURRENCY_USD -> "دولار أمريكي (USD)"
            else -> "ريال يمني (YER)"
        }
    }

    fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase()) {
            CURRENCY_SAR -> "ر.س"
            CURRENCY_USD -> "$"
            else -> "ر.ي"
        }
    }

    /**
     * تحويل أي مبلغ من عملة مصدرية إلى الريال اليمني (العملة الأساسية للنظام)
     */
    fun convertToYer(amount: Double, fromCurrency: String, sarToYer: Double, usdToYer: Double): Double {
        if (amount <= 0.0) return 0.0
        return when (fromCurrency.uppercase()) {
            CURRENCY_SAR -> amount * (if (sarToYer > 0) sarToYer else 140.0)
            CURRENCY_USD -> amount * (if (usdToYer > 0) usdToYer else 530.0)
            else -> amount
        }
    }

    /**
     * تحويل من الريال اليمني إلى عملة أخرى
     */
    fun convertFromYer(amountInYer: Double, targetCurrency: String, sarToYer: Double, usdToYer: Double): Double {
        if (amountInYer <= 0.0) return 0.0
        return when (targetCurrency.uppercase()) {
            CURRENCY_SAR -> amountInYer / (if (sarToYer > 0) sarToYer else 140.0)
            CURRENCY_USD -> amountInYer / (if (usdToYer > 0) usdToYer else 530.0)
            else -> amountInYer
        }
    }

    /**
     * تحويل بين أي عملتين
     */
    fun convertBetween(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        sarToYer: Double,
        usdToYer: Double
    ): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        val amountInYer = convertToYer(amount, fromCurrency, sarToYer, usdToYer)
        return convertFromYer(amountInYer, toCurrency, sarToYer, usdToYer)
    }

    /**
     * تنسيق المبلغ مع رمز العملة
     */
    fun formatAmount(amount: Double, currency: String = CURRENCY_YER): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = if (amount % 1.0 == 0.0) {
            DecimalFormat("#,##0", symbols)
        } else {
            DecimalFormat("#,##0.00", symbols)
        }
        val formatted = df.format(amount)
        return "$formatted ${getCurrencySymbol(currency)}"
    }
}
