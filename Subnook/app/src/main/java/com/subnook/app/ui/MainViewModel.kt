package com.subnook.app.ui

import com.subnook.app.R
import com.subnook.app.str
import com.subnook.app.plural
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
    // 首页把订阅和分期分成两个区域；统计、紫色卡片、日历仍然把两者合在一起算
    val activeSubs get() = active.filter { !it.isInstallment }
    val activeInstallments get() = active.filter { it.isInstallment }

    fun total(period: SummaryPeriod = settings.period) = Billing.total(subs, period, rates, settings.displayCurrency)

    /** 首页「订阅中」的顺序（不含分期） */
    fun sortedActive(): List<Subscription> = sorted(activeSubs)
    /** 首页「分期还款」：和订阅用同一个排序设置 */
    fun sortedInstallments(): List<Subscription> = sorted(activeInstallments)
    fun sortedInactive(): List<Subscription> = sorted(inactive)

    private fun sorted(list: List<Subscription>): List<Subscription> {
        fun monthly(s: Subscription) = rates.convert(Billing.periodCost(s, SummaryPeriod.MONTH), s.currency, settings.displayCurrency)
        // 中文名按拼音排，英文不区分大小写
        val byName = compareBy(nameCollator) { s: Subscription -> s.name.trim() }
        val key: Comparator<Subscription> = when (settings.sort) {
            SortOrder.MANUAL -> return list.sortedWith(compareBy<Subscription> { it.sortIndex }.thenBy { it.id })
            SortOrder.NEXT_DATE -> compareBy { it.nextBillingDay }
            SortOrder.AMOUNT -> compareBy { monthly(it) }
            SortOrder.NAME -> byName
            SortOrder.CATEGORY -> compareBy { it.category.ordinal }
            SortOrder.CREATED -> compareBy<Subscription> { it.createdAt }.thenBy { it.id }
        }
        // 正序 / 倒序只作用于主排序键；值相同时始终按名称正序，顺序稳定
        return list.sortedWith((if (settings.sortAscending) key else key.reversed()).then(byName))
    }

    private companion object {
        val nameCollator: java.text.Collator = java.text.Collator.getInstance(com.subnook.app.uiLocale()).apply {
            strength = java.text.Collator.SECONDARY
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

    /**
     * 保存。[keepDbBalance]：用户没改过余额输入框时为 true——编辑页开着的时候后台可能已经扣过款 / 充过值，
     * 这时用数据库里最新的余额，不用表单打开时的旧值覆盖。
     */
    fun save(sub: Subscription, keepDbBalance: Boolean = false, onDone: () -> Unit = {}) = viewModelScope.launch {
        var s = com.subnook.app.domain.Installments.normalize(sub)
        if (keepDbBalance && s.id != 0L) a.dao.get(s.id)?.let { s = s.copy(balance = it.balance) }
        // 新订阅排在手动顺序的最后
        a.dao.upsert(if (s.id == 0L) s.copy(sortIndex = a.dao.maxSortIndex() + 1) else s)
        Scheduler.runAll(a)
        onDone()
    }

    fun delete(sub: Subscription) = viewModelScope.launch {
        a.dao.delete(sub)
        a.notifier.cancel(sub.id)
        toast(str(R.string.toast_deleted, sub.name))
    }

    fun deletePayment(p: PaymentRecord) = viewModelScope.launch {
        a.processor.deletePayment(p)
        if (p.balanceUsed > 0) toast(str(R.string.toast_record_deleted_refund, com.subnook.app.data.Currencies.formatList(p.balanceUsed, p.currency)))
    }

    // ---- 自定义分类 ----
    fun addCustomCategory(name: String) = viewModelScope.launch { a.settings.addCustomCategory(name) }
    fun removeCustomCategory(name: String) = viewModelScope.launch { a.settings.removeCustomCategory(name) }
    fun addCustomPayMethod(name: String) = viewModelScope.launch { a.settings.addCustomPayMethod(name) }
    fun removeCustomPayMethod(name: String) = viewModelScope.launch { a.settings.removeCustomPayMethod(name) }

    // ---- 余额充值 ----
    fun observeTopUps(id: Long) = a.dao.observeTopUps(id)

    fun topUp(sub: Subscription, amount: Double) = viewModelScope.launch {
        a.processor.topUp(sub.id, amount)
        // 卡在余额不足的自动扣款：充值后马上再处理一次，够了就自动补扣
        Scheduler.runAll(a)
        toast(str(R.string.toast_topped_up, com.subnook.app.data.Currencies.formatList(amount, sub.currency)))
    }

    fun deleteTopUp(t: com.subnook.app.data.TopUp) = viewModelScope.launch {
        a.processor.deleteTopUp(t)
        toast(str(R.string.toast_topup_deleted))
    }

    fun confirmPaid(sub: Subscription) = viewModelScope.launch {
        val short = a.processor.confirmPaid(sub.id)
        if (short > 0.005) {
            toast(str(R.string.toast_balance_short, com.subnook.app.data.Currencies.formatList(short, sub.currency)))
        } else if (sub.isInstallment) {
            val after = a.dao.get(sub.id)
            toast(
                if (after != null && com.subnook.app.domain.Installments.isFinished(after)) str(R.string.toast_last_term_paid, sub.name)
                else str(R.string.toast_term_recorded, com.subnook.app.domain.Installments.summary(sub).current),
            )
        } else {
            toast(str(R.string.toast_payment_recorded))
        }
    }

    fun prepay(sub: Subscription, amount: Double, mode: com.subnook.app.domain.Installments.PrepayMode, onDone: () -> Unit = {}) =
        viewModelScope.launch {
            runCatching { a.processor.prepay(sub.id, amount, mode) }
                .onSuccess { r ->
                    toast(
                        when {
                            r == null -> str(R.string.common_installment_already_paid)
                            r.paidOff -> str(R.string.toast_paid_off_early, sub.name)
                            else -> plural(R.plurals.toast_prepay_recorded, r.newTerms, com.subnook.app.data.Currencies.formatList(r.newPayment, sub.currency), r.newTerms)
                        },
                    )
                    Scheduler.runAll(a)
                    onDone()
                }
                .onFailure { toast(str(R.string.toast_prepay_failed, it.message)) }
        }

    /** 删除提前还款记录并恢复分期。[onRestored] 在恢复成功后调用（编辑页据此关闭，避免旧表单覆盖恢复的数据） */
    fun undoPrepay(p: PaymentRecord, onRestored: () -> Unit) = viewModelScope.launch {
        val r = runCatching { a.processor.undoPrepay(p.id) }.getOrElse {
            toast(str(R.string.toast_undo_failed, it.message))
            return@launch
        }
        when (r) {
            com.subnook.app.domain.RenewalProcessor.UndoResult.Restored -> {
                toast(str(R.string.toast_prepay_undone))
                Scheduler.runAll(a)
                onRestored()
            }
            is com.subnook.app.domain.RenewalProcessor.UndoResult.LaterPayments ->
                toast(plural(R.plurals.toast_undo_blocked, r.count, r.count))
            com.subnook.app.domain.RenewalProcessor.UndoResult.NoSnapshot -> {
                // 旧调试版本记的提前还款没有快照：只删记录
                a.processor.undoPrepay(p.id, deleteWithoutSnapshot = true)
                toast(str(R.string.toast_undo_no_snapshot))
            }
            com.subnook.app.domain.RenewalProcessor.UndoResult.NotFound -> toast(str(R.string.toast_record_missing))
        }
    }

    fun skip(sub: Subscription) = viewModelScope.launch {
        a.processor.skip(sub.id)
        toast(str(R.string.toast_skipped))
    }

    fun setPeriod(p: SummaryPeriod) = viewModelScope.launch { a.settings.setPeriod(p) }
    fun setCurrency(c: String) = viewModelScope.launch { a.settings.setCurrency(c) }
    /** 选排序方式和方向；从其他方式切到手动排序时，把当前看到的顺序存为手动顺序的起点 */
    fun setSort(s: SortOrder, asc: Boolean) = viewModelScope.launch {
        val st = state.value
        if (s == SortOrder.MANUAL && st.settings.sort != SortOrder.MANUAL) {
            a.dao.setManualOrder(st.sortedActive().map { it.id } + st.sortedInstallments().map { it.id } + st.sortedInactive().map { it.id })
        }
        a.settings.setSort(s, asc)
    }

    /** 拖动排序结束：[subIds] / [instIds] 是启用中订阅、分期各自的新顺序，并切到手动排序 */
    fun reorder(subIds: List<Long>, instIds: List<Long>) = viewModelScope.launch {
        a.dao.setManualOrder(subIds + instIds + state.value.sortedInactive().map { it.id })
        a.settings.setSort(SortOrder.MANUAL, true)
    }
    fun setTheme(t: com.subnook.app.data.ThemeMode) = viewModelScope.launch { a.settings.setTheme(t) }
    fun setNotifyTime(h: Int, m: Int) = viewModelScope.launch {
        a.settings.setNotifyTime(h, m)
        Scheduler.scheduleDailyAlarm(a)
    }

    fun refreshRates() = viewModelScope.launch {
        a.rates.refresh()
            .onSuccess { toast(str(R.string.toast_rates_updated)) }
            .onFailure { toast(str(R.string.toast_rates_failed, it.message ?: it.javaClass.simpleName)) }
    }

    fun export(uri: Uri) = viewModelScope.launch {
        runCatching { a.backup.export(uri) }
            .onSuccess { toast(plural(R.plurals.toast_exported, it, it)) }
            .onFailure { toast(str(R.string.toast_export_failed, it.message)) }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        runCatching { a.backup.import(uri) }
            .onSuccess {
                Scheduler.runAll(a)
                toast(str(R.string.toast_imported, it.subs, it.payments))
            }
            .onFailure { toast(str(R.string.toast_import_failed, it.message)) }
    }

    // ---- WebDAV 云端备份 ----
    val webDav: StateFlow<com.subnook.app.data.WebDavConfig> =
        a.webDav.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.subnook.app.data.WebDavConfig())

    /** 测试连接成功才保存；[password] 为 null 表示不改密码 */
    suspend fun saveWebDav(url: String, user: String, password: String?): Result<Unit> =
        runCatching { a.cloud.saveAccount(url, user, password) }
            .onSuccess { toast(str(R.string.toast_webdav_saved)) }

    fun clearWebDav() = viewModelScope.launch {
        a.webDav.clear()
        com.subnook.app.data.CloudBackup.schedule(a, false)
        toast(str(R.string.toast_webdav_removed))
    }

    fun setAutoBackup(on: Boolean) = viewModelScope.launch {
        a.webDav.setAutoBackup(on)
        com.subnook.app.data.CloudBackup.schedule(a, on)
    }

    /** 在 ViewModel 里跑，离开设置页也不会中断上传 */
    fun cloudBackup(onDone: () -> Unit = {}) = viewModelScope.launch {
        runCatching { a.cloud.backup() }
            .onSuccess { toast(str(R.string.toast_cloud_backed_up)) }
            .onFailure { toast(str(R.string.toast_cloud_backup_failed, it.message)) }
        onDone()
    }

    suspend fun cloudList(): Result<List<com.subnook.app.data.RemoteFile>> = runCatching { a.cloud.list() }

    fun cloudRestore(name: String) = viewModelScope.launch {
        runCatching { a.cloud.restore(name) }
            .onSuccess {
                Scheduler.runAll(a)
                toast(str(R.string.toast_cloud_restored, it.subs, it.payments))
            }
            .onFailure { toast(str(R.string.toast_restore_failed, it.message)) }
    }

    fun onResume() = viewModelScope.launch {
        Scheduler.runAll(a)
        a.rates.refreshIfStale()
    }
}
