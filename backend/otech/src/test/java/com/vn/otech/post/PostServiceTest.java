package com.vn.otech.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.vn.otech.entity.Post;
import com.vn.otech.entity.User;
import com.vn.otech.post.dto.PostResponse;
import com.vn.otech.post.repository.PostRepository;
import com.vn.otech.post.service.PostService;
import java.time.LocalDateTime;
import java.util.List;
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

    @InjectMocks
    private PostService postService;

    @Test
    void listPostsMapsAnAuthoredPostToThePublicResponse() {
        UUID postId = UUID.randomUUID();
        User author = new User();
        author.setFullName("Ada Lovelace");
        author.setEmail("ada@example.com");
        author.setAvatarUrl("https://example.com/ada.png");

        Post post = new Post();
        post.setId(postId);
        post.setAuthor(author);
        post.setContent("A useful community post");
        post.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));

        List<PostResponse> result = postService.listPosts();

        assertEquals(1, result.size());
        PostResponse response = result.getFirst();
        assertEquals(postId, response.id());
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
        post.setContent(content);
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));

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
}