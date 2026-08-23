package com.vn.otech.post;

import com.vn.otech.entity.Post;
import com.vn.otech.repository.PostRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Map<String, Object>> listPosts() {
        return postRepository.findByHiddenFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private Map<String, Object> toResponse(Post post) {
        String authorName = post.getAuthor() == null ? "Otech member" : post.getAuthor().getFullName();
        String handle = post.getAuthor() == null ? "" : "@" + post.getAuthor().getEmail().split("@", 2)[0];
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", post.getId());
        response.put("name", authorName);
        response.put("handle", handle);
        response.put("time", formatAge(post.getCreatedAt()));
        response.put("avatar", post.getAuthor() == null || post.getAuthor().getAvatarUrl() == null ? "" : post.getAuthor().getAvatarUrl());
        response.put("image", "");
        response.put("title", titleFrom(post.getContent()));
        response.put("copy", post.getContent());
        response.put("price", "");
        response.put("likes", 0);
        response.put("comments", 0);
        response.put("tag", "Community post");
        return response;
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
