# DirectMemoryStrategy 基准测试脚本

## 📁 文件结构

```
benchmark-scripts/
├── README.md                 # 本文档
├── run-benchmark.sh         # 主要基准测试脚本（Gradle版）
└── test-strategies.sh       # 快速功能验证脚本
```

## 🚀 快速开始

### 1. 功能验证（30秒）
```bash
# 快速验证4种策略是否正常工作
./benchmark-scripts/test-strategies.sh
```

### 2. 快速性能测试（1-2分钟）
```bash
# 运行简化的性能对比，验证性能差异
./benchmark-scripts/run-benchmark.sh quick
```

### 3. 特定场景测试（2-5分钟）
```bash
# 选择特定场景进行深入测试
./benchmark-scripts/run-benchmark.sh specific
```

### 4. 完整基准测试（15-25分钟）
```bash
# 运行全面的性能基准测试
./benchmark-scripts/run-benchmark.sh
```

## 🎯 测试场景说明

### 快速测试 (QuickBenchmark)
- **配置**: 1次预热，2次测量，每次2秒
- **场景**: 单线程写入 + 4线程并发写入
- **目的**: 快速验证策略间的性能差异
- **耗时**: 约1-2分钟

### 完整测试 (DirectMemoryStrategyBenchmark)
- **配置**: 2次预热，3次测量，每次5秒
- **场景**: 7种测试场景全覆盖
- **目的**: 详细的性能分析和对比
- **耗时**: 约15-25分钟

## 📊 测试场景详解

| 场景 | 描述 | 线程数 | 关键指标 |
|------|------|--------|----------|
| **singleThreadWrite** | 单线程写入基础性能 | 1 | 延迟、吞吐量 |
| **multiThreadWrite** | 多线程写入并发性能 | 8 | 扩展性、锁竞争 |
| **singleThreadRead** | 单线程读取性能 | 1 | 读取延迟 |
| **multiThreadRead** | 多线程读取并发性能 | 8 | 读取扩展性 |
| **mixedReadWrite** | 混合读写（7读:3写） | 8 | 实际场景性能 |
| **batchWrite** | 批量写入优化 | 1 | 批处理效率 |
| **ringBuffer** | 环形缓冲区（HFT特性） | 4 | 内存重用效率 |

## 🔧 Gradle命令参数说明

脚本使用的主要Gradle JMH参数：

```bash
./gradlew jmh \
    --include="*TestPattern*"           # 包含的测试模式
    --jvm-args="-Xmx2g -Xms2g"         # JVM内存设置
    --warmup-iterations=1               # 预热轮数
    --measurement-iterations=2          # 测量轮数  
    --forks=1                          # JVM进程数
    --threads=1,4                      # 线程配置
    --result-format=JSON               # 结果格式
    --result="results.json"            # 结果文件
```

## 📈 结果解读

### 吞吐量模式 (Throughput)
- **单位**: ops/sec（每秒操作数）
- **含义**: 数字越大性能越好
- **关注**: 高并发场景下的处理能力

### 平均延迟模式 (AverageTime)
- **单位**: ns/op（每次操作纳秒数）
- **含义**: 数字越小性能越好
- **关注**: 单次操作的响应时间

## 🎯 预期性能表现

| 场景 | Synchronized | CAS | ReadWriteLock | SegmentedLock |
|------|-------------|-----|---------------|---------------|
| **单线程写入** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| **多线程写入** | ⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **多线程读取** | ⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **混合读写** | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |

## 🛠️ 故障排除

### 常见问题

1. **找不到gradlew**
   ```bash
   # 确保在项目根目录运行
   cd /path/to/memory-optimization
   ./benchmark-scripts/run-benchmark.sh quick
   ```

2. **内存不足**
   ```bash
   # 减少JVM内存配置
   # 编辑脚本中的 -Xmx2g -Xms2g 为更小值
   ```

3. **测试超时**
   ```bash
   # 使用快速测试替代完整测试
   ./benchmark-scripts/run-benchmark.sh quick
   ```

4. **编译错误**
   ```bash
   # 清理并重新编译
   ./gradlew clean build
   ```

### 性能调优建议

1. **JVM参数优化**
   ```bash
   # 低延迟GC
   -XX:+UseG1GC -XX:MaxGCPauseMillis=10
   
   # ZGC（Java 11+）
   -XX:+UnlockExperimentalVMOptions -XX:+UseZGC
   ```

2. **系统调优**
   ```bash
   # CPU绑定
   taskset -c 0-3 ./run-benchmark.sh quick
   
   # 禁用CPU频率调节
   sudo cpupower frequency-set --governor performance
   ```

## 🔗 相关文档

- [PERFORMANCE_ANALYSIS.md](../PERFORMANCE_ANALYSIS.md) - 详细性能分析报告
- [STRATEGY_IMPLEMENTATION_SUMMARY.md](../STRATEGY_IMPLEMENTATION_SUMMARY.md) - 实现总结
- [TODO.md](../TODO.md) - 项目开发计划

## 💡 使用建议

1. **开发阶段**: 使用 `test-strategies.sh` 验证功能
2. **性能验证**: 使用 `run-benchmark.sh quick` 快速对比
3. **深入分析**: 使用 `run-benchmark.sh specific` 针对性测试
4. **完整评估**: 使用 `run-benchmark.sh` 全面基准测试
5. **持续集成**: 在CI/CD中集成快速测试确保性能回归