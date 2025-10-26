# Q&A.md

## 2025-10-26 - v0.1.0 Update

### What Changed
- 梳理问题与解决过程：JMH 资源、JVM 参数冲突、Gradle 任务执行模型调整。
- 记录基准测试运行方式与锁文件处理。

### Technical Questions & Answers
- Q: 为什么 `gradle jmh` 最初失败？
  - A: JVM 参数同时启用 `G1GC` 与 `ZGC` 导致冲突；修复为仅使用 `-XX:+UseZGC`。
- Q: 为什么 `quickBenchmark` 作为 `JavaExec` 会找不到 `META-INF/BenchmarkList`？
  - A: 该资源由 JMH Annotation Processor 在“可执行 Jar”构建时生成；直接 `JavaExec` 运行未使用打包 Jar，导致资源路径缺失。改为 `Exec` 调用已生成的 JMH 可执行 Jar 解决。
- Q: JMH 锁 `Another JMH instance might be running` 如何处理？
  - A: 使用 `-Djmh.ignoreLock=true` 强制继续；或确保上次进程退出后锁文件释放（不建议手动删除，优先参数）。
- Q: 为什么选择 `ZGC`？
  - A: 低暂停特性适合低延迟场景；与 Disruptor 的对象复用策略配合，减少 GC 抖动。不得与 `G1GC` 同时启用。
- Q: 如何保证“最新价快照”读到的是一致的？
  - A: 在聚合处理器维护只读快照结构，发布时确保内存可见性（有序写/`volatile`），查询路径只读，避免锁与阻塞。

### Troubleshooting
- 资源缺失：基准类必须通过 JMH 生成器参与构建，运行应使用打包 Jar。
- 输出路径：统一使用 `build/reports/jmh/*.csv`，避免相对路径差异。
- 长时间运行：吞吐专项可能持续输出，建议在低负载环境运行，并使用 CSV 校验结果。

### Next Steps
- 记录延迟/吞吐专项的最新数据与系统规格（CPU内核、JDK版本）。
- 引入 HdrHistogram 并记录延迟分布（P50/P99/P999）。
- 整理面试问答清单（Disruptor 原理、WaitStrategy 取舍、false sharing、JMM等）。


## 会话问答快照（基于近期对话整理）

- 问：这个项目的典型应用场景是什么？
  - 答：行情归一化与最新价维护、做市/报价引擎的基础、多源聚合与“最优价”选取、事件驱动策略触发、低延迟分发到订阅者，以及性能观测与对比实验。核心目标是让“最新价的生产→加工→查询”变得快速、稳定、简洁。

- 问：引擎的工作流整体是怎样的？
  - 答：从行情源的 Producer 线程群进入 Disruptor `RingBuffer`，经过 `ValidationHandler` 验证，`AggregationHandler` 维护“最新价快照/最优价快照”，`DistributionHandler` 轻量分发到订阅者与外部查询接口。查询通过 `engine.getAggregationHandler().getLatestPrice("EURUSD")` 等只读接口完成。

- 问：实现“无锁”的关键是什么？
  - 答：单写多读（Single Writer）、序列驱动（Sequence）、预分配与对象复用、缓存行填充避免伪共享（false sharing）、最小同步保障可见性，配合可调的 `WaitStrategy` 达到低延迟。

- 问：不同 `WaitStrategy` 有什么影响？
  - 答：`YieldingWaitStrategy` 延迟低、CPU占用中等；`BusySpinWaitStrategy` 极低延迟、CPU占用高；`SleepingWaitStrategy` 延迟较高、CPU占用低；`BlockingWaitStrategy` 延迟高、CPU占用很低。选择取决于延迟目标与资源预算。

- 问：核心数据结构长什么样？
  - 答：`PriceEvent`（复用的事件对象）与 `LatestPriceSnapshot`（只读快照）。设计上强调对象复用、减少分配、缓存行对齐，快照用于低延迟查询。

- 问：多源最优价（BBO/NBBO）如何选取？
  - 答：依据源优先级、延迟阈值、质量权重、价差合理性进行过滤与打分，分别选取最佳买价（Bid）与最佳卖价（Ask）。在 `AggregationHandler` 中维护每源最新价与全局最优价，并在 `selectBest` 中完成过滤与挑选。

- 问：如何验证性能与正确性？
  - 答：运行 `quickBenchmark`、`throughputBenchmark`、`latencyBenchmark` 与全量 `jmh`；对比 `WaitStrategy`、`RingBufferSize` 等参数；建议引入 HdrHistogram 记录延迟分布（P50/P99/P999）。正确性通过快照一致性验证与多源选价的单元测试覆盖。

- 问：常见故障如何排查与修复？
  - 答：JMH 运行失败多因 JVM 参数冲突或 `META-INF/BenchmarkList` 缺失；改用打包的可执行 JMH Jar 运行并统一 GC 参数；并用 `-Djmh.ignoreLock=true` 解决锁文件冲突。资源路径与类路径统一到 Gradle 的执行方式。

- 问：如何保证“最新价快照”的一致性？
  - 答：单写线程更新、读线程只读；利用 JMM 的可见性（`volatile`/发布语义）与有序性；避免写时竞态；减少跨核迁移与假共享带来的不确定性。

- 问：术语速通是什么？
  - 答：`bid` 买价，`ask` 卖价，`spread` 买卖差，`mid` 中间价，`BBO/NBBO` 最优价（单源/多源），`RingBuffer` 为 Disruptor 的核心循环缓冲区。

- 问：面试项目需要掌握哪些内容？
  - 答：并发与内存模型（JMM/CAS/无锁）、Disruptor 原理与事件链、性能测量（JMH/指标/分布）、JVM 与硬件同理心（GC/NUMA/亲和性）、金融最小必备（bid/ask/BBO/NBBO），以及从基准到参数调优的实践路径。

- 问：当前项目的业务价值是什么？
  - 答：为做市/报价与策略提供低延迟、稳定、可观测的“最新价加工与分发”基础设施；支持多源聚合与“最优价”选取；与事件驱动策略触发联动。

- 问：下一步建议与计划是什么？
  - 答：设计“最优价快照接口”并在 `AggregationHandler` 中落地简版 BBO；补充单元测试与延迟分布采样；运行全量基准并记录环境信息；把关键数值整理为对比图表，沉淀成面试速讲稿。