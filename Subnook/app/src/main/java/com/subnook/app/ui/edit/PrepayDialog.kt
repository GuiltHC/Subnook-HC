package com.subnook.app.ui.edit

import com.subnook.app.dateFmt
import com.subnook.app.R
import com.subnook.app.str
import com.subnook.app.plural
import com.subnook.app.ui.components.AppAlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subnook.app.data.Currencies
import com.subnook.app.data.Subscription
import com.subnook.app.domain.Installments
import com.subnook.app.domain.Installments.PrepayMode
import com.subnook.app.ui.theme.LocalExtra
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 提前还款：一次还清，或部分还款后「减少每期还款 / 缩短还款期数」。
 * 实时算出提前还款后的每期金额、期数和省下的钱。
 */
@Composable
internal fun PrepayDialog(sub: Subscription, onDismiss: () -> Unit, onConfirm: (amount: Double, mode: PrepayMode) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtra.current
    val today = remember { LocalDate.now() }
    val remaining = remember(sub) { Installments.remainingPrincipal(sub) }
    val sum = remember(sub) { Installments.summary(sub) }
    val modes = if (Installments.canPartialPrepay(sub.instMethod)) PrepayMode.entries else listOf(PrepayMode.PAY_OFF)
    fun plain(v: Double) = v.toBigDecimal().stripTrailingZeros().toPlainString()
    var mode by remember { mutableStateOf(PrepayMode.PAY_OFF) }
    var amountText by remember { mutableStateOf(plain(remaining)) }
    fun money(v: Double) = Currencies.formatList(v, sub.currency)

    val amount = amountText.replace(",", "").toDoubleOrNull()
    val error = when {
        amount == null || amount <= 0 -> str(R.string.common_enter_prepay_amount)
        mode != PrepayMode.PAY_OFF && amount >= remaining -> str(R.string.prepay_err_partial, money(remaining))
        else -> null
    }
    val result = if (error == null) runCatching { Installments.prepay(sub, amount!!, mode, today) }.getOrNull() else null

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(R.string.common_prepay)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    plural(R.plurals.prepay_summary, sum.remainingTerms, money(remaining), sum.remainingTerms, money(sum.remainingAmount)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { m ->
                        Chip(m.label, mode == m) {
                            if (m == PrepayMode.PAY_OFF && mode != PrepayMode.PAY_OFF) amountText = plain(remaining)
                            if (m != PrepayMode.PAY_OFF && mode == PrepayMode.PAY_OFF) amountText = ""
                            mode = m
                        }
                    }
                }
                OutlinedTextField(
                    amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (mode == PrepayMode.PAY_OFF) str(R.string.prepay_actual_amount) else str(R.string.prepay_principal)) },
                    singleLine = true, isError = amountText.isNotEmpty() && error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null && amountText.isNotEmpty()) Text(error, style = MaterialTheme.typography.bodySmall, color = cs.error)
                result?.let { r ->
                    val saved = if (r.saved >= 0) str(R.string.prepay_saves, money(r.saved)) else str(R.string.prepay_costs_more, money(-r.saved))
                    Text(
                        if (r.paidOff) str(R.string.prepay_result_paid_off, saved)
                        else plural(R.plurals.prepay_result_partial, r.newTerms, money(r.newPayment), r.newTerms, r.newEnd?.format(dateFmt(R.string.fmt_ymd)) ?: "", saved),
                        style = MaterialTheme.typography.bodyMedium, color = if (r.saved >= 0) extra.success else extra.warning,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = result != null, onClick = { onConfirm(amount!!, mode) }) { Text(str(R.string.prepay_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str(R.string.common_cancel)) } },
    )
}
