package tv.baokan.liuageandroid.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;
import com.youth.banner.Transformer;
import com.youth.banner.listener.OnBannerClickListener;
import com.youth.banner.loader.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import tv.baokan.liuageandroid.R;
import tv.baokan.liuageandroid.model.ArticleListBean;
import tv.baokan.liuageandroid.utils.AdManager;
import tv.baokan.liuageandroid.utils.DateUtils;
import tv.baokan.liuageandroid.utils.ProgressHUD;
import tv.baokan.liuageandroid.utils.SizeUtils;

/**
 * 新闻列表数据适配器
 */
public class NewsListRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "NewsListRecyclerViewAda";

    public static interface OnItemTapListener {
        public abstract void onItemTapListener(ArticleListBean articleListBean);
    }

    // item类型枚举
    private enum NEWS_ITEM_TYPE {
        HEADER_VIEW, // 头部视图
        NO_PIC,      // 无图
        ONE_PIC,     // 单图
        BIG_ONE_PIC, // 大单图
        MORE_PIC     // 多图
    }

    private List<ArticleListBean> mArticleListBeans = new ArrayList<>(); // 列表数据
    private List<ArticleListBean> mIsGoodArticleBeans = new ArrayList<>(); // 幻灯片数据
    private Context mContext;
    private OnItemTapListener mOnItemTapListener;

    private boolean mIsShowBannerInner; // 是否显示banner - 这个是列表数据不足3条，没法显示banner的

    /**
     * 更新数据
     *
     * @param newArticleListBeans 新数据
     * @param method              0下拉刷新 1上拉加载更多
     */
    public void updateData(List<ArticleListBean> isGoodArticleBeans, List<ArticleListBean> newArticleListBeans, int method) {

        String minId = "0";
        if (mArticleListBeans.size() > 0) {
            minId = mArticleListBeans.get(mArticleListBeans.size() - 1).getId();
        }

        if (method == 0) { // 下拉刷新 - 每次都刷新 为了刷新列表的浏览量、评论数据

            // 替换数据
            mArticleListBeans.clear();
            mIsGoodArticleBeans.clear();
            mArticleListBeans.addAll(newArticleListBeans);

            // 幻灯片数据 - 如果没有标题图片就不显示
            for (int i = 0; i < isGoodArticleBeans.size(); i++) {
                ArticleListBean articleListBean = isGoodArticleBeans.get(i);
                if (!TextUtils.isEmpty(articleListBean.getTitlepic())) {
                    mIsGoodArticleBeans.add(articleListBean);
                }
            }
            mIsShowBannerInner = mIsGoodArticleBeans.size() > 0;

            // 刷新列表数据
            notifyDataSetChanged();

        } else { // 上拉加载

            for (ArticleListBean bean :
                    newArticleListBeans) {
                if (bean.getId().equals(minId)) {
                    ProgressHUD.showInfo(mContext, "没有更多数据了");
                    return;
                }
            }

            // 拼接数据
            mArticleListBeans.addAll(newArticleListBeans);
            // 刷新列表数据
            notifyDataSetChanged();

        }
    }

    /**
     * 设置item点击监听器
     *
     * @param onItemTapListener 监听器
     */
    public void setOnItemTapListener(OnItemTapListener onItemTapListener) {
        this.mOnItemTapListener = onItemTapListener;
    }

    public NewsListRecyclerViewAdapter(Context context) {
        this.mContext = context;
    }

    // 真实的item位置
    private int getRealPosition(int position) {
        if (mIsShowBannerInner) {
            return position - 1;
        }
        return position;
    }

    @Override
    public int getItemCount() {
        if (mIsShowBannerInner) {
            return mArticleListBeans.size() + 1;
        } else {
            return mArticleListBeans.size();
        }
    }

    @Override
    public int getItemViewType(int position) {

        // 头部轮播
        if (position == 0 && mIsShowBannerInner) {
            return NEWS_ITEM_TYPE.HEADER_VIEW.ordinal();
        }

        // 内容
        ArticleListBean bean = mArticleListBeans.get(getRealPosition(position));
        if (TextUtils.isEmpty(bean.getTitlepic()) || bean.getTitlepic().equals("")) {
            // 没有标题图片
            return NEWS_ITEM_TYPE.NO_PIC.ordinal();
        } else if (bean.getFirsttitle().equals("1")) {
            //  只要是头条则显示单张大图
            return NEWS_ITEM_TYPE.BIG_ONE_PIC.ordinal();
        } else if (bean.getMorepic().length == 0) {
            // 没有多图则是单张
            return NEWS_ITEM_TYPE.ONE_PIC.ordinal();
        } else if (bean.getMorepic().length >= 3) {
            // 3张或3张以上则是显示多图列表
            return NEWS_ITEM_TYPE.MORE_PIC.ordinal();
        } else {
            // 这里如果后台数据没有错乱，是不会来的 - 比如1-2张图片
            return NEWS_ITEM_TYPE.ONE_PIC.ordinal();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        RecyclerView.ViewHolder holder;

        // 头部轮播
        if (viewType == NEWS_ITEM_TYPE.HEADER_VIEW.ordinal()) {
            view = LayoutInflater.from(mContext).inflate(R.layout.header_cell_news_list_banner, parent, false);
            return new HeaderViewHolder(view);
        }

        // 内容
        if (viewType == NEWS_ITEM_TYPE.NO_PIC.ordinal()) {
            view = LayoutInflater.from(mContext).inflate(R.layout.cell_news_list_nopic, parent, false);
            holder = new NoPicViewHolder(view);
        } else if (viewType == NEWS_ITEM_TYPE.ONE_PIC.ordinal()) {
            view = LayoutInflater.from(mContext).inflate(R.layout.cell_news_list_onepic, parent, false);
            holder = new OnePicViewHolder(view);
        } else if (viewType == NEWS_ITEM_TYPE.BIG_ONE_PIC.ordinal()) {
            view = LayoutInflater.from(mContext).inflate(R.layout.cell_news_list_bigonepic, parent, false);
            holder = new BigOnePicViewHolder(view);
        } else {
            view = LayoutInflater.from(mContext).inflate(R.layout.cell_news_list_morepic, parent, false);
            holder = new MorePicViewHolder(view);
        }

        // cell点击事件
        final BaseViewHolder baseViewHolder = (BaseViewHolder) holder;
        baseViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getRealPosition(baseViewHolder.getAdapterPosition());
                if (mOnItemTapListener != null) {
                    mOnItemTapListener.onItemTapListener(mArticleListBeans.get(position));
                }
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        // 头部轮播
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            setupRecyclerViewHeader(headerViewHolder.banner);
            return;
        }

        // 内容区域
        ArticleListBean bean = mArticleListBeans.get(getRealPosition(position));
        BaseViewHolder viewHolder = (BaseViewHolder) holder;
        viewHolder.titleTextView.setText(bean.getTitle());
        viewHolder.timeTextView.setText(DateUtils.getStringTime(bean.getNewstime()));
        viewHolder.befromTextView.setText(bean.getBefrom());
        viewHolder.onclickTextView.setText(bean.getOnclick());

        if (holder instanceof OnePicViewHolder) {

            OnePicViewHolder onePicViewHolder = (OnePicViewHolder) holder;
            onePicViewHolder.imageView1.setImageURI(Uri.parse(bean.getTitlepic()));

        }  else if (holder instanceof BigOnePicViewHolder) {

            BigOnePicViewHolder onePicViewHolder = (BigOnePicViewHolder) holder;
            onePicViewHolder.imageView1.setImageURI(Uri.parse(bean.getTitlepic()));

            // 超过了3张图，就显示图片数量
            if (bean.getMorepic() != null && bean.getMorepic().length > 3) {
                onePicViewHolder.morepicIconView.setVisibility(View.VISIBLE);
                onePicViewHolder.morepicTextView.setText(bean.getMorepic().length + "图");
            } else {
                onePicViewHolder.morepicIconView.setVisibility(View.INVISIBLE);
            }

        } else if (holder instanceof MorePicViewHolder) {

            MorePicViewHolder morePicViewHolder = (MorePicViewHolder) holder;
            morePicViewHolder.imageView1.setImageURI(bean.getMorepic()[0]);
            morePicViewHolder.imageView2.setImageURI(bean.getMorepic()[1]);
            morePicViewHolder.imageView3.setImageURI(bean.getMorepic()[2]);

            // 超过了3张图，就显示图片数量
            if (bean.getMorepic().length > 3) {
                morePicViewHolder.morepicIconView.setVisibility(View.VISIBLE);
                morePicViewHolder.morepicTextView.setText(bean.getMorepic().length + "图");
            } else {
                morePicViewHolder.morepicIconView.setVisibility(View.INVISIBLE);
            }

        }

        // 广告数据则显示广告标识图标
        if (bean.getClassid().equals(AdManager.shared().classid)) {
            viewHolder.adIconImageView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.adIconImageView.setVisibility(View.GONE);
        }

    }

    // viewHolder基类
    class BaseViewHolder extends RecyclerView.ViewHolder {

        View itemView;
        TextView titleTextView;   // 标题
        TextView timeTextView;    // 时间
        TextView befromTextView;  // 文章来源
        TextView onclickTextView; // 点击数量
        ImageView adIconImageView;// 广告图标

        BaseViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            titleTextView = (TextView) itemView.findViewById(R.id.tv_cell_news_list_title);
            timeTextView = (TextView) itemView.findViewById(R.id.tv_cell_news_list_time);
            befromTextView = (TextView) itemView.findViewById(R.id.tv_cell_news_list_befrom);
            onclickTextView = (TextView) itemView.findViewById(R.id.tv_cell_news_list_onclick);
            adIconImageView = (ImageView) itemView.findViewById(R.id.iv_cell_news_list_ad);
        }
    }

    // 头部视图
    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        Banner banner; // 轮播器

        HeaderViewHolder(View itemView) {
            super(itemView);
            banner = (Banner) itemView.findViewById(R.id.b_news_list_banner);
        }
    }

    // 无图
    private class NoPicViewHolder extends BaseViewHolder {

        NoPicViewHolder(View itemView) {
            super(itemView);
        }
    }

    // 单图
    private class OnePicViewHolder extends BaseViewHolder {

        SimpleDraweeView imageView1;

        OnePicViewHolder(View itemView) {
            super(itemView);
            imageView1 = (SimpleDraweeView) itemView.findViewById(R.id.iv_cell_news_list_pic1);
        }
    }

    // 大单图
    private class BigOnePicViewHolder extends BaseViewHolder {

        SimpleDraweeView imageView1;
        View morepicIconView;
        TextView morepicTextView;

        BigOnePicViewHolder(View itemView) {
            super(itemView);
            imageView1 = (SimpleDraweeView) itemView.findViewById(R.id.iv_cell_news_list_pic1);
            morepicIconView = itemView.findViewById(R.id.ll_cell_news_list_morepic_icon);
            morepicTextView = (TextView) itemView.findViewById(R.id.tv_cell_news_list_morepic_text);
        }
    }

    // 多图
    private class MorePicViewHolder extends BaseViewHolder {

        SimpleDraweeView imageView1;
        SimpleDraweeView imageView2;
        SimpleDraweeView imageView3;
        View morepicIconView;
        TextView morepicTextView;

        MorePicViewHolder(View itemView) {
            super(itemView);
            imageView1 = (SimpleDraweeView) itemView.findViewById(R.id.iv_cell_news_list_pic1);
            imageView2 = (SimpleDraweeView) itemView.findViewById(R.id.iv_cell_news_list_pic2);
            imageView3 = (SimpleDraweeView) itemView.findViewById(R.id.iv_cell_news_list_pic3);
            morepicIconView = itemView.findViewById(R.id.ll_cell_news_list_morepic_icon);
            morepicTextView = (TextView) itemView.findViewById(R.id.tv_cell_news_list_morepic_text);
        }
    }

    /**
     * 配置recyclerView头部轮播
     */
    private void setupRecyclerViewHeader(Banner banner) {

        List<String> images = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        for (ArticleListBean bean :
                mIsGoodArticleBeans) {
            images.add(bean.getTitlepic());
            titles.add(bean.getTitle());
        }

        banner.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (SizeUtils.getScreenHeightPx(mContext) * 0.3)));

        // 配置banner
        banner.setBannerStyle(BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE)
                .setImageLoader(new FrescoImageLoader())
                .setImages(images)
                .setBannerTitles(titles)
                .isAutoPlay(true)
                .setDelayTime(5000)
                .setBannerAnimation(Transformer.Default)
                .setIndicatorGravity(BannerConfig.RIGHT)
                .start();

        // 监听banner点击事件
        banner.setOnBannerClickListener(new OnBannerClickListener() {
            // position 从1开始
            @Override
            public void OnBannerClick(int position) {
                if (mOnItemTapListener != null) {
                    mOnItemTapListener.onItemTapListener(mIsGoodArticleBeans.get(position - 1));
                }
            }
        });

    }

    // 轮播图片加载器
    private class FrescoImageLoader extends ImageLoader {

        @Override
        public void displayImage(Context context, Object path, ImageView imageView) {
            imageView.setImageURI(Uri.parse((String) path));
        }

        @Override
        public ImageView createImageView(Context context) {
            return new SimpleDraweeView(context);
        }
    }

}
