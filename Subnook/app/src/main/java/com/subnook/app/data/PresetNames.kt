package com.subnook.app.data

import com.subnook.app.uiLocale

/**
 * Названия пресетов для русского интерфейса.
 *
 * PresetCatalog генерируется скриптом из tools/presets.psv и хранит оригинальные (китайские) названия.
 * Здесь — замены для пресетов с китайскими названиями: общие слова переведены,
 * у брендов взято их официальное латинское название. Пресеты, которых нет в этом списке,
 * показываются как есть. Поиск ищет и по оригинальному названию (см. [Preset.rawName]).
 */
object PresetNames {
    private val ru: Map<String, String> = mapOf(
        // Общие (не бренды)
        "g_phone" to "Мобильная связь",
        "g_broadband" to "Домашний интернет",
        "g_rent" to "Аренда жилья",
        "g_gym" to "Спортзал",
        "g_server" to "Облачный сервер / VPS",
        "g_domain" to "Продление домена",
        "g_insurance" to "Страховка",
        "g_airport" to "Прокси / VPN-сервис",
        // Видео
        "iqiyi" to "iQIYI VIP",
        "tencentvideo" to "Tencent Video VIP",
        "youku" to "Youku VIP",
        "mgtv" to "Mango TV VIP",
        "bilibili" to "Bilibili Premium",
        "migu" to "Migu Video VIP",
        "sohu" to "Sohu Video VIP",
        // Музыка и аудио
        "qqmusic" to "QQ Music VIP",
        "neteasemusic" to "NetEase Cloud Music VIP",
        "kugou" to "Kugou Music VIP",
        "ximalaya" to "Ximalaya VIP",
        "kuwo" to "Kuwo Music VIP",
        "qishui" to "Soda Music VIP",
        "qingting" to "Qingting FM VIP",
        // Облака
        "baidunetdisk" to "Baidu Netdisk SVIP",
        "quark" to "Quark Drive VIP",
        "aliyundrive" to "Alibaba Cloud Drive VIP",
        "pikpak" to "PikPak Premium",
        "p115" to "115 Cloud VIP",
        "xunlei" to "Xunlei SVIP",
        "tianyi" to "Tianyi Cloud VIP",
        "mcloud" to "China Mobile Cloud VIP",
        "nutstore" to "Nutstore",
        "pan123" to "123Pan VIP",
        // Инструменты
        "wps" to "WPS Premium",
        "ticktick" to "TickTick Premium (Китай)",
        "yinxiang" to "Yinxiang Biji (Evernote Китай)",
        "jianying" to "Jianying (CapCut Китай)",
        "camscanner" to "CamScanner",
        "shimo" to "Shimo Docs",
        "aliyun" to "Alibaba Cloud",
        "tencentcloud" to "Tencent Cloud",
        // Учёба и чтение
        "weread" to "WeChat Read",
        "zhihu" to "Zhihu Yanxuan VIP",
        "dedao" to "Dedao",
        "fandeng" to "Fanshu (Fandeng Reading)",
        "qidian" to "Qidian Reading VIP",
        "zhangyue" to "iReader VIP",
        "baicizhan" to "Baicizhan",
        "shanbay" to "Shanbay",
        "youdao" to "Youdao Dictionary VIP",
        // Быт
        "keep" to "Keep VIP",
        "didi" to "DiDi VIP",
        "hellobike" to "Hellobike (абонемент)",
        // Покупки
        "jdplus" to "JD PLUS",
        "taobao88" to "Taobao 88VIP",
        "meituan" to "Meituan VIP",
        "eleme" to "Ele.me VIP",
        "hema" to "Freshippo X",
        "sams" to "Sam's Club",
        "vipshop" to "Vipshop SVIP",
        "costco" to "Costco (членство)",
        // Соцсети
        "qqvip" to "QQ SVIP",
        "weibo" to "Weibo VIP",
        "medium" to "Medium (подписка)",
        "zsxq" to "Zhishi Xingqiu",
        "maimai" to "Maimai VIP",
        // Игры
        "genshin" to "Genshin Impact · Благословение полой луны",
        "starrail" to "Honkai: Star Rail · Экспресс-пропуск",
        "zzz" to "Zenless Zone Zero · Членство Интернота",
        "arknights" to "Arknights · месячная карта",
        "wuthering" to "Wuthering Waves · месячная карта",
        "honorofkings" to "Honor of Kings · боевой пропуск",
        "azurlane" to "Azur Lane · месячная карта",
        "honkai3" to "Honkai Impact 3rd · месячная карта",
        "bluearchive" to "Blue Archive · месячная карта",
        "onmyoji" to "Onmyoji · месячная карта",
        "lysk" to "Love and Deepspace · месячная карта",
        "infinitynikki" to "Infinity Nikki · месячная карта",
        "evol" to "Mr Love: Queen's Choice · месячная карта",
        "reverse1999" to "Reverse: 1999 · месячная карта",
        "pgr" to "Punishing: Gray Raven · месячная карта",
        "snowbreak" to "Snowbreak · месячная карта",
        "gf2" to "Girls' Frontline 2: Exilium · месячная карта",
        "endfield" to "Arknights: Endfield · месячная карта",
        "wuhua" to "Wuhua Mixin · месячная карта",
        // Новости
        "wsj" to "The Wall Street Journal",
        "caixin" to "Caixin",
        "wallstreetcn" to "Wallstreetcn",
        // Связь
        "cmcc" to "China Mobile",
        "unicom" to "China Unicom",
        "telecom" to "China Telecom",
        "cbn" to "China Broadnet",
        "cmhk" to "China Mobile Hong Kong (CMHK)",
        "cht" to "Chunghwa Telecom",
        "twm" to "Taiwan Mobile",
        "fetnet" to "FarEasTone",
        "rakuten" to "Rakuten Mobile",
        "ultra" to "Ultra Mobile PayGo",
        "cuniq" to "China Unicom Hong Kong (CUniq)",
    )

    /**
     * Пресеты с китайскими названиями, которые нужны не только в Китае (игры и международные сервисы):
     * их показываем и в русском интерфейсе. Остальные китайские пресеты — только в китайском.
     */
    private val global: Set<String> = setOf(
        "genshin", "starrail", "zzz", "arknights", "wuthering", "honorofkings", "azurlane", "honkai3",
        "bluearchive", "onmyoji", "lysk", "infinitynikki", "evol", "reverse1999", "pgr", "snowbreak", "gf2", "endfield",
        "medium", "wsj", "costco", "camscanner", "rakuten", "ultra",
    )

    private fun isCjk(c: Char) = c in '\u4e00'..'\u9fff' || c in '\u3400'..'\u4dbf'

    /** Пресет только для китайского интерфейса (китайские сервисы). Общие пресеты g_* показываются всем. */
    fun isChinaOnly(p: Preset): Boolean = !p.key.startsWith("g_") && p.key !in global && p.rawName.any(::isCjk)

    /** Российские сервисы (ключи ru_*): для всех интерфейсов, кроме китайского. */
    fun isRussian(p: Preset): Boolean = p.key.startsWith("ru_")

    /**
     * Пресеты, которые показываются в списке выбора для текущего языка интерфейса.
     * Уже добавленные подписки с любыми пресетами работают как раньше: поиск по ключу идёт по [PresetCatalog.all].
     * В русском интерфейсе российские сервисы идут первыми в своих разделах.
     */
    fun visible(): List<Preset> {
        val zh = uiLocale().language == "zh"
        val list = PresetCatalog.all.filter { if (zh) !isRussian(it) else !isChinaOnly(it) }
        return if (zh) list else list.sortedBy { if (isRussian(it)) 0 else 1 }
    }

    /**
     * В некитайском интерфейсе цену в юанях из оригинального каталога не подставляем
     * (сумму пользователь введёт сам, валюта остаётся выбранной по умолчанию).
     */
    fun usePresetPrice(p: Preset): Boolean = p.price > 0 && (p.currency != "CNY" || uiLocale().language == "zh")

    /** Название пресета на языке интерфейса; null — показывать оригинальное. */
    fun localized(key: String): String? = if (uiLocale().language == "ru") ru[key] else null

    internal val keys: Set<String> get() = ru.keys
}
