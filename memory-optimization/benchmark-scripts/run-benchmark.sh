#!/bin/bash

# DirectMemoryStrategy 性能基准测试运行脚本 (Gradle版本)
# 
# 用法：
#   ./run-benchmark.sh              # 运行完整基准测试
#   ./run-benchmark.sh quick        # 运行快速测试
#   ./run-benchmark.sh specific     # 运行特定测试

set -e

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_ROOT")"
cd "$PROJECT_ROOT"

echo "🚀 DirectMemoryStrategy 性能基准测试 (Gradle版)"
echo "=============================================="

# 检查Gradle环境
if ! command -v gradle &> /dev/null; then
    echo "❌ 错误：未找到gradle，请先安装Gradle"
    exit 1
fi

# 检查Java环境
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "📋 Java版本: $JAVA_VERSION"

if [ "$1" = "quick" ]; then
    echo "⚡ 运行快速基准测试（约1-2分钟）..."
    echo "  使用QuickBenchmark，9个方法，简化配置"
    
    # 临时修改build.gradle的includes设置来运行特定benchmark
    sed -i 's|// includes = \['\''.*'\''\].*|includes = [".*QuickBenchmark.*"]|' build.gradle
    
    gradle clean jmh
    
    # 恢复build.gradle的includes设置
    sed -i 's|includes = \[".*QuickBenchmark.*"\]|// includes = ['\''.*'\'']  // Run all benchmarks (this is the default)|' build.gradle
        
elif [ "$1" = "specific" ]; then
    echo "🎯 运行特定基准测试..."
    echo "可选择的测试："
    echo "  1. singleThreadWrite     - 单线程写入性能"
    echo "  2. multiThreadWrite      - 多线程写入性能" 
    echo "  3. singleThreadRead      - 单线程读取性能"
    echo "  4. multiThreadRead       - 多线程读取性能"
    echo "  5. mixedReadWrite        - 混合读写性能"
    echo "  6. batchWrite           - 批量写入性能"
    echo "  7. ringBuffer           - 环形缓冲区性能"
    echo "  8. quick                - 快速验证测试"
    
    read -p "请选择测试类型（输入对应数字）: " choice
    
    case $choice in
        1) pattern=".*singleThreadWrite.*" ;;
        2) pattern=".*multiThreadWrite.*" ;;
        3) pattern=".*singleThreadRead.*" ;;
        4) pattern=".*multiThreadRead.*" ;;
        5) pattern=".*mixedReadWrite.*" ;;
        6) pattern=".*batchWrite.*" ;;
        7) pattern=".*ringBuffer.*" ;;
        8) pattern=".*QuickBenchmark.*" ;;
        *) echo "❌ 无效选择"; exit 1 ;;
    esac
    
    echo "🔥 运行测试: $pattern"
    gradle jmh \
        --include="$pattern" \
        --jvm-args="-Xmx2g -Xms2g" \
        --warmup-iterations=1 \
        --measurement-iterations=2 \
        --forks=1

elif [ "$1" = "validate" ]; then
    echo "🧪 运行功能验证测试（30秒内完成）..."
    
    # 运行JUnit测试验证基本功能
    gradle test --tests "*StrategyValidationTest*"
    
    echo ""
    echo "✅ 功能验证完成！如需性能测试，请运行："
    echo "   ./benchmark-scripts/run-benchmark.sh quick"
        
else
    echo "🔥 运行完整基准测试（预计耗时15-25分钟）..."
    echo "测试场景："
    echo "  ✓ 单线程写入性能对比"
    echo "  ✓ 多线程写入性能对比（8线程）"
    echo "  ✓ 单线程读取性能对比"  
    echo "  ✓ 多线程读取性能对比（8线程）"
    echo "  ✓ 混合读写性能对比（7读:3写）"
    echo "  ✓ 批量操作性能对比"
    echo "  ✓ 环形缓冲区性能对比"
    echo ""
    echo "策略对比："
    echo "  🔒 SynchronizedDirectMemory    - 传统synchronized"
    echo "  ⚡ CASDirectMemory             - 纯CAS无锁实现"
    echo "  📖 ReadWriteLockDirectMemory   - 读写锁分离"
    echo "  🧩 SegmentedLockDirectMemory   - 分段锁实现"
    echo ""
    
    read -p "确认开始完整测试吗？(y/N): " confirm
    if [[ $confirm != [yY] ]]; then
        echo "❌ 测试已取消"
        echo ""
        echo "💡 其他选项："
        echo "   ./benchmark-scripts/run-benchmark.sh quick      # 快速测试（1-2分钟）"
        echo "   ./benchmark-scripts/run-benchmark.sh validate   # 功能验证（30秒）"
        echo "   ./benchmark-scripts/run-benchmark.sh specific   # 特定场景测试"
        exit 0
    fi
    
    # 创建结果目录
    RESULT_DIR="benchmark-results/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$RESULT_DIR"
    
    echo "🔨 清理并编译项目..."
    gradle clean compileJava compileTestJava
    
    # 运行完整基准测试
    echo "⏱️ 开始完整基准测试..."
    echo "📊 将运行所有benchmark文件（预计66个benchmark方法）:"
    echo "  - DirectMemoryStrategyBenchmark (29个方法)"
    echo "  - MemoryOptimizationBenchmark (13个方法)" 
    echo "  - Java21FeaturesBenchmark (15个方法)"
    echo "  - QuickBenchmark (9个方法)"
    echo ""
    
    gradle clean jmh \
        | tee "${RESULT_DIR}/benchmark.log"
    
    echo ""
    echo "✅ 基准测试完成！"
    echo "📊 结果保存在: $RESULT_DIR"
    echo "   - benchmark.log: 完整测试日志"
    echo "   - results.json: JSON格式结果（可用于进一步分析）"
fi

echo ""
echo "📚 性能分析建议："
echo "   1. 单线程场景：关注 singleThreadWrite/Read 结果"
echo "   2. 高并发场景：关注 multiThreadWrite/Read 结果"
echo "   3. 读多写少：关注 ReadWriteLock 在 mixedReadWrite 中的表现"
echo "   4. 写多读少：关注 SegmentedLock 在 multiThreadWrite 中的表现"
echo "   5. 极致性能：关注 CAS 在各场景中的表现"
echo ""
echo "🎉 测试完成！详细分析请查看 PERFORMANCE_ANALYSIS.md"