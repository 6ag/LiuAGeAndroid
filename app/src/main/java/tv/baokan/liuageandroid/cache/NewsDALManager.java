package tv.baokan.liuageandroid.cache;

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import tv.baokan.liuageandroid.app.App;
import tv.baokan.liuageandroid.model.UserBean;
import tv.baokan.liuageandroid.utils.APIs;
import tv.baokan.liuageandroid.utils.AdManager;
import tv.baokan.liuageandroid.utils.LogUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.SharedPreferencesUtils;

/**
 * 资讯数据访问层
 */
public class NewsDALManager {

    // 资讯列表回调接口
    public static interface NewsListCallback {
        // 成功加载到数据
        public abstract void onSuccess(JSONArray jsonArray);

        // 加载数据失败
        public abstract void onError(String tipString);
    }

    // 资讯详情回调接口
    public static interface NewsContentCallback {
        // 成功加载到数据
        public abstract void onSuccess(JSONObject jsonObject);

        // 加载数据失败
        public abstract void onError(String tipString);
    }

    private static final String TAG = "NewsDALManager";

    // 单例对象
    public static final NewsDALManager shared = new NewsDALManager();

    private NewsDALManager() {
    }

    /**
     * 清除所有缓存
     */
    public void clearCache() {
        DataSupport.deleteAll(NewsListHomeCache.class);
        DataSupport.deleteAll(NewsListOtherCache.class);
        DataSupport.deleteAll(NewsContentCache.class);
        DataSupport.deleteAll(NewsIsGoodHomeCache.class);
        DataSupport.deleteAll(NewsIsGoodOtherCache.class);
        DataSupport.deleteAll(KeyboardCache.class);
    }

    /**
     * 对文章进行修改后需要移除旧的缓存并重新缓存
     *
     * @param classid 文章分类id
     * @param id      文章id
     */
    public void removeNewsDetail(final String classid, final String id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = DataSupport.deleteAll(NewsContentCache.class, "articleid == ?", id);
                LogUtils.d(TAG, "移除指定文章缓存 " + count + " 条");

                loadNewsContent(classid, id, new NewsContentCallback() {
                    @Override
                    public void onSuccess(JSONObject jsonObject) {
                        LogUtils.d(TAG, "重新缓存文章成功");
                    }

                    @Override
                    public void onError(String tipString) {
                        LogUtils.d(TAG, "重新缓存文章失败");
                    }
                });
            }
        }).start();
    }

    /**
     * 移除指定分类的列表数据
     * 今日头条的数据和其他分类的数据不是存在一张表里的，需要判断下哦
     * 资讯内容数据基本固定，可以根据需求写个过期清理
     *
     * @param classid 分类id
     */
    public void removeNewsList(String classid) {
        if (classid.equals("0")) {
            // 今日头条
            int deleteCount = DataSupport.deleteAll(NewsListHomeCache.class);
            LogUtils.d(TAG, "删除了今日头条分类下 " + deleteCount + " 条记录");
        } else {
            // 其他分类
            int deleteCount = DataSupport.deleteAll(NewsListOtherCache.class);
            LogUtils.d(TAG, "删除了其他分类下 " + deleteCount + " 条记录");
        }
    }

    /**
     * 加载资讯列表数据
     *
     * @param type             类型 isgood就是幻灯片
     * @param classid          分类id
     * @param pageIndex        分页页码
     * @param newsListCallback 资讯列表回调
     */
    public void loadNewsList(final String type, final String classid, final int pageIndex, final NewsListCallback newsListCallback) {

        // 先从本地加载数据
        loadNewsListFromLocal(type, classid, pageIndex, new NewsListCallback() {

            // 数据加载成功
            @Override
            public void onSuccess(JSONArray jsonArray) {
                // 本地加载数据成功，就直接返回数据
                newsListCallback.onSuccess(jsonArray);
            }

            // 数据加载失败
            @Override
            public void onError(String tipString) {
                LogUtils.d(TAG, "加载本地列表数据失败 = " + tipString);
                // 从本地加载数据失败，就去网络加载
                loadNewsListFromNetwork(type, classid, pageIndex, new NewsListCallback() {
                    @Override
                    public void onSuccess(final JSONArray jsonArray) {
                        // 返回给调用者
                        newsListCallback.onSuccess(jsonArray);

                        // 并缓存到本地
                        saveNewsList(type, classid, jsonArray);
                    }

                    @Override
                    public void onError(String tipString) {
                        newsListCallback.onError(tipString);
                    }
                });
            }
        });

    }

    /**
     * 加载资讯内容数据
     *
     * @param classid             分类id
     * @param id                  文章id
     * @param newsContentCallback 资讯内容回调
     */
    public void loadNewsContent(final String classid, final String id, final NewsContentCallback newsContentCallback) {

        loadNewsContentFromLocal(classid, id, new NewsContentCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                newsContentCallback.onSuccess(jsonObject);
            }

            @Override
            public void onError(String tipString) {
                LogUtils.d(TAG, "加载本地内容数据失败 = " + tipString);
                loadNewsContentFromNetwork(classid, id, new NewsContentCallback() {
                    @Override
                    public void onSuccess(final JSONObject jsonObject) {

                        // 返回给调用者
                        newsContentCallback.onSuccess(jsonObject);

                        // 并缓存到本地
                        saveNewsContent(classid, id, jsonObject);
                    }

                    @Override
                    public void onError(String tipString) {
                        newsContentCallback.onError(tipString);
                    }
                });
            }
        });

    }

    /**
     * 缓存资讯列表数据到本地数据库
     *
     * @param classid   分类id
     * @param jsonArray 资讯json数据
     */
    private void saveNewsList(final String type, final String classid, final JSONArray jsonArray) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (shared) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            if (classid.equals("0")) {
                                if (type.equals("isgood")) {
                                    // 今日头条 分类
                                    NewsIsGoodHomeCache homeCache = new NewsIsGoodHomeCache();
                                    homeCache.setClassid(jsonObject.getString("classid"));
                                    homeCache.setNews(jsonObject.toString());
                                    homeCache.saveThrows();
                                } else {
                                    // 今日头条 分类
                                    NewsListHomeCache homeCache = new NewsListHomeCache();
                                    homeCache.setClassid(jsonObject.getString("classid"));
                                    homeCache.setNews(jsonObject.toString());
                                    homeCache.saveThrows();
                                }
                            } else {
                                if (type.equals("isgood")) {
                                    NewsIsGoodOtherCache otherCache = new NewsIsGoodOtherCache();
                                    otherCache.setClassid(jsonObject.getString("classid"));
                                    otherCache.setNews(jsonObject.toString());
                                    otherCache.saveThrows();
                                } else {
                                    NewsListOtherCache otherCache = new NewsListOtherCache();
                                    otherCache.setClassid(jsonObject.getString("classid"));
                                    otherCache.setNews(jsonObject.toString());
                                    otherCache.saveThrows();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    LogUtils.d(TAG, "缓存资讯列表数据成功" + jsonArray.toString());
                }
            }
        }).start();

    }

    /**
     * 从本地缓存加载资讯数据列表
     *
     * @param classid          分类id
     * @param pageIndex        分页页码
     * @param newsListCallback 资讯列表回调
     */
    private void loadNewsListFromLocal(String type, String classid, int pageIndex, final NewsListCallback newsListCallback) {

        int preCount = (pageIndex - 1) * 20;
        int oneCount = 20;

        LogUtils.d(TAG, "当前分页 = " + pageIndex);

        JSONArray jsonArray = new JSONArray();

        if (classid.equals("0")) {
            if (type.equals("isgood")) {
                // 今日头条 分类
                List<NewsIsGoodHomeCache> homeCaches = DataSupport.order("id asc").limit(3).offset(0).find(NewsIsGoodHomeCache.class);
                for (NewsIsGoodHomeCache homeCache :
                        homeCaches) {
                    try {
                        JSONObject jsonObject = new JSONObject(homeCache.getNews());
                        jsonArray.put(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        newsListCallback.onError("数据解析失败");
                        break;
                    }
                }
            } else {
                // 今日头条 分类
                List<NewsListHomeCache> homeCaches = DataSupport.order("id asc").limit(oneCount).offset(preCount).find(NewsListHomeCache.class);
                for (NewsListHomeCache homeCache :
                        homeCaches) {
                    try {
                        JSONObject jsonObject = new JSONObject(homeCache.getNews());
                        jsonArray.put(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        newsListCallback.onError("数据解析失败");
                        break;
                    }
                }
            }
            if (jsonArray.length() > 0) {
                newsListCallback.onSuccess(jsonArray);
                LogUtils.d(TAG, "加载到缓存今日头条数据 = " + jsonArray.toString());
            } else {
                newsListCallback.onError("没有缓存数据");
            }
        } else {
            if (type.equals("isgood")) {
                // 其他分类
                List<NewsIsGoodOtherCache> otherCaches = DataSupport.order("id asc").where("classid = ?", classid).limit(3).offset(0).find(NewsIsGoodOtherCache.class);
                for (NewsIsGoodOtherCache otherCache :
                        otherCaches) {
                    try {
                        JSONObject jsonObject = new JSONObject(otherCache.getNews());
                        jsonArray.put(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        newsListCallback.onError("数据解析失败");
                        break;
                    }
                }
            } else {
                // 其他分类
                List<NewsListOtherCache> otherCaches = DataSupport.order("id asc").where("classid = ?", classid).limit(oneCount).offset(preCount).find(NewsListOtherCache.class);
                for (NewsListOtherCache otherCache :
                        otherCaches) {
                    try {
                        JSONObject jsonObject = new JSONObject(otherCache.getNews());
                        jsonArray.put(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        newsListCallback.onError("数据解析失败");
                        break;
                    }
                }
            }
            if (jsonArray.length() > 0) {
                newsListCallback.onSuccess(jsonArray);
                LogUtils.d(TAG, "加载到缓存其他分类数据 = " + jsonArray.toString());
            } else {
                newsListCallback.onError("没有本地数据");
            }

        }

    }

    /**
     * 从本地缓存加载资讯数据列表
     *
     * @param classid          分类id
     * @param newsListCallback 资讯列表回调
     */
    public void loadNewsListFromLocal(String classid, final NewsListCallback newsListCallback) {

        int preCount = 0;
        int oneCount = 20;
        JSONArray jsonArray = new JSONArray();
        // 其他分类
        List<NewsListOtherCache> otherCaches = DataSupport.order("id asc").where("classid = ?", classid).limit(oneCount).offset(preCount).find(NewsListOtherCache.class);
        for (NewsListOtherCache otherCache :
                otherCaches) {
            try {
                JSONObject jsonObject = new JSONObject(otherCache.getNews());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
                newsListCallback.onError("数据解析失败");
                break;
            }
        }

        if (jsonArray.length() > 0) {
            newsListCallback.onSuccess(jsonArray);
            LogUtils.d(TAG, "加载到缓存其他分类数据 = " + jsonArray.toString());
        } else {
            newsListCallback.onError("没有本地数据");
        }

    }

    /**
     * 从网络加载资讯列表数据
     *
     * @param type             类型 isgood 幻灯片
     * @param classid          分类id
     * @param pageIndex        分页页码
     * @param newsListCallback 资讯列表回调
     */
    public void loadNewsListFromNetwork(String type, String classid, int pageIndex, final NewsListCallback newsListCallback) {

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("classid", classid);
        parameters.put("pageIndex", String.valueOf(pageIndex));
        if (type.equals("isgood")) {
            parameters.put("query", type);
            parameters.put("pageSize", String.valueOf(3));
        } else {
            parameters.put("pageSize", String.valueOf(20));
        }

        NetworkUtils.shared.get(APIs.ARTICLE_LIST, parameters, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                newsListCallback.onError("您的网络不给力哦");
            }

            @Override
            public void onResponse(String response, int id) {
                try {
                    // 如果所有接口响应格式是统一的，这些判断是可以封装在网络请求工具类里的哦
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("err_msg").equals("success")) {
                        if (jsonObject.getString("data").equals("null")) {
                            newsListCallback.onError("没有更多数据了");
                        } else {
                            JSONArray jsonArray = jsonObject.getJSONArray("data");
                            LogUtils.d(TAG, "从网络请求资讯列表数据成功 " + jsonArray.toString());
                            newsListCallback.onSuccess(jsonArray);
                        }
                    } else {
                        String errorInfo = jsonObject.getString("info");
                        newsListCallback.onError(errorInfo);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    newsListCallback.onError("数据解析异常");
                }

            }
        });

    }

    /**
     * 缓存资讯内容数据到本地数据库
     *
     * @param classid    分类id
     * @param id         文章id
     * @param jsonObject 资讯内容json数据
     */
    private void saveNewsContent(final String classid, final String id, final JSONObject jsonObject) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (shared) {
                    // 如果没有被缓存，则去缓存
                    NewsContentCache newsContentCache = new NewsContentCache();
                    newsContentCache.setClassid(classid);
                    newsContentCache.setArticleid(id);
                    newsContentCache.setNews(jsonObject.toString());
                    newsContentCache.saveThrows();
                    LogUtils.d(TAG, "缓存资讯内容数据成功" + jsonObject.toString());
                }
            }
        }).start();

    }

    /**
     * 从本地加载资讯内容数据
     *
     * @param classid             分类id
     * @param id                  文章id
     * @param newsContentCallback 资讯内容回调
     */
    private void loadNewsContentFromLocal(String classid, String id, NewsContentCallback newsContentCallback) {
        NewsContentCache contentCache = DataSupport.where("classid = ? and articleid = ?", classid, id).findFirst(NewsContentCache.class);
        if (contentCache != null) {
            try {
                JSONObject jsonObject = new JSONObject(contentCache.getNews());
                newsContentCallback.onSuccess(jsonObject);
                LogUtils.d(TAG, "加载到缓存资讯内容数据 = " + jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                newsContentCallback.onError("数据解析异常");
            }
        } else {
            newsContentCallback.onError("没有缓存数据");
        }
    }

    /**
     * 从网络加载资讯内容数据
     *
     * @param classid             分类id
     * @param id                  文章id
     * @param newsContentCallback 资讯内容回调
     */
    private void loadNewsContentFromNetwork(String classid, String id, final NewsContentCallback newsContentCallback) {

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("classid", classid);
        parameters.put("id", id);
        if (UserBean.isLogin()) {
            parameters.put("username", UserBean.shared().getUsername());
            parameters.put("userid", UserBean.shared().getUserid());
            parameters.put("token", UserBean.shared().getToken());
        }

        NetworkUtils.shared.get(APIs.ARTICLE_DETAIL, parameters, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                newsContentCallback.onError("您的网络不给力哦");
            }

            @Override
            public void onResponse(String response, int id) {
                try {
                    JSONObject jsonObject = new JSONObject(response).getJSONObject("data");
                    LogUtils.d(TAG, "从网络请求资讯内容数据成功 " + jsonObject.toString());
                    newsContentCallback.onSuccess(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                    newsContentCallback.onError("数据解析异常");
                }
            }
        });

    }

    /**
     * 是否需要更新本地关键词列表
     */
    public void shouldUpdateKeyboardList() {

        NetworkUtils.shared.get(APIs.UPDATE_SEARCH_KEY_LIST, null, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                LogUtils.d(TAG, "您的网络不给力哦 - shouldUpdateKeyboardList");
            }

            @Override
            public void onResponse(String response, int id) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String updateSign = jsonObject.getString("data");
                    String locationUpdateSign = SharedPreferencesUtils.getString(App.getContext(), "isShouldUpdateKeyboard", "0");
                    if (!updateSign.equals(locationUpdateSign)) {
                        // 需要更新
                        updateKeyboardListFromNetwork(updateSign);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * 更新本地关键词列表
     */
    private void updateKeyboardListFromNetwork(final String updateSign) {
        // 先删除本地关键词数据
        int deleteCount = DataSupport.deleteAll(KeyboardCache.class);
        LogUtils.d(TAG, "删除了关键词数据 " + deleteCount + " 条记录");

        // 重新从网络请求关键词数据
        NetworkUtils.shared.get(APIs.SEARCH_KEY_LIST, null, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                LogUtils.d(TAG, "请求关键词列表数据失败");
            }

            @Override
            public void onResponse(final String response, int id) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (shared) {
                            try {
                                JSONArray jsonArray = new JSONObject(response).getJSONArray("data");
                                LogUtils.d(TAG, "从网络请求关键词列表数据成功 " + jsonArray.toString());
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    KeyboardCache keyboardCache = new KeyboardCache();
                                    keyboardCache.setPinyin(jsonObject.getString("pinyin"));
                                    keyboardCache.setKeyboard(jsonObject.getString("keyboard"));
                                    keyboardCache.setNum(jsonObject.getInt("num"));
                                    keyboardCache.saveThrows();
                                }
                                LogUtils.d(TAG, "更新本地关键词库成功");
                                // 更新成功后修改标识
                                SharedPreferencesUtils.setString(App.getContext(), "isShouldUpdateKeyboard", updateSign);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                LogUtils.d(TAG, "解析关键词列表数据失败");
                            }
                        }
                    }
                }).start();

            }
        });
    }

    /**
     * 加载本地关键词数据模型集合
     *
     * @param keyboard 需要搜索的关键词
     * @return 关键词模型集合
     */
    public List<KeyboardCache> loadKeyboardListFromLocation(String keyboard) {
        if (keyboard.length() == 0) {
            return new ArrayList<>();
        }
        List<KeyboardCache> keyboardCacheList = new ArrayList<>();
        Cursor cursor = DataSupport.findBySQL("select * from keyboardcache where keyboard like ? or pinyin like ? order by num desc limit 20", "%" + keyboard + "%", "%" + keyboard + "%");
        if (cursor.moveToFirst()) {
            do {
                KeyboardCache keyboardCache = new KeyboardCache();
                keyboardCache.setKeyboard(cursor.getString(cursor.getColumnIndex("keyboard")));
                keyboardCache.setPinyin(cursor.getString(cursor.getColumnIndex("pinyin")));
                keyboardCache.setNum(cursor.getInt(cursor.getColumnIndex("num")));
                keyboardCacheList.add(keyboardCache);
            } while (cursor.moveToNext());
        }
        return keyboardCacheList;
    }

    /**
     * 加载本地出现次数最多的关键词
     *
     * @return 关键词模型集合
     */
    public List<KeyboardCache> loadKeyboardListFromLocationOrderNum() {
        return DataSupport.order("num desc").limit(20).find(KeyboardCache.class);
    }

}
