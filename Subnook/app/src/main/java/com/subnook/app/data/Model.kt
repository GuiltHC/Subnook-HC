package com.subnook.app.data

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.subnook.app.R
import com.subnook.app.str
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * [labelRes]：周期单位的短名（编辑页的分段按钮）；[everyRes]：「每月」；[everyNRes]：「每 N 个月」（复数形式）。
 */
enum class CycleUnit(
    @StringRes val labelRes: Int,
    val approxDays: Double,
    @StringRes val everyRes: Int,
    @PluralsRes val everyNRes: Int,
) {
    DAY(R.string.cycle_unit_day, 1.0, R.string.cycle_every_day, R.plurals.cycle_every_n_days),
    WEEK(R.string.cycle_unit_week, 7.0, R.string.cycle_every_week, R.plurals.cycle_every_n_weeks),
    MONTH(R.string.cycle_unit_month, 365.2425 / 12.0, R.string.cycle_every_month, R.plurals.cycle_every_n_months),
    YEAR(R.string.cycle_unit_year, 365.2425, R.string.cycle_every_year, R.plurals.cycle_every_n_years),
    ;

    val label: String get() = str(labelRes)
}

enum class PayMethod(@StringRes val labelRes: Int, val defaultAuto: Boolean) {
    GOOGLE_PLAY(R.string.pay_google_play, true),
    APP_STORE(R.string.pay_app_store, true),
    WECHAT(R.string.pay_wechat, true),
    ALIPAY(R.string.pay_alipay, true),
    CREDIT_CARD(R.string.pay_credit_card, true),
    PAYPAL(R.string.pay_paypal, true),
    BANK_CARD(R.string.pay_bank_card, true),
    /** 预付余额（话费、游戏点券、平台钱包等）：记账时从订阅的余额里扣 */
    BALANCE(R.string.pay_balance, true),
    MANUAL(R.string.common_manual_payment, false),
    OTHER(R.string.common_other, false),
    // ---- 分期常用（只列持牌机构 / 大平台的正规消费分期，不放网贷）----
    HUABEI(R.string.pay_huabei, true),
    JD_BAITIAO(R.string.pay_jd_baitiao, true),
    WECHAT_FENFU(R.string.pay_wechat_fenfu, true),
    MEITUAN_YUEFU(R.string.pay_meituan_yuefu, true),
    DOUYIN_YUEFU(R.string.pay_douyin_yuefu, true),
    BANK_LOAN(R.string.pay_bank_loan, true),
    ;

    val label: String get() = str(labelRes)

    companion object {
        /** 订阅能选的付款方式（顺序即界面上的顺序） */
        // 「手动付款」不列：手动 / 自动由「自动续费」开关决定，和用什么方式付款无关（旧数据里的 MANUAL 照常显示、统计）
        val forSubscription = listOf(GOOGLE_PLAY, APP_STORE, WECHAT, ALIPAY, CREDIT_CARD, PAYPAL, BANK_CARD, BALANCE, OTHER)

        /** 分期能选的付款方式：应用商店这类分不了期的不列 */
        val forInstallment = listOf(CREDIT_CARD, HUABEI, JD_BAITIAO, WECHAT_FENFU, MEITUAN_YUEFU, DOUYIN_YUEFU, BANK_LOAN, OTHER)
    }
}

/**
 * 订阅和分期共用一套分类（统计页按同一套分类汇总）。
 * 旧的「分期还款」分类已去掉（DB v7 迁移成「其他」），换成几个分期常见的分类。
 */
enum class Category(@StringRes val labelRes: Int) {
    VIDEO(R.string.cat_video),
    MUSIC(R.string.cat_music),
    AI(R.string.cat_ai),
    CLOUD(R.string.cat_cloud),
    TOOLS(R.string.cat_tools),
    EDU(R.string.cat_edu),
    SHOPPING(R.string.cat_shopping),
    SOCIAL(R.string.cat_social),
    GAME(R.string.cat_game),
    VPN(R.string.cat_vpn),
    TELECOM(R.string.cat_telecom),
    NEWS(R.string.cat_news),
    LIFE(R.string.cat_life),
    DIGITAL(R.string.cat_digital),
    APPLIANCE(R.string.cat_appliance),
    CAR(R.string.cat_car),
    HOUSING(R.string.cat_housing),
    HEALTH(R.string.cat_health),
    OTHER(R.string.common_other),
    ;

    val label: String get() = str(labelRes)

    companion object {
        /** 分期的分类：常见的排前面，其余照常可选 */
        val forInstallment: List<Category> = listOf(DIGITAL, APPLIANCE, CAR, HOUSING, EDU, HEALTH, SHOPPING, LIFE).let { first ->
            first + entries.filter { it !in first && it != OTHER } + OTHER
        }
    }
}

/** 记录类型：普通订阅 / 分期付款（分期也按周期还款，复用账单日、提醒、付款记录等逻辑） */
enum class SubKind { SUBSCRIPTION, INSTALLMENT }

/**
 * 分期的计息方式。[rateLabel] 是费率输入框的名字，null 表示不需要填费率。
 * 金额按最小货币单位（分；无小数的币种按 1）计算，每期四舍五入，零头放在首期，等额本息的尾差放在末期。
 */
enum class InstallmentMethod(@StringRes val labelRes: Int, @StringRes val rateLabelRes: Int?, @StringRes val hintRes: Int) {
    INTEREST_FREE(R.string.inst_interest_free, null, R.string.inst_interest_free_hint),
    FEE(R.string.inst_fee, R.string.inst_rate_per_period, R.string.inst_fee_hint),
    FEE_UPFRONT(R.string.inst_fee_upfront, R.string.inst_rate_per_period, R.string.inst_fee_upfront_hint),
    EQUAL_PAYMENT(R.string.inst_equal_payment, R.string.inst_annual_rate, R.string.inst_equal_payment_hint),
    EQUAL_PRINCIPAL(R.string.inst_equal_principal, R.string.inst_annual_rate, R.string.inst_equal_principal_hint),
    ;

    val label: String get() = str(labelRes)
    val rateLabel: String? get() = rateLabelRes?.let { str(it) }
    val hint: String get() = str(hintRes)
}

/** 图标类型：PRESET=内置品牌图(value=预设key) / MATERIAL=设计图标(value=图标名) / CUSTOM=相册图片(value=文件名) / TEXT=文字(value=文字) */
enum class IconType { PRESET, MATERIAL, CUSTOM, TEXT }

@Serializable
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconType: IconType = IconType.TEXT,
    val iconValue: String = "",
    val color: Int = 0xFF00897B.toInt(),
    val amount: Double,
    val currency: String,
    val cycleCount: Int = 1,
    val cycleUnit: CycleUnit = CycleUnit.MONTH,
    /** 下次付款日期 (LocalDate.toEpochDay) */
    val nextBillingDay: Long,
    /** 账单日锚点(1..31)，用于按月/按年计算时保持月末日期不漂移 */
    val anchorDay: Int,
    val payMethod: PayMethod = PayMethod.OTHER,
    val autoRenew: Boolean = true,
    val category: Category = Category.OTHER,
    val remindEnabled: Boolean = true,
    val remindDaysBefore: Int = 3,
    val active: Boolean = true,
    val note: String = "",
    val presetKey: String? = null,
    /** 最后一次发送"即将扣款"提醒的日期（每天最多提醒一次） */
    val remindedForDay: Long? = null,
    /** 手动付款逾期提醒最后发送的日期 */
    val overdueNotifiedDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** 手动排序的位置，越小越靠前（DB v2 新增） */
    @ColumnInfo(defaultValue = "0") val sortIndex: Int = 0,
    // ---- DB v3 新增：分期付款。旧备份没有这些字段，按默认值导入就是普通订阅 ----
    @ColumnInfo(defaultValue = "SUBSCRIPTION") val kind: SubKind = SubKind.SUBSCRIPTION,
    /** 分期本金（分期总金额，不含手续费 / 利息） */
    @ColumnInfo(defaultValue = "0") val instPrincipal: Double = 0.0,
    /** 总期数 */
    @ColumnInfo(defaultValue = "0") val instTerms: Int = 0,
    /** 费率（百分数）：手续费方式是每期费率，等额本息 / 等额本金是年利率 */
    @ColumnInfo(defaultValue = "0") val instRate: Double = 0.0,
    @ColumnInfo(defaultValue = "FEE") val instMethod: InstallmentMethod = InstallmentMethod.FEE,
    /** 首期金额（和计算结果对不上时由用户填写账单上的数），null = 按计算 */
    val instFirstAmount: Double? = null,
    /** 第 1 期的还款日 (LocalDate.toEpochDay) */
    @ColumnInfo(defaultValue = "0") val instStartDay: Long = 0,
    // ---- DB v4 新增：提前还款 ----
    // 部分提前还款后，上面的 inst* 字段改成「剩余部分重新计算的新计划」，之前已经还掉的记在下面三个字段里
    /** 之前的计划里已经还完的期数（显示「第 N 期」时加上） */
    @ColumnInfo(defaultValue = "0") val instTermOffset: Int = 0,
    /** 之前已经付出的钱（按期还款 + 提前还款） */
    @ColumnInfo(defaultValue = "0") val instPaidBefore: Double = 0.0,
    /** 之前已经还掉的本金 */
    @ColumnInfo(defaultValue = "0") val instPrincipalBefore: Double = 0.0,
    /** 一次性提前还清的日期，null = 没有提前还清 */
    val instPaidOffDay: Long? = null,
    // ---- DB v6 新增：余额付款（移植自 PR #3，作者 wanwuzhishi）----
    /** 余额付款的剩余余额，币种与订阅相同 */
    @ColumnInfo(defaultValue = "0") val balance: Double = 0.0,
    // ---- DB v7 新增：选「其他」时用户自己填的名称（可以不填）----
    @ColumnInfo(defaultValue = "") val payMethodNote: String = "",
    @ColumnInfo(defaultValue = "") val categoryNote: String = "",
) {
    val isInstallment: Boolean get() = kind == SubKind.INSTALLMENT

    /** 显示用的付款方式：「其他」且填了名称时显示填的名称 */
    val payMethodLabel: String
        get() = if (payMethod == PayMethod.OTHER && payMethodNote.isNotBlank()) payMethodNote.trim() else payMethod.label

    /** 显示 / 统计用的分类：「其他」且填了名称时用填的名称 */
    val categoryLabel: String
        get() = if (category == Category.OTHER && categoryNote.isNotBlank()) categoryNote.trim() else category.label
}

@Serializable
@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = Subscription::class,
        parentColumns = ["id"],
        childColumns = ["subscriptionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("subscriptionId"), Index("billingDay")],
)
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subscriptionId: Long,
    /** 该笔对应的账单日 */
    val billingDay: Long,
    val amount: Double,
    val currency: String,
    /** true=自动续费自动记账，false=手动确认 */
    val auto: Boolean,
    val recordedAt: Long = System.currentTimeMillis(),
    /** 分期：这笔是第几期（DB v4 新增，旧记录为 null） */
    val term: Int? = null,
    /** 分期：提前还款（DB v4 新增） */
    @ColumnInfo(defaultValue = "0") val prepay: Boolean = false,
    /** 这笔付款从订阅余额里扣掉的钱；0 = 不是余额付款（DB v6 新增）。删除记录时退回余额 */
    @ColumnInfo(defaultValue = "0") val balanceUsed: Double = 0.0,
    /** 提前还款前的分期快照（Subscription 的 JSON，DB v5 新增），删除这条记录时用来恢复 */
    val undo: String? = null,
)

/** 余额充值记录（DB v6 新增）。不算支出（支出在扣款时记），删除时从余额里减回去 */
@Serializable
@Entity(
    tableName = "topups",
    foreignKeys = [ForeignKey(
        entity = Subscription::class,
        parentColumns = ["id"],
        childColumns = ["subscriptionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("subscriptionId")],
)
data class TopUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subscriptionId: Long,
    /** 充值日期 (LocalDate.toEpochDay) */
    val day: Long,
    val amount: Double,
    val currency: String,
    val recordedAt: Long = System.currentTimeMillis(),
)

data class Preset(
    val key: String,
    /** 原始名称（tools/presets.psv）；显示用 [name]，俄语界面下会换成 PresetNames 里的名称 */
    val rawName: String,
    val category: Category,
    val colorLong: Long,
    val price: Double,
    val currency: String,
    val cycleCount: Int,
    val cycleUnit: CycleUnit,
    val payMethod: PayMethod,
    val material: String,
) {
    val color: Int get() = colorLong.toInt()

    /** Название для показа: для русского интерфейса — из [PresetNames], иначе оригинальное */
    val name: String get() = PresetNames.localized(key) ?: rawName
}
