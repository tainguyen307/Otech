package com.vn.otech.post.repository;

import com.vn.otech.entity.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @EntityGraph(attributePaths = {"user", "parentComment"})
    List<Comment> findByPost_IdOrderByCreatedAtAsc(UUID postId);

    long countByPost_Id(UUID postId);

    @Query("select new com.vn.otech.post.repository.PostCount(c.post.id, count(c)) "
            + "from Comment c where c.post.id in :postIds group by c.post.id")
    List<PostCount> countByPostIds(@Param("postIds") List<UUID> postIds);
}
