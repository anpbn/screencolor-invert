# Contributing to ScreenColor Invert System

感谢您对 ScreenColor Invert System 项目的关注！我们欢迎并感谢您为项目做出的贡献。

## 如何贡献

### 报告问题

如果您发现了bug或有功能建议，请通过GitHub Issues提交：

1. 检查是否已有相关问题
2. 使用问题模板创建新Issue
3. 提供详细的描述、复现步骤和环境信息

### 提交代码

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范

- 遵循 Kotlin 编码规范
- 使用有意义的变量名和函数名
- 添加必要的注释
- 确保代码通过 lint 检查

### 提交信息规范

请使用以下格式提交信息：

```
<type>(<scope>): <subject>

<body>

<footer>
```

类型包括：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

## 开发环境

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

## 测试

提交PR前请确保：

1. 所有单元测试通过 (`./gradlew test`)
2. lint检查通过 (`./gradlew lint`)
3. 在真机上测试功能

## 代码审查

所有PR都需要经过代码审查。审查者会检查：

- 代码质量
- 功能正确性
- 性能影响
- 兼容性

## 许可证

通过贡献代码，您同意您的贡献将在 MIT 许可证下发布。

## 联系方式

如有任何问题，请通过 GitHub Issues 联系我们。
