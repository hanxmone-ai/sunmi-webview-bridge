<p align="center"><img src="docs/icon.png" width="96" alt=""></p>

<h1 align="center">商米网页桥接（Sunmi WebView Bridge）</h1>

<p align="center">
  让任意网页应用在商米 POS 终端上全屏运行，并通过 JavaScript 调用设备<b>自带的扫码和小票打印</b>。
</p>

<p align="center"><a href="README.md">English</a></p>

---

很多门店的后台本来就是网页（库存、贴标、订单查询），但在商米手持或台式终端上，网页调不到硬件。
这个应用用 WebView 承载网页，并提供一套有文档的小接口：

```js
PosBridge.scan();                          // 打开商米扫码
window.onPosScanResult = r => console.log(r.results[0].value);

PosBridge.printBarcode('SKU-2026-0001');   // 条码，下方单独打印一行大号数值
PosBridge.printQrCode('https://example.com');
PosBridge.printText('谢谢惠顾', 28, true, 'center');
window.onPosPrintResult = r => r.ok || alert(r.message);
```

## 功能

- **配置页，无需重新打包**：店员填一次起始网址（可选主页网址），之后启动直接打开。
- **扫码**：调用商米扫码应用，开启 QR、Data Matrix、PDF417、EAN/UPC、Code 128/39/93 等 20 多种码制。
- **打印**：通过商米 `printerx` SDK 打印条码、二维码和带样式的文字，整单作为一个事务，只回调一次结果。
- **回调安全**：结果以转义后的 JSON 通过 `evaluateJavascript` 回传，扫到的内容不会被当成代码执行。
- **非商米设备也能开发**：在模拟器或普通手机上，每个调用都会返回明确的"不可用"结果；内置演示页可以测试所有接口。
- **兼容第一版**：按第一版写的页面（`android.scan()`、`android.print(type, content)`、`scanCallBack`、`printCallBack`）不用改。
- **门店场景细节**：断网提示与重试、更换网址时清缓存（避免单页应用发版后白屏）、双击返回退出、中英文界面。

## 接口

页面里通过 `window.PosBridge` 调用，使用前先判断 `window.PosBridge && PosBridge.isReady()`。

| 方法 | 结果回调 |
|---|---|
| `scan()` | `onPosScanResult(result)` |
| `printBarcode(content)` | `onPosPrintResult(result)` |
| `printQrCode(content)` | `onPosPrintResult(result)` |
| `printText(content, size, bold, align)`：字号 12–64，对齐 `"left"`/`"center"`/`"right"` | `onPosPrintResult(result)` |
| `getPrinterStatus()`：同步返回 JSON 字符串 `{"available": true, "status": "READY"}` | 无 |

打印结果 `code`：0 成功，-1 本机没有打印机，-2 SDK 异常，-3 没有可打印内容，其他值来自打印机（如缺纸）。

## 构建

需要 JDK 17、Android SDK 34。

```bash
./gradlew testDebugUnitTest   # 单元测试
./gradlew assembleDebug       # 输出在 app/build/outputs/apk/debug/
```

正式签名：把 `keystore.properties.example` 复制为 `keystore.properties`（已加入 .gitignore）并填写，然后 `./gradlew assembleRelease`。仓库里不保存任何密钥。

国内网络如果下载依赖慢，可以在 `settings.gradle` 的 `repositories` 里加阿里云镜像。

## 设备说明

- 需要带内置打印机、并装有商米扫码应用（`com.sunmi.sunmiqrcodescanner`）的商米设备。配置页会显示两者是否可用。
- 允许 HTTP 明文访问，因为门店后台常部署在局域网（如 `http://192.168.1.20:8080`）。只用 HTTPS 的话，把 `res/xml/network_security_config.xml` 里的 `cleartextTrafficPermitted` 改为 `false`。
- WebView 里加载的所有页面都能调用这套接口，请只配置可信的网址。

## 背景

2023 年我为商米 POS 终端做的一个外包项目。本仓库是开源前的重写版：去掉了客户相关的网址、品牌和签名信息，重新整理了结构，补上测试、接口文档和 CI。

## 许可

[MIT](LICENSE) © 2023-2026 Yihan Qiu
