package cn.cordova.hikvision.sdk;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.content.Intent;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import your.app.package.name.R;
import com.blankj.utilcode.util.ToastUtils;
import com.hikvision.open.hikvideoplayer.HikVideoPlayer;
import com.hikvision.open.hikvideoplayer.HikVideoPlayerCallback;
import com.hikvision.open.hikvideoplayer.HikVideoPlayerFactory;

import java.text.MessageFormat;

/**
 * 错误码开头：17是mgc或媒体取流SDK的错误，18是vod，19是dac
 */
public class PreviewActivity extends AppCompatActivity implements View.OnClickListener, HikVideoPlayerCallback, TextureView.SurfaceTextureListener {
    private static final String TAG = "PreviewActivity";
    private static final String previewUri = "";

    protected TextureView textureView;
    protected TextView toolBarTitle;
    protected ProgressBar progressBar;
    protected Button captureButton;
    protected Button recordButton;
    protected Button soundButton;
    protected TextView playHintText;
    protected EditText reviewUriEdit;
    protected Button start;
    protected Button stop;
    protected ImageButton bt_big;
    protected ImageButton bt_small;
    private String mUri;
    private String mTitle;
    private int width;
    private int height;
    private HikVideoPlayer mPlayer;
    private boolean mSoundOpen = false;
    private boolean mRecording = false;
    private PlayerStatus mPlayerStatus = PlayerStatus.IDLE;//默认闲置
    private TextView mRecordFilePathText;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);//防止键盘弹出
        super.setContentView(R.layout.activity_preview);
        mPlayer = HikVideoPlayerFactory.provideHikVideoPlayer();
        initView();
    }

    private void initView() {
        Intent intent = getIntent();
        mUri = intent.getStringExtra("hikUrl");
        mTitle = intent.getStringExtra("hikTitle");
        toolBarTitle = findViewById(R.id.toolbar_title);
        mToolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar(mToolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        mToolbar.setNavigationIcon(R.drawable.ic_back);
        mToolbar.setNavigationOnClickListener(v -> finish());
        toolBarTitle.setText(mTitle);
        textureView = findViewById(R.id.texture_view);
        progressBar = findViewById(R.id.progress_bar);
        playHintText = findViewById(R.id.result_hint_text);
        bt_big = findViewById(R.id.bt_big);
        bt_small = findViewById(R.id.bt_small);
        captureButton = findViewById(R.id.capture_button);
        mRecordFilePathText = findViewById(R.id.record_file_path_text);
        captureButton.setOnClickListener(PreviewActivity.this);
        recordButton = findViewById(R.id.record_button);
        recordButton.setOnClickListener(PreviewActivity.this);
        soundButton = findViewById(R.id.sound_button);
        soundButton.setOnClickListener(PreviewActivity.this);
        reviewUriEdit = findViewById(R.id.review_uri_edit);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        bt_big.setOnClickListener(this);
        bt_small.setOnClickListener(this);
        reviewUriEdit.setText(previewUri);
        textureView.setSurfaceTextureListener(this);

        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;         // 屏幕宽度（像素）
        height = dm.heightPixels;       // 屏幕高度（像素）
        float density = dm.density;         // 屏幕密度（0.75 / 1.0 / 1.5）
        int densityDpi = dm.densityDpi;     // 屏幕密度dpi（120 / 160 / 240）
        // 屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        int screenWidth = (int) (width / density);  // 屏幕宽度(dp)
        int screenHeight = (int) (height / density);// 屏幕高度(dp)

        Log.d("h_bl", "屏幕宽度（像素）：" + width);
        Log.d("h_bl", "屏幕高度（像素）：" + height);
        Log.d("h_bl", "屏幕密度（0.75 / 1.0 / 1.5）：" + density);
        Log.d("h_bl", "屏幕密度dpi（120 / 160 / 240）：" + densityDpi);
        Log.d("h_bl", "屏幕宽度（dp）：" + screenWidth);
        Log.d("h_bl", "屏幕高度（dp）：" + screenHeight);

        textureView.setLayoutParams(new FrameLayout.LayoutParams(width, 800, Gravity.CENTER));

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start) {
            if (mPlayerStatus != PlayerStatus.SUCCESS && getPreviewUri()) {
                startRealPlay(textureView.getSurfaceTexture());
            }
        } else if (view.getId() == R.id.stop) {
            if (mPlayerStatus == PlayerStatus.SUCCESS) {
                mPlayerStatus = PlayerStatus.IDLE;//释放这个窗口
                mRecordFilePathText.setText(null);
                progressBar.setVisibility(View.GONE);
                playHintText.setVisibility(View.VISIBLE);
                playHintText.setText("");
                mPlayer.stopPlay();
            }
        } else if (view.getId() == R.id.capture_button) {
            executeCaptureEvent();
        } else if (view.getId() == R.id.record_button) {
            executeRecordEvent();
        } else if (view.getId() == R.id.sound_button) {
            executeSoundEvent();
        } else if (view.getId() == R.id.bt_big) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//横屏
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
            textureView.setLayoutParams(new FrameLayout.LayoutParams(height, width, Gravity.CENTER));
            bt_big.setVisibility(View.GONE);
            bt_small.setVisibility(View.VISIBLE);
            mToolbar.setVisibility(View.GONE);
        } else if (view.getId() == R.id.bt_small) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
            textureView.setLayoutParams(new FrameLayout.LayoutParams(width, 800, Gravity.CENTER));
            bt_big.setVisibility(View.VISIBLE);
            bt_small.setVisibility(View.GONE);
            mToolbar.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 执行抓图事件
     */
    private void executeCaptureEvent() {
        if (mPlayerStatus != PlayerStatus.SUCCESS) {
            ToastUtils.showShort("没有视频在播放");
        }

        //抓图
        if (mPlayer.capturePicture(MyUtils.getCaptureImagePath(this))) {
            ToastUtils.showShort("抓图成功");
        }
    }

    /**
     * 执行录像事件
     */
    private void executeRecordEvent() {
        if (mPlayerStatus != PlayerStatus.SUCCESS) {
            ToastUtils.showShort("没有视频在播放");
        }

        if (!mRecording) {
            //开始录像
            mRecordFilePathText.setText(null);
            String path = MyUtils.getLocalRecordPath(this);
            if (mPlayer.startRecord(path)) {
                ToastUtils.showShort("开始录像");
                mRecording = true;
                recordButton.setText(R.string.close_record);
                mRecordFilePathText.setText(MessageFormat.format("当前本地录像路径: {0}", path));
            }
        } else {
            //关闭录像
            mPlayer.stopRecord();
            ToastUtils.showShort("关闭录像");
            mRecording = false;
            recordButton.setText(R.string.start_record);
        }
    }

    /**
     * 执行声音开关事件
     */
    private void executeSoundEvent() {
        if (mPlayerStatus != PlayerStatus.SUCCESS) {
            ToastUtils.showShort("没有视频在播放");
        }

        if (!mSoundOpen) {
            //打开声音
            if (mPlayer.enableSound(true)) {
                ToastUtils.showShort("声音开");
                mSoundOpen = true;
//                 soundButton.setText(R.string.sound_close);
            }
        } else {
            //关闭声音
            if (mPlayer.enableSound(false)) {
                ToastUtils.showShort("声音关");
                mSoundOpen = false;
//                 soundButton.setText(R.string.sound_open);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //TODO 注意:APP前后台切换时 SurfaceTextureListener可能在有某些 华为手机 上不会回调，例如：华为P20，所以我们在这里手动调用
        if (textureView.isAvailable()) {
            Log.e(TAG, "onResume: onSurfaceTextureAvailable");
            onSurfaceTextureAvailable(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //TODO 注意:APP前后台切换时 SurfaceTextureListener可能在有某些 华为手机 上不会回调，例如：华为P20，所以我们在这里手动调用
        if (textureView.isAvailable()) {
            Log.e(TAG, "onPause: onSurfaceTextureDestroyed");
            onSurfaceTextureDestroyed(textureView.getSurfaceTexture());
        }
    }


    /**
     * 开始播放
     *
     * @param surface 渲染画面
     */
    private void startRealPlay(SurfaceTexture surface) {
        mPlayerStatus = PlayerStatus.LOADING;
        progressBar.setVisibility(View.VISIBLE);
        playHintText.setVisibility(View.GONE);
        mPlayer.setSurfaceTexture(surface);
        //TODO 注意: startRealPlay() 方法会阻塞当前线程，需要在子线程中执行,建议使用RxJava
        new Thread(() -> {
            //TODO 注意: 不要通过判断 startRealPlay() 方法返回 true 来确定播放成功，播放成功会通过HikVideoPlayerCallback回调，startRealPlay() 方法返回 false 即代表 播放失败;
            if (!mPlayer.startRealPlay(mUri, PreviewActivity.this)) {
                onPlayerStatus(Status.FAILED, mPlayer.getLastError());
            }
        }).start();
    }


    /**
     * 播放结果回调
     *
     * @param status    共四种状态：SUCCESS（播放成功）、FAILED（播放失败）、EXCEPTION（取流异常）、FINISH（回放结束）
     * @param errorCode 错误码，只有 FAILED 和 EXCEPTION 才有值
     */
    @Override
    @WorkerThread
    public void onPlayerStatus(@NonNull Status status, int errorCode) {
        //TODO 注意: 由于 HikVideoPlayerCallback 是在子线程中进行回调的，所以一定要切换到主线程处理UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                switch (status) {
                    case SUCCESS:
                        //播放成功
                        mPlayerStatus = PlayerStatus.SUCCESS;
                        playHintText.setVisibility(View.GONE);
                        textureView.setKeepScreenOn(true);//保持亮屏
                        break;
                    case FAILED:
                        //播放失败
                        mPlayerStatus = PlayerStatus.FAILED;
                        playHintText.setVisibility(View.VISIBLE);
                        playHintText.setText(MessageFormat.format("预览失败，错误码：{0}", Integer.toHexString(errorCode)));
                        break;
                    case EXCEPTION:
                        //取流异常
                        mPlayerStatus = PlayerStatus.EXCEPTION;
                        mPlayer.stopPlay();//TODO 注意:异常时关闭取流
                        playHintText.setVisibility(View.VISIBLE);
                        playHintText.setText(MessageFormat.format("取流发生异常，错误码：{0}", Integer.toHexString(errorCode)));
                        break;
                }
            }
        });
    }

    private boolean getPreviewUri() {
        if (TextUtils.isEmpty(mUri)) {
            ToastUtils.showShort("视频地址获取失败");
            return false;
        }

        if (!mUri.contains("rtsp")) {
            ToastUtils.showShort("视频地址链接错误");
            return false;
        }

        return true;
    }


    /*************************TextureView.SurfaceTextureListener 接口的回调方法********************/
    //TODO 注意:APP前后台切换时 SurfaceTextureListener可能在有某些华为手机上不会回调，例如：华为P20，因此我们需要在Activity生命周期中手动调用回调方法
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mPlayerStatus == PlayerStatus.STOPPING) {
            //恢复处于暂停播放状态的窗口
            startRealPlay(textureView.getSurfaceTexture());
            Log.d(TAG, "onSurfaceTextureAvailable: startRealPlay");
        }
        if (mPlayerStatus != PlayerStatus.SUCCESS && getPreviewUri()) {
            startRealPlay(textureView.getSurfaceTexture());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mPlayerStatus == PlayerStatus.SUCCESS) {
            mPlayerStatus = PlayerStatus.STOPPING;//暂停播放，再次进入时恢复播放
            mPlayer.stopPlay();
            Log.d(TAG, "onSurfaceTextureDestroyed: stopPlay");
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

}
