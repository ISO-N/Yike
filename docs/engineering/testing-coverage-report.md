# 忆刻项目测试覆盖报告（治理版）

> 更新时间: 2026-03-17

## 1. 当前基线

- `.\gradlew.bat testDebugUnitTest` 已在 2026-03-17 本地跑通，当前主机测试基线为绿色。
- 当前主机侧共 134 个测试，覆盖 JVM 纯单测与 Robolectric 主机集成测试。
- `androidTest` 本轮未重跑；最近一次设备侧验证记录仍以 `manual-acceptance-v0-1.md` 中 2026-03-15 的结果为准。
- 本报告统一按四层门禁记录，不再只统计 `src/test`：
  - `JVM 单测`
  - `Robolectric 主机集成`
  - `androidTest`
  - `手动验收`

---

## 2. 覆盖矩阵

| 能力 | JVM / Robolectric | androidTest / 手动 | 当前状态 | 仍需补齐 |
|---|---|---|---|---|
| 调度器与评分流程 | `ReviewSchedulerV1Test`、`InitialDueAtCalculatorTest`、`OfflineReviewRepositoryTest` | `YikeDatabaseIntegrationTest` | 强 | 继续补 `intervalStepCount=0/1`、非法分钟、非 UTC 时区 |
| 卡组管理 | `DeckListViewModelTest` | 手动内容管理验收 | 中 | 编辑已有卡组、恢复归档、搜索过滤、`OfflineDeckRepositoryTest` |
| 卡片编辑 | `QuestionEditorViewModelTest` | 手动内容管理验收 | 中 | 继续补编辑已有问题与失败重试 |
| 搜索筛选 | `QuestionSearchViewModelTest` | `YikeDatabaseIntegrationTest` | 中上 | 更多仓储层复杂筛选与排序场景 |
| 回收站 | `RecycleBinViewModelTest` | 手动回收站主路径 | 中 | 仓储层归档摘要查询与级联删除联动 |
| 备份恢复 | `BackupValidatorTest`、`BackupServiceTest`、`BackupRestoreViewModelTest` | `FeatureContentTest`、手动备份恢复验收 | 中上 | 缺失卡组/问题引用校验、设备级文件选择器联动 |
| 每日提醒 | `ReminderTimeCalculatorTest`、`ReminderCheckRunnerTest`、`ReminderSchedulerTest` | 手动通知权限 / 到点提醒 / 时区验证 | 中上 | `ReminderCheckWorker` 与 `NotificationHelper` 仍依赖设备门禁 |
| 局域网同步 | `LanSyncConflictResolverTest`、`LanSyncChangeApplierTest`、`LanSyncViewModelTest` | 手动局域网发现与配对 | 中 | `LanSyncRepositoryImpl`、传输层、发现层、加密层仍缺专项测试 |
| 统计分析 | `AnalyticsViewModelTest` | `YikeDatabaseIntegrationTest` | 中上 | 更多多卡组、跨时区、空数据结论场景 |
| 设置存储 | `DataStoreAppSettingsRepositoryTest` | 手动设置页回归 | 中上 | 同步 journal 降噪与异常恢复仍可继续加强 |

---

## 3. 本轮修复与新增

### 3.1 基线治理

- 修复主机测试基线的 6 个失败用例：
  - `BackupRestoreViewModelTest` 改为 Robolectric 主机测试
  - `LanSyncChangeApplierTest` 改为 Robolectric 主机测试，并修正 `tearDown`
  - `BackupServiceTest` 改为结构化文档断言，去掉脆弱的 JSON 字符串匹配
- 把提醒链路收敛为 `ReminderCheckRunner`，让 Worker 保持极薄并可在主机测试中覆盖核心逻辑。
- 新增本地统一测试入口脚本 `scripts/verify-testing.ps1`。

### 3.2 新增高优先级测试

- 页面层
  - `feature/editor/QuestionEditorViewModelTest`
  - `feature/search/QuestionSearchViewModelTest`
  - `feature/analytics/AnalyticsViewModelTest`
  - `feature/recyclebin/RecycleBinViewModelTest`
- 提醒与设置
  - `data/reminder/ReminderSchedulerTest`
  - `data/settings/DataStoreAppSettingsRepositoryTest` 补提醒开关、备份时间、并发写入
- 备份与卡组边界
  - `BackupServiceTest` 补空数据库导出
  - `BackupValidatorTest` 补非法 rating / stageIndex
  - `DeckListViewModelTest` 补空名称、非法间隔、关闭编辑器

---

## 4. 仍然存在的真实空白

以下空白属于“尚未补齐”，不再和“统计口径漏算”混在一起：

1. `LanSyncRepositoryImpl` 编排测试
2. `LanSyncHttpClient` / `LanSyncHttpServer` 契约测试
3. `LanSyncNsdService` 平台发现测试
4. `OfflineDeckRepositoryTest`
5. 回收站与卡组归档相关的数据层集成测试
6. 备份恢复文件选择器和设备级文件权限联动

---

## 5. 使用方式

推荐本地统一使用：

```powershell
.\scripts\verify-testing.ps1
.\scripts\verify-testing.ps1 -Connected
```

说明：

- 默认执行 `testDebugUnitTest` + `assembleDebugAndroidTest`
- 带 `-Connected` 时再补跑 `connectedDebugAndroidTest`
- 平台相关能力最终仍需配合 `manual-acceptance-v0-1.md` 做设备验收

---

## 6. 结论

当前测试体系已经从“按文件数主观打星”转成“按能力和门禁记录”的治理模式。  
本轮之后，编辑、搜索、统计、回收站、提醒调度、设置存储这些原先最薄弱的页面和边界层已经有了主机侧自动化覆盖；真正仍然高风险且未闭环的区域，已经收敛到局域网同步编排层、传输层和少量数据层仓储测试上。
