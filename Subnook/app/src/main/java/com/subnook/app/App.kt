package com.subnook.app

import android.app.Application
import android.content.Context
import com.subnook.app.data.AppDatabase
import com.subnook.app.data.BackupManager
import com.subnook.app.data.RatesRepository
import com.subnook.app.data.SettingsRepository
import com.subnook.app.domain.RenewalProcessor
import com.subnook.app.notify.Notifier
import com.subnook.app.notify.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce

class App : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db by lazy { AppDatabase.build(this) }
    val dao get() = db.dao()
    val settings by lazy { SettingsRepository(this) }
    val rates by lazy { RatesRepository(this) }
    val notifier by lazy { Notifier(this) }
    val processor by lazy { RenewalProcessor(dao, settings, notifier) }
    val backup by lazy { BackupManager(this) }

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        Scheduler.schedulePeriodicWork(this)
        appScope.launch {
            Scheduler.runAll(this@App)
            rates.refreshIfStale()
        }
        // 订阅 / 设置 / 汇率任何变化都同步到桌面小组件
        appScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            kotlinx.coroutines.flow.combine(dao.observeAll(), settings.flow, rates.table) { a, b, c -> Triple(a, b, c) }
                .debounce(400L)
                .collect { com.subnook.app.widget.Widgets.refresh(this@App) }
        }
    }

    companion object {
        fun get(context: Context): App = context.applicationContext as App
    }
}
