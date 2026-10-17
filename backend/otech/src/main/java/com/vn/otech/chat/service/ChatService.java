package com.vn.otech.chat.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.chat.dto.ConversationCreateRequest;
import com.vn.otech.chat.dto.ConversationResponse;
import com.vn.otech.chat.dto.MessagePageResponse;
import com.vn.otech.chat.dto.MessageResponse;
import com.vn.otech.chat.dto.MessageSendRequest;
import com.vn.otech.chat.dto.MessageStatusUpdate;
import com.vn.otech.chat.repository.ConversationParticipantRepository;
import com.vn.otech.chat.repository.ConversationRepository;
import com.vn.otech.chat.repository.MessageRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Conversation;
import com.vn.otech.entity.ConversationParticipant;
import com.vn.otech.entity.ConversationParticipantId;
import com.vn.otech.entity.Message;
import com.vn.otech.entity.MessageStatus;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.User;
import com.vn.otech.market.repository.ProductImageRepository;
import com.vn.otech.market.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ChatService {
    private static final int MAX_MESSAGE_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    public ChatService(ConversationRepository conversationRepository,
                       ConversationParticipantRepository participantRepository,
                       MessageRepository messageRepository,
                       ProductRepository productRepository,
                       ProductImageRepository productImageRepository,
                       UserRepository userRepository,
                       SimpMessagingTemplate messagingTemplate,
                       SimpUserRegistry simpUserRegistry) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
    }

    @Transactional
    public ConversationResponse createConversation(String email, ConversationCreateRequest request) {
        if ((request.productId() == null) == (request.recipientId() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one of productId or recipientId");
        }
        User currentUser = findActiveUser(email);
        User recipient;
        Product product = null;
        UUID productId = null;
        if (request.productId() != null) {
            product = productRepository.findWithSellerAndCategoryById(request.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            recipient = product.getSeller();
            productId = product.getId();
        } else {
            recipient = userRepository.findById(request.recipientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }
        if (currentUser.getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot start a conversation with yourself");
        }
        if (recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Conversation conversation = conversationRepository.findDirectConversation(
                        productId, currentUser.getId(), recipient.getId())
                .orElse(null);
        if (conversation == null) {
            if (product != null && product.getStatus() != ProductStatus.SELLING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This listing is no longer available");
            }
            conversation = new Conversation();
            conversation.setProduct(product);
            conversation = conversationRepository.saveAndFlush(conversation);
            participantRepository.saveAll(List.of(
                    participant(conversation, currentUser),
                    participant(conversation, recipient)));
        }
        UUID conversationId = conversation.getId();
        return listConversations(email).stream()
                .filter(item -> item.id().equals(conversationId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not load the conversation"));
    }

    public List<ConversationResponse> listConversations(String email) {
        User currentUser = findActiveUser(email);
        List<ConversationParticipant> ownMemberships = participantRepository.findByUser_Id(currentUser.getId());
        if (ownMemberships.isEmpty()) {
            return List.of();
        }
        List<Conversation> conversations = ownMemberships.stream()
                .map(ConversationParticipant::getConversation)
                .toList();
        List<UUID> conversationIds = conversations.stream().map(Conversation::getId).toList();
        Map<UUID, List<ConversationParticipant>> participantsByConversation =
                participantRepository.findByConversation_IdIn(conversationIds).stream()
                        .collect(Collectors.groupingBy(participant -> participant.getConversation().getId()));
        Map<UUID, Message> lastMessages = messageRepository.findLatestByConversationIds(conversationIds).stream()
                .collect(Collectors.toMap(message -> message.getConversation().getId(), message -> message));
        Map<UUID, Long> unreadCounts = messageRepository.countUnreadByConversationIds(
                        conversationIds, currentUser.getId()).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        Map<UUID, String> productImages = loadProductImages(conversations);
        return conversations.stream()
                .map(conversation -> toConversationResponse(
                        conversation,
                        currentUser.getId(),
                        participantsByConversation.getOrDefault(conversation.getId(), List.of()),
                        lastMessages.get(conversation.getId()),
                        unreadCounts.getOrDefault(conversation.getId(), 0L),
                        productImages))
                .sorted(Comparator.comparing(
                        item -> item.lastMessage() == null ? item.createdAt() : item.lastMessage().createdAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public MessagePageResponse listMessages(String email, UUID conversationId, String cursor, int limit) {
        if (limit < 1 || limit > MAX_MESSAGE_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_MESSAGE_PAGE_SIZE);
        }
        User currentUser = findActiveUser(email);
        requireParticipant(conversationId, currentUser.getId());
        MessageCursor decodedCursor = decodeCursor(cursor);
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Message> messages = decodedCursor == null
                ? messageRepository.findLatestByConversationId(conversationId, pageRequest)
                : messageRepository.findLatestBefore(
                        conversationId, decodedCursor.createdAt(), decodedCursor.id(), pageRequest);
        boolean hasMore = messages.size() > limit;
        List<Message> page = hasMore ? new ArrayList<>(messages.subList(0, limit)) : new ArrayList<>(messages);

        int markedRead = messageRepository.markConversationRead(conversationId, currentUser.getId());
        if (markedRead > 0) {
            for (Message message : page) {
                if (message.getSender() != null && !message.getSender().getId().equals(currentUser.getId())) {
                    message.setStatus(MessageStatus.READ);
                }
            }
            publishReadUpdates(conversationId, currentUser.getId());
        }

        String nextCursor = hasMore && !page.isEmpty() ? encodeCursor(page.getLast()) : null;
        page.sort(Comparator.comparing(Message::getCreatedAt).thenComparing(Message::getId));
        return new MessagePageResponse(page.stream().map(this::toMessageResponse).toList(), hasMore, nextCursor);
    }

    @Transactional
    public MessageResponse sendMessage(String email, UUID conversationId, MessageSendRequest request) {
        User sender = findActiveUser(email);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        List<ConversationParticipant> participants =
                participantRepository.findByConversation_IdIn(List.of(conversationId));
        User recipient = participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(sender.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a participant in this conversation"));
        if (participants.stream().noneMatch(participant -> participant.getUser().getId().equals(sender.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not a participant in this conversation");
        }
        if (recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This conversation is not available");
        }

        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }
        Message existing = messageRepository.findBySender_IdAndClientMessageId(sender.getId(), request.clientMessageId())
                .orElse(null);
        if (existing != null) {
            if (!existing.getConversation().getId().equals(conversationId) || !existing.getContent().equals(content)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The message id was already used with different content");
            }
            MessageResponse replay = toMessageResponse(existing);
            publishMessageAfterCommit(sender.getEmail(), recipient.getEmail(), replay);
            return replay;
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setClientMessageId(request.clientMessageId());
        message.setContent(content);
        if (simpUserRegistry.getUser(recipient.getEmail()) != null) {
            message.setStatus(MessageStatus.DELIVERED);
        }
        message = messageRepository.saveAndFlush(message);
        MessageResponse response = toMessageResponse(message);
        publishMessageAfterCommit(sender.getEmail(), recipient.getEmail(), response);
        return response;
    }

    @Transactional
    public void markRead(String email, UUID conversationId) {
        User currentUser = findActiveUser(email);
        requireParticipant(conversationId, currentUser.getId());
        int markedRead = messageRepository.markConversationRead(conversationId, currentUser.getId());
        if (markedRead > 0) publishReadUpdates(conversationId, currentUser.getId());
    }

    private ConversationParticipant participant(Conversation conversation, User user) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setId(new ConversationParticipantId(conversation.getId(), user.getId()));
        participant.setConversation(conversation);
        participant.setUser(user);
        return participant;
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            UUID currentUserId,
            List<ConversationParticipant> participants,
            Message lastMessage,
            long unreadCount,
            Map<UUID, String> productImages) {
        User otherUser = participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Conversation participant data is incomplete"));
        Product product = conversation.getProduct();
        ConversationResponse.ProductPreview productPreview = product == null ? null
                : new ConversationResponse.ProductPreview(
                        product.getId(), product.getTitle(), productImages.get(product.getId()));
        ConversationResponse.MessagePreview messagePreview = lastMessage == null ? null
                : new ConversationResponse.MessagePreview(
                        lastMessage.getContent(),
                        lastMessage.getSender() == null ? null : lastMessage.getSender().getId(),
                        lastMessage.getStatus(),
                        lastMessage.getCreatedAt());
        return new ConversationResponse(
                conversation.getId(),
                new ConversationResponse.ParticipantResponse(
                        otherUser.getId(), otherUser.getFullName(), otherUser.getAvatarUrl()),
                productPreview,
                messagePreview,
                unreadCount,
                conversation.getCreatedAt());
    }

    private Map<UUID, String> loadProductImages(List<Conversation> conversations) {
        List<UUID> productIds = conversations.stream()
                .map(Conversation::getProduct)
                .filter(product -> product != null)
                .map(Product::getId)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findByProductIdInOrderByDisplayOrderAsc(productIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getProduct().getId(),
                        image -> image.getImageUrl(),
                        (first, ignored) -> first));
    }

    private MessageResponse toMessageResponse(Message message) {
        User sender = message.getSender();
        return new MessageResponse(
                message.getId(),
                message.getClientMessageId(),
                message.getConversation().getId(),
                sender == null ? null : sender.getId(),
                sender == null ? "Otech member" : sender.getFullName(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt());
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findActiveUser(String email) {
        User user = findUser(email);
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is not active");
        }
        return user;
    }

    private void requireParticipant(UUID conversationId, UUID userId) {
        if (!participantRepository.existsByConversation_IdAndUser_Id(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not a participant in this conversation");
        }
    }

    private void publishMessageAfterCommit(String senderEmail, String recipientEmail, MessageResponse message) {
        afterCommit(() -> {
            messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/messages", message);
            messagingTemplate.convertAndSendToUser(senderEmail, "/queue/messages", message);
        });
    }

    private void publishReadUpdates(UUID conversationId, UUID readerId) {
        List<String> otherParticipantEmails = participantRepository
                .findByConversation_IdIn(List.of(conversationId)).stream()
                .map(ConversationParticipant::getUser)
                .filter(participant -> !participant.getId().equals(readerId))
                .map(User::getEmail)
                .toList();
        afterCommit(() -> otherParticipantEmails.forEach(recipientEmail ->
                messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/message-status",
                        new MessageStatusUpdate(conversationId, readerId, MessageStatus.READ))));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private MessageCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] values = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
                    .split("\\n", -1);
            if (values.length != 2) {
                throw new IllegalArgumentException();
            }
            return new MessageCursor(LocalDateTime.parse(values[0]), UUID.fromString(values[1]));
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message cursor");
        }
    }

    private String encodeCursor(Message message) {
        String value = message.getCreatedAt() + "\n" + message.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record MessageCursor(LocalDateTime createdAt, UUID id) {
    }
}
