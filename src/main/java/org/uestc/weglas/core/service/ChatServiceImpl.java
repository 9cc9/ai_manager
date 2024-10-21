package org.uestc.weglas.core.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.uestc.weglas.core.builder.ConversationChatBuilder;
import org.uestc.weglas.core.enums.ResultEnum;
import org.uestc.weglas.core.model.Conversation;
import org.uestc.weglas.core.model.ConversationChatDetail;
import org.uestc.weglas.util.exception.AssertUtil;
import org.uestc.weglas.util.exception.ManagerBizException;
import org.uestc.weglas.util.log.LogUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yingxian.cyx
 * @date Created in 2024/10/12
 */
@Service
@Slf4j
@Configuration
public class ChatServiceImpl implements ChatService {

    private final Logger logger = LogManager.getLogger(ChatServiceImpl.class);
    private final WebClient webClient = WebClient.builder().build();

    @Value("${spring.ai.chat-url}")
    private String aiChatURL;

    @Value("${spring.ai.stream-chat-url}")
    private String aiStreamChatURL;

    @Autowired
    private ConversationService conversationService;

    /**
     * @param conversation 历史会话
     * @param currentChat
     * @return
     */
    @Override
    public ConversationChatDetail chat(Conversation conversation, ConversationChatDetail currentChat) {

        ResponseEntity<String> response = new RestTemplate().postForEntity(aiChatURL,
                buildRequest(conversation, currentChat), String.class);

        return ConversationChatBuilder.buildAssistantChat(conversation, parseResponse(response));
    }

    /**
     * 流式读取
     *
     * @param conversation 会话
     * @param userChat     用户输入chat
     * @return 流式返回文本
     */
    @Override
    public Flux<String> streamChat(Conversation conversation, ConversationChatDetail userChat) {

        Map<String, Object> payload = buildPayload(conversation, userChat);
        StringBuilder completeResponse = new StringBuilder();

        return this.webClient.post()
                .uri(aiStreamChatURL)
                .body(Mono.just(payload), Map.class)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(responseChunk -> {
                    onNext(responseChunk, completeResponse);
                })
                .doOnComplete(() -> {
                    onComplete(conversation, completeResponse);
                });
    }

    private void onComplete(Conversation conversation, StringBuilder completeResponse) {
        // 流式响应完成时，保存完整的响应到数据库
        String finalResponse = completeResponse.toString();

        LogUtil.info(logger, "Complete response: " + finalResponse);
        ConversationChatDetail assistantChat = ConversationChatBuilder.buildAssistantChat(conversation, finalResponse);

        // 写入助手生成的chat
        conversationService.addChat(assistantChat);
    }

    private void onNext(String responseChunk, StringBuilder completeResponse) {
        completeResponse.append(responseChunk);
        LogUtil.info(logger, "Received chunk: " + responseChunk);
    }

    private HttpEntity<Map<String, Object>> buildRequest(Conversation conversation, ConversationChatDetail currentChat) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(buildPayload(conversation, currentChat), headers);
    }

    private Map<String, Object> buildPayload(Conversation conversation, ConversationChatDetail currentChat) {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("message", currentChat.getContent());
        requestPayload.put("historyMessages", conversation.getChatList());
        return requestPayload;
    }

    private String parseResponse(ResponseEntity<String> entity) {
        try {
            String json = entity.getBody();
            Map<String, String> map = JSON.parseObject(json, new TypeReference<Map<String, String>>() {
            });
            AssertUtil.notNull(map);
            String response = URLDecoder.decode(map.getOrDefault("response", ""), StandardCharsets.UTF_8.name());
            AssertUtil.notBlank(response);
            return response;
        } catch (UnsupportedEncodingException e) {
            throw new ManagerBizException(ResultEnum.INVOKE_FAIL);
        }
    }
}
