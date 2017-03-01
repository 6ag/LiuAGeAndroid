package tv.baokan.liuageandroid.ui.activity;

import android.app.Activity;
import android.content.Intent;
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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.adapter.NewsListRecyclerViewAdapter;
import tv.baokan.liuageandroid.model.ArticleListBean;
import tv.baokan.liuageandroid.model.UserBean;
import tv.baokan.liuageandroid.utils.APIs;
import tv.baokan.liuageandroid.utils.AdManager;
import tv.baokan.liuageandroid.utils.LogUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.ProgressHUD;
import tv.baokan.liuageandroid.widget.NavigationViewPush;

public class CommentRecordActivity extends BaseActivity {

    private static final String TAG = "CommentRecordActivity";
    private int pageIndex = 1;
    private NavigationViewPush mNavigationViewRed;
    private TwinklingRefreshLayout mRefreshLayout;         // 上下拉刷新
    private RecyclerView mNewsListRecyclerView;    // 列表视图
    private NewsListRecyclerViewAdapter newsListAdapter;  // 适配器

    /**
     * 便捷启动当前activity
     *
     * @param activity 启动当前activity的activity
     */
    public static void start(Activity activity) {
        Intent intent = new Intent(activity, CommentRecordActivity.class);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_enter, R.anim.push_exit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_record);

        prepareUI();
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
     * 准备UI
     */
    private void prepareUI() {
        mNavigationViewRed = (NavigationViewPush) findViewById(R.id.nav_comment);
        mRefreshLayout = (TwinklingRefreshLayout) findViewById(R.id.srl_comment_record_list_refresh);
        mNewsListRecyclerView = (RecyclerView) findViewById(R.id.rv_comment_record_list);
        mNavigationViewRed.setupNavigationView(true, false, "我的评论", new NavigationViewPush.OnClickListener() {
            @Override
            public void onBackClick(View v) {
                finish();
            }
        });
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
        mRefreshLayout.setHeaderView(sinaRefreshView);

        // 到达底部自动加载更多
        mRefreshLayout.setAutoLoadMore(true);

        // 监听刷新
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onRefresh(final TwinklingRefreshLayout refreshLayout) {

                // 重新加载并缓存数据
                pageIndex = 1;
                loadCollectionFromNetwork(1, 0);
            }

            @Override
            public void onLoadMore(final TwinklingRefreshLayout refreshLayout) {
                pageIndex += 1;
                loadCollectionFromNetwork(pageIndex, 1);
            }
        });

        // 默认加载一次数据 不使用下拉刷新
        mRefreshLayout.startRefresh();

    }

    /**
     * 加载收藏数据从网络
     *
     * @param pageIndex 页码
     * @param method    0下拉 1上拉
     */
    private void loadCollectionFromNetwork(int pageIndex, final int method) {

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("username", UserBean.shared().getUsername());
        parameters.put("userid", UserBean.shared().getUserid());
        parameters.put("token", UserBean.shared().getToken());
        parameters.put("pageIndex", String.valueOf(pageIndex));

        NetworkUtils.shared.get(APIs.GET_USER_COMMENT, parameters, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                ProgressHUD.showInfo(mContext, "您的网络不给力");
                if (method == 0) {
                    mRefreshLayout.finishRefreshing();
                } else {
                    mRefreshLayout.finishLoadmore();
                }
            }

            @Override
            public void onResponse(String response, int id) {
                LogUtils.d(TAG, "收藏 = " + response);
                try {
                    JSONArray jsonArray = new JSONObject(response).getJSONArray("data");
                    List<ArticleListBean> tempListBeans = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ArticleListBean bean = new ArticleListBean(jsonArray.getJSONObject(i));
                        tempListBeans.add(bean);
                        LogUtils.d(TAG, bean.toString());
                    }
                    if (tempListBeans.size() == 0) {
                        ProgressHUD.showInfo(mContext, "没有更多数据了");
                    } else {
                        // 刷新数据
                        newsListAdapter.updateData(new ArrayList<ArticleListBean>(), tempListBeans, method);
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


}

