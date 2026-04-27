package com.hmdp.config;

import com.hmdp.utils.tool.sqltool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ChatClientConfiguration {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Bean
    public ChatMemory chatMemory() {
        return new RedisChatMemory(redisTemplate, 100, 24); // 最大100条，过期24小时
    }

    @Bean(name = "chatClient")
    public ChatClient chatClient(OpenAiChatModel model, ChatMemory chatMemory, sqltool sqlTool) {
        System.out.println("初始化chatClient bean");
        return ChatClient.builder(model)
                .defaultSystem("你是智能购票平台的AI客服助手。请以简洁、友好的语气回答用户的问题。" +
                        "【重要】回答必须简短，控制在50-100字以内，最多2-3句话。" +
                        "你可以帮助用户咨询商户、优惠、订单、页面使用等问题。" +
                        "当用户询问商户、优惠券、推荐等信息时，请主动调用可用工具获取最新数据。")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

}
