# hikvision video SDK / Cordova Plugin

![AppVeyor branch](https://img.shields.io/appveyor/ci/Eugene2799/eugene-cordova-hikvision-sdk/master)
![npm](https://img.shields.io/npm/v/cordova-hikvision-sdk-eugene)
[![platforms](https://img.shields.io/badge/platforms-Android-lightgrey)](https://github.com/Eugene2799/eugene-cordova-hikvision-sdk)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/Eugene2799/eugene-cordova-hikvision-sdk)

个人因为工作需要，封装了hikvision 的视频SDK到cordova项目中。这里目前只封装了Android的SDK。

## hikvision SDK下载及接口文档

[视频SDK-Android版本 V1.2.0](https://open.hikvision.com/download/5c67f1e2f05948198c909700?type=10)

>注意：插件使用需修改src/android/libs/PreviewActivity.java中19行，导入自己项目包的R类。
>```java
>import your.app.package.name.R;
>```

## Install

- 通过 Cordova Plugins 安装，要求 Cordova CLI 5.0+：

  ```shell
  cordova plugin add cordova-hikvision-sdk-eugene
  ```
  
- 修改plugins/cordova-hikvision-sdk-eugene/src/android/libs/PreviewActivity.java中19行
  ```java
  import your.app.package.name.R;
  ```
  
## Usage
### init plugin
   ```html
   window.plugins.hikVisionSDK.init();
   ```
### start activity && set params

```html
let param = { 'url': yourMonitorUrl, 'title': setTitle }
window.plugins.hikVisionSDK.showHikVideoPage(param,function (msg) {
  console.log(msg)
},function (err) {
  console.log(err)
});
```
    
## FAQ
> 如果遇到了疑问，请优先参考 代码 和 海康威视API 文档。若还无法解决，可到 [Issues](https://github.com/Eugene2799/eugene-cordova-hikvision-sdk/issues) 提问。

