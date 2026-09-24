package com.vn.otech.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.vn.otech.entity.Post;
import com.vn.otech.entity.User;
import com.vn.otech.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

        List<Map<String, Object>> result = postService.listPosts();

        assertEquals(1, result.size());
        Map<String, Object> response = result.getFirst();
        assertEquals(postId, response.get("id"));
        assertEquals("Ada Lovelace", response.get("name"));
        assertEquals("@ada", response.get("handle"));
        assertEquals("15 min ago", response.get("time"));
        assertEquals("https://example.com/ada.png", response.get("avatar"));
        assertEquals("A useful community post", response.get("title"));
        assertEquals("A useful community post", response.get("copy"));
        assertEquals("Community post", response.get("tag"));
    }

    @Test
    void listPostsUsesDefaultsForPostWithoutAuthorAndMetadata() {
        Post post = new Post();
        post.setContent(null);
        post.setCreatedAt(null);
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));

        Map<String, Object> response = postService.listPosts().getFirst();

        assertEquals("Otech member", response.get("name"));
        assertEquals("", response.get("handle"));
        assertEquals("Recently", response.get("time"));
        assertEquals("", response.get("avatar"));
        assertEquals("Community post", response.get("title"));
        assertEquals(null, response.get("copy"));
    }

    @Test
    void listPostsTruncatesTitlesLongerThanFiftySixCharacters() {
        String content = "123456789012345678901234567890123456789012345678901234567890";
        Post post = new Post();
        post.setContent(content);
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of(post));

        Map<String, Object> response = postService.listPosts().getFirst();

        assertEquals(content, response.get("copy"));
        assertEquals(content.substring(0, 56) + "...", response.get("title"));
        assertEquals(59, ((String) response.get("title")).length());
    }

    @Test
    void listPostsReturnsAnEmptyListWhenRepositoryHasNoVisiblePosts() {
        when(postRepository.findByHiddenFalseOrderByCreatedAtDesc()).thenReturn(List.of());

        List<Map<String, Object>> result = postService.listPosts();

        assertTrue(result.isEmpty());
    }
}