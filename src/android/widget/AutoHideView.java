package cn.cordova.hikvision.sdk.widget;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.blankj.utilcode.util.ScreenUtils;
import cn.cordova.hikvision.sdk.MyUtils;
import cn.cordova.hikvision.sdk.R;

/**
 * 自动隐藏的工具栏View
 */
public class AutoHideView extends FrameLayout {
    /**
     * 定义的工具栏展示的时间，无操作后10s隐藏
     */
    private static final int HIDE_TIMEOUT_IN_MS = 10 * 1000;

    public AutoHideView(@NonNull Context context) {
        this(context,null);
    }

    public AutoHideView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AutoHideView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        setBackgroundColor(Color.parseColor("#ccff0000"));
        inflate(getContext(), R.layout.view_auto_hide,this);
        ImageView switchScreenImage = findViewById(R.id.switch_screen_view);
        switchScreenImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ScreenUtils.isPortrait()){
                    //切换到横屏
                    MyUtils.getActivity(AutoHideView.this).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    switchScreenImage.setImageResource(R.drawable.ic_minimum_white_nor_24);
                }else if (ScreenUtils.isLandscape()){
                    //切换到竖屏
                    MyUtils.getActivity(AutoHideView.this).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    switchScreenImage.setImageResource(R.drawable.ic_maximum_white_nor_24);
                }
                resetHideTask();
            }
        });
    }

    /**
     * 显示工具栏
     */
    public void show(){
        if (isVisible()){
            return;
        }
        this.setVisibility(View.VISIBLE);
        this.setAnimation(moveToViewLocation());
        postDelayed(hideControlViewTask, HIDE_TIMEOUT_IN_MS);
    }


    public boolean isVisible(){
        return  this.getVisibility() == VISIBLE;
    }

    /**
     * 隐藏工具栏
     */
    public void hide(){
        if (!isVisible()){
            return;
        }
        this.setVisibility(View.GONE);
        this.setAnimation(moveToViewBottom());
        removeCallbacks(hideControlViewTask);
    }

    /**
     * 被点击的时候，需要重置延时
     */
    private void resetHideTask(){
        removeCallbacks(hideControlViewTask);
        postDelayed(hideControlViewTask, HIDE_TIMEOUT_IN_MS);
    }

    /**
     *显示动画
     */
    private TranslateAnimation moveToViewLocation() {
        TranslateAnimation translateAnimation = new TranslateAnimation(1, 0.0F, 1, 0.0F, 1, 1.0F, 1, 0.0F);
        translateAnimation.setInterpolator(new DecelerateInterpolator());
        translateAnimation.setDuration(400L);
        return translateAnimation;
    }

    /**
     * 隐藏动画，避免隐藏时，控制工具栏一闪而逝
     */
    private TranslateAnimation moveToViewBottom() {
        TranslateAnimation hiddenAction = new TranslateAnimation(1, 0.0F,
                1, 0.0F, 1, 0.0F, 1, 1.0F);
        hiddenAction.setInterpolator(new DecelerateInterpolator());
        hiddenAction.setDuration(400L);
        return hiddenAction;
    }

    /**
     * 控制栏自动隐藏任务
     */
    private final Runnable hideControlViewTask = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
}
