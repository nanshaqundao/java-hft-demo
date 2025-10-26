# TODO.md

## v0.1.0 文档与基准同步（2025-10-26）

### Completed ✅
- 修复 `gradle jmh` JVM 参数冲突（移除同时启用 G1GC 与 ZGC，仅保留 `-XX:+UseZGC`）。
- 终止挂起的 Gradle 任务，清理环境后重试。
- 将 `quickBenchmark` 改为直接执行已生成的 JMH 可执行 Jar（Gradle `Exec` 任务），解决 `META-INF/BenchmarkList` 资源缺失问题。
- 运行 `./gradlew quickBenchmark`，生成 CSV：`build/reports/jmh/quick-results.csv`。
- 将 `latencyBenchmark` 与 `throughputBenchmark` 同步改为执行 JMH 可执行 Jar，统一 CSV 输出与参数路径。
- 启动 `throughputBenchmark`，识别并记录 JMH 全局锁问题（`/tmp/jmh.lock`）。
- 总结快速与专项基准的运行方式与路径，提供命令与注意事项（`-Djmh.ignoreLock=true`）。

### Pending ⏳
- 运行 `throughputBenchmark` 与 `latencyBenchmark` 全量并记录最新 CSV 指标（带 `-Djmh.ignoreLock=true`）。
- 在聚合处理器实现多源最优价（BBO/NBBO）简版规则与查询接口（`getBestPrice(symbol)`）。
- 引入 HdrHistogram 做延迟分布采样与 P99/P999 报告（文档已提供参考实现）。
- 性能对比实验：不同 `WaitStrategy` 与 `RingBufferSize` 的延迟/吞吐权衡曲线。
- 编写面试速讲稿（图解、术语速通、取舍题、指标截图清单）。

### Notes 📝
- CSV 输出路径统一：
  - `quickBenchmark` → `build/reports/jmh/quick-results.csv`
  - `latencyBenchmark` → `build/reports/jmh/latency-results.csv`
  - `throughputBenchmark` → `build/reports/jmh/throughput-results.csv`
- 人类可读报告：`build/reports/jmh/human.txt`
- JMH 锁冲突：同机上次未正常退出会占用 `/tmp/jmh.lock`，优先使用 `-Djmh.ignoreLock=true`。