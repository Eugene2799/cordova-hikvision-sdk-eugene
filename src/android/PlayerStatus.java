package cn.cordova.hikvision.sdk;

public enum PlayerStatus {
    IDLE,//闲置状态
    LOADING,//加载中状态
    SUCCESS,//播放成功
    STOPPING,//暂时停止播放
    FAILED,//播放失败
    EXCEPTION,//播放过程中出现异常
    FINISH//回放结束
}
