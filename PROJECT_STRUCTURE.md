# 项目结构说明

## 目录结构

```
ScreenColorInvertSystem/
├── .github/
│   └── workflows/
│       └── android.yml          # GitHub Actions CI/CD配置
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/screencolor/invert/
│   │   │   │   ├── data/
│   │   │   │   │   └── ColorPair.kt          # 颜色对数据模型
│   │   │   │   ├── render/
│   │   │   │   │   ├── GLRenderer.kt         # OpenGL渲染器
│   │   │   │   │   └── ShaderProgram.kt      # 着色器程序管理
│   │   │   │   ├── service/
│   │   │   │   │   ├── OverlayService.kt     # 悬浮窗服务
│   │   │   │   │   └── ScreenCaptureService.kt # 屏幕捕获服务
│   │   │   │   ├── ui/
│   │   │   │   │   ├── ColorPairAdapter.kt   # 颜色对列表适配器
│   │   │   │   │   ├── ColorPickerDialog.kt  # 颜色选择器对话框
│   │   │   │   │   └── MainActivity.kt       # 主界面
│   │   │   │   ├── utils/
│   │   │   │   │   └── PreferenceManager.kt  # 偏好设置管理
│   │   │   │   └── ScreenColorApp.kt         # Application类
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── circle_color_preview.xml
│   │   │   │   │   └── ic_launcher_foreground.xml
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml     # 主界面布局
│   │   │   │   │   ├── dialog_color_picker.xml
│   │   │   │   │   ├── item_color_pair.xml   # 颜色对列表项
│   │   │   │   │   └── overlay_control.xml   # 悬浮控制条
│   │   │   │   ├── mipmap-anydpi-v26/
│   │   │   │   │   ├── ic_launcher.xml
│   │   │   │   │   └── ic_launcher_round.xml
│   │   │   │   ├── raw/
│   │   │   │   │   ├── fragment_shader.glsl  # 片元着色器
│   │   │   │   │   └── vertex_shader.glsl    # 顶点着色器
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml            # 颜色定义
│   │   │   │   │   ├── dimens.xml            # 尺寸定义
│   │   │   │   │   ├── strings.xml           # 字符串资源
│   │   │   │   │   ├── styles.xml            # 样式定义
│   │   │   │   │   └── themes.xml            # 主题定义
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml
│   │   │   │       └── data_extraction_rules.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── settings.gradle.kts
├── .gitignore
├── CONTRIBUTING.md
├── LICENSE
├── PROJECT_STRUCTURE.md
└── README.md
```

## 核心模块说明

### 1. 数据层 (data/)
- **ColorPair.kt**: 定义颜色对数据模型，包含目标颜色、替换颜色、容差等属性

### 2. 渲染层 (render/)
- **GLRenderer.kt**: OpenGL渲染器，管理EGL上下文、纹理和渲染循环
- **ShaderProgram.kt**: 着色器程序管理，编译链接着色器，管理uniform变量

### 3. 服务层 (service/)
- **ScreenCaptureService.kt**: 前台服务，管理MediaProjection和VirtualDisplay
- **OverlayService.kt**: 悬浮窗服务，提供可拖动的控制条

### 4. UI层 (ui/)
- **MainActivity.kt**: 主界面，颜色配置和设置
- **ColorPairAdapter.kt**: RecyclerView适配器
- **ColorPickerDialog.kt**: 颜色选择对话框

### 5. 工具层 (utils/)
- **PreferenceManager.kt**: SharedPreferences封装，管理颜色对和设置

### 6. 着色器 (res/raw/)
- **vertex_shader.glsl**: 顶点着色器，处理顶点位置和纹理坐标
- **fragment_shader.glsl**: 片元着色器，实现颜色匹配和替换逻辑

## 数据流

```
用户配置 → PreferenceManager → ScreenCaptureService → GLRenderer → Shader
                                         ↑
MediaProjection → VirtualDisplay → SurfaceTexture → OpenGL纹理
```

## 构建流程

1. GitHub Actions 触发构建
2. 运行 lint 检查
3. 运行单元测试
4. 构建 Debug APK
5. 构建并签名 Release APK（需要配置 secrets）
6. 上传构建产物
