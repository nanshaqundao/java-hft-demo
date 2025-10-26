# TESTING_SUMMARY.md

## 2025-10-26 - v0.1.0 Testing Overview

### Coverage & Scenarios
- 单元测试：待补充（核心处理器接口与聚合逻辑）。
- 基准测试：已配置 JMH 快速/延迟/吞吐三类基准。

### Methodology
- 使用 JMH 进行隔离测试：`Warmup`、`Measurement`、`Fork`、`Blackhole`、`@State`。
- 通过 Gradle `Exec` 运行 JMH 可执行 Jar，确保资源完整与输出统一。

### Results Summary
- 快速基准：`build/reports/jmh/quick-results.csv` 已生成，包含吞吐与平均时间。
- 吞吐与延迟专项：建议使用 `-Djmh.ignoreLock=true` 后运行，并将结果写入对应 CSV。

### Failures & Fixes
- JMH 锁冲突：`/tmp/jmh.lock` 占用导致无法运行，添加 `-Djmh.ignoreLock=true` 参数解决。
- JVM 参数冲突：统一选择 `ZGC`，移除与 `G1GC` 的冲突配置。

### Next Steps
- 增加最优价（BBO/NBBO）相关的单元测试（规则过滤、延迟阈值、源优先级）。
- 引入 HdrHistogram 的延迟分布测试（P50/P99/P999），并记录到 `PERFORMANCE_ANALYSIS.md`。