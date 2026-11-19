package com.subnook.app.data

import com.subnook.app.R

import androidx.annotation.StringRes
import com.subnook.app.str

data class CurrencyInfo(val code: String, @StringRes val nameRes: Int, val symbol: String) {
    val name: String get() = str(nameRes)
}

object Currencies {
    val list = listOf(
        CurrencyInfo("CNY", R.string.currency_cny, "¥"),
        CurrencyInfo("USD", R.string.currency_usd, "$"),
        CurrencyInfo("EUR", R.string.currency_eur, "€"),
        CurrencyInfo("HKD", R.string.currency_hkd, "HK$"),
        CurrencyInfo("JPY", R.string.currency_jpy, "JP¥"),
        CurrencyInfo("GBP", R.string.currency_gbp, "£"),
        CurrencyInfo("TWD", R.string.currency_twd, "NT$"),
        CurrencyInfo("KRW", R.string.currency_krw, "₩"),
        CurrencyInfo("SGD", R.string.currency_sgd, "S$"),
        CurrencyInfo("MOP", R.string.currency_mop, "MOP$"),
        CurrencyInfo("AUD", R.string.currency_aud, "A$"),
        CurrencyInfo("CAD", R.string.currency_cad, "C$"),
        CurrencyInfo("CHF", R.string.currency_chf, "CHF"),
        CurrencyInfo("NZD", R.string.currency_nzd, "NZ$"),
        CurrencyInfo("INR", R.string.currency_inr, "₹"),
        CurrencyInfo("RUB", R.string.currency_rub, "₽"),
        CurrencyInfo("TRY", R.string.currency_try, "₺"),
        CurrencyInfo("THB", R.string.currency_thb, "฿"),
        CurrencyInfo("MYR", R.string.currency_myr, "RM"),
        CurrencyInfo("PHP", R.string.currency_php, "₱"),
        CurrencyInfo("VND", R.string.currency_vnd, "₫"),
        CurrencyInfo("IDR", R.string.currency_idr, "Rp"),
        CurrencyInfo("ARS", R.string.currency_ars, "AR$"),
        CurrencyInfo("BRL", R.string.currency_brl, "R$"),
        CurrencyInfo("MXN", R.string.currency_mxn, "MX$"),
        CurrencyInfo("NGN", R.string.currency_ngn, "₦"),
        CurrencyInfo("EGP", R.string.currency_egp, "E£"),
        CurrencyInfo("PKR", R.string.currency_pkr, "₨"),
        CurrencyInfo("UAH", R.string.currency_uah, "₴"),
        CurrencyInfo("KZT", R.string.currency_kzt, "₸"),
        CurrencyInfo("HUF", R.string.currency_huf, "Ft"),
    )
    private val byCode = list.associateBy { it.code }

    fun symbol(code: String) = byCode[code]?.symbol ?: "$code "
    fun name(code: String) = byCode[code]?.name ?: code

    /** 0 位小数的币种 */
    private val noDecimals = setOf("JPY", "KRW", "VND", "IDR", "HUF")

    /** 金额的小数位数（分期计算按这个精度四舍五入） */
    fun decimals(code: String): Int = if (code in noDecimals) 0 else 2

    fun format(amount: Double, code: String): String = formatWith(symbol(code), amount, code)

    private fun formatWith(symbol: String, amount: Double, code: String): String {
        // 整数金额不显示小数（CN¥3,000 而不是 CN¥3,000.00），有小数的才保留两位
        val cents = kotlin.math.round(amount * 100).toLong()
        val whole = cents % 100 == 0L
        val digits = if (code in noDecimals || whole) 0 else 2
        val sym = symbol.trim()
        // 以字母结尾的符号（CHF、RM、Rp、Ft）和数字之间留个空格
        val sep = if (sym.lastOrNull()?.isLetter() == true) " " else ""
        return sym + sep + "%,.${digits}f".format(amount)
    }

    /** 带币种代码，例如 "CNY ¥25.00"、"USD $15.49" */
    fun formatCode(amount: Double, code: String): String = code + " " + format(amount, code)

    /** 仅首页订阅列表用：¥/$ 加地区前缀，不再另外显示代码，例如 "CN¥25.00"、"US$20.00"、"JP¥3,000" */
    private val listSymbols = mapOf("CNY" to "CN¥", "USD" to "US$")

    fun listSymbol(code: String): String = (listSymbols[code] ?: symbol(code)).trim()

    fun formatList(amount: Double, code: String): String = formatWith(listSymbol(code), amount, code)

    /** 离线兜底汇率（1 USD = x），首次联网后会被实时汇率覆盖 */
    val fallbackRates: Map<String, Double> = mapOf(
        "USD" to 1.0, "CNY" to 7.12, "EUR" to 0.86, "HKD" to 7.78, "JPY" to 152.0, "GBP" to 0.76,
        "TWD" to 30.5, "KRW" to 1430.0, "SGD" to 1.30, "MOP" to 8.01, "AUD" to 1.53, "CAD" to 1.40,
        "CHF" to 0.80, "NZD" to 1.74, "INR" to 88.5, "RUB" to 81.0, "TRY" to 42.0, "THB" to 32.5,
        "MYR" to 4.20, "PHP" to 58.5, "VND" to 26300.0, "IDR" to 16600.0, "ARS" to 1450.0,
        "BRL" to 5.35, "MXN" to 18.4, "NGN" to 1460.0, "EGP" to 47.5, "PKR" to 282.0, "UAH" to 41.8,
        "KZT" to 525.0, "HUF" to 335.0,
    )
}
