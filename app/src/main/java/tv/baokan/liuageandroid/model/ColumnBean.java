package tv.baokan.liuageandroid.model;

import com.alibaba.fastjson.JSON;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import tv.baokan.liuageandroid.app.App;
import tv.baokan.liuageandroid.utils.APIs;
import tv.baokan.liuageandroid.utils.LogUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.StreamUtils;

/**
 * 分类栏目
 */
public class ColumnBean implements Serializable {

    private static final String TAG = "ColumnBean";

    // 分类id
    private String classid;

    // 分类名称
    private String classname;

    // 表名
    private String tbname;

    public ColumnBean(JSONObject jsonObject) {
        try {
            classid = jsonObject.getString("classid");
            classname = jsonObject.getString("classname");
            tbname = jsonObject.getString("tbname");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ColumnBean(String classid, String classname) {
        this.classid = classid;
        this.classname = classname;
    }

    public ColumnBean(String classid, String classname, String tbname) {
        this.classid = classid;
        this.classname = classname;
        this.tbname = tbname;
    }

    public String getClassid() {
        return classid;
    }

    public void setClassid(String classid) {
        this.classid = classid;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getTbname() {
        return tbname;
    }

    public void setTbname(String tbname) {
        this.tbname = tbname;
    }


    // 栏目集合接口
    public static interface ColumnListCallback {
        // 成功加载到数据
        public abstract void onSuccess(List<ColumnBean> selectedList, List<ColumnBean> optionalList);

        // 加载数据失败
        public abstract void onError(String tipString);
    }

    /**
     * 加载栏目集合
     *
     * @param columnListCallback
     */
    public static void loadNewsColumnList(final ColumnListCallback columnListCallback) {

        loadNewsColumnListFromCache(new ColumnListCallback() {
            @Override
            public void onSuccess(List<ColumnBean> selectedList, List<ColumnBean> optionalList) {
                // 如果有本地数据则直接回调
                columnListCallback.onSuccess(selectedList, optionalList);
            }

            @Override
            public void onError(String tipString) {
                // 没有本地数据则从网络加载
                loadNewsColumnListFromNetwork(columnListCallback);
            }
        });
    }

    /**
     * 从网络加载分类栏目数据
     *
     * @param columnListCallback
     */
    public static void loadNewsColumnListFromNetwork(final ColumnListCallback columnListCallback) {
        NetworkUtils.shared.get(APIs.GET_CLASS, null, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                columnListCallback.onError("您的网络不给力哦");
            }

            @Override
            public void onResponse(String response, int id) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("err_msg").equals("success")) {

                        List<ColumnBean> selectedList = new ArrayList<>();
                        List<ColumnBean> optionalList = new ArrayList<>();

                        // 插入推荐分类
                        selectedList.add(new ColumnBean("0", "推荐", "news"));

                        JSONArray jsonArray = jsonObject.getJSONArray("data");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonCategory = jsonArray.getJSONObject(i);
                            ColumnBean columnBean = new ColumnBean(jsonCategory);
                            if (columnBean.getTbname().equals("news")) {
                                if (i > 5) {
                                    optionalList.add(columnBean);
                                } else {
                                    selectedList.add(columnBean);
                                }
                            }
                        }

                        // 回调前先缓存分类数据
                        saveColumnListToCache(selectedList, optionalList);

                        columnListCallback.onSuccess(selectedList, optionalList);

                    } else {
                        String errorInfo = jsonObject.getString("info");
                        columnListCallback.onError(errorInfo);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    columnListCallback.onError("栏目数据解析异常");
                }
            }
        });

    }

    /**
     * 从缓存加载分类栏目数据
     *
     * @param columnListCallback
     */
    public static void loadNewsColumnListFromCache(final ColumnListCallback columnListCallback) {
        if (!StreamUtils.fileIsExists(App.getContext().getFileStreamPath("column.json").getAbsolutePath())) {
            columnListCallback.onError("本地无缓存栏目数据");
            return;
        }

        List<ColumnBean> selectedList = new ArrayList<>();
        List<ColumnBean> optionalList = new ArrayList<>();

        String jsonString = StreamUtils.readStringFromFile("column.json");
        LogUtils.d(TAG, "加载缓存栏目数据成功 " + jsonString);
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray selectedJSONArray = jsonObject.getJSONArray("selected");
            JSONArray optionalJSONArray = jsonObject.getJSONArray("optional");

            for (int i = 0; i < selectedJSONArray.length(); i++) {
                ColumnBean columnBean = new ColumnBean(
                        selectedJSONArray.getJSONObject(i).getString("classid"),
                        selectedJSONArray.getJSONObject(i).getString("classname"));
                selectedList.add(columnBean);
            }

            for (int i = 0; i < optionalJSONArray.length(); i++) {
                ColumnBean columnBean = new ColumnBean(
                        optionalJSONArray.getJSONObject(i).getString("classid"),
                        optionalJSONArray.getJSONObject(i).getString("classname"));
                optionalList.add(columnBean);
            }

            columnListCallback.onSuccess(selectedList, optionalList);
        } catch (JSONException e) {
            e.printStackTrace();
            columnListCallback.onError("解析本地缓存栏目数据异常");
        }
    }

    /**
     * 保存栏目到本地缓存
     *
     * @param selectedList
     * @param optionalList
     */
    public static void saveColumnListToCache(List<ColumnBean> selectedList, List<ColumnBean> optionalList) {
        HashMap<String, List<ColumnBean>> map = new HashMap<>();
        map.put("selected", selectedList);
        map.put("optional", optionalList);
        String jsonString = JSON.toJSONString(map);
        // 将栏目数据写入本地
        StreamUtils.writeStringToFile("column.json", jsonString);
        LogUtils.d(TAG, "栏目数据缓存成功 " + jsonString);
    }

    /**
     * 清除本地栏目缓存
     */
    public static void cleanColumnListFromCache() {
        StreamUtils.cleanFile(App.getContext().getFileStreamPath("column.json").getAbsolutePath());
    }

}
