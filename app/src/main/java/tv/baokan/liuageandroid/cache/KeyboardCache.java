package tv.baokan.liuageandroid.cache;

import org.litepal.crud.DataSupport;

/**
 * 搜索关键词缓存
 */
public class KeyboardCache extends DataSupport {

    // 关键词
    private String keyboard;

    // 关键词拼音
    private String pinyin;

    // 出现的次数
    private int num;

    public String getKeyboard() {
        return keyboard;
    }

    public void setKeyboard(String keyboard) {
        this.keyboard = keyboard;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
