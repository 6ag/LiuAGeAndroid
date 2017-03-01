package tv.baokan.liuageandroid.model;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;

public class ArticleListBean implements Serializable {

    // 文章分类id
    private String classid;

    // 分类名称
    private String classname;

    // 文章id
    private String id;

    // 文章标题
    private String title;

    // 文章来源
    private String befrom;

    // 点击量
    private String onclick;

    // 推荐 - 1为一级推荐，0为不推荐   幻灯片数据
    private String isgood;

    // 置顶 - 置顶级别
    private String istop;

    // 头条 - 1为一级头条，0为普通信息 - 大图
    private String firsttitle;

    // 外部链接 - 1为外部链接，0为普通信息
    private String isurl;

    // 关键词
    private String keyboard;

    // 外链
    private String titleurl;

    // 评论数
    private String plnum;

    // 创建文章的时间戳
    private String newstime;

    // 标题图片
    private String titlepic;

    // 标题多图
    private String[] morepic;

    public ArticleListBean(JSONObject article) {
        try {
            classid = article.getString("classid");
            if (article.has("classname")) {
                classname = article.getString("classname");
            }
            id = article.getString("id");
            title = article.getString("title");
            befrom = article.getString("befrom");
            onclick = article.getString("onclick");
            isgood = article.getString("isgood");
            istop = article.getString("istop");
            firsttitle = article.getString("firsttitle");
            isurl = article.getString("isurl");
            keyboard = article.getString("keyboard");
            if (article.has("titleurl")) {
                titleurl = article.getString("titleurl");
            }
            plnum = article.getString("plnum");
            newstime = article.getString("newstime");
            titlepic = article.getString("titlepic");
            JSONArray morepicArray = article.getJSONArray("morepic");
            morepic = new String[morepicArray.length()];
            for (int j = 0; j < morepicArray.length(); j++) {
                morepic[j] = morepicArray.get(j).toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getClassid() {
        return classid;
    }

    public String getClassname() {
        return classname;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBefrom() {
        return befrom;
    }

    public String getOnclick() {
        return onclick;
    }

    public String getIsgood() {
        return isgood;
    }

    public void setIsgood(String isgood) {
        this.isgood = isgood;
    }

    public String getIstop() {
        return istop;
    }

    public void setIstop(String istop) {
        this.istop = istop;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getFirsttitle() {
        return firsttitle;
    }

    public void setFirsttitle(String firsttitle) {
        this.firsttitle = firsttitle;
    }

    public String getIsurl() {
        return isurl;
    }

    public void setIsurl(String isurl) {
        this.isurl = isurl;
    }

    public String getKeyboard() {
        return keyboard;
    }

    public void setKeyboard(String keyboard) {
        this.keyboard = keyboard;
    }

    public String getTitleurl() {
        return titleurl;
    }

    public void setTitleurl(String titleurl) {
        this.titleurl = titleurl;
    }

    public String getPlnum() {
        return plnum;
    }

    public String getNewstime() {
        return newstime;
    }

    public String getTitlepic() {
        return titlepic;
    }

    public String[] getMorepic() {
        return morepic;
    }

    @Override
    public String toString() {
        return "ArticleListBean{" +
                "classid='" + classid + '\'' +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", befrom='" + befrom + '\'' +
                ", onclick='" + onclick + '\'' +
                ", isgood='" + isgood + '\'' +
                ", istop='" + istop + '\'' +
                ", firsttitle='" + firsttitle + '\'' +
                ", isurl='" + isurl + '\'' +
                ", keyboard='" + keyboard + '\'' +
                ", titleurl='" + titleurl + '\'' +
                ", plnum='" + plnum + '\'' +
                ", newstime='" + newstime + '\'' +
                ", titlepic='" + titlepic + '\'' +
                ", morepic=" + Arrays.toString(morepic) +
                '}';
    }
}
