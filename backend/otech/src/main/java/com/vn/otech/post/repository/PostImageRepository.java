package com.vn.otech.post.repository;

import com.vn.otech.entity.PostImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {
    List<PostImage> findByPost_IdInOrderByIdAsc(List<UUID> postIds);

    void deleteByPost_Id(UUID postId);
}
