package com.vn.otech.group.repository;

import com.vn.otech.entity.GroupPostImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupPostImageRepository extends JpaRepository<GroupPostImage, UUID> {
    List<GroupPostImage> findByGroupPost_IdOrderById(UUID postId);

    void deleteByGroupPost_Id(UUID postId);
}
