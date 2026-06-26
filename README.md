# 微博热搜安卓桌面小组件 (Weibo Hot Search Android Widget)

这是一个专为安卓系统设计的“微博热搜”桌面小组件。具有现代简约风格、轻拟物毛玻璃感，支持自适应卡片布局、自动后台刷新以及点击热搜项直接打开浏览器搜索等功能。

## 🌟 核心特性

- **直观的热搜榜单**：
  - 支持显示最新的前 8 条热搜词条。
  - 前 3 名配以专属的彩色徽标（红、橙、黄）。
  - 视觉层次分明。
- **高效与流畅性**：
  - **本地缓存优先架构**：小组件初始化时优先加载本地缓存的数据，避免白屏或黑屏。
  - **异步获取与 goAsync**：使用 `goAsync()` 机制在后台线程中请求微博官方 API，有效防止主线程阻塞（ANR），确保桌面流畅度。
  - **网络防拦截**：通过定制 HTTP Header（增加 `Referer` 与 `User-Agent`）绕过微博接口防盗链限制，保证数据获取稳定性。
- **便捷的交互**：
  - 标题栏右侧配有**手动刷新按钮**，点击即可获取最新数据，并显示“刷新中…”、“更新时间”及“网络失败 ✕”等状态。
  - 点击任何一条热搜词条，将自动调用浏览器并跳转至对应的微博搜索页面。

---

## 📸 界面布局

<img width="480" height="1066" alt="Screenshot_2026-06-26-18-42-09-117_com miui home(" src="https://github.com/user-attachments/assets/116939a2-9e5e-4986-b8bd-95ebb37732e9" />


---

## 📦 如何安装使用

1. **直接安装 APK**：
   下载项目中的 [weibo-hot-search-widget.apk](./weibo-hot-search-widget.apk) 移至安卓手机中安装。
2. **启动应用**：
   安装完成后，首次打开主程序（加载主页面）以初始化网络请求。
3. **添加小组件**：
   回到手机桌面，长按空白处选择“小组件/窗口小部件”，找到“weibo-hot-search”并将其拖放到桌面合适位置即可。

---

## 🛠️ 源码开发与编译

### 运行环境要求
- **Android SDK**: API 21+
- **JDK**: Java 17
- **Build System**: Gradle 8.x

### 编译步骤
如果您想修改源码并重新打包：

1. **克隆项目**：
   ```bash
   git clone <your-github-repo-url>
   cd weibo_hot_search_widget
   ```
2. **编译生成 Debug APK**：
   在项目根目录下执行以下命令：
   - Windows (PowerShell):
     ```powershell
     $env:JAVA_HOME="<你的JDK17路径>"; .\gradlew.bat assembleDebug
     ```
   - Linux / macOS:
     ```bash
     export JAVA_HOME="<你的JDK17路径>"
     ./gradlew assembleDebug
     ```
3. **输出路径**：
   编译成功后，生成的 APK 将位于 `app/build/outputs/apk/debug/app-debug.apk`。

---

## 📄 开源协议
MIT
