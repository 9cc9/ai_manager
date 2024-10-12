package org.uestc.weglas.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.uestc.weglas.core.model.Conversation;
import org.uestc.weglas.core.model.ConversationChatDetail;
import org.uestc.weglas.core.service.ConversationService;
import org.uestc.weglas.util.BaseResult;

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

        return BaseResult.success(conversation);
    }

    @PostMapping("/addChat.json")
    public BaseResult<ConversationChatDetail> addChat(@RequestBody ConversationChatDetail chat) {

        conversationService.addChat(chat);
        return BaseResult.success(chat);
    }

}
