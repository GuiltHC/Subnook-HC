package com.subnook.app.notify

import com.subnook.app.str
import com.subnook.app.plural
import com.subnook.app.dateFmt
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.subnook.app.MainActivity
import com.subnook.app.R
import com.subnook.app.data.Currencies
import com.subnook.app.data.IconType
import com.subnook.app.data.Subscription
import com.subnook.app.domain.Billing
import com.subnook.app.ui.components.IconFiles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class Notifier(private val context: Context) {

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_BILL, str(R.string.notif_channel_bills), NotificationManager.IMPORTANCE_HIGH).apply {
                description = str(R.string.notif_channel_bills_desc)
            }
        )
    }

    private fun canPost(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33

    fun upcoming(sub: Subscription, today: LocalDate) {
        val due = LocalDate.ofEpochDay(sub.nextBillingDay)
        val days = ChronoUnit.DAYS.between(today, due)
        val whenText = when (days) {
            0L -> str(R.string.due_today)
            1L -> str(R.string.due_tomorrow)
            else -> plural(R.plurals.notif_in_days_date, days.toInt(), days, due.format(dateFmt(R.string.fmt_md)))
        }
        val amount = Billing.amountOn(sub, due)
        // 余额付款且余额不够：提醒先充值
        val lowBalance = com.subnook.app.domain.Balance.usesBalance(sub) && !com.subnook.app.domain.Balance.canPay(sub, amount)
        val action = when {
            lowBalance -> str(R.string.notif_low_balance)
            sub.autoRenew -> str(R.string.notif_auto_debit, sub.payMethodLabel)
            else -> str(R.string.notif_manual_pay, sub.payMethodLabel)
        }
        val inst = if (sub.isInstallment) com.subnook.app.domain.Installments.progressText(sub) + " · " else ""
        val text = if (lowBalance) {
            str(R.string.notif_low_balance_text, inst, Currencies.formatList(amount, sub.currency), Currencies.formatList(sub.balance, sub.currency))
        } else {
            str(R.string.notif_amount_text, inst, Currencies.formatList(amount, sub.currency))
        }
        post(sub, str(R.string.notif_upcoming_title, sub.name, whenText, action), text, withConfirm = days <= Billing.PAYABLE_DAYS)
    }

    fun overdue(sub: Subscription, today: LocalDate) {
        val due = LocalDate.ofEpochDay(sub.nextBillingDay)
        val days = ChronoUnit.DAYS.between(due, today)
        val amount = Billing.amountOn(sub, due)
        val dueText = due.format(dateFmt(R.string.fmt_md))
        if (sub.autoRenew && com.subnook.app.domain.Balance.usesBalance(sub)) {
            post(
                sub, plural(R.plurals.notif_overdue_low_balance, days.toInt(), sub.name, days),
                str(R.string.notif_overdue_low_balance_text, dueText, Currencies.formatList(amount, sub.currency), Currencies.formatList(sub.balance, sub.currency)),
                withConfirm = true,
            )
            return
        }
        post(sub, plural(R.plurals.notif_overdue, days.toInt(), sub.name, days), str(R.string.notif_overdue_text, dueText, Currencies.formatList(amount, sub.currency)), withConfirm = true)
    }

    private fun post(sub: Subscription, title: String, text: String, withConfirm: Boolean) {
        if (!canPost()) return
        val open = PendingIntent.getActivity(
            context, sub.id.toInt(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_SUB_ID, sub.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(context, CH_BILL)
            .setSmallIcon(R.drawable.ic_stat_bill)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(sub.color)
            .setLargeIcon(largeIcon(sub))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (withConfirm) {
            val confirm = PendingIntent.getBroadcast(
                context, sub.id.toInt(),
                Intent(context, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_CONFIRM).putExtra(ActionReceiver.EXTRA_ID, sub.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            b.addAction(0, str(R.string.common_paid), confirm)
        }
        try {
            NotificationManagerCompat.from(context).notify(notifId(sub.id), b.build())
        } catch (_: SecurityException) {
        }
    }

    private fun largeIcon(sub: Subscription): Bitmap? = runCatching {
        when (sub.iconType) {
            IconType.PRESET -> {
                val id = IconFiles.presetRes(context, sub.iconValue)
                if (id != 0) BitmapFactory.decodeResource(context.resources, id) else null
            }
            IconType.CUSTOM -> BitmapFactory.decodeFile(IconFiles.file(context, sub.iconValue).path)
            else -> null
        }
    }.getOrNull()

    fun cancel(subId: Long) = NotificationManagerCompat.from(context).cancel(notifId(subId))

    private fun notifId(subId: Long) = 10_000 + subId.toInt()

    companion object {
        const val CH_BILL = "bill"
    }
}
