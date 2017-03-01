package tv.baokan.liuageandroid.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.utils.ProgressHUD;
import tv.baokan.liuageandroid.utils.StatusUtils;

public class AdWebViewActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "AdWebViewActivity";

    private String titleurl; // 网页url
    private String title;    // 标题
    private String titlepic; // 标题图片

    private View mTopBar;                   // 顶部状态栏透明条
    private ProgressBar mProgressBar;       // 进度圈
    private ScrollView mScrollView;         // 内容载体 scrollView
    private WebView mContentWebView;        // 正文载体 webView
    private ImageButton mBackButton;        // 底部条 返回
    private ImageButton mEditButton;        // 底部条 编辑发布评论信息
    private ImageButton mFontButton;        // 底部条 设置字体
    private ImageButton mCollectionButton;  // 收藏
    private ImageButton mShareButton;       // 分享

    private boolean isStatusChanged = false; // 是否已经成功修改了状态栏颜色

    /**
     * 便捷启动当前activity
     *
     * @param activity 来源activity
     * @param titleurl 网页地址
     */
    public static void start(Activity activity, String titleurl, String title, String titlepic) {
        Intent intent = new Intent(activity, AdWebViewActivity.class);
        intent.putExtra("titleurl_key", titleurl);
        intent.putExtra("title_key", title);
        intent.putExtra("titlepic_key", titlepic);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_enter, R.anim.push_exit);
    }

    public static void start(Context activity, String titleurl, String title, String titlepic) {
        Intent intent = new Intent(activity, AdWebViewActivity.class);
        intent.putExtra("titleurl_key", titleurl);
        intent.putExtra("title_key", title);
        intent.putExtra("titlepic_key", titlepic);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 将MIUI/魅族的状态栏文字图标改成暗色
        if (StatusUtils.setMiuiStatusBarDarkMode(this, true) || StatusUtils.setMeizuStatusBarDarkMode(this, true)) {
            // 已经修改状态栏文字为深灰色
            isStatusChanged = true;
        }
        setContentView(R.layout.activity_adwebview);

        prepareUI();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                prepareData();
            }
        }, 50);

    }

    /**
     * 准备UI
     */
    private void prepareUI() {
        mTopBar = findViewById(R.id.v_ad_webivew_top_bar);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_ad_webivew_progressbar);
        mScrollView = (ScrollView) findViewById(R.id.bsv_ad_webivew_scrollview);
        mContentWebView = (WebView) findViewById(R.id.wv_ad_webivew_webview);
        mBackButton = (ImageButton) findViewById(R.id.ib_ad_webivew_bottom_bar_back);
        mEditButton = (ImageButton) findViewById(R.id.ib_ad_webivew_bottom_bar_edit);
        mFontButton = (ImageButton) findViewById(R.id.ib_ad_webivew_bottom_bar_font);
        mCollectionButton = (ImageButton) findViewById(R.id.ib_ad_webivew_bottom_bar_collection);
        mShareButton = (ImageButton) findViewById(R.id.ib_ad_webivew_bottom_bar_share);

        // 如果未能修改掉状态栏的颜色，就修改状态栏的背景颜色
        if (!isStatusChanged) {
            mTopBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            mTopBar.setAlpha(1);
        }

        WebSettings webSettings = mContentWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mContentWebView.setWebChromeClient(new WebChromeClient() {
        });
        mContentWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 隐藏加载进度条
                mProgressBar.setVisibility(View.INVISIBLE);
                // 网页加载完成才去加载其他UI
                setupDetailData();
                // 页面滑动到顶部
                mScrollView.fullScroll(ScrollView.FOCUS_UP);
            }

        });

        // 底部工具条按钮点击事件
        mBackButton.setOnClickListener(this);
        mEditButton.setOnClickListener(this);
        mFontButton.setOnClickListener(this);
        mCollectionButton.setOnClickListener(this);
        mShareButton.setOnClickListener(this);

    }

    /**
     * 准备数据
     */
    private void prepareData() {

        // 取出启动activity时传递的数据
        Intent intent = getIntent();
        if (intent != null) {
            titleurl = intent.getStringExtra("titleurl_key");
            titlepic = intent.getStringExtra("titlepic_key");
            title = intent.getStringExtra("title_key");
        }

        // 加载网页
        mContentWebView.loadUrl(titleurl);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_ad_webivew_bottom_bar_back:
                finish();
                break;
            case R.id.ib_ad_webivew_bottom_bar_edit:
                ProgressHUD.showInfo(mContext, "当前不支持评论");
                break;
            case R.id.ib_ad_webivew_bottom_bar_font:
                ProgressHUD.showInfo(mContext, "当前不支持修改字体");
                break;
            case R.id.ib_ad_webivew_bottom_bar_collection:
                ProgressHUD.showInfo(mContext, "当前不支持收藏");
                break;
            case R.id.ib_ad_webivew_bottom_bar_share:
                ProgressHUD.showInfo(mContext, "当前不支持分享");
                break;
        }
    }

    /**
     * 配置页面数据
     */
    private void setupDetailData() {

        // webView渲染有点慢，延迟100毫秒显示页面展示数据的UI
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.setVisibility(View.VISIBLE);
            }
        }, 100);
    }

}
