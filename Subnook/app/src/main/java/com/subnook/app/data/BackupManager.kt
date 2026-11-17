package com.subnook.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.subnook.app.App
import com.subnook.app.ui.components.IconFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val app: String = "Subnook",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: AppSettings,
    val subscriptions: List<Subscription>,
    val payments: List<PaymentRecord>,
    /** 自定义图标：文件名 -> base64(PNG) */
    val icons: Map<String, String> = emptyMap(),
)

data class ImportResult(val subs: Int, val payments: Int)

/** 能识别的备份来源：当前名称 + 改名前的旧名称 */
private val BACKUP_APP_IDS = setOf("Subnook", "Sub" + "Keeper")

class BackupManager(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val app get() = App.get(context)

    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val subs = app.dao.getAll()
        val icons = subs.filter { it.iconType == IconType.CUSTOM }.mapNotNull { s ->
            val f = IconFiles.file(context, s.iconValue)
            if (f.exists()) s.iconValue to Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else null
        }.toMap()
        val data = BackupFile(
            settings = app.settings.get(),
            subscriptions = subs,
            payments = app.dao.getAllPayments(),
            icons = icons,
        )
        context.contentResolver.openOutputStream(uri, "wt")!!.use {
            it.write(json.encodeToString(BackupFile.serializer(), data).toByteArray())
        }
        subs.size
    }

    /** 导入会覆盖当前全部数据 */
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        val data = json.decodeFromString(BackupFile.serializer(), text)
        // 兼容改名前（旧名 Subnook）导出的备份
        require(data.app in BACKUP_APP_IDS) { "不是本应用的备份文件" }
        data.icons.forEach { (name, b64) ->
            IconFiles.file(context, name).apply { parentFile?.mkdirs() }.writeBytes(Base64.decode(b64, Base64.DEFAULT))
        }
        app.dao.replaceAll(data.subscriptions, data.payments)
        app.settings.replace(data.settings)
        ImportResult(data.subscriptions.size, data.payments.size)
    }
}
