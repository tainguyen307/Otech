package com.vn.otech.post.repository;

import com.vn.otech.entity.Post;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    List<Post> findByHiddenFalseOrderByCreatedAtDesc();

    List<Post> findByAuthor_IdAndHiddenFalseOrderByCreatedAtDesc(UUID authorId);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Post> findAll(Specification<Post> specification, Pageable pageable);

    long countByHiddenTrue();
}
