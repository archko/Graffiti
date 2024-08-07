package org.linccy.graffiti_sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tencent.mmkv.MMKV;

import org.linccy.graffiti.GraffitiView;
import org.linccy.graffiti.LineView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;

/**
 * Created by lcx on 12/21/16.
 * 画图fragment
 */
public class GraffitiFragment extends Fragment implements View.OnClickListener {
    /*public static final int[] m_penColors =
            {
                    Color.argb(128, 32, 79, 140),
                    Color.argb(156, 255, 0, 0),//红色画笔
                    Color.argb(156, 241, 221, 2),//黄色画笔
                    Color.argb(156, 0, 138, 255),//蓝色画笔
                    Color.argb(128, 40, 36, 37),
                    Color.argb(128, 226, 226, 226),
                    Color.argb(128, 219, 88, 50),
                    Color.argb(128, 129, 184, 69)
            };

    public static final float[] m_PenStrock =
            {
                    12,
                    14,
                    16,
                    18,
                    20,
                    22,
                    24,
                    26

            };*/
    public static final int REDO = 0;
    public static final int UNDO = 1;
    public static final int CLEAR = 2;
    public static final int ROTATE = 3;
    public static final int ERASER = 4;
    public static final int SELECT_COLOR = 5;

    public static int ROTATE_ANGLE = 90;
    private int color = Color.YELLOW;

    private FrameLayout checkView;
    private GraffitiView mGraffitiView;
    private GraffitiView.OnGraffitiViewOnClickListener onGraffitiViewOnClickListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graffiti, container, false);
        mGraffitiView = new GraffitiView(getContext());
        mGraffitiView.setOnGraffitiViewOnClick(onGraffitiViewOnClickListener);
        mGraffitiView.setId(R.id.graffitiview);     //setId，当页面被移除后恢复时GraffitiView调用保存状态
        mGraffitiView.setSaveEnabled(true);
        checkView = view.findViewById(R.id.graffiti);

        view.findViewById(R.id.color_selector).setOnClickListener(this);
        view.findViewById(R.id.undo).setOnClickListener(this);
        view.findViewById(R.id.redo).setOnClickListener(this);
        view.findViewById(R.id.clear).setOnClickListener(this);
        view.findViewById(R.id.round).setOnClickListener(this);
        view.findViewById(R.id.save).setOnClickListener(this);
        view.findViewById(R.id.eraser).setOnClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setImage();
    }

    @Override
    public void onPause() {
        super.onPause();
        ArrayList<LineView.MarkPath> lines = mGraffitiView.getFinishedPaths();
        if (null != lines) {
            MMKV.defaultMMKV().encode("line", LineView.MarkPath.toJson(lines));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.undo) {
            action(GraffitiFragment.UNDO);
        } else if (id == R.id.redo) {
            action(GraffitiFragment.REDO);
        } else if (id == R.id.clear) {
            action(GraffitiFragment.CLEAR);
        } else if (id == R.id.round) {
            action(GraffitiFragment.ROTATE);
        } else if (id == R.id.eraser) {
            action(GraffitiFragment.ERASER);
        } else if (id == R.id.color_selector) {
            action(GraffitiFragment.SELECT_COLOR);
        } else if (id == R.id.save) {
            String local = saveAndClear();
            Toast.makeText(requireActivity(), "文件保存在：" + local, Toast.LENGTH_LONG).show();
        }
    }

    public void setPenColor(int color) {
        mGraffitiView.setPenColor(color);
    }

    public void action(int action) {
        switch (action) {
            case REDO:
                mGraffitiView.redo();
                break;
            case UNDO:
                mGraffitiView.undo();
                break;
            case CLEAR:
                mGraffitiView.clear();
                break;
            case ROTATE:
                mGraffitiView.rotate(ROTATE_ANGLE);
                break;
            case ERASER:
                mGraffitiView.setPenType(LineView.MarkPath.MarkType.PEN_ERASER);
                break;
            case SELECT_COLOR:
                selectColor();
                break;
            default:
                break;
        }
    }

    private void selectColor() {
        new ColorPickerDialog()
                .withColor(color)
                .withListener((pickerView, color) -> {
                    GraffitiFragment.this.color = color;
                    mGraffitiView.setPenType(LineView.MarkPath.MarkType.PEN_COLOR);
                    setPenColor(color);
                })
                .show(requireActivity().getSupportFragmentManager(), "colorPicker");
    }

    /**
     * 设置画图View的背景图
     * 通过rxJava下载图片
     *
     * @param url 图片的地址
     */
    private void setImage() {
        if (mGraffitiView == null) {
            mGraffitiView = new GraffitiView(getContext());
            mGraffitiView.setOnGraffitiViewOnClick(onGraffitiViewOnClickListener);
        }
        String json = MMKV.defaultMMKV().decodeString("line");
        if (!TextUtils.isEmpty(json)) {
            ArrayList<LineView.MarkPath> lines = LineView.MarkPath.fromJson(json);
            if (null != lines) {
                mGraffitiView.setFinishedPaths(lines);
            }
        }
        Bitmap bitmap = BitmapFactory.decodeResource(getContext().getResources(), org.linccy.graffiti.R.drawable.test);
        mGraffitiView.setImage(bitmap);

        checkView.removeAllViews();
        checkView.addView(mGraffitiView);
    }

    public String saveAndClear() {
        if (mGraffitiView != null && mGraffitiView.isChanged()) {
            //local = saveEditPic(mGraffitiView.getResultBitmap());
            //setChangedOver();
        }
        return null;
    }

    public boolean graffitiIsChanged() {
        return mGraffitiView != null && mGraffitiView.isChanged();
    }

    private void setChangedOver() {
        mGraffitiView.setChangedOver();
    }

    public void setOnGraffitiViewOnClickListener(GraffitiView.OnGraffitiViewOnClickListener onGraffitiViewOnClickListener) {
        this.onGraffitiViewOnClickListener = onGraffitiViewOnClickListener;
    }

    @Override
    public void onDestroy() {
        if (null != mGraffitiView) {
            mGraffitiView.release();
            mGraffitiView.removeAllViews();
        }
        super.onDestroy();
    }
}
