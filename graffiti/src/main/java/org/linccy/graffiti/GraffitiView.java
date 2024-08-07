package org.linccy.graffiti;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * @author linchenxi
 * 画图和缩放、移动手势的判断，画图由{@link LineView}完成
 */
public class GraffitiView extends RelativeLayout {

    private static final float MAX_SCALE = 10.0F;
    private static final float MIN_SCALE = 1.0f;
    private static final float BORDER = 10f;
    private static final long TO_CANVAS_TIME = ViewConfiguration.getTapTimeout();//触发绘图板onTouch的触摸时间
    private float[] mMatrixValues = new float[9];
    private float mGraffitiX, mGraffitiY;
    private float mOldDistance;
    private boolean mIsDrag = false;
    private boolean mIsClick = false;
    private RelativeLayout mShowView;
    private ImageView mImageView;
    private LineView mLineView;
    private ArrayList<LineView.MarkPath> finishedPaths;
    private Bitmap mCutoutImage = null;
    private PointF mOldPointer = null;
    private float initImageWidth;
    private float initImageHeight;
    private int lastHashcode = 0;
    private OnGraffitiViewOnClickListener mOnGraffitiViewOnClick;

    @SuppressLint("ClickableViewAccessibility")
    public GraffitiView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GraffitiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraffitiView(Context context) {
        this(context, null, 0);
    }

    public void setOnGraffitiViewOnClick(OnGraffitiViewOnClickListener onGraffitiViewOnClick) {
        this.mOnGraffitiViewOnClick = onGraffitiViewOnClick;
    }

    public interface OnGraffitiViewOnClickListener {
        void onGraffitiClick();
    }

    public void setImage(Bitmap bitmap) {
        if (mImageView == null) {
            mImageView = new ImageView(getContext());
        }
        mCutoutImage = bitmap;
    }

    public void setPenColor(int color) {
        mLineView.setPenColor(color);
    }

    public void setPenType(LineView.MarkPath.MarkType type) {
        mLineView.setPenType(type);
    }

    public void undo() {
        mLineView.undo();
    }

    public void redo() {
        mLineView.redo();
    }

    public void clear() {
        mLineView.clear();
    }

    public void release() {
        if (null != mCutoutImage) {
            mCutoutImage.recycle();
        }
    }

    public ArrayList<LineView.MarkPath> getFinishedPaths() {
        return mLineView.getFinishedPaths();
    }

    public void setFinishedPaths(ArrayList<LineView.MarkPath> finishedPaths) {
        this.finishedPaths = finishedPaths;
        if (null != mLineView) {
            mLineView.setFinishedPaths(finishedPaths);
            mLineView.invalidate();
        }
    }

    public Bitmap getResultBitmap() {
        lastHashcode = mLineView.getLastHashCode();

        RectF clipRect = new RectF();
        clipRect.top = mImageView.getY();
        clipRect.left = mImageView.getX();
        clipRect.bottom = mImageView.getHeight();
        clipRect.right = mImageView.getWidth();

        PointF srcSize = new PointF();
        srcSize.x = mCutoutImage.getWidth();
        srcSize.y = mCutoutImage.getHeight();

        Bitmap bitmap = mLineView.getBCResutlImage(clipRect, srcSize);

        Bitmap resultBitmap = Bitmap.createBitmap(mCutoutImage.getWidth(), mCutoutImage.getHeight(), Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(mCutoutImage, 0, 0, null);
        canvas.drawBitmap(bitmap, 0, 0, null);

        return resultBitmap;
    }

    private long mOnACTION_DOWN_TIME;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mIsClick = true;
                mOnACTION_DOWN_TIME = System.currentTimeMillis();
                return mLineView.onTouchEvent(event);

            case MotionEvent.ACTION_POINTER_DOWN:
                mIsDrag = true;
                mIsClick = false;
                mOldDistance = spacingOfTwoFinger(event);
                mOldPointer = middleOfTwoFinger(event);
                //设置放大和旋转的中心
//                mShowView.setPivotX((event.getX(0) + event.getX(1)) / 2);
//                mShowView.setPivotY((event.getY(0) + event.getY(1)) / 2);
//                startScale = false;
                break;

            case MotionEvent.ACTION_MOVE:
                mIsClick = false;
                long on_move_time = System.currentTimeMillis();
                if (on_move_time - mOnACTION_DOWN_TIME <= TO_CANVAS_TIME) {
                    mIsClick = true;
                    return true;
                }
                if (!mIsDrag) return mLineView.onTouchEvent(event);
                if (event.getPointerCount() != 2) break;
                float newDistance = spacingOfTwoFinger(event);
                float scaleFactor = newDistance / mOldDistance;
                scaleFactor = checkingScale(mShowView.getScaleX(), scaleFactor);
//                if (startScale) {
                mShowView.setScaleX(mShowView.getScaleX() * scaleFactor);
                mShowView.setScaleY(mShowView.getScaleY() * scaleFactor);
//                }
                mOldDistance = newDistance;

                PointF newPointer = middleOfTwoFinger(event);
                mShowView.setX(mShowView.getX() + newPointer.x - mOldPointer.x);
                mShowView.setY(mShowView.getY() + newPointer.y - mOldPointer.y);
                mOldPointer = newPointer;
                checkingGraffiti();
//                startScale = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                break;

            case MotionEvent.ACTION_UP:
                if (mIsClick) {
                    if (mOnGraffitiViewOnClick != null) {
                        mOnGraffitiViewOnClick.onGraffitiClick();
                        return true;
                    }
                }
                if (!mIsDrag) return mLineView.onTouchEvent(event);
                mShowView.getMatrix().getValues(mMatrixValues);
                mLineView.setScaleAndOffset(mShowView.getScaleX(), mMatrixValues[2], mMatrixValues[5]);
                mIsDrag = false;
                break;
        }
        return true;

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        float bitmapWidth = (float) mCutoutImage.getWidth();
        float bitmapHeight = (float) mCutoutImage.getHeight();
        initImageWidth = 0;
        initImageHeight = 0;

        if (bitmapWidth > bitmapHeight) {
            initImageWidth = getWidth() - 2 * BORDER;
            initImageHeight = (bitmapHeight / bitmapWidth) * initImageWidth;
        } else {
            initImageHeight = getHeight() - 2 * BORDER;
            initImageWidth = (bitmapWidth / bitmapHeight) * initImageHeight;
        }

        mShowView = new RelativeLayout(getContext());
        LayoutParams showViewParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        mImageView = new ImageView(getContext());
        LayoutParams imageViewParams = new LayoutParams((int) initImageWidth, (int) initImageHeight);
        if (mCutoutImage != null) {
            mImageView.setImageBitmap(mCutoutImage);
        }
        imageViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        mLineView = new LineView(getContext());
        checkFinishedPaths();
        LayoutParams lineViewParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        mShowView.addView(mImageView, imageViewParams);
        mShowView.addView(mLineView, lineViewParams);

        addView(mShowView, showViewParams);

        mGraffitiX = (getWidth() - initImageWidth) / 2;
        mGraffitiY = (getHeight() - initImageHeight) / 2;
        mLineView.requestLayout();
    }

    private void checkFinishedPaths() {
        if (null != finishedPaths) {
            mLineView.setFinishedPaths(finishedPaths);
            mLineView.invalidate();
            finishedPaths = null;
        }
    }

    private float checkingScale(float scale, float scaleFactor) {
        if ((scale <= MAX_SCALE && scaleFactor > 1.0) || (scale >= MIN_SCALE && scaleFactor < 1.0)) {
            if (scale * scaleFactor < MIN_SCALE) {
                scaleFactor = MIN_SCALE / scale;
            }

            if (scale * scaleFactor > MAX_SCALE) {
                scaleFactor = MAX_SCALE / scale;
            }

        }

        return scaleFactor;
    }

    private void checkingGraffiti() {
        PointF offset = offsetGraffiti();
        mShowView.setX(mShowView.getX() + offset.x);
        mShowView.setY(mShowView.getY() + offset.y);
        if (mShowView.getScaleX() == 1) {
            mShowView.setX(0);
            mShowView.setY(0);
        }
    }

    private PointF offsetGraffiti() {
        PointF offset = new PointF(0, 0);
        if (mShowView.getScaleX() > 1) {
            mShowView.getMatrix().getValues(mMatrixValues);
            if (mMatrixValues[2] > -(mGraffitiX * (mShowView.getScaleX() - 1))) {
                offset.x = -(mMatrixValues[2] + mGraffitiX * (mShowView.getScaleX() - 1));
            }

            if (mMatrixValues[2] + mShowView.getWidth() * mShowView.getScaleX() - mGraffitiX * (mShowView.getScaleX() - 1) < getWidth()) {
                offset.x = getWidth() - (mMatrixValues[2] + mShowView.getWidth() * mShowView.getScaleX() - mGraffitiX * (mShowView.getScaleX() - 1));
            }

            if (mMatrixValues[5] > -(mGraffitiY * (mShowView.getScaleY() - 1))) {
                offset.y = -(mMatrixValues[5] + mGraffitiY * (mShowView.getScaleY() - 1));
            }

            if (mMatrixValues[5] + mShowView.getHeight() * mShowView.getScaleY() - mGraffitiY * (mShowView.getScaleY() - 1) < getHeight()) {
                offset.y = getHeight() - (mMatrixValues[5] + mShowView.getHeight() * mShowView.getScaleY() - mGraffitiY * (mShowView.getScaleY() - 1));
            }
        }

        return offset;
    }

    public boolean isChanged() {
        return mLineView != null && lastHashcode != mLineView.getLastHashCode();
    }

    public void setChangedOver() {
        lastHashcode = mLineView.getLastHashCode();
    }

    /**
     * 沿Z轴旋转
     * 每次加90度
     */
    public void rotate(float rotation) {
        setRotation(getRotation() + rotation);
    }

    /**
     * 计算两个触控点之间的距离
     *
     * @param event 触控事件
     * @return 触控点之间的距离
     */
    public static float spacingOfTwoFinger(MotionEvent event) {
        if (event.getPointerCount() != 2) return 0;
        double dx = event.getX(0) - event.getX(1);
        double dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算两个触控点形成的角度
     *
     * @param event 触控事件
     * @return 角度值
     */
    public static float rotationDegreeOfTwoFinger(MotionEvent event) {
        if (event.getPointerCount() != 2) return 0;
        double dx = (event.getX(0) - event.getX(1));
        double dy = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(dy, dx);
        return (float) Math.toDegrees(radians);
    }

    /**
     * 计算两个触控点的中点
     *
     * @param event 触控事件
     * @return 中点浮点类
     */
    public static PointF middlePointFOfTwoFinger(MotionEvent event) {
        if (event.getPointerCount() != 2) return null;
        float mx = (event.getX(0) + event.getX(1)) / 2;
        float my = (event.getY(0) + event.getY(1)) / 2;
        return new PointF(mx, my);
    }

    /**
     * 获得触控事件的坐标点
     *
     * @param event 触控事件
     * @return 坐标点浮点类
     */
    public static PointF getPointFFromEvent(MotionEvent event) {
        return new PointF(event.getX(), event.getY());
    }

    public static PointF middleOfTwoFinger(MotionEvent event) {
        float mx = (event.getX(0) + event.getX(1)) / 2;
        float my = (event.getY(0) + event.getY(1)) / 2;
        return new PointF(mx, my);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);
    }
}
