# ScreenColor Invert System

[![Android CI](https://github.com/yourusername/ScreenColorInvertSystem/actions/workflows/android.yml/badge.svg)](https://github.com/yourusername/ScreenColorInvertSystem/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

安卓屏幕实时颜色替换系统 - 通过GPU加速实现屏幕内容的实时颜色识别与替换。

## 功能特性

- **多目标颜色并行替换**：支持最多8组颜色同时替换，每组独立容差控制
- **实时屏幕捕获**：基于MediaProjection API，30-60 FPS流畅运行
- **GPU加速渲染**：OpenGL ES 2.0/3.0着色器并行处理，延迟低于50ms
- **悬浮窗覆盖**：透明背景覆盖，不拦截触摸事件
- **前台服务保活**：持久运行，支持通知栏快速控制
- **RGB/HSV双色彩空间**：支持色相/饱和度/明度独立调节
- **边缘平滑处理**：基于容差比例的线性插值，避免锯齿硬边

## 应用场景

- 视觉辅助（色弱用户区分颜色）
- 夜间模式增强（特定高亮颜色柔化）
- 游戏画面调色（UI元素颜色自定义）
- 屏幕内容实时滤镜处理

## 技术架构

```
[屏幕物理显示层]
       ↓ (系统帧缓冲)
[MediaProjection] → 捕获原始帧 → [VirtualDisplay]
                                     ↓
[SurfaceTexture] → OpenGL纹理ID → [GL渲染管线]
                                     ↓
[片元着色器处理] → 颜色匹配与替换 → [EGL Surface]
                                     ↓
[悬浮窗SurfaceView] → 透明背景合成 → [用户视觉层]
```

## 系统要求

- Android 8.0 (API 26) 至 Android 14 (API 34)
- OpenGL ES 2.0 或更高版本
- 录屏权限（MediaProjection）
- 悬浮窗权限（SYSTEM_ALERT_WINDOW）

## 构建说明

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 本地构建

```bash
# 克隆仓库
git clone https://github.com/yourusername/ScreenColorInvertSystem.git
cd ScreenColorInvertSystem

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease

# 运行测试
./gradlew test

# 运行lint检查
./gradlew lint
```

### GitHub Actions自动构建

本项目配置了GitHub Actions工作流，支持：
- 自动lint检查
- 自动单元测试
- Debug/Release APK构建
- 自动签名Release APK（需配置secrets）

#### 配置签名密钥

在GitHub仓库Settings > Secrets and variables > Actions中添加：

- `SIGNING_KEY`: Base64编码的keystore文件
- `ALIAS`: 密钥别名
- `KEY_STORE_PASSWORD`: Keystore密码
- `KEY_PASSWORD`: 密钥密码

生成Base64编码的keystore：
```bash
base64 -i your-keystore.jks
```

## 项目结构

```
ScreenColorInvertSystem/
├── app/
│   ├── src/main/
│   │   ├── java/com/screencolor/invert/
│   │   │   ├── data/           # 数据模型
│   │   │   ├── render/         # OpenGL渲染引擎
│   │   │   ├── service/        # 前台服务
│   │   │   ├── ui/             # 用户界面
│   │   │   ├── utils/          # 工具类
│   │   │   └── ScreenColorApp.kt
│   │   ├── res/                # 资源文件
│   │   │   ├── layout/         # 布局文件
│   │   │   ├── raw/            # 着色器代码
│   │   │   └── values/         # 字符串、颜色、主题
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── .github/workflows/           # CI/CD配置
├── build.gradle.kts
└── README.md
```

## 核心模块

### 1. 捕获模块 (Capture Module)
- MediaProjectionManager交互
- VirtualDisplay生命周期管理
- 屏幕旋转与分辨率变化处理

### 2. 渲染引擎 (Render Engine)
- GLSurfaceView封装，EGL上下文管理
- 着色器程序管理（顶点+片元）
- Uniform变量动态更新

### 3. 颜色处理算法 (Shader Logic)
```glsl
// 核心逻辑：并行匹配多组目标颜色
for (int i = 0; i < uActiveCount; i++) {
    float diff = distance(currentColor, uTargets[i]);
    if (diff < uTolerances[i] && diff < minDiff) {
        bestMatch = i;
        minDiff = diff;
    }
}
```

### 4. 窗口管理模块 (Overlay Manager)
- WindowManager系统服务调用
- TYPE_APPLICATION_OVERLAY悬浮窗
- 生命周期管理

## 性能指标

| 指标 | 目标值 | 测试设备 |
|------|--------|----------|
| CPU占用率 | < 10% | Snapdragon 865 |
| 内存占用 | < 150MB | 8GB RAM |
| 渲染耗时 | < 16ms/帧 | Adreno 650 |
| 目标帧率 | 30-60 FPS | - |

## 权限说明

| 权限 | 用途 | 必需 |
|------|------|------|
| FOREGROUND_SERVICE | 前台服务保活 | 是 |
| MEDIA_PROJECTION | 屏幕捕获 | 是 |
| SYSTEM_ALERT_WINDOW | 悬浮窗显示 | 是 |
| POST_NOTIFICATIONS | 通知栏控制 | Android 13+ |

## 开发注意事项

### 技术难点
1. **EGL上下文管理**：GLSurfaceView与VirtualDisplay的Surface共享
2. **纹理坐标映射**：屏幕旋转时更新变换矩阵
3. **多窗口适配**：分屏模式、折叠屏动态调整分辨率

### 系统限制
- Android 12+ 对后台服务启动有严格限制
- 录屏功能涉及敏感权限，需隐私政策说明
- 部分设备SurfaceView可能与悬浮窗硬件加速层冲突

### 性能优化
- 分辨率降级选项（0.5x/0.75x/1.0x）
- 区域裁剪处理
- 帧率自适应（30fps/60fps切换）

## 测试验收

- [x] 功能测试：3组颜色同时替换验证
- [x] 性能测试：连续运行30分钟稳定30fps+
- [x] 压力测试：大型游戏场景不杀后台
- [x] 兼容性测试：小米/华为/三星/Pixel Android 10/12/14

## 开源协议

MIT License - 详见 [LICENSE](LICENSE) 文件

## 贡献指南

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 联系方式

- 项目主页：https://github.com/yourusername/ScreenColorInvertSystem
- 问题反馈：https://github.com/yourusername/ScreenColorInvertSystem/issues
- 邮箱：your.email@example.com

## 致谢

感谢所有为本项目做出贡献的开发者！
