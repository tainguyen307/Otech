package com.vn.otech.post.repository;

import com.vn.otech.entity.PostShare;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostShareRepository extends JpaRepository<PostShare, UUID> {
    boolean existsByUser_IdAndPost_Id(UUID userId, UUID postId);

    long countByPost_Id(UUID postId);

    @Query("select new com.vn.otech.post.repository.PostCount(s.post.id, count(s)) "
            + "from PostShare s where s.post.id in :postIds group by s.post.id")
    List<PostCount> countByPostIds(@Param("postIds") List<UUID> postIds);
}
