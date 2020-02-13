package cn.cordova.hikvision.sdk;

import android.util.Log;
import android.content.Intent;
import java.lang.reflect.Method;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.hikvision.open.hikvideoplayer.HikVideoPlayerFactory;

/**
 * This class echoes a string called from JavaScript.
 */
public class HikVisionSDK extends CordovaPlugin {
    private static final String TAG = HikVisionSDK.class.getSimpleName();

    @Override
    public boolean execute(final String action, final JSONArray data, CallbackContext callbackContext) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Method method = HikVisionSDK.class.getDeclaredMethod(action, JSONArray.class, CallbackContext.class);
                    method.invoke(HikVisionSDK.this, data, callbackContext);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
        return true;
    }

    void init(JSONArray data, CallbackContext callbackContext) {
        //TODO: enableLog：在debug模式下打开日志，release关闭日志
        //TODO: 现阶段 appKey 不需要，直接传 null
        HikVideoPlayerFactory.initLib(null, true);
    }

    void provideHikVideoPlayer(JSONArray data, CallbackContext callbackContext) {
        HikVideoPlayerFactory.provideHikVideoPlayer();
    }

    void showHikVideoPage(JSONArray data, CallbackContext callbackContext) {
        String url = "";
        String title = "";

        try {
            url = data.getJSONObject(0).getString("url");
            title = data.getJSONObject(0).getString("title");
            Intent intent = new Intent(cordova.getActivity(), PreviewActivity.class);
            //传入参数
            intent.putExtra("hikUrl", url);
            intent.putExtra("hikTitle", title);
            cordova.getActivity().startActivity(intent);

        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error("Parameters error.");
        }

    }
}
