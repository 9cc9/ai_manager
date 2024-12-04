package org.uestc.weglas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.uestc.weglas.core.builder.ConversationChatBuilder;
import org.uestc.weglas.core.model.Conversation;
import org.uestc.weglas.core.model.ConversationChatDetail;
import org.uestc.weglas.core.service.ChatService;
import org.uestc.weglas.core.service.ConversationService;
import org.uestc.weglas.util.BaseResult;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yingxian.cyx
 * @date Created in 2024/10/11
 */
@RestController
@RequestMapping("/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ChatService chatService;

    @GetMapping("/list.json")
    public BaseResult<Conversation> queryAll(Model model) {

        // 调用服务层查询条件
        List<Conversation> conversations = conversationService.queryAll();
        return BaseResult.success(conversations);
    }

    // 会话详情
    @GetMapping("/detail.json")
    public BaseResult<Conversation> queryConversation(Model model, Integer conversationId) {
        Conversation conversation = conversationService.queryById(conversationId);
        return BaseResult.success(conversation);
    }

    @PostMapping("/add.json")
    public BaseResult<Conversation> addConversation(@RequestBody Conversation conversation) {

        conversationService.add(conversation);

        // 创建一条默认的chat
        ConversationChatDetail userChat = ConversationChatBuilder.buildDefaultChat(conversation);
        doChat(userChat);

        return BaseResult.success(conversation);
    }


    @PostMapping("/addChat.json")
    public BaseResult<ConversationChatDetail> addChat(@RequestBody ConversationChatDetail chat) {

        List<ConversationChatDetail> chats = doChat(chat);
        return BaseResult.success(chats);
    }

    @PostMapping("/streamChat.json")
    public Flux<String> streamChat(@RequestBody ConversationChatDetail chat) {
        Conversation conversation = conversationService.queryById(chat.getConversationId());

        // 写入当前聊天内容到db
        conversationService.addChat(chat);

        // 调用服务，获取流式响应
        return chatService.streamChat(conversation, chat);
    }


    private List<ConversationChatDetail> doChat(ConversationChatDetail chat) {
        List<ConversationChatDetail> chats = new ArrayList<>();
        Conversation conversation = conversationService.queryById(chat.getConversationId());
        // TODO 增加完结状态检测
        ConversationChatDetail responseChat = chatService.chat(conversation, chat);

        conversationService.addChat(chat);
        conversationService.addChat(responseChat);
        chats.add(chat);
        chats.add(responseChat);
        return chats;
    }

}
