package com.vn.otech.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.Comment;
import com.vn.otech.entity.Post;
import com.vn.otech.entity.User;
import com.vn.otech.post.dto.CommentRequest;
import com.vn.otech.post.dto.PostInteractionResponse;
import com.vn.otech.post.dto.PostRequest;
import com.vn.otech.post.dto.PostResponse;
import com.vn.otech.post.repository.CommentRepository;
import com.vn.otech.post.repository.PostCount;
import com.vn.otech.post.repository.PostImageRepository;
import com.vn.otech.post.repository.PostLikeRepository;
import com.vn.otech.post.repository.PostRepository;
import com.vn.otech.post.repository.PostShareRepository;
import com.vn.otech.post.service.PostService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostShareRepository postShareRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void listPostsMapsAnAuthoredPostToThePublicResponse() {
        UUID postId = UUID.randomUUID();
        User author = new User();
        author.setId(UUID.randomUUID());
        author.setFullName("Ada Lovelace");
        author.setEmail("ada@example.com");
        author.setAvatarUrl("https://example.com/ada.png");

        Post post = new Post();
        post.setId(postId);
        post.setAuthor(author);
        post.setContent("A useful community post");
        post.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));
        stubResponseRepositories();

        List<PostResponse> result = postService.listPosts();

        assertEquals(1, result.size());
        PostResponse response = result.getFirst();
        assertEquals(postId, response.id());
        assertEquals(author.getId(), response.authorId());
        assertEquals("Ada Lovelace", response.name());
        assertEquals("@ada", response.handle());
        assertEquals("15 min ago", response.time());
        assertEquals("https://example.com/ada.png", response.avatar());
        assertEquals("A useful community post", response.title());
        assertEquals("A useful community post", response.copy());
        assertEquals("Community post", response.tag());
    }

    @Test
    void listPostsUsesDefaultsForPostWithoutAuthorAndMetadata() {
        Post post = new Post();
        post.setContent(null);
        post.setCreatedAt(null);
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));
        stubResponseRepositories();

        PostResponse response = postService.listPosts().getFirst();

        assertEquals("Otech member", response.name());
        assertEquals("", response.handle());
        assertEquals("Recently", response.time());
        assertEquals("", response.avatar());
        assertEquals("Community post", response.title());
        assertEquals(null, response.copy());
    }

    @Test
    void listPostsTruncatesTitlesLongerThanFiftySixCharacters() {
        String content = "123456789012345678901234567890123456789012345678901234567890";
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setContent(content);
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));
        stubResponseRepositories();

        PostResponse response = postService.listPosts().getFirst();

        assertEquals(content, response.copy());
        assertEquals(content.substring(0, 56) + "...", response.title());
        assertEquals(59, response.title().length());
    }

    @Test
    void listPostsReturnsAnEmptyListWhenRepositoryHasNoVisiblePosts() {
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of());

        List<PostResponse> result = postService.listPosts();

        assertTrue(result.isEmpty());
    }

    @Test
    void createTrimsContentAndAssociatesTheAuthor() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        User author = new User();
        author.setId(authorId);
        author.setEmail("ada@example.com");
        author.setFullName("Ada Lovelace");
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(author));
        when(postRepository.saveAndFlush(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(postId);
            return post;
        });
        stubResponseRepositories();

        PostResponse result = postService.create("ada@example.com", new PostRequest("  Hello community  ", null));

        assertEquals(postId, result.id());
        assertEquals(authorId, result.authorId());
        assertEquals("Hello community", result.copy());
    }

    @Test
    void createRejectsAReplyToACommentOnAnotherPost() {
        UUID postId = UUID.randomUUID();
        User author = new User();
        author.setId(UUID.randomUUID());
        author.setEmail("ada@example.com");
        Post post = new Post();
        post.setId(postId);
        Comment parent = new Comment();
        Post otherPost = new Post();
        otherPost.setId(UUID.randomUUID());
        parent.setPost(otherPost);
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(author));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(any(UUID.class))).thenReturn(Optional.of(parent));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> postService.createComment("ada@example.com", postId,
                        new CommentRequest("A reply", UUID.randomUUID())));
    }

    @Test
    void likeIsIdempotentWhenTheUserAlreadyLikedThePost() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Post post = new Post();
        post.setId(postId);
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(true);
        when(postLikeRepository.countByPost_Id(postId)).thenReturn(4L);
        when(commentRepository.countByPost_Id(postId)).thenReturn(2L);
        when(postShareRepository.countByPost_Id(postId)).thenReturn(1L);

        PostInteractionResponse result = postService.like("ada@example.com", postId);

        assertEquals(4L, result.likes());
        assertEquals(2L, result.comments());
        assertEquals(1L, result.shares());
        assertTrue(result.likedByCurrentUser());
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    void shareIsIdempotentWhenTheUserAlreadySharedThePost() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Post post = new Post();
        post.setId(postId);
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postShareRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(true);
        when(postLikeRepository.countByPost_Id(postId)).thenReturn(4L);
        when(commentRepository.countByPost_Id(postId)).thenReturn(2L);
        when(postShareRepository.countByPost_Id(postId)).thenReturn(1L);
        when(postLikeRepository.existsByUser_IdAndPost_Id(userId, postId)).thenReturn(false);

        PostInteractionResponse result = postService.share("ada@example.com", postId);

        assertEquals(4L, result.likes());
        assertEquals(2L, result.comments());
        assertEquals(1L, result.shares());
        verify(postShareRepository, never()).save(any());
    }

    private void stubResponseRepositories() {
        when(postImageRepository.findByPost_IdInOrderByIdAsc(anyList())).thenReturn(List.of());
        when(postLikeRepository.countByPostIds(anyList())).thenReturn(List.<PostCount>of());
        when(commentRepository.countByPostIds(anyList())).thenReturn(List.<PostCount>of());
        when(postShareRepository.countByPostIds(anyList())).thenReturn(List.<PostCount>of());
    }
}