# TECHNICAL_KNOWLEDGE_MAP.md

## Concurrency & Memory
- JMM：happens-before、fences、volatile
- CAS 与原子类：ABA、重试、无锁设计
- False sharing：缓存行对齐、@Contended

## Disruptor
- RingBuffer、Sequence、ProducerType
- EventHandler 链：Validation → Aggregation → Distribution
- WaitStrategy：Yielding/BusySpin/Sleeping/Blocking 的延迟/CPU权衡

## Performance & Benchmarking
- JMH：Warmup/Measurement/Fork/Blackhole/@State
- 指标：ops/s、ns/op、P50/P99/P999（HdrHistogram）
- 实验方法：参数对比、环境稳定、避免 I/O 干扰

## JVM & Hardware Sympathy
- GC：ZGC 低暂停，避免与 G1 冲突
- 亲和性与 NUMA：绑定核心、减少迁移与抖动
- Safepoint、TLAB、逃逸分析

## Financial Basics
- bid/ask、spread、mid 的含义
- BBO/NBBO：多源最优价选取，源优先级、延迟阈值、质量权重

## Project Practices
- Exec 运行 JMH Jar 保证资源完整
- 统一 CSV 输出路径与报告（human.txt）
- 快照查询接口与只读访问设计