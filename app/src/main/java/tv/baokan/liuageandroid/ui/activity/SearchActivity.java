package tv.baokan.liuageandroid.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.cache.KeyboardCache;
import tv.baokan.liuageandroid.cache.NewsDALManager;
import tv.baokan.liuageandroid.model.ArticleListBean;
import tv.baokan.liuageandroid.utils.APIs;
import tv.baokan.liuageandroid.utils.LogUtils;
import tv.baokan.liuageandroid.utils.NetworkUtils;
import tv.baokan.liuageandroid.utils.ProgressHUD;

public class SearchActivity extends BaseActivity {

    private static final String TAG = "SearchActivity";
    private SearchView mSearchView;
    private ImageView mBackButton;

    private RecyclerView mKeyboardListRecyclerView;
    private KeyboardListAdapter mKeyboardListAdapter;
    private RecyclerView mHotKeyboardView;
    private HotKeyboardListAdapter mHotKeyboardListAdapter;

    /**
     * 便捷启动当前activity
     *
     * @param activity 启动当前activity的activity
     */
    public static void start(Activity activity) {
        Intent intent = new Intent(activity, SearchActivity.class);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.push_enter, R.anim.push_exit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        prepareUI();
        setupRecyclerView();
        loadKeyboardListOrderNum();
    }

    /**
     * 准备UI
     */
    private void prepareUI() {
        mSearchView = (SearchView) findViewById(R.id.sv_searchView);
        mBackButton = (ImageView) findViewById(R.id.iv_nav_back);
        mKeyboardListRecyclerView = (RecyclerView) findViewById(R.id.rv_search_list_recyclerview);
        mHotKeyboardView = (RecyclerView) findViewById(R.id.rv_search_hot_list_recyclerview);

        // 去掉搜索文字下划线
        Class<?> c = mSearchView.getClass();
        try {
            Field f = c.getDeclaredField("mSearchPlate");//通过反射，获得类对象的一个属性对象
            f.setAccessible(true);//设置此私有属性是可访问的
            View v = (View) f.get(mSearchView);//获得属性的值
            v.setBackgroundColor(Color.TRANSPARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 修改搜索文字大小
        int id = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) mSearchView.findViewById(id);
        try {
            android.widget.LinearLayout.LayoutParams layoutParams = (android.widget.LinearLayout.LayoutParams) textView.getLayoutParams();
            layoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.search_text_bottom_margin);
            textView.setLayoutParams(layoutParams);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        // 搜索框
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            // 点击了搜索
            @Override
            public boolean onQueryTextSubmit(String query) {
                LogUtils.d(TAG, query);
                // 根据关键词搜索结果
                loadSearchListFromNetwork(query);
                return false;
            }

            // 内容改变
            @Override
            public boolean onQueryTextChange(String newText) {
                LogUtils.d(TAG, newText);
                // 输入内容则隐藏热门关键词视图
                if (newText.length() > 0) {
                    mHotKeyboardView.setVisibility(View.INVISIBLE);
                } else {
                    mHotKeyboardView.setVisibility(View.VISIBLE);
                }
                // 更新关键词列表
                loadKeyboardList(newText);
                return false;
            }
        });

        // 返回
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * 配置recyclerView关联关键词列表
     */
    private void setupRecyclerView() {
        mKeyboardListRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mKeyboardListAdapter = new KeyboardListAdapter(mContext);
        mKeyboardListRecyclerView.setAdapter(mKeyboardListAdapter);
        mKeyboardListAdapter.setOnClickListener(new KeyboardListAdapter.OnClickListener() {
            @Override
            public void didClickItem(String keyboard) {
                loadSearchListFromNetwork(keyboard);
            }
        });

        // 监听滚动
        mKeyboardListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
     * 配置热门关键词集合视图
     *
     * @param keyboardCacheList 热门关键词集合
     */
    private void setupSearchHotKeyboardView(List<KeyboardCache> keyboardCacheList) {
        mHotKeyboardView.setLayoutManager(new GridLayoutManager(mContext, 4));
        mHotKeyboardListAdapter = new HotKeyboardListAdapter(mContext, keyboardCacheList);
        mHotKeyboardView.setAdapter(mHotKeyboardListAdapter);
        mHotKeyboardListAdapter.setOnClickListener(new HotKeyboardListAdapter.OnClickListener() {
            @Override
            public void didClickItem(String keyboard) {
                loadSearchListFromNetwork(keyboard);
            }
        });
    }

    /**
     * 搜索本地关键词库
     *
     * @param keyboard 要搜索的关键字
     */
    private void loadKeyboardList(String keyboard) {
        List<KeyboardCache> keyboardCacheList = NewsDALManager.shared.loadKeyboardListFromLocation(keyboard);
        mKeyboardListAdapter.update(keyboardCacheList);
    }

    /**
     * 加载本地出现次数最多的关键词
     */
    private void loadKeyboardListOrderNum() {
        List<KeyboardCache> keyboardCacheList = NewsDALManager.shared.loadKeyboardListFromLocationOrderNum();
        for (KeyboardCache keyboard :
                keyboardCacheList) {
            LogUtils.d(TAG, "keyboard = " + keyboard.getKeyboard() + " num = " + keyboard.getNum());
        }
        setupSearchHotKeyboardView(keyboardCacheList);
    }

    /**
     * 上拉只加载列表数据
     *
     * @param keyboard 关键词
     */
    private void loadSearchListFromNetwork(final String keyboard) {

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("keyboard", keyboard);
        parameters.put("pageIndex", String.valueOf(1));
        parameters.put("pageSize", String.valueOf(20));

        final KProgressHUD hud = ProgressHUD.show(mContext, "正在搜索...");
        NetworkUtils.shared.get(APIs.SEARCH, parameters, new NetworkUtils.StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                hud.dismiss();
                ProgressHUD.showInfo(mContext, "您的网络不给力哦");
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
                        ProgressHUD.showInfo(mContext, "没有搜索到任何内容");
                    } else {
                        // 进入搜索结果页面
                        SearchResultActivity.start(mContext, keyboard, tempListBeans);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    ProgressHUD.showInfo(mContext, "关键字只能在2~20个字符");
                } finally {
                    hud.dismiss();
                }
            }
        });

    }

    // 热门关键词集合视图适配器
    static class HotKeyboardListAdapter extends RecyclerView.Adapter<HotKeyboardListAdapter.ViewHolder> {

        private Context mContext;
        private OnClickListener onClickListener;
        private List<KeyboardCache> mKeyboardCacheList = new ArrayList<>(); // 关键词集合

        public void setOnClickListener(OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        public HotKeyboardListAdapter(Context context, List<KeyboardCache> keyboardCacheList) {
            mContext = context;
            mKeyboardCacheList = keyboardCacheList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_hot_keyboard, parent, false);
            final ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null) {
                        onClickListener.didClickItem(mKeyboardCacheList.get(viewHolder.getAdapterPosition()).getKeyboard());
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            KeyboardCache keyboardCache = mKeyboardCacheList.get(position);
            holder.keyboardText.setText(keyboardCache.getKeyboard());
        }

        @Override
        public int getItemCount() {
            return mKeyboardCacheList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            TextView keyboardText;

            public ViewHolder(View itemView) {
                super(itemView);
                keyboardText = (TextView) itemView.findViewById(R.id.tv_hot_keyboard_text);
            }
        }

        public static interface OnClickListener {

            /**
             * 点击了关键词列表item
             *
             * @param keyboard 关键词
             */
            public abstract void didClickItem(String keyboard);
        }

    }

    // 关键词列表适配器
    static class KeyboardListAdapter extends RecyclerView.Adapter<KeyboardListAdapter.ViewHolder> {

        private Context mContext;
        private OnClickListener onClickListener;
        private List<KeyboardCache> mKeyboardCacheList = new ArrayList<>(); // 关键词集合

        public void setOnClickListener(OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        /**
         * 刷新关键词列表数据
         *
         * @param keyboardCacheList 关键词列表集合
         */
        public void update(List<KeyboardCache> keyboardCacheList) {
            mKeyboardCacheList.clear();
            mKeyboardCacheList.addAll(keyboardCacheList);
            notifyDataSetChanged();
        }

        public KeyboardListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.cell_search_keyboard_list, parent, false);
            final ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener != null) {
                        onClickListener.didClickItem(mKeyboardCacheList.get(viewHolder.getAdapterPosition()).getKeyboard());
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            KeyboardCache keyboardCache = mKeyboardCacheList.get(position);
            holder.keyboardText.setText(keyboardCache.getKeyboard());
        }

        @Override
        public int getItemCount() {
            return mKeyboardCacheList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            TextView keyboardText;

            public ViewHolder(View itemView) {
                super(itemView);
                keyboardText = (TextView) itemView.findViewById(R.id.tv_search_keyboard_list_title);
            }
        }

        public static interface OnClickListener {

            /**
             * 点击了关键词列表item
             *
             * @param keyboard 关键词
             */
            public abstract void didClickItem(String keyboard);
        }

    }

}
