package tv.baokan.liuageandroid.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.lcodecore.tkrefreshlayout.header.SinaRefreshView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.adapter.NewsListRecyclerViewAdapter;
import tv.baokan.liuageandroid.cache.NewsDALManager;
import tv.baokan.liuageandroid.model.ArticleListBean;
import tv.baokan.liuageandroid.ui.activity.AdWebViewActivity;
import tv.baokan.liuageandroid.ui.activity.NewsDetailActivity;
import tv.baokan.liuageandroid.ui.activity.PhotoDetailActivity;
import tv.baokan.liuageandroid.utils.AdManager;
import tv.baokan.liuageandroid.utils.LogUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.ProgressHUD;

public class NewsListFragment extends BaseFragment {

    private static final String TAG = "NewsListFragment";
    private String classid;      // 栏目id
    private int pageIndex = 1;   // 当前页码

    private TwinklingRefreshLayout refreshLayout;  // 上下拉刷新
    private RecyclerView mNewsListRecyclerView;    // 列表视图
    private NewsListRecyclerViewAdapter newsListAdapter;       // 列表视图的适配器
    private List<ArticleListBean> mIsGoodArticleBeans = new ArrayList<>(); // 幻灯片数据

    public static NewsListFragment newInstance(String classid) {
        NewsListFragment newFragment = new NewsListFragment();
        Bundle bundle = new Bundle();
        bundle.putString("classid", classid);
        newFragment.setArguments(bundle);
        return newFragment;
    }

    @Override
    protected View prepareUI() {
        View view = View.inflate(mContext, R.layout.fragment_news_list, null);
        mNewsListRecyclerView = (RecyclerView) view.findViewById(R.id.rv_news_list_recyclerview);
        refreshLayout = (TwinklingRefreshLayout) view.findViewById(R.id.srl_news_list_refresh);
        return view;
    }

    @Override
    protected void loadData() {

        // 取出构造里的分类id和isShowBanner
        Bundle args = getArguments();
        if (args != null) {
            classid = args.getString("classid");
        }

        // 配置recyclerView资讯列表
        setupRecyclerView();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 配置刷新
                setupRefresh();
            }
        }, 100);

    }

    /**
     * 配置recyclerView资讯列表
     */
    private void setupRecyclerView() {
        mNewsListRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        newsListAdapter = new NewsListRecyclerViewAdapter(mContext);
        mNewsListRecyclerView.setAdapter(newsListAdapter);
        newsListAdapter.setOnItemTapListener(new NewsListRecyclerViewAdapter.OnItemTapListener() {
            @Override
            public void onItemTapListener(ArticleListBean articleListBean) {
                // 打开文章详情
                openArticleDetail(articleListBean);
            }
        });

        // 监听滚动
        mNewsListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        if (!Fresco.getImagePipeline().isPaused()) {
                            Fresco.getImagePipeline().pause();
                        }
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                    case RecyclerView.SCROLL_STATE_IDLE:
                        if (Fresco.getImagePipeline().isPaused()) {
                            Fresco.getImagePipeline().resume();
                        }
                        break;
                }
            }
        });
    }

    /**
     * 配置刷新控件
     */
    private void setupRefresh() {

        // 顶部刷新视图
        SinaRefreshView sinaRefreshView = new SinaRefreshView(mContext);
        sinaRefreshView.setArrowResource(R.drawable.pull_refresh_arrow);
        refreshLayout.setHeaderView(sinaRefreshView);

        // 到达底部自动加载更多
        refreshLayout.setAutoLoadMore(true);

        // 监听刷新
        refreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(final TwinklingRefreshLayout refreshLayout) {
                // 下拉刷新数据时，在有网络情况下清除本地缓存的数据
                if (NetworkUtils.shared.isNetworkConnected(mContext)) {
                    if (classid.equals("0")) {
                        // 刷新推荐数据 - 重新加载广告数据
                        AdManager.shared().loadAdList(getActivity());
                    }
                    NewsDALManager.shared.removeNewsList(classid);
                }

                // 重新加载并缓存数据
                pageIndex = 1;
                loadIsGoodList(classid);
            }

            @Override
            public void onLoadMore(final TwinklingRefreshLayout refreshLayout) {
                pageIndex += 1;
                loadNewsFromNetwork(classid, pageIndex, 1);
            }
        });

        // 默认加载一次数据
        loadIsGoodList(classid);

    }

    /**
     * 下拉加载幻灯片数据 - 也会加载列表数据
     *
     * @param classid 分类id
     */
    private void loadIsGoodList(final String classid) {

        // 从数据访问层加载数据
        NewsDALManager.shared.loadNewsList("isgood", classid, pageIndex, new NewsDALManager.NewsListCallback() {
            @Override
            public void onSuccess(JSONArray jsonArray) {
                try {
                    List<ArticleListBean> isGoodArticleBeans = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ArticleListBean bean = new ArticleListBean(jsonArray.getJSONObject(i));
                        isGoodArticleBeans.add(bean);
                    }
                    mIsGoodArticleBeans = isGoodArticleBeans;
                    loadNewsFromNetwork(classid, pageIndex, 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String tipString) {
                ProgressHUD.showInfo(mContext, tipString);
            }
        });
    }

    /**
     * 上拉只加载列表数据
     *
     * @param classid   分类id
     * @param pageIndex 页码
     * @param method    加载方式 0下拉 1上拉
     */
    private void loadNewsFromNetwork(final String classid, int pageIndex, final int method) {

        // 从数据访问层加载数据
        NewsDALManager.shared.loadNewsList("", classid, pageIndex, new NewsDALManager.NewsListCallback() {
            @Override
            public void onSuccess(JSONArray jsonArray) {
                try {
                    List<ArticleListBean> tempListBeans = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ArticleListBean bean = new ArticleListBean(jsonArray.getJSONObject(i));
                        tempListBeans.add(bean);
                        LogUtils.d(TAG, bean.toString());
                    }
                    if (tempListBeans.size() == 0) {
                        ProgressHUD.showInfo(mContext, "没有更多数据了");
                    } else {

                        // 每隔10条数据插入一条广告数据
                        if (AdManager.shared().articleListBeanList.size() > 0 && tempListBeans.size() > 10) {
                            int max = AdManager.shared().articleListBeanList.size();
                            int randomNum = new Random().nextInt(max);
                            LogUtils.d(TAG, "随机数1 = " + randomNum);
                            tempListBeans.add(tempListBeans.size() - 10, AdManager.shared().articleListBeanList.get(randomNum));
                        }
                        if (AdManager.shared().articleListBeanList.size() > 0 && tempListBeans.size() >= 20) {
                            int max = AdManager.shared().articleListBeanList.size();
                            int randomNum = new Random().nextInt(max);
                            LogUtils.d(TAG, "随机数2 = " + randomNum);
                            tempListBeans.add(tempListBeans.size() - 1, AdManager.shared().articleListBeanList.get(randomNum));
                        }

                        // 刷新数据
                        newsListAdapter.updateData(mIsGoodArticleBeans, tempListBeans, method);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (method == 0) {
                        refreshLayout.finishRefreshing();
                    } else {
                        refreshLayout.finishLoadmore();
                    }
                }
            }

            @Override
            public void onError(String tipString) {
                ProgressHUD.showInfo(mContext, tipString);
                if (method == 0) {
                    refreshLayout.finishRefreshing();
                } else {
                    refreshLayout.finishLoadmore();
                }
            }
        });

    }

    /**
     * 打开文章详情页面
     *
     * @param articleBean 文章模型
     */
    private void openArticleDetail(ArticleListBean articleBean) {

        // 是广告分类，并且是外链。则打开网页activity
        if (articleBean.getClassid().equals(AdManager.shared().classid) && articleBean.getIsurl().equals("1")) {
            AdWebViewActivity.start(getActivity(), articleBean.getTitleurl(), articleBean.getTitle(), articleBean.getTitlepic());
            return;
        }

        // 超过3张图才打开图集activity
        if (articleBean.getMorepic().length > 3) {
            PhotoDetailActivity.start(getActivity(), articleBean.getClassid(), articleBean.getId());
        } else {
            NewsDetailActivity.start(getActivity(), articleBean.getClassid(), articleBean.getId());
        }
    }

}
