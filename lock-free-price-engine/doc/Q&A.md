# Q&A - 技术问答记录

## 当前阶段主要问答

### Q: JMH测试中CSV文件的生成和位置问题  
**你的问题**: "我想问问jmh测试在哪里，运行结果能否像memory-optimization一样的输出文件。"

**我的解答**: JMH测试在`src/jmh/java`目录下，运行结果输出到`build/reports/jmh/`：
- `results.csv` - 结构化数据
- `human.txt` - 人类可读报告
- `quick-results.csv` - 快速测试结果
- `latency-results.csv` - 延迟专项结果
- `throughput-results.csv` - 吞吐量专项结果

### Q: 核心学习目标的完成情况
**你的问题**: "lock-free-price-engine 也就是项目2已经生成。请你和目标对比一下，是不是第二和第四条还没有实现？"

**我的分析**: 确实，5个核心目标中有2个未实现：
1. ✅ LMAX Disruptor - 完全实现
2. ⚠️ CPU亲和性 - 依赖已添加，代码未集成
3. ✅ 内存屏障 - 通过Disruptor实现
4. ⚠️ 缓存行对齐 - 文档已有，PriceEvent未优化
5. ✅ HdrHistogram - 依赖已配置

缺失的是CPU亲和性和缓存行对齐这两个性能优化特性。

### Q: 项目实现策略和节奏
**你的观点**: "先不着急动代码"，"目前还是讨论阶段和文档阶段"

**我的理解**: 采用"文档先行"的工程思维，先完善计划和分析，避免"一下子考虑太多反而会束手束脚"。这体现了专注核心、逐步迭代的策略。

## 技术深度问答

### 无锁并发核心
**Q: 实现"无锁"的关键是什么？**
**A**: 单写多读（Single Writer）、CAS操作、内存屏障、对象复用、缓存行对齐避免false sharing，配合合适的WaitStrategy达到低延迟。

**Q: 不同WaitStrategy的影响？**
**A**: 
- `YieldingWaitStrategy` - 延迟低、CPU中等
- `BusySpinWaitStrategy` - 极低延迟、CPU高
- `SleepingWaitStrategy` - 延迟高、CPU低  
- `BlockingWaitStrategy` - 延迟最高、CPU最低

### 架构设计
**Q: 引擎的工作流程？**
**A**: Producer → RingBuffer → ValidationHandler → AggregationHandler → DistributionHandler，查询通过只读接口`getLatestPrice()`完成。

**Q: 如何保证最新价快照的一致性？**
**A**: 单写线程更新、读线程只读，利用volatile/发布语义保证可见性和有序性。

### 性能测试
**Q: 当前测试的层次？**
**A**: 核心引擎的微基准测试，测试`publishPrice()`的吞吐量和端到端延迟，验证Disruptor架构性能上限。

**Q: 目标性能指标？**
**A**: 端到端延迟<10μs（P99.9），吞吐量>100万TPS。

## 工程实践问答

### JMH使用技巧
**Q: JMH测试时间太长怎么办？**
**A**: 修改build.gradle的`includes`配置指定单个测试，可从数小时缩短到1-2分钟。

**Q: JMH锁冲突怎么解决？**
**A**: 使用`-Djmh.ignoreLock=true`参数强制继续执行。

### 文档组织
**Q: Q&A文档的价值定位？**
**A**: 不仅是技术知识点罗列，更重要的是记录真实对话过程和思考路径，帮助重现问题场景和解决思路。

## 下一步计划

基于当前差距分析，优先级如下：
1. **高优先级**: 实现CPU亲和性功能
2. **中优先级**: 实现缓存行对齐优化
3. **低优先级**: 完善HdrHistogram集成和多源聚合功能

---

*记录真实的技术讨论过程，为后续开发和面试准备提供参考*