# Subnook 交接文档

> 不含敏感信息，可以提交到仓库；签名密码等只在本机的 `Subnook/keystore.properties` 里。

## 现状（2026-11-17）

- 版本：**1.1.0**（versionCode 2），包名 `com.subnook.app`
- 仓库：<https://github.com/capllen/Subnook>（public，main 分支，MIT 许可）
- Release：<https://github.com/capllen/Subnook/releases/tag/v1.1.0>，附 `Subnook-1.1.0.apk`
- 一加 11 上装着 1.1.0 正式版（用户真实数据：1 条 Netflix）
- 提交身份（仅本仓库）：`capllen <133580566+capllen@users.noreply.github.com>`

## 目录

| 路径 | 说明 | 进 git |
|---|---|---|
| `Subnook/` | Android 项目（Kotlin + Compose + Room + DataStore + WorkManager + Glance） | ✅ |
| `Subnook/tools/` | 预设数据 `presets.psv`、图标抓取/生成脚本、图标核对工具 | ✅ |
| `scripts/` | `env.ps1/env.sh`（指向 toolchain）、`build.ps1`、`install.ps1` | ✅ |
| `docs/screenshots/` | README 截图 | ✅ |
| `CHANGELOG.md` | 版本记录 | ✅ |
| `toolchain/` | 便携 JDK21 / Android SDK / Gradle 8.14.3 及全部缓存、临时目录 | ❌ |
| `.work/` | 测试截图、测试备份、迁移备份 `.work/migrate/` | ❌ |
| `Subnook/keystore/release.jks` + `Subnook/keystore.properties` | **release 签名，务必备份，丢了就无法覆盖升级** | ❌ |

## 约定（用户偏好）

- **不往 C 盘写任何东西**：编译前先 `. scripts/env.sh`（Git Bash）或 `. .\scripts\env.ps1`，JAVA/Gradle/Android/TMP/user.home 都指向 `toolchain/`。用户自己 2025 年的 `C:\Users\chunh\.android\adbkey` 不要动。
- 金额显示：除首页紫色总额卡用 `¥3,267.42` 外，全部用 `CN¥ / US$ / JP¥ / HK$ / €`（`Currencies.formatList`），整数金额不带小数。
- 设计图标调色板避开蓝/紫色（与首页紫卡撞色）。
- 提醒：默认提前 3 天，提醒期内每天提醒一次；3 天内到期的订阅（不分自动/手动）进「待付款」。
- 发版：versionCode +1；versionName「主.功能.修复」；更新 `CHANGELOG.md`；打 tag `vX.Y.Z`，Release 附 `Subnook-X.Y.Z.apk`。
- 用户在一加 11 上调试，最终设备是一加 16（ColorOS）。

## 常用命令

```bash
. scripts/env.sh
cd Subnook
gradle :app:testDebugUnitTest      # 14 个单元测试
gradle :app:assembleDebug          # 包名 com.subnook.app.debug，可与正式版共存
gradle :app:assembleRelease        # 用 keystore.properties 签名
adb install -r app/build/outputs/apk/release/app-release.apk
```

预设：改 `tools/presets.psv` → `bash tools/fetch_icons.sh`（新增的才下载）→ `bash tools/gen_presets.sh` → `java tools/IconSheet.java ../.work/sheet.png key1 key2` 核对图标。

## 发布新版本

1. 改代码，跑测试，真机验证
2. `app/build.gradle.kts` 改 versionCode / versionName，写 `CHANGELOG.md`
3. `gradle :app:assembleRelease`，确认签名指纹仍是 `7ce4e4a9…452a`（`apksigner verify --print-certs`）
4. `git commit`、`git tag -a vX.Y.Z`、`git push origin main --tags`
5. GitHub → Releases → Draft new release → 选 tag → 上传 `Subnook-X.Y.Z.apk`（本机没装 gh，1.1.0 是用 git 凭据调 API 发的）

## 已知注意点

- ColorOS 会静默拒绝 App 内「添加到桌面」，小组件需手动加：长按桌面 → 卡片 → 插件 → Subnook。
- ColorOS 后台严格：需在 App 设置里开「准时提醒」「忽略电池优化」，并允许自启动。
- 系统文件选择器「最近」依赖媒体索引，新文件可能暂不出现；导入/导出已默认打开 Download。
- 用户的 Netflix 记录金额 15.49 币种却是 JPY（显示 JP¥15），可能本意是美元，已告知用户。
- 7 个预设无品牌图标（Play Pass、Parallels、腾讯云、Vercel、EA Play、Ubisoft+、SmarTone），用设计图标兜底。
- 唯品会、帆书、咪咕视频的 App Store 图标带活动字样。
- 备份导入兼容旧名 `SubKeeper` 的 JSON（`BackupManager.BACKUP_APP_IDS`）。

## 可以做的后续

- Room 数据库目前 version 1、无迁移；以后改表结构必须写 Migration，否则覆盖安装会丢数据。
- 可考虑加 GitHub Actions 自动编译 / 发 Release。
