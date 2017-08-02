package com.example.tuionf.swipeview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by tuion on 2017/7/26.
 */

public class SwipeMenuLayout extends ViewGroup {

    private Scroller mScroller;
    private int mScaledTouchSlop;
    private int mContentWidth;
    private int mRightMenuWidths;
    private float lastx;
    private float lasty;
    private float firstx;
    private float firsty;

    private static final String TAG = "SwipeMenuLayout";

    public SwipeMenuLayout(Context context) {
        this(context,null);
    }

    public SwipeMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SwipeMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mScaledTouchSlop = viewConfiguration.getScaledTouchSlop();
        mScroller = new Scroller(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int mHeight = 0;
        mContentWidth = 0;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View childView = getChildAt(i);
            childView.setClickable(true);

            if (childView.getVisibility() != GONE) {
                measureChild(childView,widthMeasureSpec,heightMeasureSpec);
                mHeight = Math.max(mHeight,childView.getMeasuredHeight());

                if (i == 0) {
                    //第一个为内容view
                    mContentWidth = childView.getMeasuredWidth();
                } else {
                    //第二个为左滑按钮
                    mRightMenuWidths = childView.getMeasuredWidth();
                }

            }
        }

        setMeasuredDimension(mContentWidth+getPaddingLeft()+getPaddingRight(),
                getPaddingBottom()+getPaddingTop()+mHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView.getVisibility() != GONE) {
                if (i == 0) {
                    childView.layout(getPaddingLeft(), getPaddingTop(), getPaddingLeft() + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());

                } else {
                    childView.layout(mContentWidth, getPaddingTop(), mContentWidth + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                }
            }
        }

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.d(TAG, "dispatchTouchEvent() called with: " + "ev = [" + event + "]");

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                lastx = event.getRawX();
                lasty = event.getRawY();
                firstx = event.getRawX();
                firsty = event.getRawY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                //对左边界进行处理
                float distance = lastx - event.getRawX();

                if (Math.abs(distance) > mScaledTouchSlop) {
                    // 当手指拖动值大于mScaledTouchSlop值时，认为应该进行滚动，拦截子控件的事件
                    return true; // true——被当前视图消费，不再分发
                }
                break;

            }

        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch(event.getAction()){
            case MotionEvent.ACTION_MOVE:
                float distance = lastx - event.getRawX();
                Log.e(TAG, "onTouchEvent() ACTION_MOVE getScrollX:" + getScrollX());

                lastx = event.getRawX();
                lasty = event.getRawY();
                scrollBy((int) distance, 0);
                break;
            case MotionEvent.ACTION_UP:
                if (getScrollX() <= 0) {
                    //对右边界进行处理，不让其滑出
                    mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);
                } else if (getScrollX() > 0 && getScrollX() >= mRightMenuWidths / 3) {
                    //删除按钮滑出区域大于1／3，滑出删除按钮
                    mScroller.startScroll(getScrollX(), 0, mRightMenuWidths-getScrollX(), 0);

                } else {
                    //删除按钮滑出区域小于1／3，滑回原来的位置
                    mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);

                }
                invalidate();
                //通知View重绘-invalidate()->onDraw()->computeScroll()

                break;
             default:
                break;
        }

        return super.onTouchEvent(event);
    }
}
