package tv.baokan.liuageandroid.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.model.ArticleListBean;
import tv.baokan.liuageandroid.utils.AdManager;

public class SplashActivity extends Activity {

    private View mAdView;
    private SimpleDraweeView mSimpleImageView;
    private TextView mTimeView;
    private Button mSkipButton;
    private int mTime = 5; // 广告时间5秒
    private ArticleListBean mArticleListBean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        prepareUI();
        loadLaunchAd();
    }

    /**
     * 加载启动广告
     */
    private void loadLaunchAd() {
        AdManager.shared().loadLaunchAd(new AdManager.LaunchAdCallback() {
            @Override
            public void onSuccess(boolean isSuccess, ArticleListBean articleListBean) {
                mAdView.setVisibility(View.VISIBLE);
                mSimpleImageView.setImageURI(articleListBean.getMorepic()[0]);
                mArticleListBean = articleListBean;
                handler.postDelayed(runnable, 1000); //每隔1s执行
            }

            @Override
            public void onError(String tipString) {
                mAdView.setVisibility(View.GONE);
                // 延迟2秒进入主界面
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                }, 1000);
            }
        });
    }

    /*
    准备UI
     */
    private void prepareUI() {
        mAdView = findViewById(R.id.rl_splash_show_ad);
        mSimpleImageView = (SimpleDraweeView) findViewById(R.id.sdv_splash_ad_img);
        mTimeView = (TextView) findViewById(R.id.tv_splash_time);
        mSkipButton = (Button) findViewById(R.id.ll_splash_ad_skip_btn);

        // 跳过
        mSkipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable);
                // 进入主页
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });

        // 广告
        mAdView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable);
                // 无动画进入主页 - 再进广告页
                MainActivity.start(SplashActivity.this, mArticleListBean);
                finish();
            }
        });

    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {

        @Override
        public void run() {
            try {
                if (mTime > 0) {
                    mTimeView.setText(Integer.toString(--mTime));
                    handler.postDelayed(this, 1000);
                } else {
                    // 进入主页
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("exception...");
            }
        }
    };
}
