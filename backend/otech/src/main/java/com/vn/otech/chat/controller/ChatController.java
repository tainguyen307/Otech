package com.vn.otech.chat.controller;

import com.vn.otech.chat.dto.ConversationCreateRequest;
import com.vn.otech.chat.dto.ConversationResponse;
import com.vn.otech.chat.dto.MessagePageResponse;
import com.vn.otech.chat.dto.MessageResponse;
import com.vn.otech.chat.dto.MessageSendRequest;
import com.vn.otech.chat.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
@RequestMapping("/api/conversations")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ConversationCreateRequest request) {
        return chatService.createConversation(principal.getUsername(), request);
    }

    @GetMapping
    public List<ConversationResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return chatService.listConversations(principal.getUsername());
    }

    @GetMapping("/{conversationId}/messages")
    public MessagePageResponse messages(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return chatService.listMessages(principal.getUsername(), conversationId, cursor, limit);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody MessageSendRequest request) {
        return chatService.sendMessage(principal.getUsername(), conversationId, request);
    }

    @PostMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID conversationId) {
        chatService.markRead(principal.getUsername(), conversationId);
    }

    @MessageMapping("/conversations/{conversationId}/messages")
    public void websocketSend(
            @DestinationVariable UUID conversationId,
            @Valid MessageSendRequest request,
            Principal principal) {
        chatService.sendMessage(principal.getName(), conversationId, request);
    }

    @MessageMapping("/conversations/{conversationId}/read")
    public void websocketMarkRead(@DestinationVariable UUID conversationId, Principal principal) {
        chatService.markRead(principal.getName(), conversationId);
    }

    @MessageExceptionHandler({ResponseStatusException.class, AccessDeniedException.class})
    public void websocketError(Exception exception, Principal principal) {
        if (principal == null) {
            return;
        }
        HttpStatus status = exception instanceof ResponseStatusException responseStatusException
                ? HttpStatus.valueOf(responseStatusException.getStatusCode().value())
                : HttpStatus.FORBIDDEN;
        String message = exception instanceof ResponseStatusException responseStatusException
                ? responseStatusException.getReason()
                : "The chat request is not allowed";
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors",
                new ChatError(status.value(), message == null ? "The chat request failed" : message));
    }

    public record ChatError(int status, String message) {
    }
}
