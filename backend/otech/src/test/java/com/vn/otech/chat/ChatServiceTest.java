package com.vn.otech.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.chat.dto.MessageSendRequest;
import com.vn.otech.chat.dto.ConversationCreateRequest;
import com.vn.otech.chat.repository.ConversationParticipantRepository;
import com.vn.otech.chat.repository.ConversationRepository;
import com.vn.otech.chat.repository.MessageRepository;
import com.vn.otech.chat.service.ChatService;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Conversation;
import com.vn.otech.entity.ConversationParticipant;
import com.vn.otech.entity.Message;
import com.vn.otech.entity.MessageStatus;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.User;
import com.vn.otech.market.repository.ProductImageRepository;
import com.vn.otech.market.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SimpUserRegistry simpUserRegistry;
    @InjectMocks private ChatService chatService;

    private User sender;
    private User recipient;
    private Conversation conversation;
    private UUID clientMessageId;

    @BeforeEach
    void setUp() {
        sender = user("sender@example.com", "Sender");
        recipient = user("recipient@example.com", "Recipient");
        conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        clientMessageId = UUID.randomUUID();

        when(userRepository.findByEmailIgnoreCase(sender.getEmail())).thenReturn(Optional.of(sender));
    }

    @Test
    void sendsAndPersistsMessageForConversationParticipant() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversation_IdIn(List.of(conversation.getId())))
                .thenReturn(List.of(participant(sender), participant(recipient)));
        when(messageRepository.findBySender_IdAndClientMessageId(sender.getId(), clientMessageId))
                .thenReturn(Optional.empty());
        when(simpUserRegistry.getUser(recipient.getEmail())).thenReturn(null);
        when(messageRepository.saveAndFlush(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            message.setCreatedAt(LocalDateTime.of(2026, 10, 1, 12, 0));
            return message;
        });

        var response = chatService.sendMessage(sender.getEmail(), conversation.getId(),
                new MessageSendRequest(clientMessageId, "  Hello there  "));

        assertEquals("Hello there", response.content());
        assertEquals(sender.getId(), response.senderId());
        assertEquals(MessageStatus.SENT, response.status());
        verify(messageRepository).saveAndFlush(any(Message.class));
        verify(messagingTemplate).convertAndSendToUser(eq(recipient.getEmail()), eq("/queue/messages"), any());
        verify(messagingTemplate).convertAndSendToUser(eq(sender.getEmail()), eq("/queue/messages"), any());
    }

    @Test
    void rejectsMessageWhenSenderIsNotParticipant() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversation_IdIn(List.of(conversation.getId())))
                .thenReturn(List.of(participant(recipient)));

        assertThrows(ResponseStatusException.class, () -> chatService.sendMessage(
                sender.getEmail(), conversation.getId(), new MessageSendRequest(clientMessageId, "Hello")));

        verify(messageRepository, never()).saveAndFlush(any(Message.class));
    }

    @Test
    void replaysAnExistingClientMessageIdWithoutCreatingADuplicate() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversation_IdIn(List.of(conversation.getId())))
                .thenReturn(List.of(participant(sender), participant(recipient)));
        Message existing = new Message();
        existing.setId(UUID.randomUUID());
        existing.setConversation(conversation);
        existing.setSender(sender);
        existing.setClientMessageId(clientMessageId);
        existing.setContent("Hello");
        existing.setStatus(MessageStatus.DELIVERED);
        existing.setCreatedAt(LocalDateTime.of(2026, 10, 1, 12, 0));
        when(messageRepository.findBySender_IdAndClientMessageId(sender.getId(), clientMessageId))
                .thenReturn(Optional.of(existing));

        var response = chatService.sendMessage(sender.getEmail(), conversation.getId(),
                new MessageSendRequest(clientMessageId, "Hello"));

        assertEquals(existing.getId(), response.id());
        assertEquals(MessageStatus.DELIVERED, response.status());
        verify(messageRepository, never()).saveAndFlush(any(Message.class));
        verify(messagingTemplate, org.mockito.Mockito.times(2))
                .convertAndSendToUser(anyString(), eq("/queue/messages"), any());
    }

    @Test
    void rejectsReusingClientMessageIdWithDifferentContent() {
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversation_IdIn(List.of(conversation.getId())))
                .thenReturn(List.of(participant(sender), participant(recipient)));
        Message existing = new Message();
        existing.setConversation(conversation);
        existing.setSender(sender);
        existing.setClientMessageId(clientMessageId);
        existing.setContent("Original text");
        when(messageRepository.findBySender_IdAndClientMessageId(sender.getId(), clientMessageId))
                .thenReturn(Optional.of(existing));

        assertThrows(ResponseStatusException.class, () -> chatService.sendMessage(
                sender.getEmail(), conversation.getId(), new MessageSendRequest(clientMessageId, "Changed text")));

        verify(messageRepository, never()).saveAndFlush(any(Message.class));
    }

    @Test
    void loadsFirstMessagePageWithoutBindingNullableCursorParameters() {
        when(participantRepository.existsByConversation_IdAndUser_Id(conversation.getId(), sender.getId()))
                .thenReturn(true);
        when(messageRepository.findLatestByConversationId(eq(conversation.getId()), any(PageRequest.class)))
                .thenReturn(List.of());

        var page = chatService.listMessages(sender.getEmail(), conversation.getId(), null, 50);

        assertEquals(List.of(), page.items());
        verify(messageRepository).findLatestByConversationId(
                eq(conversation.getId()), eq(PageRequest.of(0, 51)));
        verify(messageRepository, never()).findLatestBefore(
                any(), any(), any(), any(PageRequest.class));
    }

    @Test
    void loadsOlderMessagesUsingTypedCursorParameters() {
        LocalDateTime beforeTime = LocalDateTime.of(2026, 10, 1, 12, 0);
        UUID beforeId = UUID.randomUUID();
        String cursor = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (beforeTime + "\n" + beforeId).getBytes(StandardCharsets.UTF_8));
        when(participantRepository.existsByConversation_IdAndUser_Id(conversation.getId(), sender.getId()))
                .thenReturn(true);
        when(messageRepository.findLatestBefore(
                eq(conversation.getId()), eq(beforeTime), eq(beforeId), any(PageRequest.class)))
                .thenReturn(List.of());

        var page = chatService.listMessages(sender.getEmail(), conversation.getId(), cursor, 20);

        assertEquals(List.of(), page.items());
        verify(messageRepository).findLatestBefore(
                conversation.getId(), beforeTime, beforeId, PageRequest.of(0, 21));
        verify(messageRepository, never()).findLatestByConversationId(any(), any(PageRequest.class));
    }

    @Test
    void productMessageReusesAnExistingDirectConversationWithTheSeller() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setSeller(recipient);
        product.setStatus(ProductStatus.SELLING);
        conversation.setCreatedAt(LocalDateTime.of(2026, 10, 1, 12, 0));
        when(productRepository.findWithSellerAndCategoryById(productId)).thenReturn(Optional.of(product));
        when(conversationRepository.findDirectConversations(sender.getId(), recipient.getId()))
                .thenReturn(List.of(conversation));
        when(participantRepository.findByUser_Id(sender.getId())).thenReturn(List.of(participant(sender)));
        when(participantRepository.findByConversation_IdIn(List.of(conversation.getId())))
                .thenReturn(List.of(participant(sender), participant(recipient)));
        when(messageRepository.findLatestByConversationIds(List.of(conversation.getId()))).thenReturn(List.of());
        when(messageRepository.countUnreadByConversationIds(List.of(conversation.getId()), sender.getId()))
                .thenReturn(List.of());

        var result = chatService.createConversation(
                sender.getEmail(), new ConversationCreateRequest(productId, null));

        assertEquals(conversation.getId(), result.id());
        verify(conversationRepository).findDirectConversations(sender.getId(), recipient.getId());
        verify(conversationRepository, never()).saveAndFlush(any());
    }

    @Test
    void conversationListCollapsesExistingThreadsForTheSamePerson() {
        Conversation duplicate = new Conversation();
        duplicate.setId(UUID.randomUUID());
        duplicate.setCreatedAt(LocalDateTime.of(2026, 10, 2, 12, 0));
        ConversationParticipant senderInOriginal = participantFor(conversation, sender);
        ConversationParticipant recipientInOriginal = participantFor(conversation, recipient);
        ConversationParticipant senderInDuplicate = participantFor(duplicate, sender);
        ConversationParticipant recipientInDuplicate = participantFor(duplicate, recipient);
        when(participantRepository.findByUser_Id(sender.getId()))
                .thenReturn(List.of(senderInOriginal, senderInDuplicate));
        when(participantRepository.findByConversation_IdIn(List.of(conversation.getId(), duplicate.getId())))
                .thenReturn(List.of(senderInOriginal, recipientInOriginal, senderInDuplicate, recipientInDuplicate));
        when(messageRepository.findLatestByConversationIds(List.of(conversation.getId(), duplicate.getId())))
                .thenReturn(List.of());
        when(messageRepository.countUnreadByConversationIds(
                List.of(conversation.getId(), duplicate.getId()), sender.getId())).thenReturn(List.of());

        var result = chatService.listConversations(sender.getEmail());

        assertEquals(1, result.size());
        assertEquals(recipient.getId(), result.getFirst().participant().id());
        assertEquals(duplicate.getId(), result.getFirst().id());
    }

    private User user(String email, String name) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName(name);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    private ConversationParticipant participant(User user) {
        return participantFor(conversation, user);
    }

    private ConversationParticipant participantFor(Conversation target, User user) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(target);
        participant.setUser(user);
        return participant;
    }
}
