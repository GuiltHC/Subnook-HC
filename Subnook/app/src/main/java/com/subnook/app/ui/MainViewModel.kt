package com.subnook.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.subnook.app.App
import com.subnook.app.data.AppSettings
import com.subnook.app.data.PaymentRecord
import com.subnook.app.data.RateTable
import com.subnook.app.data.SortOrder
import com.subnook.app.data.Subscription
import com.subnook.app.data.SummaryPeriod
import com.subnook.app.domain.Billing
import com.subnook.app.notify.Scheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val subs: List<Subscription> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val rates: RateTable = RateTable(emptyMap(), 0),
    val loaded: Boolean = false,
) {
    val active get() = subs.filter { it.active }
    val inactive get() = subs.filter { !it.active }

    fun total(period: SummaryPeriod = settings.period) = Billing.total(subs, period, rates, settings.displayCurrency)

    fun sortedActive(): List<Subscription> = when (settings.sort) {
        SortOrder.NEXT_DATE -> active.sortedBy { it.nextBillingDay }
        SortOrder.NAME -> active.sortedBy { it.name.lowercase() }
        SortOrder.AMOUNT -> active.sortedByDescending {
            rates.convert(Billing.periodCost(it, SummaryPeriod.MONTH), it.currency, settings.displayCurrency)
        }
    }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as App
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    val state: StateFlow<UiState> = combine(a.dao.observeAll(), a.settings.flow, a.rates.table) { subs, s, r ->
        UiState(subs, s, r, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun observeSub(id: Long): Flow<Subscription?> = a.dao.observe(id)
    fun observePayments(id: Long): Flow<List<PaymentRecord>> = a.dao.observePayments(id)
    fun observePaymentsBetween(from: Long, to: Long) = a.dao.observePaymentsBetween(from, to)
    fun observeAllPayments() = a.dao.observeAllPayments()

    fun toast(msg: String) { _messages.tryEmit(msg) }

    fun save(sub: Subscription, onDone: () -> Unit = {}) = viewModelScope.launch {
        a.dao.upsert(sub)
        Scheduler.runAll(a)
        onDone()
    }

    fun delete(sub: Subscription) = viewModelScope.launch {
        a.dao.delete(sub)
        a.notifier.cancel(sub.id)
        toast("已删除「${sub.name}」")
    }

    fun deletePayment(p: PaymentRecord) = viewModelScope.launch { a.dao.deletePayment(p) }

    fun confirmPaid(sub: Subscription) = viewModelScope.launch {
        a.processor.confirmPaid(sub.id)
        toast("已记录付款，下次账单日已更新")
    }

    fun skip(sub: Subscription) = viewModelScope.launch {
        a.processor.skip(sub.id)
        toast("已跳过本期")
    }

    fun setPeriod(p: SummaryPeriod) = viewModelScope.launch { a.settings.setPeriod(p) }
    fun setCurrency(c: String) = viewModelScope.launch { a.settings.setCurrency(c) }
    fun setSort(s: SortOrder) = viewModelScope.launch { a.settings.setSort(s) }
    fun setTheme(t: com.subnook.app.data.ThemeMode) = viewModelScope.launch { a.settings.setTheme(t) }
    fun setNotifyTime(h: Int, m: Int) = viewModelScope.launch {
        a.settings.setNotifyTime(h, m)
        Scheduler.scheduleDailyAlarm(a)
    }

    fun refreshRates() = viewModelScope.launch {
        a.rates.refresh()
            .onSuccess { toast("汇率已更新") }
            .onFailure { toast("汇率更新失败：${it.message ?: it.javaClass.simpleName}") }
    }

    fun export(uri: Uri) = viewModelScope.launch {
        runCatching { a.backup.export(uri) }
            .onSuccess { toast("已导出 $it 个订阅") }
            .onFailure { toast("导出失败：${it.message}") }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        runCatching { a.backup.import(uri) }
            .onSuccess {
                Scheduler.runAll(a)
                toast("已导入 ${it.subs} 个订阅、${it.payments} 条付款记录")
            }
            .onFailure { toast("导入失败：${it.message}") }
    }

    fun onResume() = viewModelScope.launch {
        Scheduler.runAll(a)
        a.rates.refreshIfStale()
    }
}
