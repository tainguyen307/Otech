package com.vn.otech.post.repository;

import com.vn.otech.entity.PostLike;
import com.vn.otech.entity.PostLikeId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    boolean existsByUser_IdAndPost_Id(UUID userId, UUID postId);

    void deleteByUser_IdAndPost_Id(UUID userId, UUID postId);

    long countByPost_Id(UUID postId);

    @Query("select new com.vn.otech.post.repository.PostCount(l.post.id, count(l)) "
            + "from PostLike l where l.post.id in :postIds group by l.post.id")
    List<PostCount> countByPostIds(@Param("postIds") List<UUID> postIds);

    @Query("select l.post.id from PostLike l "
            + "where l.user.id = :userId and l.post.id in :postIds")
    Set<UUID> findLikedPostIds(
            @Param("userId") UUID userId,
            @Param("postIds") List<UUID> postIds);
}
