package com.subnook.app.ui.home

import com.subnook.app.R
import com.subnook.app.str
import com.subnook.app.plural
import com.subnook.app.dateFmt
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.subnook.app.domain.Installments
import com.subnook.app.ui.MainViewModel
import com.subnook.app.ui.UiState
import com.subnook.app.ui.components.CurrencyPickerDialog
import com.subnook.app.ui.components.PageHeader
import com.subnook.app.ui.components.Pill
import com.subnook.app.ui.components.SectionHeader
import com.subnook.app.ui.components.SoftCard
import com.subnook.app.ui.components.SubIcon
import com.subnook.app.ui.theme.LocalExtra
import com.subnook.app.ui.theme.TabularNums
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 订阅列表筛选：全部 / 自动续费 / 手动付款 */
private enum class RenewFilter(
    val labelRes: Int,
    val shortRes: Int,
    val emptySubsRes: Int,
    val emptyInstRes: Int,
    val test: (Subscription) -> Boolean,
) {
    ALL(R.string.filter_all, R.string.filter_all, R.string.empty_subs_all, R.string.empty_inst_all, { true }),
    AUTO(R.string.common_auto_renew, R.string.filter_auto_short, R.string.empty_subs_auto, R.string.empty_inst_auto, { it.autoRenew }),
    MANUAL(R.string.common_manual_payment, R.string.filter_manual_short, R.string.empty_subs_manual, R.string.empty_inst_manual, { !it.autoRenew }),
    ;

    val label: String get() = str(labelRes)
    val short: String get() = str(shortRes)
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

private fun SortOrder.icon(): ImageVector = when (this) {
    SortOrder.NEXT_DATE -> Icons.Rounded.Event
    SortOrder.AMOUNT -> Icons.Rounded.Payments
    SortOrder.NAME -> Icons.Rounded.SortByAlpha
    SortOrder.CATEGORY -> Icons.Rounded.Category
    SortOrder.CREATED -> Icons.Rounded.Schedule
    SortOrder.MANUAL -> Icons.Rounded.DragIndicator
}

/**
 * 订阅列表排序：胶囊按钮显示「依据 + 方向箭头」，和旁边的筛选滑块同高同字号。
 * 菜单分两段：排序依据（选中后菜单不关，方便接着选方向）、顺序（正序 / 倒序，附带含义说明）。
 */
@Composable
private fun SortButton(
    current: SortOrder,
    ascending: Boolean,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    onPick: (SortOrder, Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box {
        Row(
            Modifier.height(34.dp).clip(CircleShape).background(cs.surfaceContainerHigh).clickable { onExpand(true) }
                .padding(start = 12.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current.short, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            Icon(
                when {
                    current == SortOrder.MANUAL -> Icons.Rounded.SwapVert
                    ascending -> Icons.Rounded.ArrowUpward
                    else -> Icons.Rounded.ArrowDownward
                },
                contentDescription = str(R.string.sort_cd, current.label),
                modifier = Modifier.size(14.dp), tint = cs.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded, onDismissRequest = { onExpand(false) },
            shape = RoundedCornerShape(18.dp), containerColor = cs.surfaceContainerLowest,
        ) {
            MenuLabel(str(R.string.sort_by))
            SortOrder.entries.forEach { s ->
                SortMenuItem(
                    icon = s.icon(), text = s.label, selected = s == current,
                    hint = if (s == SortOrder.MANUAL) str(R.string.sort_drag_hint) else null,
                ) {
                    // 换依据时用它的默认方向；手动排序没有方向
                    if (s != current) onPick(s, s.defaultAsc)
                    if (s == SortOrder.MANUAL) onExpand(false)
                }
            }
            if (current != SortOrder.MANUAL) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cs.outlineVariant)
                MenuLabel(str(R.string.sort_direction))
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(true to str(R.string.sort_asc), false to str(R.string.sort_desc)).forEach { (asc, label) ->
                        val sel = asc == ascending
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) cs.primary.copy(alpha = 0.12f) else cs.surfaceContainer)
                                .clickable { onPick(current, asc); onExpand(false) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (asc) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null,
                                    Modifier.size(15.dp), tint = if (sel) cs.primary else cs.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    label, style = MaterialTheme.typography.bodyMedium,
                                    color = if (sel) cs.primary else cs.onSurface,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                            Text(
                                if (asc) current.ascHint else current.descHint,
                                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

/** 菜单项：统一 bodyMedium 字号；选中项用主色加粗 + 勾，右侧灰色小字说明 */
@Composable
private fun SortMenuItem(icon: ImageVector, text: String, selected: Boolean, hint: String?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val color = if (selected) cs.primary else cs.onSurface
    DropdownMenuItem(
        modifier = Modifier.height(44.dp),
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp), tint = if (selected) cs.primary else cs.onSurfaceVariant) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text, style = MaterialTheme.typography.bodyMedium, color = color,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (!hint.isNullOrEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(hint, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                }
            }
        },
        trailingIcon = { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = if (selected) cs.primary else Color.Transparent) },
        onClick = onClick,
    )
}


@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAddSubscription: () -> Unit,
    onAddInstallment: () -> Unit,
    onOpen: (Long) -> Unit,
    onStats: () -> Unit = {},
) {
    val st by vm.state.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }
    var pickCurrency by remember { mutableStateOf(false) }
    var addSheet by remember { mutableStateOf(false) }
    var showInactive by rememberSaveable { mutableStateOf(false) }
    var showSubs by rememberSaveable { mutableStateOf(true) }
    var showInst by rememberSaveable { mutableStateOf(true) }
    var renewFilter by rememberSaveable { mutableStateOf(RenewFilter.ALL) }
    val today = LocalDate.now()
    val cur = st.settings.displayCurrency
    // 3 天内到期（含已逾期）的都进「待付款」，不分自动/手动——Google 等可能提前扣款
    val pending = st.active.filter { Billing.isPayable(it, today) }.sortedBy { it.nextBillingDay }
    val extra = LocalExtra.current

    // ---- 本月小计：本月已付 + 本月逾期未付 + 本月剩下还要付的 ----
    val month = java.time.YearMonth.from(today)
    val monthFirst = month.atDay(1)
    val monthLast = month.atEndOfMonth()
    val monthPayments by remember(month) {
        vm.observePaymentsBetween(monthFirst.toEpochDay(), monthLast.toEpochDay())
    }.collectAsState(emptyList())
    fun monthCost(list: List<Subscription>): Double = list.sumOf { s ->
        val paid = monthPayments.filter { it.subscriptionId == s.id }.sumOf { st.rates.convert(it.amount, it.currency, cur) }
        val next = LocalDate.ofEpochDay(s.nextBillingDay)
        val overdue = if (next < today && next >= monthFirst) Billing.amountOn(s, next) else 0.0
        val upcoming = if (today <= monthLast) Billing.upcomingCost(s, maxOf(today, monthFirst), monthLast) else 0.0
        paid + st.rates.convert(overdue + upcoming, s.currency, cur)
    }

    // ---- 长按拖动排序：订阅和分期各自在自己的区域里拖 ----
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val reorder = remember(listState) {
        with(density) { ReorderState(listState, scope, edgeTopPx = 72.dp.toPx(), edgeBottomPx = 160.dp.toPx()) }
    }
    val computedSubs = st.sortedActive().filter(renewFilter.test)
    val computedInst = st.sortedInstallments().filter(renewFilter.test)
    // 拖动中（以及松手后、数据库写完前）显示的顺序，避免松手瞬间闪回旧顺序
    var dragSubs by remember { mutableStateOf<List<Long>?>(null) }
    var dragInst by remember { mutableStateOf<List<Long>?>(null) }
    var moved by remember { mutableStateOf(false) }
    val subIds = computedSubs.map { it.id }
    val instIds = computedInst.map { it.id }
    LaunchedEffect(subIds, renewFilter) {
        val d = dragSubs ?: return@LaunchedEffect
        if (reorder.draggingKey == null && (d == subIds || d.toSet() != subIds.toSet())) dragSubs = null
    }
    LaunchedEffect(instIds, renewFilter) {
        val d = dragInst ?: return@LaunchedEffect
        if (reorder.draggingKey == null && (d == instIds || d.toSet() != instIds.toSet())) dragInst = null
    }
    val activeById = st.active.associateBy { it.id }
    val shownSubs = dragSubs?.mapNotNull { activeById[it] } ?: computedSubs
    val shownInst = dragInst?.mapNotNull { activeById[it] } ?: computedInst
    // 订阅卡片的 key 是 id（Long），分期卡片是 "i<id>"（String），拖动时只能和同一区域的卡片交换
    fun instKey(id: Long) = "i$id"
    fun inSubs(key: Any) = showSubs && key is Long && shownSubs.any { it.id == key }
    fun inInst(key: Any) = showInst && key is String && shownInst.any { instKey(it.id) == key }
    reorder.canDrag = { key ->
        when (reorder.draggingKey) {
            null -> inSubs(key) || inInst(key)
            is Long -> inSubs(key)
            else -> inInst(key)
        }
    }
    reorder.onStart = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (reorder.draggingKey is Long) dragSubs = shownSubs.map { it.id } else dragInst = shownInst.map { it.id }
        moved = false
    }
    reorder.onMove = { from, to ->
        if (from is Long && to is Long) {
            dragSubs = dragSubs?.toMutableList()?.apply {
                val i = indexOf(from); val j = indexOf(to)
                if (i >= 0 && j >= 0) add(j, removeAt(i))
            }
        } else if (from is String && to is String) {
            dragInst = dragInst?.toMutableList()?.apply {
                val i = indexOfFirst { instKey(it) == from }; val j = indexOfFirst { instKey(it) == to }
                if (i >= 0 && j >= 0) add(j, removeAt(i))
            }
        }
        moved = true
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    reorder.onEnd = {
        // 筛选「自动 / 手动」时只拖了一部分：把新顺序填回它们在完整列表里原来占的位置
        fun merge(full: List<Long>, part: List<Long>): List<Long> {
            val base = full.toMutableList()
            val set = part.toSet()
            base.indices.filter { base[it] in set }.forEachIndexed { k, slot -> base[slot] = part[k] }
            return base
        }
        val ds = dragSubs
        val di = dragInst
        if (moved && (ds != null || di != null)) {
            val subsOrder = st.sortedActive().map { it.id }.let { if (ds != null) merge(it, ds) else it }
            val instOrder = st.sortedInstallments().map { it.id }.let { if (di != null) merge(it, di) else it }
            if (st.settings.sort != SortOrder.MANUAL) vm.toast(str(R.string.sort_switched_manual))
            vm.reorder(subsOrder, instOrder)
        } else {
            dragSubs = null
            dragInst = null
        }
    }

    @Composable
    fun DragBox(key: Any, content: @Composable () -> Unit) {
        val dragging = reorder.isDragging(key)
        Box(
            Modifier
                .zIndex(if (dragging) 1f else 0f)
                .graphicsLayer {
                    if (dragging) {
                        translationY = reorder.draggingOffset
                        scaleX = 1.03f; scaleY = 1.03f
                        shadowElevation = 12.dp.toPx()
                        shape = RoundedCornerShape(22.dp)
                    }
                },
        ) { content() }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).reorderable(reorder),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageHeader(
                title = "Subnook",
                subtitle = today.format(dateFmt(R.string.fmt_md_dow)),
            ) {
                Box(
                    Modifier.size(42.dp).shadow(10.dp, CircleShape, spotColor = Color(0xFF4F46E5)).clip(CircleShape)
                        .background(extra.accentBrush).clickable { addSheet = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, str(R.string.home_add), tint = Color.White) }
            }
        }

        item { HeroCard(st, onPeriod = vm::setPeriod, onCurrency = { pickCurrency = true }, onClick = onStats) }

        if (pending.isNotEmpty()) {
            item { SectionHeader(str(R.string.home_pending), pending.size) }
            items(pending, key = { "p${it.id}" }) { s ->
                PendingCard(s, today, onPaid = { vm.confirmPaid(s) }, onSkip = { vm.skip(s) }, onClick = { onOpen(s.id) })
            }
        }

        if (st.loaded && st.subs.isEmpty()) {
            item { EmptyHint { addSheet = true } }
        }

        // 排序和「全部 / 自动 / 手动」筛选同时作用于订阅和分期两个区域
        if (st.activeSubs.isNotEmpty() || st.activeInstallments.isNotEmpty()) {
            item(key = "toolbar") {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SortButton(
                        st.settings.sort, st.settings.sortAscending,
                        expanded = sortMenu, onExpand = { sortMenu = it }, onPick = vm::setSort,
                    )
                    Spacer(Modifier.weight(1f))
                    FilterSlider(renewFilter) { renewFilter = it }
                }
            }
        }

        if (st.activeSubs.isNotEmpty()) {
            item(key = "h_subs") {
                CollapsibleHeader(str(R.string.home_active_subs), shownSubs.size, showSubs, { showSubs = !showSubs }) {
                    Text(str(R.string.home_this_month) + Currencies.format(monthCost(shownSubs), cur), style = MaterialTheme.typography.labelMedium.merge(TabularNums), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showSubs) {
                if (shownSubs.isEmpty()) item { EmptyFilterText(str(renewFilter.emptySubsRes)) }
                items(shownSubs, key = { it.id }) { s ->
                    Box(Modifier.animateItem(placementSpec = if (reorder.isDragging(s.id)) null else spring(stiffness = Spring.StiffnessMediumLow))) {
                        DragBox(s.id) { SubCard(s, st, today) { onOpen(s.id) } }
                    }
                }
            }
        }
        if (st.activeInstallments.isNotEmpty()) {
            item(key = "h_inst") {
                CollapsibleHeader(str(R.string.home_installments), shownInst.size, showInst, { showInst = !showInst }) {
                    val left = shownInst.sumOf { s -> st.rates.convert(Installments.summary(s).remainingAmount, s.currency, cur) }
                    Text(
                        str(R.string.home_this_month) + Currencies.format(monthCost(shownInst), cur) + str(R.string.home_remaining_sep) + Currencies.format(left, cur),
                        style = MaterialTheme.typography.labelMedium.merge(TabularNums), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showInst) {
                if (shownInst.isEmpty()) item { EmptyFilterText(str(renewFilter.emptyInstRes)) }
                items(shownInst, key = { instKey(it.id) }) { s ->
                    val key = instKey(s.id)
                    Box(Modifier.animateItem(placementSpec = if (reorder.isDragging(key)) null else spring(stiffness = Spring.StiffnessMediumLow))) {
                        DragBox(key) { InstallmentCard(s, st, today) { onOpen(s.id) } }
                    }
                }
            }
        }
        if (st.inactive.isNotEmpty()) {
            item(key = "h_inactive") {
                val title = when {
                    st.inactive.all { Installments.isFinished(it) } -> str(R.string.common_paid_off)
                    st.inactive.any { Installments.isFinished(it) } -> str(R.string.home_inactive_mixed)
                    else -> str(R.string.common_disabled)
                }
                CollapsibleHeader(title, st.inactive.size, showInactive, { showInactive = !showInactive }) {}
            }
            if (showInactive) items(st.sortedInactive(), key = { "x${it.id}" }) { s ->
                Box(Modifier.alpha(0.6f)) {
                    if (s.isInstallment) InstallmentCard(s, st, today) { onOpen(s.id) } else SubCard(s, st, today) { onOpen(s.id) }
                }
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
    if (addSheet) {
        AddChooserSheet(
            onDismiss = { addSheet = false },
            onSubscription = { addSheet = false; onAddSubscription() },
            onInstallment = { addSheet = false; onAddInstallment() },
        )
    }
}

/** 可折叠的分区标题：标题 + 计数 + 右侧小计 + 展开箭头，点整行切换 */
@Composable
private fun CollapsibleHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit, info: @Composable () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, label = "chevron")
    SectionHeader(title, count, Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle)) {
        info()
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Rounded.ExpandMore, if (expanded) str(R.string.home_collapse) else str(R.string.home_expand),
            Modifier.size(22.dp).graphicsLayer { rotationZ = rotation },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyFilterText(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

/** 点首页「+」后先选：添加订阅 / 添加分期付款。两个选项同一种样式 */
@Composable
private fun AddChooserSheet(onDismiss: () -> Unit, onSubscription: () -> Unit, onInstallment: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cs.background,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(str(R.string.home_add), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            AddOption(Icons.Rounded.Autorenew, str(R.string.common_add_subscription), onSubscription)
            AddOption(Icons.Rounded.CreditCard, str(R.string.home_add_installment), onInstallment)
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, onClick: () -> Unit) {
    val extra = LocalExtra.current
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerLowest)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(extra.accentBrush),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(26.dp), tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = cs.outline)
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
        st.rates.convert(Billing.upcomingCost(s, today, today.plusDays(29)), s.currency, cur)
    }
    // 全年预计：订阅按年折算；分期按未来 12 个月实际还要还的期数（快还完的分期不会被算满一年）
    val yearTotal = st.activeSubs.sumOf { st.rates.convert(Billing.periodCost(it, SummaryPeriod.YEAR), it.currency, cur) } +
        st.activeInstallments.sumOf { st.rates.convert(Billing.upcomingCost(it, today, today.plusYears(1).minusDays(1)), it.currency, cur) }
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
                Text(str(R.string.home_avg_spend, period.unitLabel), style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.78f))
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
                Stat(str(R.string.common_next_30_days), Currencies.format(next30, cur), Modifier.weight(1f))
                Stat(str(R.string.home_year_forecast), Currencies.format(yearTotal, cur), Modifier.weight(1f))
                val inst = st.activeInstallments.size
                if (inst == 0) Stat(str(R.string.home_subs_count), "${st.active.size}", Modifier.weight(0.6f))
                else Stat(str(R.string.home_subs_and_inst), "${st.activeSubs.size}+$inst", Modifier.weight(0.6f))
            }
            Spacer(Modifier.height(18.dp))
            PeriodSwitch(period, onPeriod)
            Spacer(Modifier.height(10.dp))
            val ratesText = if (st.rates.updatedAt == 0L) str(R.string.common_builtin_rates)
            else str(R.string.home_rates_updated) + java.time.Instant.ofEpochMilli(st.rates.updatedAt).atZone(java.time.ZoneId.systemDefault())
                .format(dateFmt(R.string.fmt_md_hm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ratesText, style = MaterialTheme.typography.labelSmall, color = white.copy(alpha = 0.55f), modifier = Modifier.weight(1f))
                Text(str(R.string.home_details), style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.85f))
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
                    str(R.string.home_period_chip, p.label), style = MaterialTheme.typography.labelLarge,
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
                    BalancePill(s)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        (if (s.isInstallment) Installments.progressText(s) + " · " else "") + str(if (s.autoRenew) R.string.common_auto else R.string.common_manual),
                        style = MaterialTheme.typography.labelMedium,
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
            ) { Text(str(R.string.common_skip_period), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Box(
                Modifier.weight(1.4f).clip(RoundedCornerShape(14.dp)).background(extra.accentBrush)
                    .clickable(onClick = onPaid).padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(str(R.string.home_i_paid), style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

/** 余额付款：余额不足 / 只够 1 期时提醒（移植自 PR #3） */
@Composable
private fun BalancePill(s: Subscription) {
    if (!s.active || !com.subnook.app.domain.Balance.usesBalance(s)) return
    val f = com.subnook.app.domain.Balance.forecast(s) ?: return
    if (f.coversAll || f.periods > 1) return
    val extra = LocalExtra.current
    Spacer(Modifier.width(6.dp))
    Pill(if (f.periods == 0) str(R.string.home_balance_low) else str(R.string.home_balance_one), if (f.periods == 0) extra.danger else extra.warning)
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
    val dayFmt = dateFmt(if (date.year == today.year) R.string.fmt_md else R.string.fmt_yymd)
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
                        date.format(dayFmt) + " · " + when {
                            s.payMethod == com.subnook.app.data.PayMethod.MANUAL -> str(R.string.common_manual_payment)
                            s.autoRenew -> s.payMethodLabel
                            else -> str(R.string.home_paymethod_manual, s.payMethodLabel)
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.active) {
                        Pill(Billing.dueText(days), dueColor(days))
                        BalancePill(s)
                    } else {
                        Pill(str(R.string.common_disabled), MaterialTheme.colorScheme.outline)
                    }
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

/** 分期卡片：第几期 + 进度条 + 本期金额 / 剩余应还 */
@Composable
private fun InstallmentCard(s: Subscription, st: UiState, today: LocalDate, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtra.current
    val sum = Installments.summary(s)
    val days = Billing.daysUntil(s, today)
    val date = LocalDate.ofEpochDay(s.nextBillingDay)
    val dayFmt = dateFmt(if (date.year == today.year) R.string.fmt_md else R.string.fmt_yymd)
    SoftCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubIcon(s.iconType, s.iconValue, s.color, s.name, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    // 和订阅卡片一样：自动扣款 / 手动还款图标 + 日期 + 付款方式
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (s.autoRenew) Icons.Rounded.Autorenew else Icons.Rounded.TouchApp, null,
                            Modifier.size(13.dp), tint = cs.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (sum.finished) plural(R.plurals.home_inst_finished, sum.total, s.instMethod.label, sum.total)
                            else date.format(dayFmt) + " · " + when {
                                s.payMethod == com.subnook.app.data.PayMethod.MANUAL -> str(R.string.home_manual_repay)
                                s.autoRenew -> s.payMethodLabel
                                else -> s.payMethodLabel // 手动已由左边的图标表示，省点宽度给金额
                            },
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            sum.finished -> Pill(if (Installments.isPaidOff(s)) str(R.string.common_paid_off_early) else str(R.string.common_paid_off), extra.success)
                            !s.active -> Pill(str(R.string.common_disabled), cs.outline)
                            else -> {
                                Pill(Billing.dueText(days), dueColor(days))
                                BalancePill(s)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (sum.finished) {
                        AmountText(sum.totalPayment, s.currency)
                        Spacer(Modifier.height(2.dp))
                        Text(str(R.string.common_total), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    } else {
                        AmountText(s.amount, s.currency)
                        Spacer(Modifier.height(2.dp))
                        Text(str(R.string.home_this_period), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            str(R.string.home_remaining) + Currencies.formatList(sum.remainingAmount, s.currency),
                            style = MaterialTheme.typography.labelSmall.merge(TabularNums), color = cs.outline, maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val progress = if (sum.total > 0) sum.paidTerms.toFloat() / sum.total else 0f
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(cs.surfaceContainerHigh)) {
                Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(CircleShape).background(extra.accentBrush))
            }
            Spacer(Modifier.height(5.dp))
            Row {
                Text(
                    if (Installments.isPaidOff(s)) str(R.string.common_paid_off_early) else if (sum.finished) plural(R.plurals.home_paid_terms, sum.paidTerms, sum.paidTerms) else plural(R.plurals.home_term_progress, sum.paidTerms, sum.current, sum.total, sum.paidTerms),
                    style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    (if (Installments.isPaidOff(s)) str(R.string.home_paid_off_on) else str(R.string.home_last_term)) + sum.endDate.format(dateFmt(R.string.fmt_ym)),
                    style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                )
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
            Text(str(R.string.home_empty), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.clip(CircleShape).background(extra.accentBrush).clickable(onClick = onAdd).padding(horizontal = 22.dp, vertical = 11.dp),
            ) { Text(str(R.string.home_add_first), style = MaterialTheme.typography.labelLarge, color = Color.White) }
        }
    }
}
