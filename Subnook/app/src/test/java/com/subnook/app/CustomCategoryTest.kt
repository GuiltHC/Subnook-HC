package com.subnook.app

import com.subnook.app.data.AppSettings
import com.subnook.app.data.BackupCodec
import com.subnook.app.data.BackupFile
import com.subnook.app.data.cleanCategory
import com.subnook.app.ui.edit.needsAsk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomCategoryTest {
    @Test fun askOnlyForNewNames() {
        val custom = listOf("宠物")
        assertTrue(needsAsk("健身", custom, askedFor = null))
        assertFalse(needsAsk("宠物", custom, null)) // 已经在列表里
        assertFalse(needsAsk("健身", custom, askedFor = "健身")) // 这次已经问过
        assertFalse(needsAsk("", custom, null))
        assertFalse(needsAsk("影视", custom, null)) // 和内置分类重名
    }

    @Test fun cleanName() {
        assertEquals("宠物 用品", cleanCategory("  宠物\n用品 "))
        assertEquals(20, cleanCategory("一".repeat(30)).length)
    }

    @Test fun editPageHasNoManualPayMethod() {
        val f = com.subnook.app.ui.edit.Form()
        f.payMethod = com.subnook.app.data.PayMethod.ALIPAY
        val choices = com.subnook.app.ui.edit.payMethodChoices(f)
        assertFalse(com.subnook.app.data.PayMethod.MANUAL in choices)
        assertFalse(com.subnook.app.data.PayMethod.OTHER in choices) // 「其他」单独放在最后
        // 旧数据正选着「手动付款」时照常显示，改选别的之后就没了
        f.payMethod = com.subnook.app.data.PayMethod.MANUAL
        assertTrue(com.subnook.app.data.PayMethod.MANUAL in com.subnook.app.ui.edit.payMethodChoices(f))
        // 分期没有应用商店
        f.payMethod = com.subnook.app.data.PayMethod.HUABEI
        f.kind = com.subnook.app.data.SubKind.INSTALLMENT
        assertFalse(com.subnook.app.data.PayMethod.GOOGLE_PLAY in com.subnook.app.ui.edit.payMethodChoices(f))
    }

    @Test fun askForPayMethodName() {
        val labels = com.subnook.app.ui.edit.payMethodLabels()
        assertTrue(com.subnook.app.ui.edit.needsAskName("云闪付", emptyList(), null, labels))
        assertFalse(com.subnook.app.ui.edit.needsAskName("支付宝", emptyList(), null, labels))
        assertFalse(com.subnook.app.ui.edit.needsAskName("云闪付", listOf("云闪付"), null, labels))
    }

    @Test fun customCategoriesInBackup() {
        val s = AppSettings(customCategories = listOf("宠物", "健身"), customPayMethods = listOf("云闪付"))
        val back = BackupCodec.decode(BackupCodec.encode(BackupFile(settings = s)))
        assertEquals(listOf("宠物", "健身"), back.settings.customCategories)
        assertEquals(listOf("云闪付"), back.settings.customPayMethods)
        // 旧备份的设置里没有这一项
        val old = BackupCodec.decode("""{"app":"Subnook","settings":{"displayCurrency":"CNY"},"subscriptions":[],"payments":[]}""")
        assertTrue(old.settings.customCategories.isEmpty())
    }
}
