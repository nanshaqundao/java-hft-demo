# STRATEGY_IMPLEMENTATION_SUMMARY.md

## 2025-10-26 - v0.1.0 Architecture Evolution

### Problem → Solution Journey
- 问题：`gradle jmh` 执行失败（GC 参数冲突），以及专用任务 `quickBenchmark` 缺少 `BenchmarkList`。
- 方案：统一 GC 参数为 `ZGC`；将各专项基准任务改为执行 JMH 可执行 Jar（Gradle `Exec`）。
- 效果：快速与专项基准均可运行并输出 CSV；路径统一、资源可用。

### Trade-offs & Decisions
- 选择 Disruptor：低延迟与稳定性优于常见并发队列；预分配事件、无锁序列推进。
- 等待策略：默认选择 `YieldingWaitStrategy`，以延迟为优；在极端低延迟可试 `BusySpin`，在节能场景考虑 `Sleeping/Blocking`。
- GC 策略：选择 `ZGC` 以降低暂停；避免与 `G1GC` 同时存在的冲突。
- 运行模型：打包执行 Jar 保证 JMH 资源完整，降低类路径不一致带来的不确定性。

### Business Value & Technical Impact
- 形成可复用的“低延迟价格生产线”，典型场景涵盖行情归一化、做市基础、最优价聚合与分发。
- 提供标准化基准与报告路径，便于面试展示与后续迭代。

### Lessons Learned & Anti-Patterns
- 基准必须使用正确的运行模型（JMH 执行 Jar），否则资源丢失导致不可用。
- JVM 参数冲突会让问题难以定位，应统一并最小化配置，先保证稳定性。
- I/O 或阻塞逻辑不应在处理链主路径中执行，避免延迟抖动。

### Next Steps
- 聚合器实现多源最优价（BBO/NBBO）与查询接口，准备面试演示样例。
- 用 JMH 形成延迟/吞吐的参数化对比图表，支撑取舍说明。