package tv.baokan.liuageandroid.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import tv.baokan.liuageandroid.model.ColumnBean;
import tv.baokan.liuageandroid.ui.fragment.BaseFragment;

/**
 * 资讯、图秀里viewPager的适配器
 */
public class TabFragmentPagerAdapter extends FragmentPagerAdapter {

    private List<ColumnBean> mSelectedList = new ArrayList<>();
    private List<BaseFragment> mListFragments = new ArrayList<>();
    private FragmentManager mFm;

    public TabFragmentPagerAdapter(FragmentManager fm, List<? extends BaseFragment> listFragments, List<ColumnBean> selectedList) {
        super(fm);
        mFm = fm;
        mListFragments.addAll(listFragments);
        mSelectedList.addAll(selectedList);
    }

    /**
     * 重新加载数据
     *
     * @param newListFragments 新的fragment集合
     * @param newSelectedList  新的选中分类集合
     */
    public void reloadData(List<? extends BaseFragment> newListFragments, List<ColumnBean> newSelectedList) {

        // 每次刷新顺序都需要清除缓存
        if (mListFragments.size() > 0) {
            FragmentTransaction ft = mFm.beginTransaction();
            for (Fragment f :
                    mListFragments) {
                ft.remove(f);
            }
            ft.commitAllowingStateLoss();
            mFm.executePendingTransactions();
        }

        // 清除原有数据源
        mListFragments.clear();
        mSelectedList.clear();

        // 重新添加数据源
        mListFragments.addAll(newListFragments);
        mSelectedList.addAll(newSelectedList);

        // 刷新数据
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        return super.instantiateItem(container, position);
    }

    @Override
    public Fragment getItem(int position) {
        return mListFragments.get(position);
    }

    @Override
    public int getCount() {
        return mListFragments.size();
    }

//    @Override
//    public void destroyItem(ViewGroup container, int position, Object object) {
        // 重写父类销毁方法，就切换viewPager上的列表就不会重复去加载数据，但是会增加内存占用
//        container.removeView(mListFragments.get(position).getView());
//    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mSelectedList.get(position).getClassname();
    }

}
