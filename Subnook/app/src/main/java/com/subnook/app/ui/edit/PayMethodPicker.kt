package com.subnook.app.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.subnook.app.data.PayMethod
import com.subnook.app.data.cleanCategory
import com.subnook.app.ui.components.AppAlertDialog
import com.subnook.app.ui.theme.LocalExtra

/** 编辑页可选的内置付款方式（订阅 / 分期各一套）；旧数据里的「手动付款」只在正选着它时显示 */
internal fun payMethodChoices(form: Form): List<PayMethod> {
    val base = if (form.isInstallment) PayMethod.forInstallment else PayMethod.forSubscription
    val builtIn = base.filter { it != PayMethod.OTHER }
    return if (form.payMethod in base || form.payMethod == PayMethod.OTHER) builtIn else builtIn + form.payMethod
}

internal fun payMethodLabels(): List<String> = PayMethod.entries.map { it.label }

/**
 * 编辑页「付款方式」：内置方式 + 用户自己加的方式 + 其他。
 * 「手动」不是一种付款方式，由下面的「自动续费」开关决定，所以切换方式时不改开关。
 * 自定义方式存成 OTHER + payMethodNote；「其他」填了名称后问一次要不要加入（不直接加），长按可删除。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PayMethodPicker(
    form: Form,
    custom: List<String>,
    onAddCustom: (String) -> Unit,
    onRemoveCustom: (String) -> Unit,
    askedFor: String?,
    onAsked: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtra.current
    val focus = LocalFocusManager.current
    var removeDlg by remember { mutableStateOf<String?>(null) }
    var askDlg by remember { mutableStateOf<String?>(null) }
    val note = cleanCategory(form.payMethodNote)
    val customSelected = form.payMethod == PayMethod.OTHER && note in custom

    @Composable
    fun Pill(text: String, selected: Boolean, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
        Box(
            Modifier.clip(RoundedCornerShape(12.dp))
                .then(if (selected) Modifier.background(extra.accentBrush) else Modifier.background(cs.surfaceContainer))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else cs.onSurface)
        }
    }

    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            payMethodChoices(form).forEach { m ->
                Pill(m.label, form.payMethod == m) { form.payMethod = m; form.payMethodNote = "" }
            }
            custom.forEach { name ->
                Pill(name, form.payMethod == PayMethod.OTHER && note == name, onLongClick = { removeDlg = name }) {
                    form.payMethod = PayMethod.OTHER; form.payMethodNote = name
                }
            }
            Pill(PayMethod.OTHER.label, form.payMethod == PayMethod.OTHER && !customSelected) {
                if (form.payMethod != PayMethod.OTHER || customSelected) form.payMethodNote = ""
                form.payMethod = PayMethod.OTHER
            }
        }
        if (form.payMethod == PayMethod.OTHER && !customSelected) {
            Spacer(Modifier.height(10.dp))
            var hadFocus by remember { mutableStateOf(false) }
            TextField(
                form.payMethodNote, { form.payMethodNote = it.replace("\n", "").take(20) },
                Modifier.fillMaxWidth().onFocusChanged { f ->
                    // 输入完（离开输入框）时问一次
                    if (hadFocus && !f.isFocused) {
                        val n = cleanCategory(form.payMethodNote)
                        if (needsAskName(n, custom, askedFor, payMethodLabels())) askDlg = n
                    }
                    hadFocus = f.isFocused
                },
                label = { Text("付款方式名称（可不填）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = cs.surfaceContainer, unfocusedContainerColor = cs.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
    }

    askDlg?.let { name ->
        AddCategoryDialog(name, what = "付款方式") { add -> if (add) onAddCustom(name); onAsked(name); askDlg = null }
    }
    removeDlg?.let { name ->
        AppAlertDialog(
            onDismissRequest = { removeDlg = null },
            text = { DialogMessage("删除付款方式「$name」？") },
            confirmButton = {
                TextButton(onClick = { onRemoveCustom(name); removeDlg = null }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removeDlg = null }) { Text("取消") } },
        )
    }
}
