package com.subnook.app.data

import android.content.Context
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

enum class SortOrder(val label: String) {
    NEXT_DATE("按下次付款日期"), AMOUNT("按月均金额"), NAME("按名称")
}

enum class ThemeMode(val label: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }

@Serializable
data class AppSettings(
    val displayCurrency: String = "CNY",
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    val period: SummaryPeriod = SummaryPeriod.MONTH,
    val sort: SortOrder = SortOrder.NEXT_DATE,
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val currency = stringPreferencesKey("display_currency")
        val hour = intPreferencesKey("notify_hour")
        val minute = intPreferencesKey("notify_minute")
        val period = stringPreferencesKey("period")
        val sort = stringPreferencesKey("sort")
        val theme = stringPreferencesKey("theme")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            displayCurrency = p[K.currency] ?: "CNY",
            notifyHour = p[K.hour] ?: 9,
            notifyMinute = p[K.minute] ?: 0,
            period = p[K.period]?.let { runCatching { SummaryPeriod.valueOf(it) }.getOrNull() } ?: SummaryPeriod.MONTH,
            sort = p[K.sort]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.NEXT_DATE,
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun setTheme(t: ThemeMode) = context.dataStore.edit { it[K.theme] = t.name }

    suspend fun get(): AppSettings = flow.first()

    suspend fun setCurrency(c: String) = context.dataStore.edit { it[K.currency] = c }
    suspend fun setNotifyTime(h: Int, m: Int) = context.dataStore.edit { it[K.hour] = h; it[K.minute] = m }
    suspend fun setPeriod(p: SummaryPeriod) = context.dataStore.edit { it[K.period] = p.name }
    suspend fun setSort(s: SortOrder) = context.dataStore.edit { it[K.sort] = s.name }

    suspend fun replace(s: AppSettings) = context.dataStore.edit {
        it[K.currency] = s.displayCurrency
        it[K.hour] = s.notifyHour
        it[K.minute] = s.notifyMinute
        it[K.period] = s.period.name
        it[K.sort] = s.sort.name
        it[K.theme] = s.theme.name
    }
}
