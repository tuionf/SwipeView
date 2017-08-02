package com.example.tuionf.swipeview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.example.tuionf.swipeview.State.State;

import java.util.ArrayList;

import static com.example.tuionf.swipeview.State.State.CLOSE;

/**
 * Created by tuion on 2017/7/23.
 */

public class SwipeView extends ViewGroup {

    private Context mContext;
    private Scroller mScroller;

    private int leftResId;
    private int rightResId;
    private int contentResId;
    private View leftView;
    private View rightView;
    private View contentView;
    private MarginLayoutParams contentViewLp;
    private boolean isSwipeing;
    private PointF mLastP;
    private PointF mFirstP;

    private boolean mCanLeftSwipe = true;
    private boolean mCanRightSwipe = true;

    private static SwipeView mViewCache;
    private static State mStateCache;
    private float distanceX;
    private float finalyDistanceX;
    private int mScaledTouchSlop;

    private boolean canLeftSwipe = true;
    private boolean canRightSwipe = true;

    private float mFraction = 0.3f;

    private final ArrayList<View> mMatchParentChildren = new ArrayList<>();

    private static final String TAG = "SwipeView";

    public SwipeView(Context context) {
        this(context,null);
    }

    public SwipeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SwipeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init(context, attrs, defStyleAttr);

    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {

        mScroller = new Scroller(context);
        //获取自定义的属性值
        getAtt(attrs);
    }



    private void getAtt(AttributeSet attrs) {
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs,R.styleable.SwipeView);
        leftResId = typedArray.getResourceId(R.styleable.SwipeView_leftView,-1);
        rightResId = typedArray.getResourceId(R.styleable.SwipeView_rightView,-1);
        contentResId = typedArray.getResourceId(R.styleable.SwipeView_contentView,-1);
        canLeftSwipe = typedArray.getBoolean(R.styleable.SwipeView_canLeftSwipe,true);
        canRightSwipe = typedArray.getBoolean(R.styleable.SwipeView_canRightSwipe,true);
        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childCount = getChildCount();

        //TODO  hhp
        final boolean measureMatchParentChildren =
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                        MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        mMatchParentChildren.clear();
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() != GONE) {
//                measureChild();
//                measureChildren();
//                测量子childView
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                //获取childView中宽高的最大值
                maxWidth = Math.max(maxWidth,child.getMeasuredWidth()+lp.leftMargin+lp.rightMargin);
                maxHeight = Math.max(maxHeight,child.getMeasuredHeight()+lp.topMargin+lp.bottomMargin);

                childState = combineMeasuredStates(childState, child.getMeasuredState());

                if (measureMatchParentChildren) {
                    if (lp.height == LayoutParams.MATCH_PARENT ||
                            lp.width == LayoutParams.MATCH_PARENT){
                        mMatchParentChildren.add(child);
                    }
                }
            }

        }

        //宽度和高度还要考虑背景大小
        maxWidth = Math.max(maxWidth,getSuggestedMinimumWidth());
        maxHeight = Math.max(maxHeight,getSuggestedMinimumHeight());

        //设置具体宽高
        setMeasuredDimension(resolveSizeAndState(maxWidth,widthMeasureSpec,childState),
                resolveSizeAndState(maxHeight,heightMeasureSpec,childState << MEASURED_HEIGHT_STATE_SHIFT));

        //设置MATCH_PARENT的child的宽高
        int count = mMatchParentChildren.size();
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = mMatchParentChildren.get(i);
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                final int childWidthMeasureSpec;
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    final int width = Math.max(0, getMeasuredWidth()
                            - lp.leftMargin - lp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            lp.leftMargin + lp.rightMargin,
                            lp.width);
                }

                final int childHeightMeasureSpec;
                if (lp.height == FrameLayout.LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0, getMeasuredHeight()
                            - lp.topMargin - lp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            lp.topMargin + lp.bottomMargin,
                            lp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int count = getChildCount();
        int leftPadding = getPaddingLeft();
        int rightPadding = getPaddingRight();
        int topPadding = getPaddingTop();
        int bottomPadding = getPaddingBottom();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            if (leftView == null && leftResId == child.getId()) {
                leftView = child;
                leftView.setClickable(true);
            }else if (rightView == null && rightResId == child.getId()) {
                rightView = child;
                rightView.setClickable(true);
            }else if (contentView == null && contentResId == child.getId()) {
                contentView = child;
                contentView.setClickable(true);
            }

        }

        //布局contentView
        int cRight = 0;
        if (contentView != null) {
            contentViewLp = (MarginLayoutParams) contentView.getLayoutParams();
            int cTop = topPadding + contentViewLp.topMargin;
            int cLeft = leftPadding + contentViewLp.leftMargin;
            cRight = rightPadding + contentViewLp.rightMargin + contentView.getMeasuredWidth();
            int cBottom = cTop + contentViewLp.bottomMargin + contentView.getMeasuredHeight();
            contentView.layout(cLeft,cTop,cRight,cBottom);
        }

        if (leftView != null) {
            MarginLayoutParams leftViewLp = (MarginLayoutParams) leftView.getLayoutParams();
            int lTop = topPadding + leftViewLp.topMargin;
            int lLeft = 0 - leftView.getMeasuredWidth() + leftViewLp.leftMargin + leftViewLp.rightMargin;
            int lRight = 0 - leftViewLp.rightMargin;
            int lBottom = lTop + leftView.getMeasuredHeight();
            leftView.layout(lLeft, lTop, lRight, lBottom);
        }
        if (rightView != null) {
            MarginLayoutParams rightViewLp = (MarginLayoutParams) rightView.getLayoutParams();
            int lTop = topPadding + rightViewLp.topMargin;
            int lLeft = contentView.getRight() + contentViewLp.rightMargin + rightViewLp.leftMargin;
            int lRight = lLeft + rightView.getMeasuredWidth();
            int lBottom = lTop + rightView.getMeasuredHeight();
            rightView.layout(lLeft, lTop, lRight, lBottom);
        }
    }

    State result;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        switch(ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                //   System.out.println(">>>>dispatchTouchEvent() ACTION_DOWN");
                isSwipeing = false;
                if (mLastP == null) {
                    mLastP = new PointF();
                }
                mLastP.set(ev.getRawX(), ev.getRawY());
                if (mFirstP == null) {
                    mFirstP = new PointF();
                }
                mFirstP.set(ev.getRawX(), ev.getRawY());
                if (mViewCache != null) {
                    if (mViewCache != this) {
                        mViewCache.handlerSwipeMenu(CLOSE);
                    }
                    // Log.i(TAG, ">>>有菜单被打开");
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE: {
                //   System.out.println(">>>>dispatchTouchEvent() ACTION_MOVE getScrollX:" + getScrollX());
                float distanceX = mLastP.x - ev.getRawX();
                float distanceY = mLastP.y - ev.getRawY();
                if (Math.abs(distanceY) > mScaledTouchSlop && Math.abs(distanceX) > mScaledTouchSlop && Math.abs(distanceY) > Math.abs(distanceX)) {
                    break;
                }
                if (Math.abs(distanceX) <= mScaledTouchSlop){
                    break;
                }
                // Log.i(TAG, ">>>>>distanceX:" + distanceX);

                scrollBy((int) (distanceX), 0);//滑动使用scrollBy
                //越界修正
                if (getScrollX() < 0) {
                    if (!mCanRightSwipe || leftView == null) {
                        scrollTo(0, 0);
                    } else {//左滑
                        if (getScrollX() < leftView.getLeft()) {

                            scrollTo(leftView.getLeft(), 0);
                        }

                    }
                } else if (getScrollX() > 0) {
                    if (!mCanLeftSwipe || rightView == null) {
                        scrollTo(0, 0);
                    } else {
                        if (getScrollX() > rightView.getRight() - contentView.getRight() - contentViewLp.rightMargin) {
                            scrollTo(rightView.getRight() - contentView.getRight() - contentViewLp.rightMargin, 0);
                        }
                    }
                }
                //当处于水平滑动时，禁止父类拦截
                if (Math.abs(distanceX) > mScaledTouchSlop
//                        || Math.abs(getScrollX()) > mScaledTouchSlop
                        ) {
                    //  Log.i(TAG, ">>>>当处于水平滑动时，禁止父类拦截 true");
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                mLastP.set(ev.getRawX(), ev.getRawY());


                break;
            }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    //     System.out.println(">>>>dispatchTouchEvent() ACTION_CANCEL OR ACTION_UP");

                    finalyDistanceX = mFirstP.x - ev.getRawX();
                    if (Math.abs(finalyDistanceX) > mScaledTouchSlop) {
                        //  System.out.println(">>>>P");

                        isSwipeing = true;
                    }
                    result = isShouldOpen(getScrollX());
                    handlerSwipeMenu(result);


                    break;
                }
                default:
                    break;

            }


            return super.dispatchTouchEvent(ev);

        }


        /**
         * 自动设置状态
         *
         * @param result
         */

        private void handlerSwipeMenu(State result) {
            if (result == State.LEFTOPEN) {
                mScroller.startScroll(getScrollX(), 0, leftView.getLeft() - getScrollX(), 0);
                mViewCache = this;
                mStateCache = result;
            } else if (result == State.RIGHTOPEN) {
                mViewCache = this;
                mScroller.startScroll(getScrollX(), 0, rightView.getRight() - contentView.getRight() - contentViewLp.rightMargin - getScrollX(), 0);
                mStateCache = result;
            } else {
                mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);
                mViewCache = null;
                mStateCache = null;
            }
            invalidate();
        }


    @Override
    public void computeScroll() {
        //判断Scroller是否执行完毕：
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            //通知View重绘-invalidate()->onDraw()->computeScroll()
            invalidate();
        }
    }


    /**
     * 根据当前的scrollX的值判断松开手后应处于何种状态
     *
     * @param
     * @param scrollX
     * @return
     */
    private State isShouldOpen(int scrollX) {
        if (!(mScaledTouchSlop < Math.abs(finalyDistanceX))) {
            return mStateCache;
        }
        Log.i(TAG, ">>>finalyDistanceX:" + finalyDistanceX);
        if (finalyDistanceX < 0) {
            //➡滑动
            //1、展开左边按钮
            //获得leftView的测量长度
            if (getScrollX() < 0 && leftView != null) {
                if (Math.abs(leftView.getWidth() * mFraction) < Math.abs(getScrollX())) {
                    return State.LEFTOPEN;
                }
            }
            //2、关闭右边按钮

            if (getScrollX() > 0 && rightView != null) {
                return State.CLOSE;
            }
        } else if (finalyDistanceX > 0) {
            //⬅️滑动
            //3、开启右边菜单按钮
            if (getScrollX() > 0 && rightView != null) {

                if (Math.abs(rightView.getWidth() * mFraction) < Math.abs(getScrollX())) {
                    return State.RIGHTOPEN;
                }

            }
            //关闭左边
            if (getScrollX() < 0 && leftView != null) {
                return State.CLOSE;
            }
        }

        return State.CLOSE;

    }


    @Override
    protected void onDetachedFromWindow() {
        if (this == mViewCache) {
            mViewCache.handlerSwipeMenu(CLOSE);
        }
        super.onDetachedFromWindow();
        //  Log.i(TAG, ">>>>>>>>onDetachedFromWindow");

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this == mViewCache) {
            mViewCache.handlerSwipeMenu(mStateCache);
        }
        // Log.i(TAG, ">>>>>>>>onAttachedToWindow");
    }

    public void resetStatus() {
        if (mViewCache != null) {
            if (mStateCache != null && mStateCache != CLOSE && mScroller != null) {
                mScroller.startScroll(mViewCache.getScrollX(), 0, -mViewCache.getScrollX(), 0);
                mViewCache.invalidate();
                mViewCache = null;
                mStateCache = null;
            }
        }
    }


    public float getFraction() {
        return mFraction;
    }

    public void setFraction(float mFraction) {
        this.mFraction = mFraction;
    }

    public boolean isCanLeftSwipe() {
        return mCanLeftSwipe;
    }

    public void setCanLeftSwipe(boolean mCanLeftSwipe) {
        this.mCanLeftSwipe = mCanLeftSwipe;
    }

    public boolean isCanRightSwipe() {
        return mCanRightSwipe;
    }

    public void setCanRightSwipe(boolean mCanRightSwipe) {
        this.mCanRightSwipe = mCanRightSwipe;
    }

    public static SwipeView getViewCache() {
        return mViewCache;
    }


    public static State getStateCache() {
        return mStateCache;
    }

    private boolean isLeftToRight() {
        if (distanceX < 0) {
            //➡滑动
            return true;
        } else {
            return false;
        }

    }
}
