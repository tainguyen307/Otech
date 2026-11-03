package com.vn.otech.group.repository;

import com.vn.otech.entity.Group;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    @Query(
            value = """
                    select p.id as id,
                           p.name as name,
                           p.description as description,
                           p.avatar_url as "avatarUrl",
                           p.cover_url as "coverUrl",
                           p.category_id as "categoryId",
                           category.name as "categoryName",
                           p.visibility::text as visibility,
                           p.require_post_approval as "requirePostApproval",
                           coalesce(owner.full_name, 'Otech member') as "ownerName",
                           (select count(*) from group_members member where member.group_id = p.id) as "memberCount",
                           (select count(*) from group_posts post
                            where post.group_id = p.id and post.status = 'APPROVED') as "postCount",
                           p.created_at as "createdAt"
                    from groups p
                    left join users owner on owner.id = p.owner_id
                    left join categories category on category.id = p.category_id
                    where p.is_hidden = false
                      and (:search = '' or lower(p.name) like lower(concat('%', :search, '%'))
                           or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
                      and (:categoryId is null or p.category_id = :categoryId)
                    order by p.created_at desc, p.id
                    """,
            countQuery = """
                    select count(*) from groups p
                    where p.is_hidden = false
                      and (:search = '' or lower(p.name) like lower(concat('%', :search, '%'))
                           or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
                      and (:categoryId is null or p.category_id = :categoryId)
                    """,
            nativeQuery = true)
    org.springframework.data.domain.Page<GroupProjection> findDiscoverableGroups(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            org.springframework.data.domain.Pageable pageable);

    @Query(
            value = """
                    select p.id as id,
                           p.name as name,
                           p.description as description,
                           p.avatar_url as "avatarUrl",
                           p.cover_url as "coverUrl",
                           p.category_id as "categoryId",
                           category.name as "categoryName",
                           p.visibility::text as visibility,
                           p.require_post_approval as "requirePostApproval",
                           coalesce(owner.full_name, 'Otech member') as "ownerName",
                           (select count(*) from group_members member where member.group_id = p.id) as "memberCount",
                           (select count(*) from group_posts post
                            where post.group_id = p.id and post.status = 'APPROVED') as "postCount",
                           p.created_at as "createdAt"
                    from groups p
                    left join users owner on owner.id = p.owner_id
                    left join categories category on category.id = p.category_id
                    where p.owner_id = :ownerId and p.is_hidden = false
                    order by p.created_at desc, p.id
                    """,
            countQuery = "select count(*) from groups p where p.owner_id = :ownerId and p.is_hidden = false",
            nativeQuery = true)
    org.springframework.data.domain.Page<GroupProjection> findOwnedGroups(
            @Param("ownerId") UUID ownerId, org.springframework.data.domain.Pageable pageable);
}
