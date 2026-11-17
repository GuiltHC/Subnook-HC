package com.subnook.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subnook.app.data.Currencies
import com.subnook.app.data.SortOrder
import com.subnook.app.data.Subscription
import com.subnook.app.data.SummaryPeriod
import com.subnook.app.domain.Billing
import com.subnook.app.ui.MainViewModel
import com.subnook.app.ui.UiState
import com.subnook.app.ui.components.CurrencyPickerDialog
import com.subnook.app.ui.components.PageHeader
import com.subnook.app.ui.components.Pill
import com.subnook.app.ui.components.RoundIconButton
import com.subnook.app.ui.components.SectionHeader
import com.subnook.app.ui.components.SoftCard
import com.subnook.app.ui.components.SubIcon
import com.subnook.app.ui.theme.LocalExtra
import com.subnook.app.ui.theme.TabularNums
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 订阅列表筛选：全部 / 自动续费 / 手动付款 */
private enum class RenewFilter(val label: String, val short: String, val test: (Subscription) -> Boolean) {
    ALL("全部", "全部", { true }),
    AUTO("自动续费", "自动", { it.autoRenew }),
    MANUAL("手动付款", "手动", { !it.autoRenew }),
}

/** 三段滑块：点一下即切换，选中块平滑滑动 */
@Composable
private fun FilterSlider(selected: RenewFilter, onSelect: (RenewFilter) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtra.current
    val segW = 52.dp
    val index = RenewFilter.entries.indexOf(selected)
    val offset by androidx.compose.animation.core.animateDpAsState(segW * index, label = "slider")
    Box(
        Modifier.clip(CircleShape).background(cs.surfaceContainerHigh).padding(3.dp),
    ) {
        Box(
            Modifier.offset(x = offset).width(segW).height(28.dp).clip(CircleShape).background(extra.accentBrush),
        )
        Row {
            RenewFilter.entries.forEach { f ->
                Box(
                    Modifier.width(segW).height(28.dp).clip(CircleShape)
                        .clickable(interactionSource = null, indication = null) { onSelect(f) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        f.short, style = MaterialTheme.typography.labelMedium,
                        color = if (f == selected) Color.White else cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: MainViewModel, onAdd: () -> Unit, onOpen: (Long) -> Unit, onStats: () -> Unit = {}) {
    val st by vm.state.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }
    var pickCurrency by remember { mutableStateOf(false) }
    var showInactive by rememberSaveable { mutableStateOf(false) }
    var renewFilter by rememberSaveable { mutableStateOf(RenewFilter.ALL) }
    val today = LocalDate.now()
    // 3 天内到期（含已逾期）的都进「待付款」，不分自动/手动——Google 等可能提前扣款
    val pending = st.active.filter { Billing.isPayable(it, today) }.sortedBy { it.nextBillingDay }
    val extra = LocalExtra.current

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageHeader(
                title = "我的订阅",
                subtitle = today.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)),
            ) {
                Box {
                    RoundIconButton(Icons.AutoMirrored.Rounded.Sort, "排序", { sortMenu = true })
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }, shape = RoundedCornerShape(16.dp)) {
                        SortOrder.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.label) },
                                trailingIcon = { if (s == st.settings.sort) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = { vm.setSort(s); sortMenu = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(42.dp).shadow(10.dp, CircleShape, spotColor = Color(0xFF4F46E5)).clip(CircleShape)
                        .background(extra.accentBrush).clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, "添加订阅", tint = Color.White) }
            }
        }

        item { HeroCard(st, onPeriod = vm::setPeriod, onCurrency = { pickCurrency = true }, onClick = onStats) }

        if (pending.isNotEmpty()) {
            item { SectionHeader("待付款", pending.size) }
            items(pending, key = { "p${it.id}" }) { s ->
                PendingCard(s, today, onPaid = { vm.confirmPaid(s) }, onSkip = { vm.skip(s) }, onClick = { onOpen(s.id) })
            }
        }

        if (st.loaded && st.subs.isEmpty()) {
            item { EmptyHint(onAdd) }
        }
        if (st.active.isNotEmpty()) {
            val shown = st.sortedActive().filter(renewFilter.test)
            item {
                SectionHeader("订阅中", shown.size) {
                    FilterSlider(renewFilter) { renewFilter = it }
                }
            }
            if (shown.isEmpty()) item {
                Text(
                    "没有${renewFilter.label}的订阅", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
            items(shown, key = { it.id }) { s -> SubCard(s, st, today) { onOpen(s.id) } }
        }
        if (st.inactive.isNotEmpty()) {
            item {
                SectionHeader("已停用", st.inactive.size, Modifier.clickable { showInactive = !showInactive }) {
                    Icon(
                        if (showInactive) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (showInactive) items(st.inactive, key = { it.id }) { s ->
                Box(Modifier.alpha(0.6f)) { SubCard(s, st, today) { onOpen(s.id) } }
            }
        }
        item {
            Spacer(Modifier.height(96.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (pickCurrency) {
        CurrencyPickerDialog(st.settings.displayCurrency, onDismiss = { pickCurrency = false }) {
            vm.setCurrency(it); pickCurrency = false
        }
    }
}

@Composable
private fun HeroCard(st: UiState, onPeriod: (SummaryPeriod) -> Unit, onCurrency: () -> Unit, onClick: () -> Unit) {
    val extra = LocalExtra.current
    val cur = st.settings.displayCurrency
    val period = st.settings.period
    val total = st.total(period)
    val today = LocalDate.now()
    val next30 = st.active.sumOf { s ->
        Billing.upcoming(s, today, today.plusDays(29)).size * st.rates.convert(s.amount, s.currency, cur)
    }
    val yearTotal = st.total(SummaryPeriod.YEAR)
    val shape = RoundedCornerShape(28.dp)
    val white = Color.White

    Box(
        Modifier.fillMaxWidth()
            .shadow(22.dp, shape, ambientColor = Color(0xFF4F46E5), spotColor = Color(0xFF4F46E5).copy(alpha = if (extra.dark) 0f else 0.45f))
            .clip(shape)
            .background(extra.heroBrush)
            .clickable(onClick = onClick),
    ) {
        // 装饰光斑
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                Brush.radialGradient(listOf(white.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width * 0.95f, 0f), radius = size.width * 0.6f),
                radius = size.width * 0.6f, center = Offset(size.width * 0.95f, 0f),
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFF472B6).copy(alpha = 0.28f), Color.Transparent), center = Offset(0f, size.height), radius = size.width * 0.55f),
                radius = size.width * 0.55f, center = Offset(0f, size.height),
            )
        }
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${period.unitLabel}平均支出", style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.78f))
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(CircleShape).background(white.copy(alpha = 0.16f)).clickable(onClick = onCurrency)
                        .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$cur · ${Currencies.name(cur)}", style = MaterialTheme.typography.labelLarge, color = white)
                    Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp), tint = white)
                }
            }
            Spacer(Modifier.height(10.dp))
            // 右上角已标币种，卡片内金额不再重复显示 CNY 等代码
            Text(
                Currencies.format(total, cur),
                style = MaterialTheme.typography.displayMedium.merge(TabularNums).copy(fontSize = 44.sp),
                color = white, maxLines = 1,
            )
            Spacer(Modifier.height(16.dp))
            SpendBar(st)
            Spacer(Modifier.height(16.dp))
            Row {
                Stat("未来 30 天", Currencies.format(next30, cur), Modifier.weight(1f))
                Stat("全年预计", Currencies.format(yearTotal, cur), Modifier.weight(1f))
                Stat("订阅数", "${st.active.size}", Modifier.weight(0.6f))
            }
            Spacer(Modifier.height(18.dp))
            PeriodSwitch(period, onPeriod)
            Spacer(Modifier.height(10.dp))
            val ratesText = if (st.rates.updatedAt == 0L) "使用内置参考汇率 · 联网后自动更新"
            else "汇率更新于 " + java.time.Instant.ofEpochMilli(st.rates.updatedAt).atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ratesText, style = MaterialTheme.typography.labelSmall, color = white.copy(alpha = 0.55f), modifier = Modifier.weight(1f))
                Text("详细统计 ›", style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.85f))
            }
        }
    }
}

/** 按订阅品牌色分段的支出占比条 */
@Composable
private fun SpendBar(st: UiState) {
    val cur = st.settings.displayCurrency
    val parts = st.active.map { it to st.rates.convert(Billing.periodCost(it, SummaryPeriod.MONTH), it.currency, cur) }
        .filter { it.second > 0 }.sortedByDescending { it.second }
    val sum = parts.sumOf { it.second }
    val anim by animateFloatAsState(if (sum > 0) 1f else 0f, label = "bar")
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))) {
        if (sum > 0) Canvas(Modifier.matchParentSize()) {
            var x = 0f
            val gap = 2.dp.toPx()
            parts.forEach { (s, v) ->
                val w = (v / sum).toFloat() * size.width * anim
                drawRect(Color(s.color).copy(alpha = 0.95f), topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size((w - gap).coerceAtLeast(1f), size.height))
                x += w
            }
        }
    }
    if (parts.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            parts.take(3).forEach { (s, v) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(s.color)))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "${s.name} ${(v / sum * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall.merge(TabularNums), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PeriodSwitch(period: SummaryPeriod, onPeriod: (SummaryPeriod) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)).padding(4.dp),
    ) {
        SummaryPeriod.entries.forEach { p ->
            val sel = p == period
            Box(
                Modifier.weight(1f).clip(CircleShape)
                    .background(if (sel) Color.White else Color.Transparent)
                    .clickable { onPeriod(p) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "按${p.label}", style = MaterialTheme.typography.labelLarge,
                    color = if (sel) Color(0xFF3730A3) else Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun dueColor(days: Long): Color {
    val extra = LocalExtra.current
    return when {
        days < 0 -> extra.danger
        days <= 3 -> extra.warning
        days <= 7 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun PendingCard(s: Subscription, today: LocalDate, onPaid: () -> Unit, onSkip: () -> Unit, onClick: () -> Unit) {
    val extra = LocalExtra.current
    val days = Billing.daysUntil(s, today)
    SoftCard(Modifier.fillMaxWidth().animateContentSize(), onClick = onClick) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SubIcon(s.iconType, s.iconValue, s.color, s.name, size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(Billing.dueText(days), if (days < 0) extra.danger else extra.warning)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (s.autoRenew) "自动" else "手动", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    )
                }
            }
            AmountText(s.amount, s.currency)
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(onClick = onSkip).padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) { Text("跳过本期", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Box(
                Modifier.weight(1.4f).clip(RoundedCornerShape(14.dp)).background(extra.accentBrush)
                    .clickable(onClick = onPaid).padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("我已付款", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AmountText(amount: Double, currency: String) {
    // 首页订阅列表：CN¥ / US$ / JP¥ / HK$ 直接区分币种，不再单独显示 CNY 等代码
    Text(Currencies.formatList(amount, currency), style = MaterialTheme.typography.titleMedium.merge(TabularNums), fontWeight = FontWeight.Bold)
}

@Composable
private fun SubCard(s: Subscription, st: UiState, today: LocalDate, onClick: () -> Unit) {
    val cur = st.settings.displayCurrency
    val days = Billing.daysUntil(s, today)
    val date = LocalDate.ofEpochDay(s.nextBillingDay)
    val dateFmt = if (date.year == today.year) "M月d日" else "yy年M月d日"
    SoftCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            SubIcon(s.iconType, s.iconValue, s.color, s.name, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (s.autoRenew) Icons.Rounded.Autorenew else Icons.Rounded.TouchApp, null,
                        Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        date.format(DateTimeFormatter.ofPattern(dateFmt)) + " · " + when {
                            s.payMethod == com.subnook.app.data.PayMethod.MANUAL -> "手动付款"
                            s.autoRenew -> s.payMethod.label
                            else -> "${s.payMethod.label} · 手动"
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(7.dp))
                if (s.active) {
                    Pill(Billing.dueText(days), dueColor(days))
                } else {
                    Pill("已停用", MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                AmountText(s.amount, s.currency)
                Spacer(Modifier.height(2.dp))
                Text(Billing.cycleText(s), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.currency != cur) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "≈ " + Currencies.formatList(st.rates.convert(s.amount, s.currency, cur), cur),
                        style = MaterialTheme.typography.labelSmall.merge(TabularNums), color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(onAdd: () -> Unit) {
    val extra = LocalExtra.current
    SoftCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(extra.accentBrush), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Autorenew, null, Modifier.size(32.dp), tint = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Text("还没有订阅", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "从 220+ 常用软件预设里一键添加，\n自动续费会自动滚动账单日",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.clip(CircleShape).background(extra.accentBrush).clickable(onClick = onAdd).padding(horizontal = 22.dp, vertical = 11.dp),
            ) { Text("添加第一个订阅", style = MaterialTheme.typography.labelLarge, color = Color.White) }
        }
    }
}
