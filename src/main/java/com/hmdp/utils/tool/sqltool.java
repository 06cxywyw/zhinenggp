package com.hmdp.utils.tool;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.VoucherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL 工具类 - 为 AI 客服提供数据库查询能力
 * 所有公共方法都会自动暴露为 AI Function Calling 接口
 */
@Component
public class sqltool {

    @Autowired(required = false)
    private ShopMapper shopMapper;

    @Autowired(required = false)
    private VoucherMapper voucherMapper;

    /**
     * 查询商户列表
     */
    @Description("根据关键词查询智能购票平台中的商户信息，返回商户名称、地址、评分等。" +
            "当用户询问商户、店铺、餐厅、咖啡厅等时使用此函数。")
    public String queryShops(String keyword) {
        try {
            if (keyword == null || keyword.isEmpty()) {
                return "搜索关键词不能为空，请提供商户名称或类型";
            }

            List<Shop> shops = shopMapper.selectList(new QueryWrapper<Shop>()
                    .like("name", keyword)
                    .or()
                    .like("type", keyword)
                    .last("limit 10"));

            if (shops.isEmpty()) {
                return "未找到与「" + keyword + "」相关的商户";
            }

            return shops.stream()
                    .map(shop -> String.format("【%s】 %s (⭐ %.1f分)",
                            shop.getName(),
                            shop.getAddress() != null ? shop.getAddress() : "位置未知",
                            shop.getScore() != null ? shop.getScore() : 0))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "查询商户失败";
        }
    }

    /**
     * 查询优惠券
     */
    @Description("查询智能购票平台中的优惠券信息，返回优惠券标题、折扣金额、库存等。" +
            "当用户询问优惠券、折扣、活动、促销等信息时使用此函数。")
    public String queryVouchers(String keyword) {
        try {
            if (keyword == null || keyword.isEmpty()) {
                keyword = "优惠";
            }

            List<Voucher> vouchers = voucherMapper.selectList(new QueryWrapper<Voucher>()
                    .like("title", keyword)
                    .last("limit 10"));

            if (vouchers.isEmpty()) {
                return "暂无相关优惠券";
            }

            return vouchers.stream()
                    .map(v -> String.format("%s → %d元 (库存%d份)",
                            v.getTitle(),
                            v.getActualValue(),
                            v.getStock() != null ? v.getStock() : 0))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "查询优惠券失败";
        }
    }

    /**
     * 获取热门商户推荐
     */
    @Description("获取智能购票平台中评分最高的热门商户推荐。" +
            "当用户要求推荐商户、获取热门商户等时使用此函数。")
    public String queryPopularShops() {
        try {
            List<Shop> shops = shopMapper.selectList(new QueryWrapper<Shop>()
                    .orderByDesc("score")
                    .last("limit 5"));

            if (shops.isEmpty()) {
                return "暂无商户数据";
            }

            return shops.stream()
                    .map(shop -> String.format("• %s (⭐ %.1f分)",
                            shop.getName(),
                            shop.getScore() != null ? shop.getScore() : 0))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "获取推荐商户失败";
        }
    }

    /**
     * 检查是否需要查询数据库
     * 根据用户问题的关键词判断
     * @param question 用户问题
     * @return true 需要查询数据库，false 不需要
     */
    public static boolean shouldQueryDatabase(String question) {
        if (question == null || question.isEmpty()) {
            return false;
        }

        String q = question.toLowerCase();

        // 触发数据库查询的关键词
        String[] triggerWords = {
                "商户", "店铺", "餐厅", "咖啡", "奶茶", "火锅",
                "优惠", "券", "折扣", "活动", "促销",
                "推荐", "热门", "怎么样", "评价", "好", "有"
        };

        for (String word : triggerWords) {
            if (q.contains(word)) {
                return true;
            }
        }

        return false;
    }
}
