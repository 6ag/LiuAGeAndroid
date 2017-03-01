package tv.baokan.liuageandroid.utils;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tv.baokan.liuageandroid.cache.NewsDALManager;
import tv.baokan.liuageandroid.model.ArticleListBean;

// 广告管理单例类
public class AdManager {

    private static final String TAG = "AdManager";

    public static AdManager shared() {
        return single;
    }

    private static final AdManager single = new AdManager();

    private AdManager() {
    }

    // 广告分类
    public final String classid = "32";

    // 广告软文集合
    public List<ArticleListBean> articleListBeanList = new ArrayList<>();

    /**
     * 加载广告软文集合
     */
    public void loadAdList(final Activity activity) {

        NewsDALManager.shared.loadNewsList("", classid, 1, new NewsDALManager.NewsListCallback() {
            @Override
            public void onSuccess(JSONArray jsonArray) {
                try {
                    List<ArticleListBean> tempListBeans = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ArticleListBean bean = new ArticleListBean(jsonArray.getJSONObject(i));
                        tempListBeans.add(bean);
                        if (bean.getIstop().equals("1") && bean.getMorepic().length > 0) {
                            // 判断本地磁盘是否已经缓存 - 预缓存开屏启动大图
                            final String url = bean.getMorepic()[0];
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    FileCacheUtils.checkCacheInDisk(url, new FileCacheUtils.OnCheckCacheInDiskListener() {
                                        @Override
                                        public void checkCacheInDisk(boolean isExist, final String filePath) {
                                            if (!isExist) {
                                                FileCacheUtils.downloadImage(activity, url, new FileCacheUtils.OnDownloadImageToDiskListener() {
                                                    @Override
                                                    public void downloadFinished(boolean success, final String filePath) {
                                                        if (success) {
                                                            LogUtils.d(TAG, "开屏广告图下载文件成功 url = " + url);
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            }).start();
                        }
                    }
                    articleListBeanList = tempListBeans;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String tipString) {
                LogUtils.d(TAG, tipString);
            }
        });
    }

    /**
     * 加载开屏广告数据
     *
     * @param launchAdCallback 完成回调
     */
    public void loadLaunchAd(final LaunchAdCallback launchAdCallback) {
        NewsDALManager.shared.loadNewsListFromLocal(classid, new NewsDALManager.NewsListCallback() {
            @Override
            public void onSuccess(JSONArray jsonArray) {
                try {
                    List<ArticleListBean> tempListBeans = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ArticleListBean bean = new ArticleListBean(jsonArray.getJSONObject(i));
                        if (bean.getIstop().equals("1") && bean.getMorepic().length > 0) {
                            tempListBeans.add(bean);
                        }
                    }
                    if (tempListBeans.size() > 0) {
                        int max = tempListBeans.size();
                        int randomNum = new Random().nextInt(max);
                        launchAdCallback.onSuccess(true, tempListBeans.get(randomNum));
                    } else {
                        launchAdCallback.onError("没有广告数据");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    launchAdCallback.onError("没有广告数据");
                }
            }

            @Override
            public void onError(String tipString) {
                launchAdCallback.onError("没有广告数据");
            }
        });
    }

    // 启动广告回调接口
    public static interface LaunchAdCallback {
        // 成功加载到数据
        public abstract void onSuccess(boolean isSuccess, ArticleListBean articleListBean);

        // 加载数据失败
        public abstract void onError(String tipString);
    }

}
