package com.vn.otech.post.service;

import com.vn.otech.entity.Post;
import com.vn.otech.post.dto.PostResponse;
import com.vn.otech.post.repository.PostRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<PostResponse> listPosts() {
        return postRepository.findByHiddenFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private PostResponse toResponse(Post post) {
        String authorName = post.getAuthor() == null ? "Otech member" : post.getAuthor().getFullName();
        String handle = post.getAuthor() == null ? "" : "@" + post.getAuthor().getEmail().split("@", 2)[0];
        String avatarUrl = post.getAuthor() == null || post.getAuthor().getAvatarUrl() == null ? "" : post.getAuthor().getAvatarUrl();
        return new PostResponse(
                post.getId(),
                authorName,
                handle,
                formatAge(post.getCreatedAt()),
                avatarUrl,
                "",
                titleFrom(post.getContent()),
                post.getContent(),
                "",
                0,
                0,
                "Community post");
    }

    private String titleFrom(String content) {
        if (content == null || content.isBlank()) return "Community post";
        return content.length() > 56 ? content.substring(0, 56) + "..." : content;
    }

    private String formatAge(LocalDateTime createdAt) {
        if (createdAt == null) return "Recently";
        long minutes = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toMinutes());
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hr ago";
        return (hours / 24) + " days ago";
    }
}