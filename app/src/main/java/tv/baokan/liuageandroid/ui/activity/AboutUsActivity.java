package tv.baokan.liuageandroid.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

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
import tv.baokan.liuageandroid.utils.AdManager;
import tv.baokan.liuageandroid.utils.LogUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.ProgressHUD;
import tv.baokan.liuageandroid.widget.NavigationViewPush;

public class AboutUsActivity extends BaseActivity {

    private WebView mWebView;
    private NavigationViewPush mNavigationViewRed;     // 导航栏
    private ProgressBar mProgressBar;

    /**
     * 便捷启动当前activity
     *
     * @param activity 启动当前activity的activity
     */
    public static void start(Activity activity) {
        Intent intent = new Intent(activity, AboutUsActivity.class);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_enter, R.anim.push_exit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);

        mNavigationViewRed = (NavigationViewPush) findViewById(R.id.nav_agreement);
        mWebView = (WebView) findViewById(R.id.wv_agreement_webview);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_agreement_progressbar);

        mNavigationViewRed.setupNavigationView(true, false, "关于我们", new NavigationViewPush.OnClickListener() {
            @Override
            public void onBackClick(View v) {
                finish();
            }
        });

        mWebView.loadUrl("file:///android_asset/www/html/aboutus.html");
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 隐藏加载进度条
                mProgressBar.setVisibility(View.INVISIBLE);
            }

        });
    }

}
