#!/bin/bash

# 策略功能验证脚本 (Gradle版本) - 30秒内完成
# 
# 用法：
#   ./test-strategies.sh

set -e

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_ROOT")"
cd "$PROJECT_ROOT"

echo "🧪 DirectMemoryStrategy 功能验证测试 (Gradle版)"
echo "=============================================="

# 检查Gradle环境
if ! command -v gradle &> /dev/null; then
    echo "❌ 错误：未找到gradle，请先安装Gradle"
    exit 1
fi

echo "🔨 编译测试..."
gradle compileTestJava --quiet

echo "🏃 运行新策略单元测试..."

echo "  🔒 测试 SynchronizedDirectMemory..."
gradle test --tests "SynchronizedDirectMemoryTest" --quiet

echo "  ⚡ 测试 CASDirectMemory..."
gradle test --tests "CASDirectMemoryTest" --quiet

echo "  📖 测试 ReadWriteLockDirectMemory..."
gradle test --tests "ReadWriteLockDirectMemoryTest" --quiet

echo "  🧩 测试 SegmentedLockDirectMemory..."
gradle test --tests "SegmentedLockDirectMemoryTest" --quiet

echo "  🏁 运行策略对比测试..."
gradle test --tests "AllStrategiesComparisonTest" --quiet

echo ""
echo "🎉 所有策略功能验证完成！"
echo ""
echo "💡 如需性能基准测试："
echo "   ./benchmark-scripts/run-benchmark.sh quick      # 快速测试（1-2分钟）"
echo "   ./benchmark-scripts/run-benchmark.sh specific   # 特定场景测试"
echo "   ./benchmark-scripts/run-benchmark.sh            # 完整测试（15-25分钟）"