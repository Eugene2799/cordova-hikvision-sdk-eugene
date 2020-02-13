package cn.cordova.hikvision.sdk.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.hikvision.open.hikvideoplayer.CustomRect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.support.v4.widget.ViewDragHelper.INVALID_POINTER;

/**
 * 播放窗口容器，用于处理开启、关闭电子放大的手势事件
 */
public class PlayWindowContainer extends FrameLayout {
    /**
     * 定义三种触摸模式
     */
    private static final int NONE = 0;
    private static final int ZOOM_DRAG = 1;
    private static final int ZOOM_SCALE = 2;

    /**
     * 触摸模式注解
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NONE, ZOOM_DRAG, ZOOM_SCALE})
    private @interface TouchMode {
    }

    @TouchMode
    private int mClickMode = NONE;

    /**
     * 每次手势缩放的比例
     */
    private static final float UNIT_SCALE_RATIO = 0.005f;
    /**
     * 最大放大倍数
     */
    private static final int MAX_SCALE = 10;
    /**
     * 上次两指间距离
     */
    private float mLastDis = 0;
    /**
     * 上次的放大倍数
     */
    private float mLastScale = 1;

    private float mLastMotionY = 0;
    private float mLastMotionX = 0;
    private float mRatioX = 1;
    private float mRatioY = 1;
    private int mActionPointerId = INVALID_POINTER;

    private final CustomRect mOriginalRect = new CustomRect();
    private final CustomRect mVirtualRect = new CustomRect();

    /**
     * 是否允许打开电子放大。TODO:电子放大需要在开启播放后才可以打开，此条件需要外部进行设置
     */
    private boolean mAllowOpenDigitalZoom = false;
    /**
     * 是否开启了电子放大。只有在允许开启后，才有机会变为true
     */
    private boolean mDigitalZoomOpen = false;
    /**
     * 是否应该继续处理Scale手势
     */
    private boolean mShouldHandleScale = true;

    /**
     * 点击监听
     */
    private OnClickListener onClickListener;
    /**
     * 电子放大开启关闭监听
     */
    private OnDigitalZoomListener onDigitalListener;

    /**
     * 电子放大开启关闭监听
     */
    private OnDigitalScaleChangeListener onScaleChangeListener;

    private GestureDetector mGestureDetector;


    public PlayWindowContainer(@NonNull Context context) {
        this(context, null);
    }

    public PlayWindowContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayWindowContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent ev) {
                if (!mDigitalZoomOpen || !mAllowOpenDigitalZoom) {
                    return false;
                }
                if (mLastScale == MAX_SCALE) {
                    midPointDoubleClick(ev);
                    callBackOnScaleChangeListener(1);
                    scale(1);
                } else {
                    callBackOnScaleChangeListener(MAX_SCALE);
                    midPointDoubleClick(ev);
                    scale(MAX_SCALE);
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (onClickListener != null && !mDigitalZoomOpen) {
                    onClickListener.onSingleClick();
                }
                return !mDigitalZoomOpen;
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mOriginalRect.setValue(l, t, r, b);
        if (changed) {
            mVirtualRect.setValue(l, t, r, b);
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (onClickListener == null && onDigitalListener == null && onScaleChangeListener == null) {
            return super.onTouchEvent(event);
        }
        //使用手势检测器来判断单击和双击
        mGestureDetector.onTouchEvent(event);
        //处理缩放事件
        zoom(event);
        return true;
    }


    /**
     * 处理手势放大和缩小事件
     *
     * @param ev 触摸事件
     */
    private void zoom(MotionEvent ev) {
        //不允许开启电子放大，忽略所有事件
        if (!mAllowOpenDigitalZoom) {
            return;
        }
        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (ev.getPointerCount() < 1) {
                    return;
                }

                mActionPointerId = ev.getPointerId(0);

                if (mActionPointerId < 0) {
                    return;
                }

                mLastMotionX = ev.getX();
                mLastMotionY = ev.getY();

                break;
            case MotionEvent.ACTION_MOVE:
                //当电子放大未开启时，需要判断是否需要开启电子放大
                if (!mDigitalZoomOpen) {
                    shouldOpenDigitalZoom(ev);
                    return;
                }
                //即便开启了电子放大，如果未设置监听，也是没效果
                if (onScaleChangeListener == null) {
                    return;
                }
                //判断是否应该继续处理电子放大缩放手势，一开始是true，当缩小手势到关闭电子放大后，为false,
                // 当手指抬离时，也会重置为true
                if (!mShouldHandleScale) {
                    return;
                }

                if (ZOOM_DRAG == mClickMode) {
                    final int index = ev.findPointerIndex(mActionPointerId);
                    if (index < 0) {
                        return;
                    }

                    final float x = ev.getX(index);
                    final float y = ev.getY(index);

                    move(mLastMotionX, mLastMotionY, x, y);

                    mLastMotionX = x;
                    mLastMotionY = y;
                } else if (ZOOM_SCALE == mClickMode) {
                    if (ev.getPointerCount() != 2) {
                        return;
                    }

                    float dis = spacing(ev);
                    float scale = mLastScale + (dis - mLastDis) * UNIT_SCALE_RATIO;

                    mLastDis = dis;

                    if (scale > MAX_SCALE) {
                        scale = MAX_SCALE;
                    }

                    //这个回调需要在两个倍率重置之间，避免出现倍率超过和无法通知上层需要关闭电子放大的问题
                    callBackOnScaleChangeListener(scale);
                    mLastScale = scale;
                    if (scale < 1) {
                        scale = 1;
                        mShouldHandleScale = false;
                        return;
                    }

                    scale(scale);

                    midPoint(ev);

                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mLastDis = spacing(ev);
                mClickMode = ZOOM_SCALE;
                midPoint(ev);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mClickMode = ZOOM_DRAG;
                break;
            case MotionEvent.ACTION_UP:
                //当手指抬离的时候，重置
                mShouldHandleScale = true;
                //当手指抬离时，判断当前的电子放大是否已经关闭
                if (mLastScale < 1){
                    mDigitalZoomOpen = false;
                }
                break;
        }
    }

    /**
     * 判断是否需要开启电子放大
     *
     * @param ev 手势事件
     */
    private void shouldOpenDigitalZoom(MotionEvent ev) {
        float dis = spacing(ev);
        float scale = mLastScale + (dis - mLastDis) * UNIT_SCALE_RATIO;
        //初始开启时的放大倍数需要设置的相对大一点，避免开启的太灵敏
        if (scale > 2.0 && !mDigitalZoomOpen) {
            //开启后把放大倍数重置为1
            mLastScale = 1;
            //记得把距离也进行赋值，否则倍率计算会加上开启之前的倍率
            mLastDis = dis;
            mDigitalZoomOpen = true;
            //电子放大开启回调
            callBackOnScaleOpenListener();
        }
    }


    /**
     * 当有两个手指按在屏幕上时，计算两指之间的距离
     *
     * @param event 触摸事件
     * @return 两指之间的距离
     */
    private float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);

        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 缩放处理Rect
     *
     * @param newScale 缩放比例
     */
    private void scale(float newScale) {
        float w = mOriginalRect.getWidth() * newScale;
        float h = mOriginalRect.getHeight() * newScale;

        float newL = mVirtualRect.getLeft() - mRatioX * (w - mVirtualRect.getWidth());
        float newT = mVirtualRect.getTop() - mRatioY * (h - mVirtualRect.getHeight());
        float newR = newL + w;
        float newB = newT + h;

        mVirtualRect.setValue(newL, newT, newR, newB);

        judge(mOriginalRect, mVirtualRect);

        if (onScaleChangeListener != null) {
            mLastScale = newScale;
            onScaleChangeListener.onDigitalRectChange(mOriginalRect, mVirtualRect);
        }
    }

    private void midPoint(MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);

        mRatioX = Math.abs(x / 2 - mVirtualRect.getLeft()) / mVirtualRect.getWidth();
        mRatioY = Math.abs(y / 2 - mVirtualRect.getTop()) / mVirtualRect.getHeight();
    }


    private void midPointDoubleClick(MotionEvent event) {
        float x = event.getX(0);
        float y = event.getY(0);

        mRatioX = Math.abs(x - mVirtualRect.getLeft()) / mVirtualRect.getWidth();
        mRatioY = Math.abs(y - mVirtualRect.getTop()) / mVirtualRect.getHeight();
    }

    private void move(float lastX, float lastY, float curX, float curY) {

        final float deltaX = curX - lastX;
        final float deltaY = curY - lastY;

        float left = mVirtualRect.getLeft();
        float top = mVirtualRect.getTop();
        float right = mVirtualRect.getRight();
        float bottom = mVirtualRect.getBottom();

        float newL = left + deltaX;
        float newT = top + deltaY;
        float newR = right + deltaX;
        float newB = bottom + deltaY;

        mVirtualRect.setValue(newL, newT, newR, newB);

        judge(mOriginalRect, mVirtualRect);

        if (onScaleChangeListener != null) {
            onScaleChangeListener.onDigitalRectChange(mOriginalRect, mVirtualRect);
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);

        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
        mLastMotionX = ev.getX(newPointerIndex);
        mLastMotionY = ev.getY(newPointerIndex);
        if (pointerId == mActionPointerId) {
            mActionPointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void judge(CustomRect oRect, CustomRect curRect) {

        float oL = oRect.getLeft();
        float oT = oRect.getTop();
        float oR = oRect.getRight();
        float oB = oRect.getBottom();

        float newL = curRect.getLeft();
        float newT = curRect.getTop();
        float newR = curRect.getRight();
        float newB = curRect.getBottom();

        float newW = curRect.getWidth();
        float newH = curRect.getHeight();

        if (newL > oL) {
            newL = oL;
        }
        newR = newL + newW;

        if (newT > oT) {
            newT = oT;
        }
        newB = newT + newH;

        if (newR < oR) {
            newR = oR;
            newL = oR - newW;
        }

        if (newB < oB) {
            newB = oB;
            newT = oB - newH;
        }
        //Log.i("move, " + "scale 1 move: " + " newL: " + newL + " newT: " + newT + " newR: " + newR + " newB: " + newB);
        curRect.setValue(newL, newT, newR, newB);
    }

    /**
     * 设置是否允许开启电子放大 ，只有在视频播放的时候才可以开启电子放大
     *
     * @param mAllowOpenDigitalZoom true-允许  false-禁止
     */
    public void setAllowOpenDigitalZoom(boolean mAllowOpenDigitalZoom) {
        this.mAllowOpenDigitalZoom = mAllowOpenDigitalZoom;
    }

    /**
     * 点击监听
     */
    public interface OnClickListener {
        void onSingleClick();
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    /**
     * 放大倍率监听
     */
    public interface OnDigitalZoomListener {
        /**
         * 电子放大开启监听
         */
        void onDigitalZoomOpen();
    }

    public void setOnDigitalListener(OnDigitalZoomListener onDigitalListener) {
        this.onDigitalListener = onDigitalListener;
    }

    public void callBackOnScaleOpenListener() {
        if (onDigitalListener == null) {
            return;
        }
        onDigitalListener.onDigitalZoomOpen();
    }


    public interface OnDigitalScaleChangeListener {
        /**
         * 电子放大倍率变化回调
         *
         * @param scale 放大倍率
         */
        void onDigitalScaleChange(float scale);

        /**
         * 电子放大显示区域变化
         *
         * @param oRect   上一次的显示区域
         * @param curRect 当前的显示区域
         */
        void onDigitalRectChange(CustomRect oRect, CustomRect curRect);
    }

    public void setOnScaleChangeListener(OnDigitalScaleChangeListener onScaleChangeListener) {
        this.onScaleChangeListener = onScaleChangeListener;
        //关闭电子放大的时候会设置监听为null,这里也将参数进行重置
        if (onScaleChangeListener == null) {
            mVirtualRect.setValue(mOriginalRect.getLeft(), mOriginalRect.getTop(), mOriginalRect.getRight(),
                    mOriginalRect.getBottom());
            mLastMotionY = 0;
            mLastMotionX = 0;
            mLastDis = 0;
            mRatioX = 1;
            mRatioY = 1;
            mLastScale = 0.9999f;//这里重置时，设置为0.9999是为了让手指抬离时的判断成立，标记电子放大为关闭状态
        }
    }

    public void callBackOnScaleChangeListener(float scale) {
        if (onScaleChangeListener == null) {
            return;
        }
        onScaleChangeListener.onDigitalScaleChange(scale);
    }
}
