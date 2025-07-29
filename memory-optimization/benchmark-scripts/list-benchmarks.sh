#!/bin/bash

# 列出所有可用的JMH基准测试
# 
# 用法：
#   ./list-benchmarks.sh

set -e

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_ROOT")"
cd "$PROJECT_ROOT"

echo "📊 查找所有JMH基准测试方法..."
echo "=================================="

# 检查Gradle环境
if ! command -v gradle &> /dev/null; then
    echo "❌ 错误：未找到gradle"
    exit 1
fi

echo "🔍 扫描benchmark文件..."

total_benchmarks=0

for file in src/jmh/java/com/hft/memory/benchmark/*.java; do
    if [[ -f "$file" ]]; then
        filename=$(basename "$file")
        benchmark_count=$(grep -c "@Benchmark" "$file" 2>/dev/null || echo "0")
        benchmark_count=${benchmark_count//[^0-9]/}  # 只保留数字
        
        if [[ $benchmark_count -gt 0 ]]; then
            echo "📁 $filename: $benchmark_count 个@Benchmark方法"
            
            # 列出具体的benchmark方法名
            grep -n "public.*@Benchmark\|@Benchmark.*public" "$file" -A 1 2>/dev/null | \
                grep "public" | \
                sed 's/.*public [^(]* \([^(]*\)(.*/    - \1/' | \
                head -3  # 只显示前3个作为示例
            
            if [[ $benchmark_count -gt 3 ]]; then
                echo "    ... 还有 $((benchmark_count - 3)) 个方法"
            fi
            echo ""
            
            total_benchmarks=$((total_benchmarks + benchmark_count))
        fi
    fi
done

echo "🎯 总计: $total_benchmarks 个@Benchmark方法"
echo ""

echo "🚀 验证JMH配置..."

# 检查当前的JMH配置
if grep -q "includes.*=.*\[" build.gradle; then
    echo "⚠️  检测到includes配置，可能会限制运行的benchmark:"
    grep "includes.*=" build.gradle
    echo ""
    echo "💡 建议注释掉includes配置来运行所有benchmark"
else
    echo "✅ JMH配置正常，将运行所有 $total_benchmarks 个benchmark方法"
fi

echo ""
echo "🎯 运行命令："
echo "   gradle clean jmh                              # 运行所有benchmark"
echo "   ./benchmark-scripts/run-benchmark.sh quick    # 运行快速测试"
echo "   ./benchmark-scripts/run-benchmark.sh          # 运行完整测试"