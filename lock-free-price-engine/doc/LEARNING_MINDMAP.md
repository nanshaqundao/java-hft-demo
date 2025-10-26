# LEARNING_MINDMAP.md

## 2025-10-26 - Learning Journey Snapshot

### Questions → Understanding
- 为什么 Disruptor 更稳？（单写多读、序列推进、预分配、等待策略可调）
- JMH 资源为何缺失？（执行 Jar 生成 `BenchmarkList`）
- 低延迟如何避免 GC 抖动？（对象复用、ZGC、减少分配）

### Misconceptions & Corrections
- 认为 `JavaExec` 即可运行基准 → 实际需要执行打包 Jar。
- 认为平均时间足够衡量延迟 → 需要分布（P99/P999）。

### Aha Moments
- `-Djmh.ignoreLock=true` 解决并发锁文件阻塞，显著提升运行稳定性。
- 统一 GC 参数后，很多“玄学问题”自然消失。

### Resources
- LMAX Disruptor 官方文档、JMH 用户指南、Java 内存模型（JLS 17）

### Next Steps
- 形成面试速讲稿与图解，串联原理与数据。
- 用专项基准对不同参数做对比图。