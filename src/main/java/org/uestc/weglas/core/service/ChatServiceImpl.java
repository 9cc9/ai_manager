package org.uestc.weglas.core.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
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
public class ChatServiceImpl implements ChatService {

    public static final String AI_INVOKE_URL = "http://localhost:8080/chat";

    private final WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080").build(); ;


    /**
     * @param conversation 历史会话
     * @param currentChat
     * @return
     */
    @Override
    public ConversationChatDetail chat(Conversation conversation, ConversationChatDetail currentChat) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = new RestTemplate().postForEntity(AI_INVOKE_URL,
                buildHttpRequest(conversation, currentChat, headers), String.class);

        return ConversationChatBuilder.buildAssistantChat(conversation, parseResponse(response));
    }

    @Override
    public Flux<String> streamChat(Conversation conversation, ConversationChatDetail chat) {
        Map<String, Object> payload = buildPayload(conversation, chat);

        // 发送 POST 请求并处理流式响应
        return this.webClient.post()
                .uri("/chat")
                .body(Mono.just(payload), Map.class)
                .retrieve()
                .bodyToFlux(String.class)  // 使用 Flux 处理流式响应
                .doOnNext(responseChunk -> {
                    System.out.println("Received chunk: " + responseChunk);
                    // 可以在这里处理每一块流式响应，例如显示在 UI 上
                });
    }

    private HttpEntity<Map<String, Object>> buildHttpRequest(Conversation conversation, ConversationChatDetail currentChat, HttpHeaders headers) {
        Map<String, Object> requestPayload = buildPayload(conversation, currentChat);

        return new HttpEntity<>(requestPayload, headers);
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
            String response = URLDecoder.decode(map.getOrDefault("response", ""), StandardCharsets.UTF_8.name());
            AssertUtil.notBlank(response);
            return response;
        } catch (UnsupportedEncodingException e) {
            throw new ManagerBizException(ResultEnum.INVOKE_FAIL);
        }
    }
}
