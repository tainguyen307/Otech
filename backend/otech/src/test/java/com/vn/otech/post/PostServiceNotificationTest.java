package com.vn.otech.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Comment;
import com.vn.otech.entity.NotificationType;
import com.vn.otech.entity.Post;
import com.vn.otech.entity.User;
import com.vn.otech.notification.service.NotificationService;
import com.vn.otech.post.dto.CommentRequest;
import com.vn.otech.post.repository.CommentRepository;
import com.vn.otech.post.repository.PostImageRepository;
import com.vn.otech.post.repository.PostLikeRepository;
import com.vn.otech.post.repository.PostRepository;
import com.vn.otech.post.repository.PostShareRepository;
import com.vn.otech.post.service.PostService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceNotificationTest {
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostImageRepository postImageRepository;
    @Mock private PostShareRepository postShareRepository;
    @Mock private NotificationService notificationService;

    @Test
    void notifiesPostAuthorOnlyForNewLikes() {
        User actor = user("reader@example.com");
        User author = user("author@example.com");
        Post post = post(author);
        when(userRepository.findByEmailIgnoreCase(actor.getEmail())).thenReturn(Optional.of(actor));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUser_IdAndPost_Id(actor.getId(), post.getId()))
                .thenReturn(false, true, true);

        service().like(actor.getEmail(), post.getId());
        service().like(actor.getEmail(), post.getId());

        verify(postLikeRepository).save(any());
        verify(notificationService, times(1)).create(
                eq(author),
                eq(NotificationType.LIKE_POST),
                eq("New like"),
                eq("Reader liked your post."),
                eq(post.getId()));
    }

    @Test
    void notifiesPostAuthorWhenAnotherUserComments() {
        User commenter = user("commenter@example.com");
        User author = user("author@example.com");
        Post post = post(author);
        when(userRepository.findByEmailIgnoreCase(commenter.getEmail())).thenReturn(Optional.of(commenter));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            comment.setCreatedAt(LocalDateTime.of(2026, 11, 20, 12, 0));
            return comment;
        });

        var response = service().createComment(commenter.getEmail(), post.getId(), new CommentRequest("Nice find", null));

        assertEquals("Nice find", response.content());
        verify(notificationService).create(
                eq(author),
                eq(NotificationType.COMMENT_POST),
                eq("New comment"),
                eq("Commenter: Nice find"),
                eq(post.getId()));
    }

    private PostService service() {
        return new PostService(
                postRepository,
                userRepository,
                postLikeRepository,
                commentRepository,
                postImageRepository,
                postShareRepository,
                notificationService);
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        String name = email.substring(0, email.indexOf('@'));
        user.setFullName(name.substring(0, 1).toUpperCase() + name.substring(1));
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    private Post post(User author) {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.of(2026, 11, 20, 10, 0));
        post.setHidden(false);
        return post;
    }
}
