package my.project.sakuraproject.main.desc;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import my.project.sakuraproject.R;
import my.project.sakuraproject.bean.AnimeDescDetailsBean;
import my.project.sakuraproject.bean.AnimeDescListBean;
import my.project.sakuraproject.bean.AnimeDescRecommendBean;
import my.project.sakuraproject.bean.AnimeListBean;
import my.project.sakuraproject.database.DatabaseUtil;
import my.project.sakuraproject.main.base.BaseModel;
import my.project.sakuraproject.net.HttpGet;
import my.project.sakuraproject.util.Utils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class DescModel extends BaseModel implements DescContract.Model {
    private String fid;
    private String dramaStr;
    private AnimeDescListBean animeDescListBean = new AnimeDescListBean();

    @Override
    public void getData(String url, DescContract.LoadDataCallback callback) {
        getHtml(url, callback);
    }

    private void getHtml(String url, DescContract.LoadDataCallback callback) {
        new HttpGet(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.error(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    Document doc = Jsoup.parse(response.body().string());
                    if (hasRefresh(doc)) getHtml(url, callback);
                    else {
                        String animeTitle = doc.select("h1").text();
                        //是否收藏
                        callback.isFavorite(DatabaseUtil.checkFavorite(animeTitle));
                        //创建番剧索引
                        DatabaseUtil.addAnime(animeTitle);
                        fid = DatabaseUtil.getAnimeID(animeTitle);
                        dramaStr = DatabaseUtil.queryAllIndex(fid);
                        AnimeListBean bean = new AnimeListBean();
                        //番剧名称
                        bean.setTitle(animeTitle);
                        //番剧简介
                        bean.setDesc(doc.select("div.info").text());
                        bean.setSy(doc.select("div.sinfo > span").get(0).text().replaceAll("上映:", "上映: "));
                        bean.setDq(doc.select("div.sinfo > span").get(1).text().replaceAll("地区:", "地区: "));
                        bean.setLx(doc.select("div.sinfo > span").get(2).text());
                        bean.setBq(doc.select("div.sinfo > span").get(4).text());
                        //番剧图片
                        bean.setImg(doc.select("div.thumb > img").attr("src"));
                        //番剧地址
                        bean.setUrl(url);
                        callback.successDesc(bean);
                        //剧集列表
                        Elements detail = doc.select("div.movurl > ul > li");
                        //多季
                        Elements multi = doc.select("div.img > ul > li");
                        //相关推荐
                        Elements recommend = doc.select("div.pics > ul > li");
                        if (detail.size() > 0) {
                            setPlayData(detail);
                            if (multi.size() > 0) setMulti(multi);
                            if (recommend.size() > 0) setRecommend(recommend);
                            callback.successMain(animeDescListBean);
                        } else {
                            callback.error(Utils.getString(R.string.no_playlist_error));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.error(e.getMessage());
                }
            }
        });
    }

    private void setPlayData(Elements els) {
        List<AnimeDescDetailsBean> animeDescDetailsBeans = new ArrayList<>();
        boolean select;
        for (int i = 0, size = els.size(); i < size; i++) {
            String name = els.get(i).select("a").text();
            String watchUrl = els.get(i).select("a").attr("href");
            if (dramaStr.contains(watchUrl)) select = true;
            else select = false;
            animeDescDetailsBeans.add(new AnimeDescDetailsBean(name, watchUrl, select));
        }
        animeDescListBean.setAnimeDescDetailsBeans(animeDescDetailsBeans);
    }

    private void setMulti(Elements els) {
        List<AnimeDescRecommendBean> animeDescMultiBeans = new ArrayList<>();
        for (int i = 0, size = els.size(); i < size; i++) {
            String title = els.get(i).select("p.tname > a").text();
            String img = els.get(i).select("img").attr("src");
            String url = els.get(i).select("p.tname > a").attr("href");
            animeDescMultiBeans.add(new AnimeDescRecommendBean(title, img, url));
        }
        animeDescListBean.setAnimeDescMultiBeans(animeDescMultiBeans);
    }

    private void setRecommend(Elements els) {
        List<AnimeDescRecommendBean> animeDescRecommendBeans = new ArrayList<>();
        for (int i = 0, size = els.size(); i < size; i++) {
            String title = els.get(i).select("h2 > a").text();
            String img = els.get(i).select("img").attr("src");
            String url = els.get(i).select("h2 > a").attr("href");
            animeDescRecommendBeans.add(new AnimeDescRecommendBean(title, img, url));
        }
        animeDescListBean.setAnimeDescRecommendBeans(animeDescRecommendBeans);
    }
}
