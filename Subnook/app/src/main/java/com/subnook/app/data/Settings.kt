package com.subnook.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

enum class SummaryPeriod(val label: String, val unitLabel: String) {
    WEEK("周", "每周"), MONTH("月", "每月"), YEAR("年", "每年")
}

/**
 * 首页订阅列表排序。AMOUNT / NAME / NEXT_DATE 是旧版本就有的名字，保存在设置和备份里，不能改名。
 * [defaultAsc]：刚选中这个排序时默认的方向；[ascHint] / [descHint]：正序 / 倒序的含义说明。
 */
enum class SortOrder(val label: String, val short: String, val defaultAsc: Boolean, val ascHint: String, val descHint: String) {
    NEXT_DATE("下次付款日期", "日期", true, "近 → 远", "远 → 近"),
    AMOUNT("月均金额", "金额", false, "低 → 高", "高 → 低"),
    NAME("名称", "名称", true, "A → Z", "Z → A"),
    CATEGORY("分类", "分类", true, "影视 → 其他", "其他 → 影视"),
    CREATED("添加时间", "添加", false, "旧 → 新", "新 → 旧"),
    /** 长按拖动排出来的顺序，没有正序 / 倒序 */
    MANUAL("手动排序", "手动", true, "", ""),
}

enum class ThemeMode(val label: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }

@Serializable
data class AppSettings(
    val displayCurrency: String = "CNY",
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    val period: SummaryPeriod = SummaryPeriod.MONTH,
    val sort: SortOrder = SortOrder.NEXT_DATE,
    /** null = 用该排序的默认方向（旧版本没有这个设置） */
    val sortAsc: Boolean? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** 用户自己加的分类（选「其他」时填的名称，确认后加入），订阅里存成 OTHER + categoryNote */
    val customCategories: List<String> = emptyList(),
    /** 用户自己加的付款方式（选「其他」时填的名称，确认后加入），订阅里存成 OTHER + payMethodNote */
    val customPayMethods: List<String> = emptyList(),
) {
    val sortAscending: Boolean get() = sortAsc ?: sort.defaultAsc
}

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val currency = stringPreferencesKey("display_currency")
        val hour = intPreferencesKey("notify_hour")
        val minute = intPreferencesKey("notify_minute")
        val period = stringPreferencesKey("period")
        val sort = stringPreferencesKey("sort")
        val sortAsc = booleanPreferencesKey("sort_asc")
        val theme = stringPreferencesKey("theme")
        /** 用户自己加的分类，一行一个 */
        val customCategories = stringPreferencesKey("custom_categories")
        val customPayMethods = stringPreferencesKey("custom_pay_methods")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            displayCurrency = p[K.currency] ?: "CNY",
            notifyHour = p[K.hour] ?: 9,
            notifyMinute = p[K.minute] ?: 0,
            period = p[K.period]?.let { runCatching { SummaryPeriod.valueOf(it) }.getOrNull() } ?: SummaryPeriod.MONTH,
            sort = p[K.sort]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.NEXT_DATE,
            sortAsc = p[K.sortAsc],
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            customCategories = parseCategories(p[K.customCategories]),
            customPayMethods = parseCategories(p[K.customPayMethods]),
        )
    }

    suspend fun setTheme(t: ThemeMode) = context.dataStore.edit { it[K.theme] = t.name }

    /** 自定义分类：加在最后，重名不重复加 */
    suspend fun addCustomCategory(name: String) = context.dataStore.edit {
        val list = parseCategories(it[K.customCategories])
        val n = cleanCategory(name)
        if (n.isNotEmpty() && n !in list) it[K.customCategories] = (list + n).joinToString(SEP)
    }

    suspend fun removeCustomCategory(name: String) = context.dataStore.edit {
        it[K.customCategories] = (parseCategories(it[K.customCategories]) - name).joinToString(SEP)
    }

    suspend fun addCustomPayMethod(name: String) = context.dataStore.edit {
        val list = parseCategories(it[K.customPayMethods])
        val n = cleanCategory(name)
        if (n.isNotEmpty() && n !in list) it[K.customPayMethods] = (list + n).joinToString(SEP)
    }

    suspend fun removeCustomPayMethod(name: String) = context.dataStore.edit {
        it[K.customPayMethods] = (parseCategories(it[K.customPayMethods]) - name).joinToString(SEP)
    }

    suspend fun get(): AppSettings = flow.first()

    suspend fun setCurrency(c: String) = context.dataStore.edit { it[K.currency] = c }
    suspend fun setNotifyTime(h: Int, m: Int) = context.dataStore.edit { it[K.hour] = h; it[K.minute] = m }
    suspend fun setPeriod(p: SummaryPeriod) = context.dataStore.edit { it[K.period] = p.name }
    suspend fun setSort(s: SortOrder, asc: Boolean) = context.dataStore.edit { it[K.sort] = s.name; it[K.sortAsc] = asc }

    suspend fun replace(s: AppSettings) = context.dataStore.edit {
        it[K.currency] = s.displayCurrency
        it[K.hour] = s.notifyHour
        it[K.minute] = s.notifyMinute
        it[K.period] = s.period.name
        it[K.sort] = s.sort.name
        s.sortAsc?.let { v -> it[K.sortAsc] = v } ?: it.remove(K.sortAsc)
        it[K.theme] = s.theme.name
        it[K.customCategories] = s.customCategories.joinToString(SEP)
        it[K.customPayMethods] = s.customPayMethods.joinToString(SEP)
    }
}

private fun parseCategories(raw: String?): List<String> =
    raw.orEmpty().split('\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

/** 分类名：去掉换行和首尾空格，最多 20 个字 */
fun cleanCategory(name: String): String = name.replace('\n', ' ').trim().take(20)

private const val SEP = "\n"
