package org.linccy.graffiti;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linchenxi
 * 画图操作，由{@link GraffitiView}传递触摸事件
 */
@SuppressLint("ClickableViewAccessibility")
public class LineView extends View {

    private float mCurrentLineWidth = MarkPath.NORMAL_LINE_WIDTH;
    private boolean mIsDoubleTouch = false;
    private int mPathCount = 0;
    private ArrayList<MarkPath> mFinishedPaths = new ArrayList<>();
    private MarkPath mCurrentPath = null;
    private int color = Color.RED;

    private Bitmap mBitmap = null;
    private Canvas mTempCanvas = null;
    private Paint mPaint = null;
    private float mScale = 1;
    private PointF mOffset = new PointF(0, 0);

    /**
     * 保存down下的坐标点
     */
    private PointF mCurrentPoint;

    /**
     * 当前的标记类型
     */
    private MarkPath.MarkType mCurrentType = MarkPath.MarkType.PEN_COLOR;
    private int width;
    private int height;

    public LineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public LineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineView(Context context) {
        this(context, null, 0);
    }

    public void setScaleAndOffset(float scale, float dx, float dy) {
        mScale = scale;
        mCurrentLineWidth = MarkPath.NORMAL_LINE_WIDTH / mScale;
        mOffset.x = dx;
        mOffset.y = dy;
    }

    public void setPenColor(int color) {
        this.color = color;
        if (mCurrentPath != null) {
            mCurrentPath.setPaintColor(color);
        }
    }

    public void setPenType(MarkPath.MarkType type) {
        mCurrentType = type;
    }

    /**
     * 撤销 上一个MarkPath 对象画的线
     */
    public void undo() {
        if (mPathCount > 0) {
            mPathCount--;
        }
        invalidate();
    }

    public void redo() {
        if (!mFinishedPaths.isEmpty()) {
            if (mPathCount < mFinishedPaths.size()) {
                mPathCount++;
            }
        }
        invalidate();
    }

    public void clear() {
        if (mPathCount != 0) {
            mPathCount = 0;
            mFinishedPaths.clear();
            invalidate();
        }
    }

    public ArrayList<MarkPath> getFinishedPaths() {
        return mFinishedPaths;
    }

    public void setFinishedPaths(ArrayList<MarkPath> finishedPaths) {
        this.mFinishedPaths.clear();
        this.mFinishedPaths.addAll(finishedPaths);
        mPathCount = finishedPaths.size();
    }

    public Bitmap getBCResutlImage(RectF clipRect, PointF srcSize) {
        Bitmap drawBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(drawBitmap);
        canvas.drawColor(Color.TRANSPARENT);
        for (int i = 0; i < mPathCount; i++) {
            mFinishedPaths.get(i).drawBCResultPath(canvas);
        }

        Bitmap clipBitmap = Bitmap.createBitmap(drawBitmap, (int) clipRect.left, (int) clipRect.top, (int) clipRect.right, (int) clipRect.bottom, null, false);
        Bitmap resultBitmap = Bitmap.createScaledBitmap(clipBitmap, (int) srcSize.x, (int) srcSize.y, true);
        drawBitmap.recycle();
        clipBitmap.recycle();
        return resultBitmap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCurrentPoint = new PointF((event.getX() - mOffset.x) / mScale, (event.getY() - mOffset.y) / mScale);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsDoubleTouch = false;
                mCurrentPath = MarkPath.newMarkPath(mCurrentPoint, color);
                mCurrentPath.setCurrentMarkType(mCurrentType);
                mCurrentPath.setWidth(mCurrentLineWidth);
                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mIsDoubleTouch = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mCurrentPath == null || mIsDoubleTouch) break;
                mCurrentPath.addMarkPointToPath(mCurrentPoint);
                postInvalidateDelayed(40);
                break;

            case MotionEvent.ACTION_UP:
                if (mCurrentPath != null && !mIsDoubleTouch) {
                    mCurrentPath.addMarkPointToPath(mCurrentPoint);
                    //如果是点击了撤销后，撤销的笔画移出栈，并将新的笔画压入栈
                    if (mPathCount < mFinishedPaths.size()) {
                        int oldSize = mFinishedPaths.size();
                        for (int i = oldSize; i > mPathCount; i--) {
                            mFinishedPaths.remove(i - 1);
                        }
                    }
                    mFinishedPaths.add(mCurrentPath);

                    mPathCount++;
                }

                mIsDoubleTouch = false;
                mCurrentPath = null;
                invalidate();
                break;
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mTempCanvas = new Canvas(mBitmap);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //清空Bitmap画布
        mTempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        width = getWidth();
        height = getHeight();
        if (mFinishedPaths.size() >= 0) {
            for (int i = 0; i < mPathCount; i++) {
                mFinishedPaths.get(i).drawMarkPath(mTempCanvas);
            }
        }

        if (mCurrentPath != null) {
            mCurrentPath.drawMarkPath(mTempCanvas);
        }

        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }

    /**
     * 用于记录绘制路径
     */
    public static class MarkPath implements Parcelable {

        protected MarkPath(Parcel in) {
            mCurrentWidth = in.readFloat();
            mPrevPoint = in.readParcelable(PointF.class.getClassLoader());
        }

        public static final Creator<MarkPath> CREATOR = new Creator<>() {
            @Override
            public MarkPath createFromParcel(Parcel in) {
                return new MarkPath(in);
            }

            @Override
            public MarkPath[] newArray(int size) {
                return new MarkPath[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeFloat(mCurrentWidth);
            dest.writeParcelable(mPrevPoint, flags);
        }

        public enum MarkType {
            PEN_COLOR,
            PEN_ERASER
        }

        public static final float NORMAL_LINE_WIDTH = 10.0f;

        private enum LineType {
            MARK,
            BCRESULT,
        }

        private static final float ERASER_FACTOT = (float) 1.5;
        private Paint sPaint = null;
        private static PorterDuffXfermode sClearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

        private static final float TOUCH_TOLERANCE = 4.0f;

        private CustomPath mPath;
        private float mCurrentWidth = NORMAL_LINE_WIDTH;
        private PointF mPrevPoint;
        private MarkType mCurrentMarkType = MarkType.PEN_COLOR;

        private MarkPath() {
            mPath = new CustomPath();
        }

        public static MarkPath newMarkPath(PointF point, int color) {
            MarkPath newPath = new MarkPath();
            newPath.mPath.moveTo(point.x, point.y);
            newPath.mPrevPoint = point;

            newPath.sPaint = new Paint();
            newPath.sPaint.setAntiAlias(true);
            newPath.sPaint.setDither(true);
            newPath.sPaint.setStyle(Paint.Style.STROKE);
            newPath.sPaint.setStrokeJoin(Paint.Join.ROUND);
            newPath.sPaint.setStrokeCap(Paint.Cap.ROUND);
            newPath.sPaint.setColor(color);
            return newPath;
        }

        private static MarkPath restoreMarkPath(CustomPath path, int color, PointF point) {
            MarkPath newPath = new MarkPath();
            newPath.mPath = path;
            newPath.mPrevPoint = point;

            newPath.sPaint = new Paint();
            newPath.sPaint.setAntiAlias(true);
            newPath.sPaint.setDither(true);
            newPath.sPaint.setStyle(Paint.Style.STROKE);
            newPath.sPaint.setStrokeJoin(Paint.Join.ROUND);
            newPath.sPaint.setStrokeCap(Paint.Cap.ROUND);
            newPath.sPaint.setColor(color);
            return newPath;
        }

        /**
         * addMarkPointToPath 将坐标点添加到路径当中
         *
         * @param point， p2当前的点
         */
        public void addMarkPointToPath(PointF point) {
            float dx = Math.abs(point.x - mPrevPoint.x);
            float dy = Math.abs(point.y - mPrevPoint.y);

            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mPrevPoint.x, mPrevPoint.y, (point.x + mPrevPoint.x) / 2, (point.y + mPrevPoint.y) / 2);
            }
            mPrevPoint = point;
        }

        public void drawMarkPath(Canvas canvas) {
            resetPaint(LineType.MARK);
            canvas.drawPath(mPath, sPaint);
        }

        public void drawBCResultPath(Canvas canvas) {
            resetPaint(LineType.BCRESULT);
            canvas.drawPath(mPath, sPaint);
        }

        public MarkType getCurrentMarkType() {
            return mCurrentMarkType;
        }

        public void setCurrentMarkType(MarkType currentMarkType) {
            mCurrentMarkType = currentMarkType;
        }

        public void setWidth(float width) {
            mCurrentWidth = width;
        }

        private void setPaintColor(int color) {
            sPaint.setColor(color);
        }

        //	ERASER
        private void resetPaint(LineType lineType) {
            switch (mCurrentMarkType) {
                case PEN_COLOR:
                    setNormalPaint();
                    break;
                case PEN_ERASER:
                    sPaint.setAlpha(Color.TRANSPARENT);
                    sPaint.setXfermode(sClearMode);
                    sPaint.setStrokeWidth(mCurrentWidth * ERASER_FACTOT);
                    break;

                default:
                    break;
            }
        }

        private void setNormalPaint() {
            sPaint.setXfermode(null);
            sPaint.setAntiAlias(true);
            sPaint.setDither(true);
            sPaint.setStrokeWidth(mCurrentWidth);
        }

        public static String toJson(ArrayList<MarkPath> finishedPaths) {
            JSONObject object = new JSONObject();
            JSONArray ja = new JSONArray();
            try {
                object.put("ja", ja);
                object.put("version", "1");
                for (MarkPath markPath : finishedPaths) {
                    JSONObject obj = new JSONObject();
                    obj.put("page", 0);
                    obj.put("marktype", markPath.mCurrentMarkType.name());
                    obj.put("stroke", markPath.mCurrentWidth);
                    obj.put("color", markPath.sPaint.getColor());
                    List<CustomPath.PathAction> actions = markPath.mPath.getActions();
                    if (null != actions && actions.size() > 0) {
                        JSONArray pathJa = new JSONArray();
                        obj.put("actions", pathJa);
                        for (CustomPath.PathAction action : actions) {
                            JSONObject actionObj = new JSONObject();
                            actionObj.put("x", action.getX());
                            actionObj.put("y", action.getY());
                            actionObj.put("type", action.getType().name());
                            pathJa.put(actionObj);
                        }
                        obj.put("path", pathJa);
                    }
                    ja.put(obj);
                }
            } catch (JSONException e) {
                Log.e("TAG", e.getMessage());
            }
            String rs = object.toString();
            Log.d("TAG", "toJson:" + rs);
            return rs;
        }

        public static ArrayList<MarkPath> fromJson(String json) {
            Log.d("TAG", "fromJson:" + json);
            ArrayList<MarkPath> paths = new ArrayList<>();
            try {
                JSONObject object = new JSONObject(json);
                JSONArray ja = object.optJSONArray("ja");
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject obj = ja.optJSONObject(i);
                    CustomPath customPath = null;
                    int color = obj.optInt("color");
                    JSONArray pathJa = obj.optJSONArray("path");
                    PointF point = null;
                    if (null != pathJa && pathJa.length() > 0) {
                        customPath = new CustomPath();
                        List<CustomPath.PathAction> actions = new ArrayList<>();
                        customPath.setActions(actions);
                        for (int j = 0; j < pathJa.length(); j++) {
                            JSONObject actionObj = pathJa.optJSONObject(j);
                            float x = actionObj.optInt("x");
                            float y = actionObj.optInt("y");
                            if (j == 0) {
                                point = new PointF(x, y);
                            }
                            String type = actionObj.optString("type");
                            CustomPath.PathAction.PathActionType actionType = CustomPath.PathAction.PathActionType.valueOf(type);
                            if (actionType == CustomPath.PathAction.PathActionType.LINE_TO) {
                                CustomPath.PathAction action = new CustomPath.ActionLine(x, y);
                                actions.add(action);
                            } else {
                                CustomPath.PathAction action = new CustomPath.ActionMove(x, y);
                                actions.add(action);
                            }
                        }
                        customPath.drawThisPath();
                    }
                    MarkPath markPath = restoreMarkPath(customPath, color, point);
                    markPath.mCurrentWidth = obj.optInt("stroke");
                    markPath.mCurrentMarkType = MarkType.valueOf(obj.optString("marktype"));
                    paths.add(markPath);
                }
            } catch (JSONException e) {
                Log.e("TAG", e.getMessage());
            }
            return paths;
        }
    }

    public int getPathCount() {
        return mPathCount;
    }

    public int getLastHashCode() {
        if (mPathCount > 0) {
            return mFinishedPaths.get(mPathCount - 1).hashCode();
        }
        return 0;

    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        Parcelable superData = super.onSaveInstanceState();
        bundle.putParcelable("super_data", superData);
        bundle.putParcelableArrayList("finish_path", mFinishedPaths);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        Parcelable superData = bundle.getParcelable("super_data");
        mFinishedPaths = bundle.getParcelableArrayList("finish_path");
        super.onRestoreInstanceState(superData);
    }
}
