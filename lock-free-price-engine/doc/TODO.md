# TODO.md

## v0.2.0 核心技术点完善与性能优化（2025-01-01）

### Completed ✅
- 修复 `gradle jmh` JVM 参数冲突（移除同时启用 G1GC 与 ZGC，仅保留 `-XX:+UseZGC`）。
- 终止挂起的 Gradle 任务，清理环境后重试。
- 将 `quickBenchmark` 改为直接执行已生成的 JMH 可执行 Jar（Gradle `Exec` 任务），解决 `META-INF/BenchmarkList` 资源缺失问题。
- 运行 `./gradlew quickBenchmark`，生成 CSV：`build/reports/jmh/quick-results.csv`。
- 将 `latencyBenchmark` 与 `throughputBenchmark` 同步改为执行 JMH 可执行 Jar，统一 CSV 输出与参数路径。
- 启动 `throughputBenchmark`，识别并记录 JMH 全局锁问题（`/tmp/jmh.lock`）。
- 总结快速与专项基准的运行方式与路径，提供命令与注意事项（`-Djmh.ignoreLock=true`）。
- **更新Q&A文档v0.2.0**，补充核心技术点实现状态分析、性能测试策略、多源测试方案等内容。
- **核心技术点实现状态评估**：LMAX Disruptor(✅)、内存屏障(✅)、HdrHistogram(✅)、CPU亲和性(⚠️)、缓存行对齐(⚠️)。

### Recently Completed 🔄
- **JMH throughputBenchmark 已完成** ✅
  - 基准测试执行完毕，需要整理和分析结果
  - 单源基线性能指标已生成

### High Priority ⭐
- **整理和分析JMH基线指标** 🆕
  - 分析吞吐量、延迟分布、GC影响等关键指标  
  - 建立性能基线，为后续优化提供对比参照
  - 检查结果文件位置，整理人类可读的性能报告

- **实现CPU亲和性功能**：将关键线程绑定到特定CPU核心，减少上下文切换和缓存失效。
  - 已有依赖：`net.openhft:affinity:3.21ea0`
  - 已有文档：完整的`CpuAffinityManager`实现示例
  - 待集成：在`LockFreePriceEngine`中使用`AffinityThreadFactory`
  
- **实现缓存行对齐优化**：避免false sharing问题，提升多核性能。
  - 已有文档：`@Contended`注解和手动填充示例
  - 待优化：`PriceEvent`类的缓存行对齐
  - 待验证：性能提升效果测量

### Medium Priority 📋
- 基于基线结果，设计多源标记的增量测试方案。

### Low Priority 📝
- 运行 `latencyBenchmark` 全量并记录最新 CSV 指标（带 `-Djmh.ignoreLock=true`）。
- 在聚合处理器实现多源最优价（BBO/NBBO）简版规则与查询接口（`getBestPrice(symbol)`）。
- 引入 HdrHistogram 做延迟分布采样与 P99/P999 报告（文档已提供参考实现）。
- 性能对比实验：不同 `WaitStrategy` 与 `RingBufferSize` 的延迟/吞吐权衡曲线。
- 编写面试速讲稿（图解、术语速通、取舍题、指标截图清单）。

### 核心学习目标完成情况 🎯
1. **LMAX Disruptor**: ✅ 高性能无锁环形队列 - 完全实现
2. **CPU亲和性**: ⚠️ 线程绑定到特定CPU核心 - 依赖就绪，待集成
3. **内存屏障**: ✅ 确保内存操作的顺序性和可见性 - 通过Disruptor实现
4. **缓存行对齐**: ⚠️ 避免false sharing - 文档完整，待实现
5. **HdrHistogram**: ✅ 高精度延迟统计 - 依赖配置，测试中使用

**完成度**: 3/5 完全实现，2/5 待补充（锦上添花的优化）

### Technical Notes 📝
- CSV 输出路径统一：
  - `quickBenchmark` → `build/reports/jmh/quick-results.csv`
  - `latencyBenchmark` → `build/reports/jmh/latency-results.csv`
  - `throughputBenchmark` → `build/reports/jmh/throughput-results.csv`
- 人类可读报告：`build/reports/jmh/human.txt`
- JMH 锁冲突：同机上次未正常退出会占用 `/tmp/jmh.lock`，优先使用 `-Djmh.ignoreLock=true`。

### 多源行情模拟与最优价实验计划 🔬
- 在发布路径设置 `sourceId`（或新增 `publishPrice` 重载）
- 在 JMH/Demo 层模拟多源（单线程标记），保持 `ProducerType.SINGLE`
- `AggregationHandler` 维护 `symbol -> sourceId -> 最新价快照`
- 输出对比：单源 vs 多源 的吞吐/延迟（CSV/human.txt）
- 后续评估 `ProducerType.MULTI` 并行生产者的取舍与影响

### 策略与原则 💡
- **专注核心，逐步迭代**：先摸清核心无锁引擎的性能基线
- **避免过度优化**：CPU亲和性和缓存行对齐是锦上添花，核心无锁并发已足够展示技能
- **数据驱动决策**：基于JMH基准测试结果决定后续优化方向
- **文档先行**：保持文档与代码同步，便于面试准备和技术交流