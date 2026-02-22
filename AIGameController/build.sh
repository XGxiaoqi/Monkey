#!/bin/bash

# AI Game Controller 构建脚本
# 用于在Linux/macOS环境下构建APK

set -e

echo "========================================="
echo "AI Game Controller Build Script"
echo "========================================="

# 检查环境变量
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "错误: 请设置 ANDROID_HOME 或 ANDROID_SDK_ROOT 环境变量"
    echo ""
    echo "示例:"
    echo "  export ANDROID_HOME=/path/to/android-sdk"
    echo "  或"
    echo "  export ANDROID_SDK_ROOT=/path/to/android-sdk"
    exit 1
fi

# 设置SDK路径
SDK_ROOT="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo "Android SDK: $SDK_ROOT"

# 检查Java版本
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java，请安装JDK 17"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "警告: 推荐使用JDK 17，当前版本: $JAVA_VERSION"
fi

echo "Java版本: $(java -version 2>&1 | head -n 1)"

# 进入项目目录
cd "$(dirname "$0")"
PROJECT_DIR=$(pwd)
echo "项目目录: $PROJECT_DIR"

# 创建local.properties
echo "创建 local.properties..."
echo "sdk.dir=$SDK_ROOT" > local.properties

# 检查并下载Gradle Wrapper
if [ ! -f "gradlew" ]; then
    echo "创建Gradle Wrapper..."
    gradle wrapper --gradle-version 8.2 2>/dev/null || {
        echo "警告: 无法创建Gradle Wrapper，请手动安装Gradle"
    }
fi

# 检查Gradle Wrapper权限
if [ -f "gradlew" ]; then
    chmod +x gradlew
fi

# 创建assets目录（如果不存在）
mkdir -p app/src/main/assets

# 检查AI模型文件
if [ ! -f "app/src/main/assets/paligemma.tflite" ]; then
    echo ""
    echo "========================================="
    echo "注意: 未找到AI模型文件"
    echo "应用将以模拟模式运行（使用简化图像处理）"
    echo ""
    echo "如需完整功能，请将PaliGemma模型文件放置到:"
    echo "  app/src/main/assets/paligemma.tflite"
    echo "========================================="
    echo ""
fi

# 构建类型
BUILD_TYPE="${1:-debug}"
echo "构建类型: $BUILD_TYPE"

# 执行构建
if [ -f "gradlew" ]; then
    echo "开始构建..."
    if [ "$BUILD_TYPE" = "release" ]; then
        ./gradlew assembleRelease
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
    else
        ./gradlew assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    fi
else
    echo "使用系统Gradle构建..."
    if [ "$BUILD_TYPE" = "release" ]; then
        gradle assembleRelease
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
    else
        gradle assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    fi
fi

# 检查构建结果
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "========================================="
    echo "构建成功!"
    echo "APK位置: $APK_PATH"
    echo ""
    ls -lh "$APK_PATH"
    echo "========================================="
    echo ""
    echo "安装命令:"
    echo "  adb install $APK_PATH"
else
    echo ""
    echo "构建失败，请检查错误信息"
    exit 1
fi
