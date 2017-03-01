package tv.baokan.liuageandroid.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.lcodecore.tkrefreshlayout.header.SinaRefreshView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.adapter.NewsListRecyclerViewAdapter;
import tv.baokan.liuageandroid.model.ArticleListBean;
import tv.baokan.liuageandroid.utils.APIs;
import tv.baokan.liuageandroid.utils.AdManager;
import tv.baokan.liuageandroid.utils.LogUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.ProgressHUD;
import tv.baokan.liuageandroid.widget.NavigationViewPush;

public class SearchResultActivity extends BaseActivity {

    private static final String TAG = "SearchActivity";

    private List<ArticleListBean> mResultList = new ArrayList<>();
    private String mCurrentKeyboard; // 当前关键词
    private int pageIndex = 1;   // 当前页码
    private TwinklingRefreshLayout mRefreshLayout;  // 上下拉刷新
    private RecyclerView mNewsListRecyclerView;    // 列表视图
    private NewsListRecyclerViewAdapter mNewsListAdapter;       // 列表视图的适配器
    private NavigationViewPush mNavigationView;

    /**
     * 便捷启动当前activity
     *
     * @param activity 启动当前activity的activity
     */
    public static void start(Activity activity, String keyboard, List<ArticleListBean> resultList) {
        Intent intent = new Intent(activity, SearchResultActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("resultList_key", (Serializable) resultList);
        intent.putExtras(bundle);
        intent.putExtra("keyboard_key", keyboard);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_enter, R.anim.push_exit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        // 获取传递过来的数据
        Intent intent = getIntent();
        mCurrentKeyboard = intent.getStringExtra("keyboard_key");
        mResultList = (List<ArticleListBean>) intent.getSerializableExtra("resultList_key");

        prepareUI();
    }

    /**
     * 准备UI
     */
    private void prepareUI() {
        mNavigationView = (NavigationViewPush) findViewById(R.id.nav_search_result);
        mNewsListRecyclerView = (RecyclerView) findViewById(R.id.rv_search_result_list_recyclerview);
        mRefreshLayout = (TwinklingRefreshLayout) findViewById(R.id.srl_search_result_list_refresh);

        mNavigationView.setupNavigationView(true, false, "搜索结果", new NavigationViewPush.OnClickListener() {
            @Override
            public void onBackClick(View v) {
                finish();
            }
        });

        setupRecyclerView();
        setupRefresh();
    }

    /**
     * 配置刷新控件
     */
    private void setupRefresh() {

        // 顶部刷新视图
        SinaRefreshView sinaRefreshView = new SinaRefreshView(mContext);
        sinaRefreshView.setArrowResource(R.drawable.pull_refresh_arrow);
        mRefreshLayout.setHeaderView(sinaRefreshView);

        // 到达底部自动加载更多
        mRefreshLayout.setAutoLoadMore(true);

        // 监听刷新
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(final TwinklingRefreshLayout refreshLayout) {
                // 重新加载并缓存数据
                pageIndex = 1;
                loadSearchListFromNetwork(mCurrentKeyboard, pageIndex, 0);
            }

            @Override
            public void onLoadMore(final TwinklingRefreshLayout refreshLayout) {
                pageIndex += 1;
                loadSearchListFromNetwork(mCurrentKeyboard, pageIndex, 1);
            }
        });

        // 默认加载一次列表数据
        mNewsListAdapter.updateData(new ArrayList<ArticleListBean>(), mResultList, 0);
    }

    /**
     * 配置recyclerView资讯列表
     */
    private void setupRecyclerView() {
        mNewsListRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mNewsListAdapter = new NewsListRecyclerViewAdapter(mContext);
        mNewsListRecyclerView.setAdapter(mNewsListAdapter);
        mNewsListAdapter.setOnItemTapListener(new NewsListRecyclerViewAdapter.OnItemTapListener() {
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
     * 打开文章详情页面
     *
     * @param articleBean 文章模型
     */
    private void openArticleDetail(ArticleListBean articleBean) {
        // 是广告分类，并且是外链。则打开网页activity
        if (articleBean.getClassid().equals(AdManager.shared().classid) && articleBean.getIsurl().equals("1")) {
            AdWebViewActivity.start(mContext, articleBean.getTitleurl(), articleBean.getTitle(), articleBean.getTitlepic());
            return;
        }

        // 超过3张图才打开图集activity
        if (articleBean.getMorepic().length > 3) {
            PhotoDetailActivity.start(mContext, articleBean.getClassid(), articleBean.getId());
        } else {
            NewsDetailActivity.start(mContext, articleBean.getClassid(), articleBean.getId());
        }
    }

    /**
     * 上拉只加载列表数据
     *
     * @param keyboard  关键词
     * @param pageIndex 页码
     * @param method    加载方式 0下拉 1上拉
     */
    private void loadSearchListFromNetwork(final String keyboard, int pageIndex, final int method) {

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("keyboard", keyboard);
        parameters.put("pageIndex", String.valueOf(pageIndex));
        parameters.put("pageSize", String.valueOf(20));

        NetworkUtils.shared.get(APIs.SEARCH, parameters, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                ProgressHUD.showInfo(mContext, "您的网络不给力哦");
                if (method == 0) {
                    mRefreshLayout.finishRefreshing();
                } else {
                    mRefreshLayout.finishLoadmore();
                }
            }

            @Override
            public void onResponse(String response, int id) {
                try {
                    LogUtils.d(TAG, "搜索结果 " + response);
                    JSONArray jsonArray = new JSONObject(response).getJSONArray("data");
                    List<ArticleListBean> tempListBeans = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ArticleListBean bean = new ArticleListBean(jsonArray.getJSONObject(i));
                        tempListBeans.add(bean);
                    }
                    if (tempListBeans.size() == 0) {
                        ProgressHUD.showInfo(mContext, "没有数据了~");
                    } else {
                        // 刷新数据
                        mNewsListAdapter.updateData(new ArrayList<ArticleListBean>(), tempListBeans, method);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (method == 0) {
                        mRefreshLayout.finishRefreshing();
                    } else {
                        mRefreshLayout.finishLoadmore();
                    }
                }
            }
        });

    }

}
