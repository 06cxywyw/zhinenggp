package com.hmdp.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

/**
 * 基于 Redis 的 ChatMemory 实现（可直接使用版本）
 */
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate redisTemplate;
    private final int maxHistory;
    private final long expireHours;

    public RedisChatMemory(StringRedisTemplate redisTemplate,
                           int maxHistory,
                           long expireHours) {
        this.redisTemplate = redisTemplate;
        this.maxHistory = maxHistory;
        this.expireHours = expireHours;
    }

    /**
     * SpringAI 会调用这个方法保存对话
     */
    @Override
    public void add(String conversationId, List<Message> messages) {

        String redisKey = CHAT_MEMORY_CONVERSATION_ID_KEY + conversationId;

        for (Message message : messages) {
            // 判断角色
            String role;

            if (message instanceof UserMessage) {
                role = "USER";
            } else if (message instanceof AssistantMessage) {
                role = "ASSISTANT";
            } else {
                role = "SYSTEM";
            }

            String value = role + ": " + message.getText();
            redisTemplate.opsForList().rightPush(redisKey, value);
        }

        // 只保留最近 maxHistory 条
        redisTemplate.opsForList().trim(redisKey, -maxHistory, -1);

        // 设置过期时间
        redisTemplate.expire(redisKey, expireHours, TimeUnit.HOURS);
    }

    /**
     * SpringAI 会调用这个方法读取历史
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {

        String redisKey = CHAT_MEMORY_CONVERSATION_ID_KEY + conversationId;

        List<String> history = redisTemplate.opsForList()
                .range(redisKey, -lastN, -1);

        List<Message> result = new ArrayList<>();

        if (history != null) {
            for (String record : history) {

                if (record.startsWith("USER: ")) {
                    result.add(new UserMessage(record.substring(6)));
                } else if (record.startsWith("ASSISTANT: ")) {
                    result.add(new AssistantMessage(record.substring(11)));
                } else {
                    result.add(new SystemMessage(record));
                }
            }
        }

        return result;
    }

    /**
     * 清空会话
     */
    @Override
    public void clear(String conversationId) {
        String redisKey = CHAT_MEMORY_CONVERSATION_ID_KEY + conversationId;
        redisTemplate.delete(redisKey);
    }
}