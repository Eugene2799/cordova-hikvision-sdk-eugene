var HikVisionSDK = function() {};
// private plugin function

HikVisionSDK.prototype.isPlatformIOS = function() {
    return (
      device.platform === "iPhone" ||
      device.platform === "iPad" ||
      device.platform === "iPod touch" ||
      device.platform === "iOS"
    );
};

HikVisionSDK.prototype.errorCallback = function(msg) {
    console.log("HikVisionSDK Callback Error: " + msg);
};

HikVisionSDK.prototype.callNative = function(
  name,
  args,
  successCallback,
  errorCallback
) {
    if (errorCallback) {
        cordova.exec(successCallback, errorCallback, "HikVisionSDK", name, args);
    } else {
        cordova.exec(
          successCallback,
          this.errorCallback,
          "HikVisionSDK",
          name,
          args
        );
    }
};
// Common methods
HikVisionSDK.prototype.init = function() {
    if (this.isPlatformIOS()) {
        this.callNative("initial", [], null);
    } else {
        console.log("init hik vision android sdk")
        this.callNative("init", [], null);
    }
};

HikVisionSDK.prototype.provideHikVideoPlayer = function(params, successCallback, errorCallback) {
    this.callNative("provideHikVideoPlayer", [], successCallback, errorCallback);
};
/**
 * 展示监控视频
 * 注意：该接口是覆盖逻辑，而不是增量逻辑。即新的调用会覆盖之前的设置。
 *
 * @param params = { 'url': string, 'title': string }
 */
HikVisionSDK.prototype.showHikVideoPage = function(params, successCallback, errorCallback) {

    this.callNative("showHikVideoPage", [params], successCallback, errorCallback);
};


if (!window.plugins) {
    window.plugins = {};
}

if (!window.plugins.hikVisionSDK) {
    window.plugins.hikVisionSDK = new HikVisionSDK();
}

module.exports = new HikVisionSDK();
