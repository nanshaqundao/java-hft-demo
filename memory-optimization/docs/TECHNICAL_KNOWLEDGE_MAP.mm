<?xml version="1.0" encoding="UTF-8"?>
<map version="1.0.1">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node CREATED="1706427600000" ID="ID_ROOT" MODIFIED="1706427600000" TEXT="Java HFT Memory Optimization Project">
<font NAME="SansSerif" SIZE="20"/>
<hook NAME="accessories/plugins/AutomaticLayout.properties"/>

<node CREATED="1706427600001" ID="ID_1" MODIFIED="1706427600001" POSITION="right" TEXT="1. 项目架构与核心概念">
<font NAME="SansSerif" SIZE="16"/>
<node CREATED="1706427600002" ID="ID_11" MODIFIED="1706427600002" TEXT="1.1 HFT系统设计理念">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600003" ID="ID_111" MODIFIED="1706427600003" TEXT="延迟可预测性 > 内存利用率">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600004" ID="ID_112" MODIFIED="1706427600004" TEXT="正确性优先，性能优化在后">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600005" ID="ID_113" MODIFIED="1706427600005" TEXT="数据驱动的架构决策">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1706427600006" ID="ID_12" MODIFIED="1706427600006" TEXT="1.2 内存优化策略">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600007" ID="ID_121" MODIFIED="1706427600007" TEXT="Object Pool Pattern - 减少GC压力">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600008" ID="ID_122" MODIFIED="1706427600008" TEXT="Direct Memory Management - 堆外内存">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600009" ID="ID_123" MODIFIED="1706427600009" TEXT="Primitive Collections - 避免装箱开销">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600010" ID="ID_124" MODIFIED="1706427600010" TEXT="Memory Layout Optimization - 紧凑数据结构">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1706427600011" ID="ID_13" MODIFIED="1706427600011" TEXT="1.3 核心组件设计">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600012" ID="ID_131" MODIFIED="1706427600012" TEXT="Order对象 - 64字节对齐，位操作优化">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600013" ID="ID_132" MODIFIED="1706427600013" TEXT="ObjectPool&lt;T&gt; - 泛型对象池，线程安全">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600014" ID="ID_133" MODIFIED="1706427600014" TEXT="OrderCache - Trove4j基础的高性能缓存">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600015" ID="ID_134" MODIFIED="1706427600015" TEXT="DirectMemoryManager → DirectMemoryStrategy (演进)">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="idea"/>
</node>
</node>
</node>

<node CREATED="1706427600016" ID="ID_2" MODIFIED="1706427600016" POSITION="right" TEXT="2. 关键问题发现与解决历程">
<font NAME="SansSerif" SIZE="16"/>
<node CREATED="1706427600017" ID="ID_21" MODIFIED="1706427600017" TEXT="2.1 ObjectPool线程安全问题 (v1.1.0)">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="messagebox_warning"/>
<node CREATED="1706427600018" ID="ID_211" MODIFIED="1706427600018" TEXT="问题：竞态条件导致pool overflow">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600019" ID="ID_2111" MODIFIED="1706427600019" TEXT="症状：多线程下pool.size() > maxSize">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600020" ID="ID_2112" MODIFIED="1706427600020" TEXT="根因：release()方法中的检查与操作非原子">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600021" ID="ID_212" MODIFIED="1706427600021" TEXT="解决方案：CAS操作防止竞态">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="button_ok"/>
<node CREATED="1706427600022" ID="ID_2121" MODIFIED="1706427600022" TEXT="compareAndSet()确保原子性">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600023" ID="ID_2122" MODIFIED="1706427600023" TEXT="do-while循环处理并发冲突">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600024" ID="ID_213" MODIFIED="1706427600024" TEXT="学习点：并发编程中的原子操作重要性">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="idea"/>
</node>
</node>
<node CREATED="1706427600025" ID="ID_22" MODIFIED="1706427600025" TEXT="2.2 DirectMemoryManager缓冲区安全 (v1.1.0)">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="messagebox_warning"/>
<node CREATED="1706427600026" ID="ID_221" MODIFIED="1706427600026" TEXT="问题：多线程IndexOutOfBoundsException">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600027" ID="ID_2211" MODIFIED="1706427600027" TEXT="症状：随机性缓冲区越界">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600028" ID="ID_2212" MODIFIED="1706427600028" TEXT="根因：volatile position的读-改-写竞态">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600029" ID="ID_222" MODIFIED="1706427600029" TEXT="解决方案：AtomicInteger position管理">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="button_ok"/>
<node CREATED="1706427600030" ID="ID_2221" MODIFIED="1706427600030" TEXT="getAndAdd()原子位置预留">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600031" ID="ID_2222" MODIFIED="1706427600031" TEXT="synchronized方法防止并发写入">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600032" ID="ID_223" MODIFIED="1706427600032" TEXT="学习点：volatile vs AtomicXxx的区别">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="idea"/>
</node>
</node>
<node CREATED="1706427600033" ID="ID_23" MODIFIED="1706427600033" TEXT="2.3 对象生命周期Bug (v1.2.0) 🔥关键问题">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="stop"/>
<node CREATED="1706427600034" ID="ID_231" MODIFIED="1706427600034" TEXT="问题：缓存数据损坏">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600035" ID="ID_2311" MODIFIED="1706427600035" TEXT="症状：缓存中Order对象数据异常">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600036" ID="ID_2312" MODIFIED="1706427600036" TEXT="根因：缓存存储对象引用，对象被pool重置">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600037" ID="ID_2313" MODIFIED="1706427600037" TEXT="影响：数据完整性严重威胁">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600038" ID="ID_232" MODIFIED="1706427600038" TEXT="解决方案：分离临时对象与持久对象">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="button_ok"/>
<node CREATED="1706427600039" ID="ID_2321" MODIFIED="1706427600039" TEXT="临时对象：从pool获取，用于处理">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600040" ID="ID_2322" MODIFIED="1706427600040" TEXT="持久对象：独立拷贝，用于缓存">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600041" ID="ID_2323" MODIFIED="1706427600041" TEXT="copy构造函数和copyFrom方法">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600042" ID="ID_233" MODIFIED="1706427600042" TEXT="学习点：对象共享vs对象拷贝的权衡">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="idea"/>
</node>
</node>
<node CREATED="1706427600043" ID="ID_24" MODIFIED="1706427600043" TEXT="2.4 线程安全优化 (v1.3.0)">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="messagebox_warning"/>
<node CREATED="1706427600044" ID="ID_241" MODIFIED="1706427600044" TEXT="问题：synchronized + CAS双重保护开销">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600045" ID="ID_2411" MODIFIED="1706427600045" TEXT="症状：性能开销但无额外安全收益">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600046" ID="ID_2412" MODIFIED="1706427600046" TEXT="根因：设计过度保守">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600047" ID="ID_242" MODIFIED="1706427600047" TEXT="解决方案：简化为纯synchronized">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="button_ok"/>
<node CREATED="1706427600048" ID="ID_2421" MODIFIED="1706427600048" TEXT="移除AtomicInteger，使用int">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600049" ID="ID_2422" MODIFIED="1706427600049" TEXT="原子写入机制：两阶段提交">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600050" ID="ID_2423" MODIFIED="1706427600050" TEXT="HFT优化：环形缓冲区简单重置">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600051" ID="ID_243" MODIFIED="1706427600051" TEXT="学习点：性能优化需要测量而非假设">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="idea"/>
</node>
</node>
</node>

<node CREATED="1706427600052" ID="ID_3" MODIFIED="1706427600052" POSITION="right" TEXT="3. DirectMemoryStrategy演进 (v1.4.0核心突破)">
<font NAME="SansSerif" SIZE="16" BOLD="true"/>
<icon BUILTIN="bell"/>
<node CREATED="1706427600053" ID="ID_31" MODIFIED="1706427600053" TEXT="3.1 从妥协到科学验证的转变">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="idea"/>
<node CREATED="1706427600054" ID="ID_311" MODIFIED="1706427600054" TEXT="妥协阶段：选择synchronized，放弃性能探索">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600055" ID="ID_312" MODIFIED="1706427600055" TEXT="转变契机：需要验证想法和性能表现">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600056" ID="ID_313" MODIFIED="1706427600056" TEXT="科学验证：实现多种策略，数据驱动选择">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1706427600057" ID="ID_32" MODIFIED="1706427600057" TEXT="3.2 DirectMemoryStrategy接口设计">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600058" ID="ID_321" MODIFIED="1706427600058" TEXT="统一API：4种实现的一致接口">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600059" ID="ID_322" MODIFIED="1706427600059" TEXT="核心方法：serializeOrder, deserializeOrder">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600060" ID="ID_323" MODIFIED="1706427600060" TEXT="状态查询：getUsedSize, getRemainingSize">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600061" ID="ID_324" MODIFIED="1706427600061" TEXT="元数据：getStrategyName策略识别">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1706427600062" ID="ID_33" MODIFIED="1706427600062" TEXT="3.3 四种并发策略实现">
<font NAME="SansSerif" SIZE="14" BOLD="true"/>
<node CREATED="1706427600063" ID="ID_331" MODIFIED="1706427600063" TEXT="3.3.1 SynchronizedDirectMemory (基线策略)">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="full-1"/>
<node CREATED="1706427600064" ID="ID_3311" MODIFIED="1706427600064" TEXT="实现方式：传统synchronized关键字">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600065" ID="ID_3312" MODIFIED="1706427600065" TEXT="适用场景：中等并发，简单可靠">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600066" ID="ID_3313" MODIFIED="1706427600066" TEXT="优势：实现简单，无复杂并发问题">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600067" ID="ID_3314" MODIFIED="1706427600067" TEXT="劣势：所有操作串行化，扩展性差">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600068" ID="ID_332" MODIFIED="1706427600068" TEXT="3.3.2 CASDirectMemory (无锁策略)">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="full-2"/>
<node CREATED="1706427600069" ID="ID_3321" MODIFIED="1706427600069" TEXT="实现方式：CompareAndSwap无锁操作">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600070" ID="ID_3322" MODIFIED="1706427600070" TEXT="核心技术">
<font NAME="SansSerif" SIZE="10"/>
<node CREATED="1706427600071" ID="ID_33221" MODIFIED="1706427600071" TEXT="AtomicInteger版本号防ABA问题">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600072" ID="ID_33222" MODIFIED="1706427600072" TEXT="AtomicInteger位置管理">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600073" ID="ID_33223" MODIFIED="1706427600073" TEXT="指数退避重试机制">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600074" ID="ID_33224" MODIFIED="1706427600074" TEXT="版本一致性检查">
<font NAME="SansSerif" SIZE="9"/>
</node>
</node>
<node CREATED="1706427600075" ID="ID_3323" MODIFIED="1706427600075" TEXT="适用场景：高并发写入，追求极致性能">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600076" ID="ID_3324" MODIFIED="1706427600076" TEXT="优势：无线程阻塞，理论性能上限高">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600077" ID="ID_3325" MODIFIED="1706427600077" TEXT="挑战：ABA问题，重试开销，实现复杂">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600078" ID="ID_333" MODIFIED="1706427600078" TEXT="3.3.3 ReadWriteLockDirectMemory (读写分离)">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="full-3"/>
<node CREATED="1706427600079" ID="ID_3331" MODIFIED="1706427600079" TEXT="实现方式：ReentrantReadWriteLock">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600080" ID="ID_3332" MODIFIED="1706427600080" TEXT="核心特性">
<font NAME="SansSerif" SIZE="10"/>
<node CREATED="1706427600081" ID="ID_33321" MODIFIED="1706427600081" TEXT="并发读取：多线程同时读取">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600082" ID="ID_33322" MODIFIED="1706427600082" TEXT="独占写入：写入时阻塞所有读写">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600083" ID="ID_33323" MODIFIED="1706427600083" TEXT="批量优化：减少锁获取频次">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600084" ID="ID_33324" MODIFIED="1706427600084" TEXT="智能锁选择：读多写少优化">
<font NAME="SansSerif" SIZE="9"/>
</node>
</node>
<node CREATED="1706427600085" ID="ID_3333" MODIFIED="1706427600085" TEXT="适用场景：读多写少（70%读取以上）">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600086" ID="ID_3334" MODIFIED="1706427600086" TEXT="优势：读取并发性能最佳">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600087" ID="ID_3335" MODIFIED="1706427600087" TEXT="注意：写锁可能饥饿，需要监控">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600088" ID="ID_334" MODIFIED="1706427600088" TEXT="3.3.4 SegmentedLockDirectMemory (分段并发)">
<font NAME="SansSerif" SIZE="12"/>
<icon BUILTIN="full-4"/>
<node CREATED="1706427600089" ID="ID_3341" MODIFIED="1706427600089" TEXT="实现方式：16个ReentrantLock分段">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600090" ID="ID_3342" MODIFIED="1706427600090" TEXT="核心技术">
<font NAME="SansSerif" SIZE="10"/>
<node CREATED="1706427600091" ID="ID_33421" MODIFIED="1706427600091" TEXT="Hash分段：基于对象哈希选择段">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600092" ID="ID_33422" MODIFIED="1706427600092" TEXT="段独立：不同段可并行操作">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600093" ID="ID_33423" MODIFIED="1706427600093" TEXT="负载均衡：段间负载分布">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600094" ID="ID_33424" MODIFIED="1706427600094" TEXT="局部重置：按段管理环形缓冲">
<font NAME="SansSerif" SIZE="9"/>
</node>
</node>
<node CREATED="1706427600095" ID="ID_3343" MODIFIED="1706427600095" TEXT="适用场景：高并发写入密集">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600096" ID="ID_3344" MODIFIED="1706427600096" TEXT="优势：写入并发扩展性最佳">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600097" ID="ID_3345" MODIFIED="1706427600097" TEXT="权衡：内存开销大，读取需要定位段">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
</node>
<node CREATED="1706427600098" ID="ID_34" MODIFIED="1706427600098" TEXT="3.4 策略选择决策树">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="idea"/>
<node CREATED="1706427600099" ID="ID_341" MODIFIED="1706427600099" TEXT="单线程/低并发 → SynchronizedDirectMemory">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600100" ID="ID_342" MODIFIED="1706427600100" TEXT="高并发写入密集 → SegmentedLockDirectMemory">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600101" ID="ID_343" MODIFIED="1706427600101" TEXT="读多写少（70%+读取）→ ReadWriteLockDirectMemory">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600102" ID="ID_344" MODIFIED="1706427600102" TEXT="极致性能追求 → CASDirectMemory">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
</node>

<node CREATED="1706427600103" ID="ID_4" MODIFIED="1706427600103" POSITION="left" TEXT="4. 测试框架与质量保证">
<font NAME="SansSerif" SIZE="16"/>
<node CREATED="1706427600104" ID="ID_41" MODIFIED="1706427600104" TEXT="4.1 单元测试架构 (28个测试用例)">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="list"/>
<node CREATED="1706427600105" ID="ID_411" MODIFIED="1706427600105" TEXT="4.1.1 设计模式：Template Method">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600106" ID="ID_4111" MODIFIED="1706427600106" TEXT="DirectMemoryStrategyTestBase抽象基类">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600107" ID="ID_4112" MODIFIED="1706427600107" TEXT="12个通用测试方法">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600108" ID="ID_4113" MODIFIED="1706427600108" TEXT="createStrategy()抽象工厂方法">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600109" ID="ID_412" MODIFIED="1706427600109" TEXT="4.1.2 策略特化测试">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600110" ID="ID_4121" MODIFIED="1706427600110" TEXT="CASDirectMemoryTest">
<font NAME="SansSerif" SIZE="10"/>
<node CREATED="1706427600111" ID="ID_41211" MODIFIED="1706427600111" TEXT="testHighConcurrencyWrites：16线程重试验证">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600112" ID="ID_41212" MODIFIED="1706427600112" TEXT="testVersionConsistency：ABA问题防护">
<font NAME="SansSerif" SIZE="9"/>
</node>
</node>
<node CREATED="1706427600113" ID="ID_4122" MODIFIED="1706427600113" TEXT="ReadWriteLockDirectMemoryTest">
<font NAME="SansSerif" SIZE="10"/>
<node CREATED="1706427600114" ID="ID_41221" MODIFIED="1706427600114" TEXT="testConcurrentReadsPerformance：20线程并发读">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600115" ID="ID_41222" MODIFIED="1706427600115" TEXT="testBatchWriteOptimization：批量写入优化">
<font NAME="SansSerif" SIZE="9"/>
</node>
</node>
<node CREATED="1706427600116" ID="ID_4123" MODIFIED="1706427600116" TEXT="SegmentedLockDirectMemoryTest">
<font NAME="SansSerif" SIZE="10"/>
<node CREATED="1706427600117" ID="ID_41231" MODIFIED="1706427600117" TEXT="testLoadBalancingAcrossSegments：负载均衡">
<font NAME="SansSerif" SIZE="9"/>
</node>
<node CREATED="1706427600118" ID="ID_41232" MODIFIED="1706427600118" TEXT="testSegmentedConcurrentWrites：分段并发">
<font NAME="SansSerif" SIZE="9"/>
</node>
</node>
</node>
<node CREATED="1706427600119" ID="ID_413" MODIFIED="1706427600119" TEXT="4.1.3 对比测试">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600120" ID="ID_4131" MODIFIED="1706427600120" TEXT="AllStrategiesComparisonTest：4策略同条件对比">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600121" ID="ID_4132" MODIFIED="1706427600121" TEXT="重要修复：SegmentedLock 50%成功率阈值">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600122" ID="ID_4133" MODIFIED="1706427600122" TEXT="性能基线：相同负载下的表现差异">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600123" ID="ID_414" MODIFIED="1706427600123" TEXT="4.1.4 快速验证">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600124" ID="ID_4141" MODIFIED="1706427600124" TEXT="QuickValidationTest：30秒功能检查">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600125" ID="ID_4142" MODIFIED="1706427600125" TEXT="CI/CD集成：适合持续集成流水线">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
</node>
<node CREATED="1706427600126" ID="ID_42" MODIFIED="1706427600126" TEXT="4.2 JMH性能测试框架 (66个benchmark方法)">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="hourglass"/>
<node CREATED="1706427600127" ID="ID_421" MODIFIED="1706427600127" TEXT="4.2.1 主要策略对比 (DirectMemoryStrategyBenchmark - 29方法)">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600128" ID="ID_4211" MODIFIED="1706427600128" TEXT="7个测试场景 × 4种策略">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600129" ID="ID_4212" MODIFIED="1706427600129" TEXT="单线程写入/读取性能基线">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600130" ID="ID_4213" MODIFIED="1706427600130" TEXT="多线程并发性能扩展性">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600131" ID="ID_4214" MODIFIED="1706427600131" TEXT="混合读写负载真实场景">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600132" ID="ID_4215" MODIFIED="1706427600132" TEXT="批量操作优化效果">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600133" ID="ID_4216" MODIFIED="1706427600133" TEXT="环形缓冲区HFT特性">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600134" ID="ID_422" MODIFIED="1706427600134" TEXT="4.2.2 快速验证基准 (QuickBenchmark - 9方法)">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600135" ID="ID_4221" MODIFIED="1706427600135" TEXT="1-2分钟快速性能检查">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600136" ID="ID_4222" MODIFIED="1706427600136" TEXT="关键性能指标验证">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600137" ID="ID_423" MODIFIED="1706427600137" TEXT="4.2.3 组件级基准 (28方法)">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600138" ID="ID_4231" MODIFIED="1706427600138" TEXT="MemoryOptimizationBenchmark：底层组件">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600139" ID="ID_4232" MODIFIED="1706427600139" TEXT="Java21FeaturesBenchmark：新特性验证">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600140" ID="ID_424" MODIFIED="1706427600140" TEXT="4.2.4 JMH配置优化">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600141" ID="ID_4241" MODIFIED="1706427600141" TEXT="Gradle插件集成">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600142" ID="ID_4242" MODIFIED="1706427600142" TEXT="预热2轮，测量3轮，1个fork">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600143" ID="ID_4243" MODIFIED="1706427600143" TEXT="CSV和human-readable输出">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600144" ID="ID_4244" MODIFIED="1706427600144" TEXT="配置问题修复：移除restrictive includes">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
</node>
<node CREATED="1706427600145" ID="ID_43" MODIFIED="1706427600145" TEXT="4.3 测试自动化">
<font NAME="SansSerif" SIZE="14"/>
<icon BUILTIN="executable"/>
<node CREATED="1706427600146" ID="ID_431" MODIFIED="1706427600146" TEXT="test-strategies.sh：30秒功能验证">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600147" ID="ID_432" MODIFIED="1706427600147" TEXT="run-benchmark.sh：多模式性能测试">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600148" ID="ID_433" MODIFIED="1706427600148" TEXT="list-benchmarks.sh：测试方法清单">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600149" ID="ID_434" MODIFIED="1706427600149" TEXT="Gradle集成：统一构建和测试流程">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
</node>

<node CREATED="1706427600150" ID="ID_5" MODIFIED="1706427600150" POSITION="left" TEXT="5. 性能优化学习要点">
<font NAME="SansSerif" SIZE="16"/>
<node CREATED="1706427600151" ID="ID_51" MODIFIED="1706427600151" TEXT="5.1 并发编程核心概念">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600152" ID="ID_511" MODIFIED="1706427600152" TEXT="volatile vs AtomicXxx">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600153" ID="ID_5111" MODIFIED="1706427600153" TEXT="volatile：可见性，非原子性">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600154" ID="ID_5112" MODIFIED="1706427600154" TEXT="AtomicXxx：原子操作，CAS基础">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600155" ID="ID_5113" MODIFIED="1706427600155" TEXT="应用：counter++需要AtomicLong">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600156" ID="ID_512" MODIFIED="1706427600156" TEXT="CAS操作与ABA问题">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600157" ID="ID_5121" MODIFIED="1706427600157" TEXT="Compare-And-Swap原理">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600158" ID="ID_5122" MODIFIED="1706427600158" TEXT="ABA问题：A→B→A的值变化掩盖">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600159" ID="ID_5123" MODIFIED="1706427600159" TEXT="解决：版本号/引用标记">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600160" ID="ID_513" MODIFIED="1706427600160" TEXT="锁的类型与选择">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600161" ID="ID_5131" MODIFIED="1706427600161" TEXT="synchronized：JVM内置，简单可靠">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600162" ID="ID_5132" MODIFIED="1706427600162" TEXT="ReentrantLock：可中断，公平性选择">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600163" ID="ID_5133" MODIFIED="1706427600163" TEXT="ReadWriteLock：读写分离优化">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600164" ID="ID_5134" MODIFIED="1706427600164" TEXT="无锁：最高性能，最高复杂度">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600165" ID="ID_514" MODIFIED="1706427600165" TEXT="内存模型与happens-before">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600166" ID="ID_5141" MODIFIED="1706427600166" TEXT="指令重排序影响">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600167" ID="ID_5142" MODIFIED="1706427600167" TEXT="内存可见性保证">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600168" ID="ID_5143" MODIFIED="1706427600168" TEXT="同步边界建立">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
</node>
<node CREATED="1706427600169" ID="ID_52" MODIFIED="1706427600169" TEXT="5.2 性能测量与分析">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600170" ID="ID_521" MODIFIED="1706427600170" TEXT="JMH基准测试最佳实践">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600171" ID="ID_5211" MODIFIED="1706427600171" TEXT="预热的重要性：JIT编译优化">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600172" ID="ID_5212" MODIFIED="1706427600172" TEXT="多轮测量：减少测量误差">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600173" ID="ID_5213" MODIFIED="1706427600173" TEXT="进程隔离：避免互相影响">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600174" ID="ID_5214" MODIFIED="1706427600174" TEXT="黑洞消费：防止死代码消除">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600175" ID="ID_522" MODIFIED="1706427600175" TEXT="性能指标选择">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600176" ID="ID_5221" MODIFIED="1706427600176" TEXT="延迟：50th, 95th, 99th, 99.9th百分位">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600177" ID="ID_5222" MODIFIED="1706427600177" TEXT="吞吐量：ops/sec">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600178" ID="ID_5223" MODIFIED="1706427600178" TEXT="扩展性：多线程下的性能保持">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600179" ID="ID_5224" MODIFIED="1706427600179" TEXT="资源使用：CPU、内存、GC影响">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600180" ID="ID_523" MODIFIED="1706427600180" TEXT="瓶颈识别方法">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600181" ID="ID_5231" MODIFIED="1706427600181" TEXT="锁竞争分析">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600182" ID="ID_5232" MODIFIED="1706427600182" TEXT="CPU缓存命中率">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600183" ID="ID_5233" MODIFIED="1706427600183" TEXT="GC影响评估">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
</node>
<node CREATED="1706427600184" ID="ID_53" MODIFIED="1706427600184" TEXT="5.3 高频交易系统特殊考虑">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600185" ID="ID_531" MODIFIED="1706427600185" TEXT="延迟可预测性">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600186" ID="ID_5311" MODIFIED="1706427600186" TEXT="最大延迟 vs 平均延迟">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600187" ID="ID_5312" MODIFIED="1706427600187" TEXT="延迟尖刺的控制">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600188" ID="ID_5313" MODIFIED="1706427600188" TEXT="实时系统要求">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600189" ID="ID_532" MODIFIED="1706427600189" TEXT="内存管理策略">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600190" ID="ID_5321" MODIFIED="1706427600190" TEXT="避免GC：对象池、堆外内存">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600191" ID="ID_5322" MODIFIED="1706427600191" TEXT="预分配：避免运行时分配">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600192" ID="ID_5323" MODIFIED="1706427600192" TEXT="内存局部性：缓存友好访问">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600193" ID="ID_533" MODIFIED="1706427600193" TEXT="系统调优">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600194" ID="ID_5331" MODIFIED="1706427600194" TEXT="JVM参数优化">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600195" ID="ID_5332" MODIFIED="1706427600195" TEXT="操作系统调优">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600196" ID="ID_5333" MODIFIED="1706427600196" TEXT="硬件亲和性设置">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
</node>
</node>

<node CREATED="1706427600197" ID="ID_6" MODIFIED="1706427600197" POSITION="left" TEXT="6. 工具链与开发流程">
<font NAME="SansSerif" SIZE="16"/>
<node CREATED="1706427600198" ID="ID_61" MODIFIED="1706427600198" TEXT="6.1 构建系统演进">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600199" ID="ID_611" MODIFIED="1706427600199" TEXT="Maven → Gradle迁移">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600200" ID="ID_6111" MODIFIED="1706427600200" TEXT="原因：更好的JMH集成支持">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600201" ID="ID_6112" MODIFIED="1706427600201" TEXT="Gradle JMH插件配置">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600202" ID="ID_6113" MODIFIED="1706427600202" TEXT="脚本兼容性调整">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
<node CREATED="1706427600203" ID="ID_612" MODIFIED="1706427600203" TEXT="Java版本选择：Java 21 LTS">
<font NAME="SansSerif" SIZE="12"/>
<node CREATED="1706427600204" ID="ID_6121" MODIFIED="1706427600204" TEXT="Virtual Threads支持">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600205" ID="ID_6122" MODIFIED="1706427600205" TEXT="Pattern Matching增强">
<font NAME="SansSerif" SIZE="10"/>
</node>
<node CREATED="1706427600206" ID="ID_6123" MODIFIED="1706427600206" TEXT="Records不可变数据类">
<font NAME="SansSerif" SIZE="10"/>
</node>
</node>
</node>
<node CREATED="1706427600207" ID="ID_62" MODIFIED="1706427600207" TEXT="6.2 测试工具链">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600208" ID="ID_621" MODIFIED="1706427600208" TEXT="JUnit 5：单元测试框架">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600209" ID="ID_622" MODIFIED="1706427600209" TEXT="JMH 1.37：性能基准测试">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600210" ID="ID_623" MODIFIED="1706427600210" TEXT="Gradle Test：集成测试执行">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600211" ID="ID_624" MODIFIED="1706427600211" TEXT="自动化脚本：便捷测试执行">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1706427600212" ID="ID_63" MODIFIED="1706427600212" TEXT="6.3 文档与知识管理">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600213" ID="ID_631" MODIFIED="1706427600213" TEXT="README.md：项目概览和使用指南">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600214" ID="ID_632" MODIFIED="1706427600214" TEXT="docs/目录：分析和技术文档">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600215" ID="ID_633" MODIFIED="1706427600215" TEXT="Q&amp;A.md：问题发现和解决记录">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600216" ID="ID_634" MODIFIED="1706427600216" TEXT="TODO.md：开发计划和完成记录">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600217" ID="ID_635" MODIFIED="1706427600217" TEXT="本文件：技术知识图谱和学习轨迹">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
</node>

<node CREATED="1706427600218" ID="ID_7" MODIFIED="1706427600218" POSITION="left" TEXT="7. 未来发展方向">
<font NAME="SansSerif" SIZE="16"/>
<icon BUILTIN="forward"/>
<node CREATED="1706427600219" ID="ID_71" MODIFIED="1706427600219" TEXT="7.1 技术深化">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600220" ID="ID_711" MODIFIED="1706427600220" TEXT="动态内存分配：自适应缓冲区扩展">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600221" ID="ID_712" MODIFIED="1706427600221" TEXT="混合策略：负载自适应策略切换">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600222" ID="ID_713" MODIFIED="1706427600222" TEXT="无锁数据结构：更复杂的lock-free算法">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600223" ID="ID_714" MODIFIED="1706427600223" TEXT="NUMA优化：内存访问局部性优化">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1706427600224" ID="ID_72" MODIFIED="1706427600224" TEXT="7.2 系统集成">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600225" ID="ID_721" MODIFIED="1706427600225" TEXT="完整交易系统：订单匹配引擎集成">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600226" ID="ID_722" MODIFIED="1706427600226" TEXT="网络优化：零拷贝、用户态网络栈">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600227" ID="ID_723" MODIFIED="1706427600227" TEXT="监控系统：实时性能指标收集">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600228" ID="ID_724" MODIFIED="1706427600228" TEXT="分布式扩展：跨机器内存管理">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
<node CREATED="1706427600229" ID="ID_73" MODIFIED="1706427600229" TEXT="7.3 学习路径">
<font NAME="SansSerif" SIZE="14"/>
<node CREATED="1706427600230" ID="ID_731" MODIFIED="1706427600230" TEXT="深入并发：无锁编程、内存模型">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600231" ID="ID_732" MODIFIED="1706427600231" TEXT="系统调优：JVM、OS、硬件优化">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600232" ID="ID_733" MODIFIED="1706427600232" TEXT="架构设计：大规模系统设计">
<font NAME="SansSerif" SIZE="12"/>
</node>
<node CREATED="1706427600233" ID="ID_734" MODIFIED="1706427600233" TEXT="领域知识：金融系统、实时计算">
<font NAME="SansSerif" SIZE="12"/>
</node>
</node>
</node>

</node>
</map>