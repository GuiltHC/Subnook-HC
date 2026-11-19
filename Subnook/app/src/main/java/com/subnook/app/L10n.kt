package com.subnook.app

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Локализация (форк с русским интерфейсом).
 *
 * Все строки интерфейса лежат в ресурсах: res/values (русский, язык по умолчанию)
 * и res/values-zh (китайский оригинал). Язык выбирается системой.
 *
 * Строки нужны и вне Compose (enum-подписи, уведомления, виджет, тосты из ViewModel),
 * поэтому здесь простые функции поверх контекста приложения. Контекст задаётся в [App.onCreate].
 */
object L10n {
    @Volatile
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext ?: context
    }

    /**
     * Только для JVM-тестов (без Android): строка по id ресурса вместо контекста.
     * В приложении всегда null.
     */
    @Volatile
    var testStrings: ((id: Int) -> String)? = null

    internal val ctx: Context
        get() = context ?: error("L10n не инициализирован: вызовите L10n.init() в Application.onCreate()")
}

/** Строка из ресурсов; с аргументами — как getString(id, args). */
fun str(@StringRes id: Int, vararg args: Any?): String {
    L10n.testStrings?.let { t -> return if (args.isEmpty()) t(id) else String.format(t(id), *args) }
    return if (args.isEmpty()) L10n.ctx.getString(id) else L10n.ctx.getString(id, *args)
}

/** Строка с учётом числа (plurals): для русского — 1 платёж / 2 платежа / 5 платежей. */
fun plural(@PluralsRes id: Int, count: Int, vararg args: Any?): String {
    L10n.testStrings?.let { t -> return if (args.isEmpty()) t(id) else String.format(t(id), *args) }
    return if (args.isEmpty()) L10n.ctx.resources.getQuantityString(id, count)
    else L10n.ctx.resources.getQuantityString(id, count, *args)
}

/** Язык, на котором сейчас показан интерфейс (по ресурсам, а не по системе): нужен для дат и сортировки. */
fun uiLocale(): Locale = Locale.forLanguageTag(str(R.string.locale_tag))

/** Форматтер даты по шаблону из ресурсов (fmt_*), с языком интерфейса. */
fun dateFmt(@StringRes patternId: Int): DateTimeFormatter =
    DateTimeFormatter.ofPattern(str(patternId), uiLocale())
