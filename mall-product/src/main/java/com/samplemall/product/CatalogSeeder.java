package com.samplemall.product;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 确定性商品目录（PRD 附录 A）：名称 / 价格 / 库存均为固定值，自动化可直接断言。
 * 覆盖四类库存状态：常规 / 低库存 / 限时特惠（划线原价）/ 售罄。内存库每次启动重建。
 */
@Component
class CatalogSeeder implements ApplicationRunner {

    private final CategoryRepository categories;
    private final ProductRepository products;

    CatalogSeeder(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (products.count() > 0) {
            return;
        }
        categories.saveAll(List.of(
                new Category("green", "绿茶", 1),
                new Category("oolong", "乌龙茶", 2),
                new Category("black", "红茶", 3),
                new Category("white", "白茶", 4),
                new Category("dark", "黑茶", 5)));

        products.saveAll(List.of(
                new Product("green-mist", "云雾绿茶", "green", "#7bb77a", price(128), null, 42,
                        "浙江 · 杭州", "明前嫩芽，一杯春天的味道",
                        "选用清明前采摘的单芽嫩叶，经传统手工炒制，茶汤清澈明亮，滋味鲜爽回甘。冷泡热泡皆宜，是日常口粮茶的首选。",
                        1, specs("250g", "18 个月", "80-85℃")),
                new Product("peach-oolong", "蜜桃乌龙", "oolong", "#f2a6a0", price(89), null, 8,
                        "福建 · 安溪", "果香与茶香的清爽碰撞",
                        "高山乌龙茶坯融合天然蜜桃果干，香气馥郁不腻，茶感清爽顺滑。冰镇后风味更佳，夏日冷泡推荐榜第一名。",
                        2, specs("200g", "12 个月", "95-100℃")),
                new Product("golden-junmee", "金骏眉红茶", "black", "#c98a4b", price(258), price(328), 15,
                        "福建 · 武夷山", "限时特惠，薯蜜香经典红茶",
                        "武夷山国家级自然保护区原料，传统正山小种工艺精制。干茶金毫显露，汤色金黄透亮，入口甘甜润滑，带天然薯蜜香。",
                        3, specs("150g", "24 个月", "95-100℃")),
                new Product("jasmine-white", "茉莉白茶", "white", "#e8e0c9", price(168), null, 0,
                        "福建 · 福鼎", "七窨茉莉，本季售罄",
                        "福鼎白茶为坯，经七次茉莉鲜花窨制，花香入骨、茶味清甜。本季批次已全部售罄，下季预售请留意首页公告。",
                        4, specs("180g", "18 个月", "85-90℃")),
                new Product("aged-puer", "陈年普洱", "dark", "#6b4f3a", price(388), null, 23,
                        "云南 · 勐海", "十年陈化，越陈越香",
                        "2015 年勐海春料，干仓自然陈化十年。汤色红浓如琥珀，陈香纯正，滋味醇厚饱满，叶底柔韧有活性。",
                        5, specs("357g 饼", "长期保存", "100℃")),
                new Product("osmanthus-longjing", "桂花龙井", "green", "#d4b23c", price(108), null, 36,
                        "浙江 · 杭州", "秋桂配春茶，限时复刻",
                        "明前龙井与满觉陇金桂按 10:1 比例拼配，桂花香融进豆香里，甜润不抢味。每年秋季限定批次。",
                        6, specs("150g", "12 个月", "80-85℃"))));
    }

    private static BigDecimal price(int yuan) {
        return BigDecimal.valueOf(yuan).setScale(2);
    }

    private static List<Product.Spec> specs(String weight, String shelfLife, String temperature) {
        return List.of(
                new Product.Spec("净含量", weight),
                new Product.Spec("保质期", shelfLife),
                new Product.Spec("冲泡水温", temperature));
    }
}
