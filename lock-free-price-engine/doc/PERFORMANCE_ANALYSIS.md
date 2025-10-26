# PERFORMANCE_ANALYSIS.md

## 2025-10-26 - v0.1.0 Benchmark Summary

### Test Conditions
- JDK: `jdk-21`
- GC: `ZGC`（`-XX:+UseZGC`）
- Runner: Gradle `Exec` 调用 JMH 可执行 Jar
- Output: CSV 位于 `build/reports/jmh/*.csv`，人类可读报告 `build/reports/jmh/human.txt`

### Latest Results (Snapshot)
- Quick Benchmarks (`quick-results.csv`):
  - 含 `QuickBenchmark.quickQueryTest/quickThroughputTest/quickLatencyTest` 的吞吐与平均时间数据。
  - 已确认文件生成，建议后续粘贴具体数值用于对比。
- Throughput Benchmarks:
  - 运行需加 `-Djmh.ignoreLock=true` 以绕过 `/tmp/jmh.lock`，CSV 将写入 `build/reports/jmh/throughput-results.csv`。
- Latency Benchmarks:
  - 输出至 `build/reports/jmh/latency-results.csv`。

### Observations
- 资源路径：使用执行 Jar 保证 `BenchmarkList` 可用，避免类路径问题。
- JVM 参数：统一 `ZGC` 后基准运行稳定，无 GC 参数冲突。
- JMH 锁：同机并发或异常退出会导致锁文件占用；已记录处理方式。

### Recommendations
- 将 `Quick/Latency/Throughput` 最新一次运行的关键数值（吞吐 ops/s、平均时间 ns/op）整理为表格，附系统规格。
- 对比不同 `WaitStrategy` 与 `RingBufferSize`，形成延迟/吞吐权衡报告。
- 引入延迟分布（P50/P99/P999）以避免平均值误导。

### Next Steps
- 跑一次全量：`./gradlew clean jmh -Djmh.ignoreLock=true`，抽取汇总表。
- 保存基准环境信息（CPU 核数、频率、内存、OS），确保可复现实验。