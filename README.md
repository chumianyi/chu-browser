# Chu浏览器 (Chu Browser)

基于 Mozilla GeckoView (Firefox 内核) 的 Android 浏览器，功能丰富，注重隐私与安全。

## 功能特性

- **Firefox 内核**：集成 Mozilla GeckoView，非系统 WebView
- **广告拦截**：内置广告拦截器，可一键开关
- **安全检测**：网址安全验证 + 恶意网页检测拦截
- **密码管理器**：本地加密保存密码，支持超长强密码生成（64/128/256/512位），用户名快捷填充
- **验证码识别**：ML Kit 自动识别验证码并粘贴
- **证书查看**：完整的网站 SSL 证书信息查看
- **Cookie 管理**：完整 Cookie 查看与管理
- **开发者工具**：控制台、网络请求、JavaScript 执行
- **隐私隔离**：隐私隔离空间，独立 Cookie/缓存/历史
- **下载管理**：下载确认弹窗、自动获取文件名和后缀、进度显示
- **书签管理**：书签收藏与文件夹管理
- **多标签页**：最多 50 个标签页，支持隐私标签
- **搜索引擎**：默认 Bing，可切换 Google/Baidu/DuckDuckGo/Yahoo
- **Material 3 设计**：蓝粉配色，支持深色/浅色主题

## 技术栈

- Kotlin 原生开发
- Mozilla GeckoView 127
- Mozilla Android Components
- Material 3
- Room 数据库
- DataStore
- ML Kit 文本识别
- OkHttp

## 构建

```bash
./gradlew assembleRelease
```

输出 APK 位于 `app/build/outputs/apk/release/`

## 系统要求

- Android 8.0 (API 26) 及以上
- arm64-v8a 架构

## 开源协议

本项目采用 **GNU General Public License v3.0** 开源协议。

```
Chu Browser - Android browser based on Mozilla GeckoView
Copyright (C) 2026 Chu Browser

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
