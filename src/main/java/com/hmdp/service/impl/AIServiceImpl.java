package com.hmdp.service.impl;

import com.hmdp.service.AIService;
import com.hmdp.utils.tool.sqltool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
public class AIServiceImpl implements AIService {

    @Autowired(required = false)
    private ChatClient chatClient;

    @Autowired(required = false)
    private sqltool sqlTool;

    @Override
    public Flux<String> chat(String message, String sessionId) {
        if (chatClient == null) {
            System.err.println("警告: chatClient bean未初始化，AI模型可能未正确配置");
            return Flux.just("AI服务暂不可用，请检查服务器配置");
        }
        
        try {
            System.out.println("发送消息到AI: " + message + ", sessionId: " + sessionId);
            
            // 根据用户问题自动查询数据库，增强消息
            String enhancedMessage = message;
            if (sqlTool != null && sqltool.shouldQueryDatabase(message)) {
                String dbData = queryDatabase(message);
                if (dbData != null && !dbData.isEmpty()) {
                    enhancedMessage = message + "\n\n【查询结果】\n" + dbData;
                    System.out.println("已使用工具增强消息");
                }
            }
            
            return chatClient.prompt()
                    .user(enhancedMessage)
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                    .stream()
                    .content()
                    .doOnError(e -> {
                        System.err.println("AI流异常: " + e.getMessage());
                        e.printStackTrace();
                    });
        } catch (Exception e) {
            System.err.println("调用AI异常: " + e.getMessage());
            e.printStackTrace();
            return Flux.just("AI处理请求出错: " + e.getMessage());
        }
    }

    /**
     * 根据用户消息自动查询数据库
     */
    private String queryDatabase(String message) {
        if (sqlTool == null) {
            return null;
        }

        try {
            String m = message.toLowerCase();
            
            // 优惠券相关
            if (m.contains("优惠") || m.contains("券") || m.contains("折扣") || m.contains("活动")) {
                return sqlTool.queryVouchers("优惠");
            }
            
            // 商户相关 - 热门推荐
            if (m.contains("推荐") || m.contains("热门") || m.contains("排名")) {
                return sqlTool.queryPopularShops();
            }
            
            // 商户搜索
            if (m.contains("商户") || m.contains("店") || m.contains("餐厅") || 
                m.contains("咖啡") || m.contains("奶茶") || m.contains("超市")) {
                // 尝试从消息中提取关键词
                String keyword = extractKeyword(message);
                if (keyword != null && !keyword.isEmpty()) {
                    return sqlTool.queryShops(keyword);
                } else {
                    return sqlTool.queryPopularShops();
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("数据库查询异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从用户消息中提取关键词
     */
    private String extractKeyword(String message) {
        // 移除常见问询词
        String cleaned = message
                .replaceAll("附近|有|什么|好|哪|怎样|怎么|请|帮我|给我|推荐", "")
                .replaceAll("商户|店|餐厅|店铺|地方", "")
                .trim();
        
        // 如果清理后还有内容，作为关键词
        if (!cleaned.isEmpty() && cleaned.length() <= 20) {
            return cleaned;
        }
        
        return null;
    }
}
