package com.ecanaveras.gde.waudio.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Created by ecanaveras on 13/07/2017.
 */

public class MarkerView extends androidx.appcompat.widget.AppCompatImageView {

    public interface MarkerListener {
        public void markerTouchStart(MarkerView marker, float pos);

        public void markerTouchMove(MarkerView marker, float pos);

        public void markerTouchEnd(MarkerView marker);

        public void markerFocus(MarkerView marker);

        public void markerLeft(MarkerView marker, int velocity);

        public void markerRight(MarkerView marker, int velocity);

        public void markerEnter(MarkerView marker);

        public void markerKeyUp();

        public void markerDraw();
    }

    private int mVelocity;
    private MarkerListener mListener;

    public MarkerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setFocusable(true);

        mVelocity = 0;
        mListener = null;
    }

    public void setListener(MarkerListener mListener) {
        this.mListener = mListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                requestFocus();
                mListener.markerTouchStart(this, event.getRawX());
                break;
            case MotionEvent.ACTION_MOVE:
                mListener.markerTouchMove(this, event.getRawX());
                break;
            case MotionEvent.ACTION_UP:
                mListener.markerTouchEnd(this);
        }
        return true;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus && mListener != null)
            mListener.markerFocus(this);
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mListener != null)
            mListener.markerDraw();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mVelocity++;
        int v = (int) Math.sqrt(1 + mVelocity / 2);
        if (mListener != null) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mListener.markerLeft(this, v);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mListener.markerRight(this, v);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                mListener.markerEnter(this);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mVelocity = 0;
        if (mListener != null)
            mListener.markerKeyUp();
        return super.onKeyUp(keyCode, event);
    }
}
