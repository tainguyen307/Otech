package com.vn.otech.post.repository;

import com.vn.otech.entity.Post;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findByHiddenFalseOrderByCreatedAtDesc();
}
