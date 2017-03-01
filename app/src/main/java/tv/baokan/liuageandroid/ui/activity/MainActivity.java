package tv.baokan.liuageandroid.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.sharesdk.onekeyshare.OnekeyShare;
import okhttp3.Call;
import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.adapter.TabFragmentPagerAdapter;
import tv.baokan.liuageandroid.app.App;
import tv.baokan.liuageandroid.cache.NewsDALManager;
import tv.baokan.liuageandroid.model.ArticleListBean;
import tv.baokan.liuageandroid.model.ColumnBean;
import tv.baokan.liuageandroid.model.UserBean;
import tv.baokan.liuageandroid.ui.fragment.NewsListFragment;
import tv.baokan.liuageandroid.utils.APIs;
import tv.baokan.liuageandroid.utils.AdManager;
import tv.baokan.liuageandroid.utils.FileCacheUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.ProgressHUD;
import tv.baokan.liuageandroid.utils.SizeUtils;
import tv.baokan.liuageandroid.utils.StatusBarUtils;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 10000; // 写入SD卡权限
    public static final int REQUEST_CODE_COLUMN = 10001;

    private ProgressDialog mDownloadDialog;    // 加载进度

    private String serverVersion; // 服务器版本号
    private String description;   // 新版本更新描述
    private String apkUrl;        // 新版本apk下载地址

    private ImageButton mMenuButton;    // 左上角菜单
    private ImageButton mSearchButton;  // 右上角搜索
    private ImageButton mNewsClassAdd;  // 栏目管理
    private TabLayout mNewsTabLayout;   // 标签
    private ViewPager mNewsViewPager;   // 列表主体

    // 侧栏菜单
    private SlidingMenu mSlidingMenu;            // 侧栏
    private View mPortraitView;                  // 头像（包括头像和昵称）
    private SimpleDraweeView mPortraitImageView; // 头像
    private TextView mNicknameTextView;          // 昵称
    private View mCollectionView;                // 我的收藏
    private View mCommentView;                   // 我的足迹
    private View mClearCacheView;                // 清除缓存
    private View mChangModeView;                 // 夜间模式
    private View mFeedbackView;                  // 意见反馈
    private View mCommendView;                   // 推荐给好友
    private View mAboutView;                     // 关于我们

    private List<ColumnBean> mSelectedList = new ArrayList<>(); // 已经选择
    private List<ColumnBean> mOptionalList = new ArrayList<>(); // 可选项
    private List<NewsListFragment> mNewsListFragmentList = new ArrayList<>();
    private TabFragmentPagerAdapter mFragmentPageAdapter;

    /**
     * 快捷启动方法
     *
     * @param activity
     * @param articleListBean
     */
    public static void start(Activity activity, ArticleListBean articleListBean) {
        Intent intent = new Intent(activity, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("articleListBean_key", articleListBean);
        intent.putExtras(bundle);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.column_bottom, R.anim.column_bottom);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 修改状态栏颜色
        StatusBarUtils.initStatusBar(this, R.color.colorPrimary);
        setContentView(R.layout.activity_main);

        prepareUI();
        prepareMenu();

        // 加载广告数据
        AdManager.shared().loadAdList(mContext);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadData();

                // 检查版本更新
                checkVersion();

                // 检查是否需要更新本地关键词库
                NewsDALManager.shared.shouldUpdateKeyboardList();
            }
        }, 100);

        // 判断是否是点击广告进入
        isAdClick();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 更新头像
        if (UserBean.isLogin()) {
            mPortraitImageView.setImageURI(UserBean.shared().getAvatarUrl());
            mNicknameTextView.setText(UserBean.shared().getNickname());
        } else {
            mPortraitImageView.setImageURI("");
            mNicknameTextView.setText("登录账号");
        }
    }

    /**
     * 是否是点击广告
     */
    private void isAdClick() {
        // 如果有数据说明是点击广告过来的
        Intent intent = getIntent();
        if (intent != null && intent.getSerializableExtra("articleListBean_key") != null) {
            ArticleListBean articleListBean = (ArticleListBean) intent.getSerializableExtra("articleListBean_key");
            // 是广告分类，并且是外链。则打开网页activity
            if (articleListBean.getClassid().equals(AdManager.shared().classid) && articleListBean.getIsurl().equals("1")) {
                AdWebViewActivity.start(mContext, articleListBean.getTitleurl(), articleListBean.getTitle(), articleListBean.getTitlepic());
                return;
            }
            // 超过3张图才打开图集activity
            if (articleListBean.getMorepic().length > 3) {
                PhotoDetailActivity.start(mContext, articleListBean.getClassid(), articleListBean.getId());
            } else {
                NewsDetailActivity.start(mContext, articleListBean.getClassid(), articleListBean.getId());
            }
        }
    }

    /**
     * 准备侧滑菜单
     */
    private void prepareMenu() {
        mSlidingMenu = new SlidingMenu(mContext);
        mSlidingMenu.setMode(SlidingMenu.LEFT);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        mSlidingMenu.setShadowWidth(0);
        mSlidingMenu.setBehindOffset((int) (SizeUtils.getScreenWidthPx(mContext) * 0.5));
        mSlidingMenu.setFadeDegree(0.0f);
        mSlidingMenu.setBehindScrollScale(0.0f);
        mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);

        // 为侧滑菜单设置布局
        View view = View.inflate(mContext, R.layout.slidingmenu_profile, null);
        mSlidingMenu.setMenu(view);

        mPortraitView = view.findViewById(R.id.ll_slidingmenu_profile_portrait_layout);
        mPortraitImageView = (SimpleDraweeView) view.findViewById(R.id.sdv_slidingmenu_profile_portrait_image);
        mNicknameTextView = (TextView) view.findViewById(R.id.tv_slidingmenu_profile_nickname);
        mCollectionView = view.findViewById(R.id.rl_slidingmenu_profile_collection_layout);
        mCommentView = view.findViewById(R.id.rl_slidingmenu_profile_comment_layout);
        mClearCacheView = view.findViewById(R.id.rl_slidingmenu_profile_clear_cache_layout);
        mChangModeView = view.findViewById(R.id.rl_slidingmenu_profile_change_mode_layout);
        mFeedbackView = view.findViewById(R.id.rl_slidingmenu_profile_feekback_layout);
        mCommendView = view.findViewById(R.id.rl_slidingmenu_profile_commend_layout);
        mAboutView = view.findViewById(R.id.rl_slidingmenu_profile_aboutme_layout);

        mPortraitView.setOnClickListener(this);
        mCollectionView.setOnClickListener(this);
        mCommentView.setOnClickListener(this);
        mClearCacheView.setOnClickListener(this);
        mChangModeView.setOnClickListener(this);
        mFeedbackView.setOnClickListener(this);
        mCommendView.setOnClickListener(this);
        mAboutView.setOnClickListener(this);

    }

    /**
     * 准备UI
     */
    private void prepareUI() {

        mMenuButton = (ImageButton) findViewById(R.id.nav_main_left_menu);
        mSearchButton = (ImageButton) findViewById(R.id.nav_main_right_search);
        mNewsTabLayout = (TabLayout) findViewById(R.id.tl_news_tabLayout);
        mNewsViewPager = (ViewPager) findViewById(R.id.vp_news_viewPager);
        mNewsClassAdd = (ImageButton) findViewById(R.id.ib_news_class_add);

        mMenuButton.setOnClickListener(this);
        mSearchButton.setOnClickListener(this);
        mNewsClassAdd.setOnClickListener(this);

        // 配置viewPager
        setupViewPager();
    }

    /**
     * 配置ViewPager
     */
    private void setupViewPager() {
        mFragmentPageAdapter = new TabFragmentPagerAdapter(getSupportFragmentManager(), mNewsListFragmentList, mSelectedList);
        mNewsViewPager.setAdapter(mFragmentPageAdapter);
        mNewsTabLayout.setupWithViewPager(mNewsViewPager);
        mNewsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mNewsViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                mNewsViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                mNewsViewPager.setCurrentItem(tab.getPosition());
            }
        });
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.nav_main_left_menu: // 左上角侧边栏
                if (!mSlidingMenu.isMenuShowing()) {
                    mSlidingMenu.toggle();
                }
                break;
            case R.id.nav_main_right_search: // 右上角搜索
                SearchActivity.start(mContext);
                break;
            case R.id.ib_news_class_add: // 栏目管理
                ColumnActivity.start(mContext, mSelectedList, mOptionalList);
                break;
            default: // 点击了侧滑视图里的控件
                if (mSlidingMenu.isMenuShowing()) {
                    mSlidingMenu.toggle();
                }
                // 延迟调用事件，让关闭侧滑动画结束
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onClickMenu(v);
                    }
                }, 500);
                break;
        }

    }

    /**
     * 点击了侧滑menu里的控件
     *
     * @param v
     */
    private void onClickMenu(View v) {
        switch (v.getId()) {
            case R.id.ll_slidingmenu_profile_portrait_layout: // 头像
                if (UserBean.isLogin()) {
                    UserInfoActivity.start(mContext);
                } else {
                    LoginActivity.start(mContext);
                }
                break;
            case R.id.rl_slidingmenu_profile_collection_layout: // 收藏
                if (UserBean.isLogin()) {
                    CollectionRecordActivity.start(mContext);
                } else {
                    LoginActivity.start(mContext);
                }
                break;
            case R.id.rl_slidingmenu_profile_comment_layout: // 评论
                if (UserBean.isLogin()) {
                    CommentRecordActivity.start(mContext);
                } else {
                    LoginActivity.start(mContext);
                }
                break;
            case R.id.rl_slidingmenu_profile_clear_cache_layout: // 清除缓存
                showClearCacheDialog();
                break;
            case R.id.rl_slidingmenu_profile_change_mode_layout: // 夜间模式

                break;
            case R.id.rl_slidingmenu_profile_feekback_layout: // 意见反馈
                FeedbackActivity.start(mContext);
                break;
            case R.id.rl_slidingmenu_profile_commend_layout: // 推荐给好友
                showShareApp();
                break;
            case R.id.rl_slidingmenu_profile_aboutme_layout: // 关于我们
                AboutUsActivity.start(mContext);
                break;
        }

    }

    /**
     * 分享app
     */
    private void showShareApp() {
        OnekeyShare oks = new OnekeyShare();
        // 关闭sso授权
        oks.disableSSOWhenAuthorize();
        // title标题，印象笔记、邮箱、信息、微信、人人网、QQ和QQ空间使用
        oks.setTitle("六阿哥");
        // titleUrl是标题的网络链接，仅在Linked-in,QQ和QQ空间使用
        oks.setTitleUrl("https://www.6ag.cn");
        // text是分享文本，所有平台都需要这个字段
        oks.setText("六阿哥网是国内最大的以奇闻异事探索为主题的网站之一，为广大探索爱好者提供丰富的探索资讯内容。进入app下载界面...");
        // 分享网络图片，新浪微博分享网络图片需要通过审核后申请高级写入接口，否则请注释掉测试新浪微博
        oks.setImageUrl(APIs.BASE_URL + "icon.png");
        // url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl("https://www.6ag.cn");
        // site是分享此内容的网站名称，仅在QQ空间使用
        oks.setSite("六阿哥");
        // siteUrl是分享此内容的网站地址，仅在QQ空间使用
        oks.setSiteUrl("https://www.6ag.cn");
        // 启动分享GUI
        oks.show(mContext);
    }

    /**
     * 清除缓存前需要询问一下用户
     */
    private void showClearCacheDialog() {
        String cacheString = FileCacheUtils.getTotalCacheSize(mContext);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
        builder.setCancelable(true);
        builder.setTitle("您确定要清除缓存吗？一共有" + cacheString + "缓存");
        builder.setMessage("保留缓存可以节省您的流量哦！");
        builder.setPositiveButton("确定清除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                clearCache();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * 清除缓存
     */
    private void clearCache() {
        // 清理新闻json数据 - 不清理json数据
//        NewsDALManager.shared.clearCache();

        // Fresco清除图片缓存
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        imagePipeline.clearCaches();

        // 清除缓存目录 - 清除所有缓存目录文件
        FileCacheUtils.clearAllCache(mContext);

        final KProgressHUD hud = ProgressHUD.show(mContext, "正在清理...");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hud.dismiss();
                ProgressHUD.showInfo(mContext, "清理缓存完成");
            }
        }, 2000);

    }

    /**
     * 加载数据
     */
    private void loadData() {
        // 加载栏目集合
        loadNewsColumnList();
    }

    /**
     * 从网络加载分类栏目数据
     */
    private void loadNewsColumnList() {

        ColumnBean.loadNewsColumnList(new ColumnBean.ColumnListCallback() {
            @Override
            public void onSuccess(List<ColumnBean> selectedList, List<ColumnBean> optionalList) {
                // 刷新栏目
                refreshColumn(selectedList, optionalList);
            }

            @Override
            public void onError(String tipString) {
                ProgressHUD.showInfo(mContext, tipString);
            }
        });

    }

    /**
     * 刷新栏目
     *
     * @param selectedList
     * @param optionalList
     */
    private void refreshColumn(List<ColumnBean> selectedList, List<ColumnBean> optionalList) {
        // 清空集合数据
        mSelectedList.clear();
        mOptionalList.clear();
        mNewsListFragmentList.clear();

        mSelectedList.addAll(selectedList);
        mOptionalList.addAll(optionalList);

        for (int i = 0; i < mSelectedList.size(); i++) {
            NewsListFragment newsListFragment = NewsListFragment.newInstance(mSelectedList.get(i).getClassid());
            mNewsListFragmentList.add(newsListFragment);
        }

        // 重新加载ViewPager数据
        mFragmentPageAdapter.reloadData(mNewsListFragmentList, mSelectedList);
        mNewsViewPager.setCurrentItem(0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_COLUMN:
                // 处理修改栏目顺序或数量后重新加载
                if (resultCode == RESULT_OK) {
                    refreshColumn(
                            (List<ColumnBean>) data.getSerializableExtra("selectedList_key"),
                            (List<ColumnBean>) data.getSerializableExtra("optionalList_key"));
                }
                break;
        }
    }

    /**
     * 检查是否有新版本
     */
    private void checkVersion() {

        NetworkUtils.shared.get(APIs.UPDATE, new HashMap<String, String>(), new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                ProgressHUD.showInfo(mContext, "您的网络不给力");
            }

            @Override
            public void onResponse(String response, int id) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("err_msg").equals("success")) {
                        JSONObject versionInfo = jsonObject.getJSONObject("data");
                        serverVersion = versionInfo.getString("version");
                        description = versionInfo.getString("description");
                        apkUrl = versionInfo.getString("url");

                        // 更新版本
                        showUpdateDialog();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    ProgressHUD.showInfo(mContext, "数据解析异常");
                }
            }
        });

    }

    /**
     * 弹出对话框更新app版本
     */
    protected void showUpdateDialog() {

        // 检查是否是新版本
        String currentVersion = App.app.getVersionName();
        if (currentVersion.equals(serverVersion)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发现新版本:" + serverVersion);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage(description);
        builder.setCancelable(false);
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 判断是否有写入SD权限
                if (ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(mContext,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    // 有写入权限直接下载apk
                    downloadAPK();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 运行时权限请求回调结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadAPK();
                } else {
                    Toast.makeText(getApplicationContext(), "你没有文件写入权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * 下载新版本
     */
    protected void downloadAPK() {

        // apk文件保存路径
        String apkPath = null;
        final String apkName = "liuage" + serverVersion + ".apk";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            apkPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        if (apkPath == null) {
            ProgressHUD.showInfo(mContext, "您的手机没有SD卡");
            return;
        }

        // 弹出下载进度会话框
        showDownloadDialog();

        // 下载文件
        OkHttpUtils
                .get()
                .url(apkUrl)
                .build()
                .execute(new FileCallBack(apkPath, apkName) {

                    @Override
                    public void inProgress(float progress, long total, int id) {
                        // 更新下载进度
                        mDownloadDialog.setProgress(Math.round(progress * 100));
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        mDownloadDialog.dismiss();
                        ProgressHUD.showInfo(mContext, "您的网络不给力哦");
                    }

                    @Override
                    public void onResponse(File response, int id) {
                        mDownloadDialog.dismiss();
                        // 下载完成安装apk
                        installAPK(response.getAbsolutePath());
                    }
                });

    }

    /**
     * 弹出下载对话框
     */
    public void showDownloadDialog() {
        mDownloadDialog = new ProgressDialog(mContext);
        mDownloadDialog.setIcon(R.mipmap.ic_launcher);
        mDownloadDialog.setTitle("版本更新");
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setMessage("正在玩命下载中......");
        mDownloadDialog.getWindow().setGravity(Gravity.CENTER);
        mDownloadDialog.setMax(100);
        mDownloadDialog.show();
    }

    /**
     * 安装下载的新版本apk
     *
     * @param apkPath apk存放路径
     */
    private void installAPK(String apkPath) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.parse("file://" + apkPath), "application/vnd.android.package-archive");
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
        }
        return true;
    }

    // 记录两次点击退出时的第一次有效点击时间
    private long time = 0;

    /**
     * 2秒内连续点击返回2次back才退出app
     */
    private void exit() {
        if (System.currentTimeMillis() - time > 2000) {
            time = System.currentTimeMillis();
            showToast("再次点击将退出");
        } else {
            removeAllActivity();
        }
    }

}
