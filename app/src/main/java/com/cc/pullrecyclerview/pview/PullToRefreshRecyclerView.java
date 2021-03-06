package com.cc.pullrecyclerview.pview;

import android.animation.Animator;
import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cc.pullrecyclerview.pview.animation.BaseAnimation;
import com.cc.pullrecyclerview.pview.animation.TranslateAnimation;
import com.cc.pullrecyclerview.pview.listener.ILoadMoreState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * description:支持下拉刷新和加载更多的RecyclerView
 * author: chencong
 * date: 2017/10/24
 */

public class PullToRefreshRecyclerView extends RecyclerView {
    private final String TAG = "PullToRefreshRecycler";
    /**
     * 底部布局
     */
    private ILoadMoreState mLoadMore;
    /**
     * 空布局
     */
    private FrameLayout mEmptyContainer;
    /**
     * 空布局展示时是否展示header
     */
    private boolean mShowEmptyWithHeader = false;
    /**
     * 空布局展示时是否展示footer
     */
    private boolean mShowEmptyWithFooter = false;
    /**
     * 上下文
     */
    private Context mContext;
    /**
     * 下拉布局
     */
    private ArrowRefreshHeader mArrowView;
    /**
     * 加载更多的标记
     */
    private boolean isNoMore = false;
    /**
     * 进入动画的加载市场
     */
    private int mLoadAnimationDuration = 400;
    /**
     * 动画插值器
     */
    private Interpolator mInterpolator;
    /**
     * 进入动画
     */
    private BaseAnimation mAnimation;
    /**
     * 开启进入动画的标记
     */
    private boolean isCanStartLoadAnimation = false;
    /**
     * 头布局父容器
     */
    private LinearLayout mHeaderContainer;
    /**
     * 尾布局容器
     */
    private LinearLayout mFooterContainer;
    /**
     * 是否正在加载,为了防止加载更多的时候多次触发
     */
    private boolean isLoadingMore = false;
    /**
     * 加载模式
     */
    private int mCurrentMode = MODE_REFRESH_ALL;
    /**
     * 相关的type
     */
    private final int VIEW_TYPE_ARROW = 10000;
    private final int VIEW_TYPE_HEADER = 20000;
    private final int VIEW_TYPE_FOOTER = 30000;
    private final int VIEW_TYPE_EMPTY = 50000;
    private final int VIEW_TYPE_LOADING = 40000;
    /**
     * 相关滑动模式
     */
    public static final int MODE_NONE = 997;
    public static final int MODE_REFRESH_TOP = 998;
    public static final int MODE_REFRESH_BOTTOM = 999;
    public static final int MODE_REFRESH_ALL = 1000;
    /**
     * 内部封转adapter
     */
    private WarpAdapter mWrapAdapter;
    /**
     * 回调监听
     */
    private LoadingListener mLoadingListener;
    private float mLastY = -1;
    /**
     * 数据观察触发
     */
    private final AdapterDataObserver mDataObserver = new DataObserver();

    private boolean isOnlyFirstPlayAnima = true;

    private int mLastPosition = -1;

    @RestrictTo(LIBRARY_GROUP)
    @IntDef(value = {MODE_NONE, MODE_REFRESH_BOTTOM, MODE_REFRESH_TOP, MODE_REFRESH_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FreshMode {
    }

    public interface LoadingListener {

        void onRefresh();

        void onLoadMore();
    }


    public PullToRefreshRecyclerView(Context context) {
        this(context, null);
    }

    public PullToRefreshRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    private void init() {
        mArrowView = new ArrowRefreshHeader(mContext);
        mInterpolator = new DecelerateInterpolator();
    }

    private void initFooterContainer() {
        if (mFooterContainer != null)
            return;
        mFooterContainer = new LinearLayout(mContext);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mFooterContainer.setLayoutParams(params);
        mFooterContainer.setOrientation(LinearLayout.VERTICAL);
        mFooterContainer.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
    }

    /**
     * 初始化头布局
     */
    private void initHeaderContainer() {
        if (mHeaderContainer != null)
            return;
        mHeaderContainer = new LinearLayout(mContext);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHeaderContainer.setLayoutParams(params);
        mHeaderContainer.setOrientation(LinearLayout.VERTICAL);
        mHeaderContainer.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
    }

    /**
     * 添加头部布局
     *
     * @param headerView
     */
    public void addHeaderView(@NonNull View headerView) {
        if (mHeaderContainer == null)
            initHeaderContainer();
        addView2Container(mHeaderContainer, headerView);
    }

    /**
     * 添加为布局
     *
     * @param footerView
     */
    public void addFooterView(View footerView) {
        if (mFooterContainer == null)
            initFooterContainer();
        addView2Container(mFooterContainer, footerView);
    }

    /**
     * 添加控件到指定容器
     *
     * @param parent 指定容器
     * @param child  子控件
     */
    private void addView2Container(@Nullable ViewGroup parent, @Nullable View child) {
        if (child.getParent() != null) {
            throw new NullPointerException("child has parent");
        }
        int childCount = parent.getChildCount();
        parent.addView(child, childCount);
    }

    /**
     * 是否开启进入动画
     *
     * @param canStartLoadAnimation
     */
    public void setCanStartLoadAnimation(boolean canStartLoadAnimation) {
        isCanStartLoadAnimation = canStartLoadAnimation;
    }


    /**
     * 设置空布局
     *
     * @param emptyView
     */
    public void setEmptyView(@NonNull View emptyView) {
        if (mEmptyContainer == null) {
            mEmptyContainer = new FrameLayout(mContext);
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            ViewGroup.LayoutParams lp = emptyView.getLayoutParams();
            if (lp != null) {
                layoutParams.width = lp.width;
                layoutParams.height = lp.height;
            }
            mEmptyContainer.setLayoutParams(layoutParams);
        }
        mEmptyContainer.removeAllViews();
        mEmptyContainer.addView(emptyView);
        int position = 0;
        if (mWrapAdapter != null) {
            if (mShowEmptyWithHeader & mWrapAdapter.getHeaderViewCount() != 0) {
                position += mWrapAdapter.getHeaderViewCount();
            }
            position += mWrapAdapter.getArrowViewCount();
            mWrapAdapter.notifyItemInserted(position);
        }

    }


    /**
     * 设置动画加载时长
     *
     * @param loadAnimationDuration
     */
    public void setLoadAnimationDuration(int loadAnimationDuration) {
        this.mLoadAnimationDuration = loadAnimationDuration;
    }

    /**
     * 设置加载动画
     *
     * @param animation
     */
    public void setLoadAnimation(BaseAnimation animation) {
        this.mAnimation = animation;
    }

    /**
     * 拿到headerView的数量
     *
     * @return
     */
    public int getHeaderViewSize() {
        return mHeaderContainer == null ? 0 : mHeaderContainer.getChildCount();
    }

    /**
     * footerview的数量
     *
     * @return
     */
    public int getFooterViewSize() {
        return mFooterContainer == null ? 0 : mFooterContainer.getChildCount();
    }

    /**
     * 设置底部布局
     *
     * @param loadMoreView
     */
    public void setLoadMoreView(@NonNull ILoadMoreState loadMoreView) {
        mLoadMore = loadMoreView;
        if (mLoadMore.getLoadMoreView() == null || mLoadMore.getLoadMoreView().getParent() != null) {
            throw new IllegalArgumentException("loadView is null or loadView had parent");
        }
    }

    /**
     * 进入的时候进行刷新
     */
    public void refresh() {
        if (isCanRefresh() && mLoadingListener != null) {
            mArrowView.setState(ArrowRefreshHeader.STATE_REFRESHING);
            mLoadingListener.onRefresh();
        }
    }

    /**
     * 设置下拉刷新布局
     *
     * @param view
     */
    public void setArrowView(BaseArrowView view) {
        mArrowView.setChildView(view);
    }

    /**
     * 对除加载数据外的内容进行重置
     */
    public void reset() {
        setNoMore(false);
        loadMoreComplete();
        refreshComplete();
    }

    /**
     * 加载更多完成之后要调用
     */
    public void loadMoreComplete() {
        isLoadingMore = false;
        if (mLoadMore == null)
            return;
        mLoadMore.onComplete();
    }

    /**
     * 刷新之后的要调用的
     */
    public void refreshComplete() {
        mArrowView.onRefreshComplete();
        setNoMore(false);
    }

    /**
     * 设置滑动模式
     *
     * @param mode
     */
    public void setRefreshMode(@FreshMode int mode) {
        mCurrentMode = mode;
    }

    /**
     * 设置是否可以加载更多
     *
     * @param noMore
     */
    public void setNoMore(boolean noMore) {
        isNoMore = noMore;
        isLoadingMore = false;
        if (mLoadMore == null)
            return;
        if (isNoMore)
            mLoadMore.onNoMore();
        else
            mLoadMore.onComplete();
    }


    /**
     * 将现有的Adapter进行包装
     *
     * @param adapter
     */
    @Override
    public void setAdapter(Adapter adapter) {
        mWrapAdapter = new WarpAdapter(adapter);
        super.setAdapter(mWrapAdapter);
        adapter.registerAdapterDataObserver(mDataObserver);
        mDataObserver.onChanged();
    }

    @Override
    public void addItemDecoration(ItemDecoration decor) {
        super.addItemDecoration(decor);
    }


    /**
     * 为了解决ClassCastException
     * 因为对adapter进行了包装
     *
     * @return
     */
    @Override
    public Adapter getAdapter() {
        if (mWrapAdapter != null) {
            return mWrapAdapter.getRealAdapter();
        }
        return null;
    }

    private class DataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            if (mWrapAdapter != null) {
                mWrapAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mWrapAdapter.notifyItemMoved(fromPosition, toPosition);
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE && isCanLoadMore() && mLoadingListener != null && !isLoadingMore && !isNoMore) {
            LayoutManager layoutManager = getLayoutManager();
            int lastVisibleItemPosition;
            if (layoutManager instanceof GridLayoutManager) {
                lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] into = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
                lastVisibleItemPosition = findMax(into);
            } else {
                lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            }
            if (layoutManager.getChildCount() > 0 && lastVisibleItemPosition >= layoutManager.getItemCount() - 1 && layoutManager.getItemCount() > layoutManager.getChildCount()) {
                isLoadingMore = true;
                if (mLoadMore != null) {
                    mLoadMore.onLoading();
                }
                if (mLoadingListener != null) {
                    mLoadingListener.onLoadMore();
                }


            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaY = ev.getRawY() - mLastY;
                mLastY = ev.getRawY();
                if (isOnTop() && isCanRefresh()) {
                    mArrowView.onMove((int) deltaY / 2);
                    if (mArrowView.getVisibleHeight() > 0 && mArrowView.getState() < ArrowRefreshHeader.STATE_REFRESHING) {
                        return false;
                    }
                }
                break;
            default:
                mLastY = -1; // reset
                if (isOnTop() && isCanRefresh()) {
                    if (mArrowView.releaseAction()) {
                        if (mLoadingListener != null) {
                            mLoadingListener.onRefresh();
                        }
                    }

                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private boolean isOnTop() {
        if (mArrowView != null && mArrowView.getParent() != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 设置监听
     *
     * @param listener
     */
    public void setLoadingListener(LoadingListener listener) {
        mLoadingListener = listener;
    }


    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }


    /**
     * 是否可以加载更多
     *
     * @return
     */
    public boolean isCanLoadMore() {
        return mCurrentMode == MODE_REFRESH_BOTTOM || mCurrentMode == MODE_REFRESH_ALL;
    }

    /**
     * 是否可以刷新
     *
     * @return
     */
    public boolean isCanRefresh() {
        return mCurrentMode == MODE_REFRESH_ALL || mCurrentMode == MODE_REFRESH_TOP;
    }

    /**
     * 内部包装的adapter,主要是处理多布局和矫正列表数量的问题
     */
    private class WarpAdapter extends RecyclerView.Adapter<ViewHolder> {

        private Adapter adapter;

        public WarpAdapter(Adapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ARROW) {
                return new SimpleViewHolder(mArrowView);
            } else if (viewType == VIEW_TYPE_HEADER) {
                return new SimpleViewHolder(mHeaderContainer);
            } else if (viewType == VIEW_TYPE_FOOTER) {
                return new SimpleViewHolder(mFooterContainer);
            } else if (viewType == VIEW_TYPE_LOADING) {
                return new SimpleViewHolder(mLoadMore.getLoadMoreView());
            } else if (viewType == VIEW_TYPE_EMPTY) {
                return new SimpleViewHolder(mEmptyContainer);
            } else {
                return adapter.onCreateViewHolder(parent, viewType);
            }
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position < getArrowViewCount() + getHeaderViewCount() + getEmptyViewCount()) {
                return;
            }
            int adjPosition = position - getHeaderViewCount() - getEmptyViewCount() - getArrowViewCount();
            int adapterCount;
            if (adapter != null) {
                adapterCount = adapter.getItemCount();
                if (adjPosition < adapterCount) {
                    adapter.onBindViewHolder(holder, adjPosition);
                }
            }
            if (isCanStartLoadAnimation) {
                if (!isOnlyFirstPlayAnima || adjPosition > mLastPosition) {
                    BaseAnimation animation;
                    if (mAnimation == null) {
                        animation = new TranslateAnimation();
                    } else {
                        animation = mAnimation;
                    }
                    Animator animators = animation.getAnimators(holder.itemView);
                    animators.setInterpolator(mInterpolator);
                    animators.setDuration(mLoadAnimationDuration).start();
                    mLastPosition = adjPosition;
                }
            }

        }

        @Override
        public int getItemCount() {
            int count = adapter.getItemCount();
            if (mArrowView != null && isCanRefresh()) {
                count++;
            }
            if (getEmptyViewCount() == 1) {
                count++;
                if (mShowEmptyWithHeader && getHeaderViewCount() != 0) {
                    count++;
                }
                if (mShowEmptyWithFooter && getFooterViewCount() != 0) {
                    count++;
                }
            } else {
                count += getHeaderViewCount() + getFooterViewCount() + getLoadingViewCount();
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            if (getEmptyViewCount() == 1) {
                boolean isHeader = mShowEmptyWithHeader && getHeaderViewCount() != 0;
                boolean isArrow = getArrowViewCount() != 0;
                switch (position) {
                    case 0:
                        if (isArrow) {
                            return VIEW_TYPE_ARROW;
                        } else if (isHeader) {
                            return VIEW_TYPE_HEADER;
                        } else {
                            return VIEW_TYPE_EMPTY;
                        }
                    case 1:
                        if (isHeader) {
                            return VIEW_TYPE_HEADER;
                        } else {
                            return VIEW_TYPE_EMPTY;
                        }
                    case 2:
                        if (isHeader) {
                            return VIEW_TYPE_EMPTY;
                        } else {
                            return VIEW_TYPE_FOOTER;
                        }
                    case 3:
                        return VIEW_TYPE_FOOTER;
                    default:
                        return VIEW_TYPE_EMPTY;
                }
            } else {
                int adjPosition = position - getArrowViewCount() - getHeaderViewCount();
                boolean isHeader = getHeaderViewCount() != 0;
                boolean isArrow = getArrowViewCount() != 0;
                if (position == 0) {
                    if (isArrow) {
                        return VIEW_TYPE_ARROW;
                    } else if (isHeader) {
                        return VIEW_TYPE_HEADER;
                    }
                } else if (position == 1) {
                    if (isHeader) {
                        return VIEW_TYPE_HEADER;
                    }
                } else if (isFooter(position)) {
                    return VIEW_TYPE_FOOTER;
                } else if (isLoadingMoreView(position)) {
                    return VIEW_TYPE_LOADING;
                } else {
                    if (adapter != null) {
                        int adapterCount = adapter.getItemCount();
                        if (adjPosition < adapterCount) {
                            return adapter.getItemViewType(adjPosition);
                        }
                    }
                }
            }
            return -999;
        }


        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            adapter.onDetachedFromRecyclerView(recyclerView);
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null
                    && lp instanceof StaggeredGridLayoutManager.LayoutParams
                    && (isLoadingMoreView(holder.getLayoutPosition()) || isEmpty(holder.getLayoutPosition()) || isHeader(holder.getLayoutPosition()) || isArrow(holder.getLayoutPosition()) || isFooter(holder.getLayoutPosition()))) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
            adapter.onViewAttachedToWindow(holder);
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            adapter.onViewDetachedFromWindow(holder);
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            adapter.onViewRecycled(holder);
        }

        @Override
        public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
            return adapter.onFailedToRecycleView(holder);
        }

        @Override
        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
            adapter.unregisterAdapterDataObserver(observer);
        }

        @Override
        public void registerAdapterDataObserver(AdapterDataObserver observer) {
            adapter.registerAdapterDataObserver(observer);
        }


        /**
         * 判断是否为下拉View
         *
         * @param position
         * @return
         */
        private boolean isArrow(int position) {
            return position == 0 && getArrowViewCount() == 1;
        }

        /**
         * 判断是否是LoadMoreView
         *
         * @param position
         * @return
         */
        private boolean isLoadingMoreView(int position) {
            return position == adapter.getItemCount() + getFooterViewCount() + getEmptyViewCount() + getHeaderViewCount() + getArrowViewCount();
        }

        /**
         * 判断是否为底部View
         *
         * @param position
         * @return
         */
        private boolean isFooter(int position) {
            return position >= getArrowViewCount() + getHeaderViewCount() + getEmptyViewCount() + adapter.getItemCount() && position < getItemCount() - 1;
        }

        private boolean isEmpty(int position) {
            return position == 2 && getEmptyViewCount() == 1;
        }

        /**
         * 判断是否为头部View
         *
         * @param position
         * @return
         */
        private boolean isHeader(int position) {
            return position == 1 && getHeaderViewCount() == 1;
        }

        private int getArrowViewCount() {
            if (mArrowView == null || !isCanRefresh()) {
                return 0;
            }
            return 1;
        }

        private int getHeaderViewCount() {
            if (mHeaderContainer == null || mHeaderContainer.getChildCount() == 0) {
                return 0;
            }
            return 1;
        }

        private int getFooterViewCount() {
            if (mFooterContainer == null || mFooterContainer.getChildCount() == 0) {
                return 0;
            }
            return 1;
        }

        private int getEmptyViewCount() {
            if (mEmptyContainer == null || mEmptyContainer.getChildCount() == 0) {
                return 0;
            }
            if (adapter.getItemCount() != 0) {
                return 0;
            }
            return 1;
        }

        private int getLoadingViewCount() {
            if (mLoadMore == null || !isCanLoadMore()) {
                return 0;
            }
            return 1;
        }

        /**
         * 拿到真实adapter
         *
         * @return
         */
        public Adapter getRealAdapter() {
            return adapter;
        }

        /**
         * 为了解决在使用GridLayoutManager出现位置不正确的BUG
         *
         * @param recyclerView
         */
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) manager);
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {


                        return (isLoadingMoreView(position - getHeaderViewCount() - getArrowViewCount()) || getHeaderViewCount() == 1 || isFooter(position - getHeaderViewCount() - getArrowViewCount() - adapter.getItemCount()) || getArrowViewCount() == 1)
                                ? gridManager.getSpanCount() : 1;
                    }
                });
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    /**
     * 使用简单的ViewHolder,来对View进行包装
     */
    private class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View itemView) {
            super(itemView);
        }
    }


}
