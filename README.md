# C5X Traffic HUD（第一阶段：蓝牙链路与悬浮窗）

本项目包含两个独立 Android APK 工程：

- `car-receiver`：车机端，最低 Android 4.4 / API 19。通过 RFCOMM SPP 接收一行一条的数据，并显示科技风格悬浮窗。
- `phone-sender`：手机端，Android 6.0+，面向 Android 14。扫描已配对设备并通过 RFCOMM SPP 发送测试信号。

## 重要说明

1. 车机“能播放蓝牙音乐”只证明 A2DP 音频链路可用，**不保证 RFCOMM/SPP 可用**。本项目先验证 SPP。
2. 目前手机端提供红/黄/绿测试按钮，不会伪称已经读取百度地图数据。
3. 百度地图倒计时识别是第二阶段：需要验证百度地图界面是否持续显示可读倒计时，再加入 MediaProjection + OCR。屏幕采集必须由用户在手机上主动授权。
4. 车机悬浮窗在 Android 4.4 使用 `TYPE_PHONE`，不需要现代 Android 的“显示在其他应用上层”运行时授权；如果厂商系统限制悬浮窗，仍需检查系统权限。
5. 先在停车状态测试，不要在驾驶过程中操作手机或调试。

## 构建

使用 Android Studio（建议 JDK 17、Android SDK Platform 35）打开本目录，等待 Gradle Sync，然后分别构建：
- `car-receiver` -> Build APK
- `phone-sender` -> Build APK

项目使用 Android Gradle Plugin 8.7.3。此压缩包提供完整源码工程，不是已经签名的 APK；当前构建环境没有 Android SDK/Gradle，无法在这里编译和实机验证。

## 使用步骤

### 车机
1. 安装 `car-receiver` APK。
2. 打开应用，点击“启动接收与悬浮窗”。
3. 保持应用运行。首次测试时不要退出应用。
4. 在手机蓝牙设置中确认已与车机配对。

### 手机
1. 安装 `phone-sender` APK。
2. 授予附近设备/蓝牙权限。
3. 从已配对设备列表中选择车机。
4. 点击连接。
5. 点击红灯/黄灯/绿灯测试按钮，观察车机悬浮窗是否更新。

## 数据协议

UTF-8，每条以换行结束：

`RED|20|STRAIGHT`
`GREEN|28|RIGHT`
`YELLOW|3|LEFT`

字段依次为：灯色、秒数、转向（LEFT/STRAIGHT/RIGHT/NONE）。
接收端拒绝未知灯色或不合理倒计时（0-180 秒以外）。

## 故障排查

- 手机列表中看不到车机：先在系统蓝牙设置中完成配对，再重新打开手机 App。
- 连接失败：车机端需已启动接收服务；部分车机蓝牙固件不开放 RFCOMM 服务端能力。
- 能配对但不能连接：不代表代码一定有问题，可能是车机蓝牙栈限制 SPP。届时建议改用 Wi-Fi 局域网/热点传输，不要破坏原车 A2DP。


## GitHub Actions 自动编译

1. 在 GitHub 新建一个空仓库，例如 `C5X_Traffic_HUD`。
2. 解压本 ZIP，将里面的全部文件和 `.github` 隐藏目录一起上传到仓库根目录。
3. 推送到 `main` 或 `master` 分支，或者进入仓库的 **Actions** 页面，手动运行 `Build C5X Traffic HUD APKs`。
4. 等待工作流完成，在运行记录底部的 **Artifacts** 下载 `C5X-Traffic-HUD-debug-apks`。
5. 解压 Artifact，里面有车机端和手机端两个 debug APK。

工作流使用 JDK 17、Gradle 8.9、Android Gradle Plugin 8.7.3。APK 是 debug 签名，仅用于测试；正式发布需要配置自己的 release 签名。
