package com.hmdp.controller;



import com.hmdp.dto.ChatDTO;
import com.hmdp.dto.Result;
import com.hmdp.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController("AiController")
@RequestMapping("/ai/chat")
public class AiChatController {

    @Autowired
    private AIService aiService;

    @PostMapping("/chat")
    public Result chat(@RequestBody ChatDTO chatDTO) {
        try {
            String message = chatDTO.getMessage();
            String sessionId = chatDTO.getSessionid();
            
            if (message == null || message.trim().isEmpty()) {
                return Result.fail("消息不能为空");
            }
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return Result.fail("会话ID不能为空");
            }
            
            System.out.println("收到请求: sessionId=" + sessionId + ", message=" + message);
            
            // 同步方式获取完整回复
            String answer = aiService.chat(message, sessionId)
                    .collect(java.util.stream.Collectors.joining())
                    .timeout(java.time.Duration.ofSeconds(120))
                    .block();
            
            System.out.println("AI回复长度: " + (answer != null ? answer.length() : 0) + " 字符");
            
            if (answer == null || answer.isEmpty()) {
                return Result.ok("我暂时没有想好回复，请稍后再问");
            }
            
            // 格式化AI回复
            String formattedAnswer = formatCustomerServiceResponse(answer);
            return Result.ok(formattedAnswer);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("AI聊天异常: " + e.getMessage());
            return Result.fail("服务异常，请稍后重试: " + e.getMessage());
        }
    }
    
    /**
     * 格式化AI回复，使其符合客服身份并简化长度
     */
    private String formatCustomerServiceResponse(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }
        
        // 1. 清理多余的空白
        response = response.replaceAll("\\n\\n\\n+", "\n\n");  // 多个空行合并为两个
        response = response.trim();
        
        // 2. 限制长度：如果超过300字，只保留前2-3个句子
        if (response.length() > 300) {
            response = truncateToShortResponse(response);
        }
        
        // 3. 移除不适合客服的措辞
        response = response.replace("我是一个AI助手", "我是智能购票客服");
        response = response.replace("作为AI", "作为您的客服助手");
        
        // 4. 将连续的句号改为单个
        response = response.replace("。。", "。");
        
        // 5. 添加表情符号（仅在特定场景）
        if (response.contains("推荐") || response.contains("建议")) {
            response = response.replace("推荐", "👉 推荐");
        }
        if (response.contains("优惠")) {
            response = response.replace("优惠", "🎉 优惠");
        }
        if (response.contains("感谢")) {
            response = response.replace("感谢", "🙏 感谢");
        }
        
        // 6. 只在短回复中添加结尾问候
        if (!response.contains("还有其他") && !response.contains("还需要帮助") && response.length() < 150) {
            response = response + "\n随时告诉我！😊";
        }
        
        return response;
    }
    
    /**
     * 截断长回复为简短版本（保留前2-3句话）
     */
    private String truncateToShortResponse(String response) {
        // 按句号分割
        String[] sentences = response.split("(?<=[。！？])");
        
        // 取前2-3句
        StringBuilder result = new StringBuilder();
        int charCount = 0;
        int sentenceCount = 0;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            charCount += sentence.length();
            sentenceCount++;
            
            result.append(sentence);
            
            // 达到2-3句或200字就停止
            if (sentenceCount >= 3 || charCount >= 200) {
                break;
            }
        }
        
        String truncated = result.toString().trim();
        
        // 确保以句号结尾
        if (!truncated.endsWith("。") && !truncated.endsWith("！") && !truncated.endsWith("？")) {
            truncated += "。";
        }
        
        return truncated;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatDTO chatDTO) {
        try {
            String message = chatDTO.getMessage();
            String sessionId = chatDTO.getSessionid();
            
            if (message == null || message.trim().isEmpty()) {
                return Flux.just("data: 消息不能为空\n\n");
            }
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return Flux.just("data: 会话ID不能为空\n\n");
            }
            
            System.out.println("收到流式请求: sessionId=" + sessionId + ", message=" + message);
            
            // 返回流式数据，每个字符作为一个事件
            return aiService.chat(message, sessionId)
                    .map(content -> {
                        System.out.print(content);
                        return "data: " + content + "\n\n";
                    })
                    .doOnComplete(() -> System.out.println("\n[流式回复完成]"))
                    .doOnError(e -> {
                        System.err.println("流式处理异常: " + e.getMessage());
                        e.printStackTrace();
                    });
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("流式聊天异常: " + e.getMessage());
            return Flux.just("data: 服务异常\n\n");
        }
    }
}
