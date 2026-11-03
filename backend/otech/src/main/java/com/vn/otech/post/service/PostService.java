package com.vn.otech.post.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Comment;
import com.vn.otech.entity.Post;
import com.vn.otech.entity.PostImage;
import com.vn.otech.entity.PostLike;
import com.vn.otech.entity.PostLikeId;
import com.vn.otech.entity.PostShare;
import com.vn.otech.entity.User;
import com.vn.otech.post.dto.CommentRequest;
import com.vn.otech.post.dto.CommentResponse;
import com.vn.otech.post.dto.PostInteractionResponse;
import com.vn.otech.post.dto.PostRequest;
import com.vn.otech.post.dto.PostResponse;
import com.vn.otech.post.repository.CommentRepository;
import com.vn.otech.post.repository.PostCount;
import com.vn.otech.post.repository.PostImageRepository;
import com.vn.otech.post.repository.PostLikeRepository;
import com.vn.otech.post.repository.PostRepository;
import com.vn.otech.post.repository.PostShareRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final PostImageRepository postImageRepository;
    private final PostShareRepository postShareRepository;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            PostLikeRepository postLikeRepository,
            CommentRepository commentRepository,
            PostImageRepository postImageRepository,
            PostShareRepository postShareRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.postImageRepository = postImageRepository;
        this.postShareRepository = postShareRepository;
    }

    public List<PostResponse> listPosts() {
        return listPosts(null);
    }

    public List<PostResponse> listPosts(String email) {
        UUID viewerId = email == null ? null : findActiveUser(email).getId();
        return toResponses(postRepository.findByHiddenFalseOrderByCreatedAtDesc(), viewerId);
    }

    public List<PostResponse> listPostsByAuthor(UUID authorId) {
        return toResponses(postRepository.findByAuthor_IdAndHiddenFalseOrderByCreatedAtDesc(authorId), null);
    }

    @Transactional
    public PostResponse create(String email, PostRequest request) {
        User author = findActiveUser(email);
        Post post = new Post();
        post.setAuthor(author);
        post.setContent(request.content().trim());
        post.setHidden(false);
        post = postRepository.saveAndFlush(post);
        replaceImage(post, request.imageUrl());
        return toResponses(List.of(post), author.getId()).getFirst();
    }

    @Transactional
    public PostResponse update(String email, UUID postId, PostRequest request) {
        User author = findActiveUser(email);
        Post post = requireOwner(findVisiblePost(postId), author);
        post.setContent(request.content().trim());
        post = postRepository.saveAndFlush(post);
        replaceImage(post, request.imageUrl());
        return toResponses(List.of(post), author.getId()).getFirst();
    }

    @Transactional
    public void delete(String email, UUID postId) {
        User author = findActiveUser(email);
        Post post = requireOwner(findVisiblePost(postId), author);
        post.setHidden(true);
        postRepository.save(post);
    }

    @Transactional
    public PostInteractionResponse like(String email, UUID postId) {
        User user = findActiveUser(email);
        Post post = findVisiblePost(postId);
        if (!postLikeRepository.existsByUser_IdAndPost_Id(user.getId(), postId)) {
            PostLike like = new PostLike();
            like.setId(new PostLikeId(user.getId(), postId));
            like.setUser(user);
            like.setPost(post);
            postLikeRepository.save(like);
        }
        return interactions(postId, user.getId());
    }

    @Transactional
    public PostInteractionResponse unlike(String email, UUID postId) {
        User user = findActiveUser(email);
        findVisiblePost(postId);
        postLikeRepository.deleteByUser_IdAndPost_Id(user.getId(), postId);
        return interactions(postId, user.getId());
    }

    public List<CommentResponse> listComments(UUID postId) {
        findVisiblePost(postId);
        return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(String email, UUID postId, CommentRequest request) {
        User user = findActiveUser(email);
        Post post = findVisiblePost(postId);
        Comment parent = request.parentCommentId() == null
                ? null
                : commentRepository.findById(request.parentCommentId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (parent != null && !parent.getPost().getId().equals(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post);
        comment.setParentComment(parent);
        comment.setContent(request.content().trim());
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public PostInteractionResponse share(String email, UUID postId) {
        User user = findActiveUser(email);
        Post post = findVisiblePost(postId);
        if (!postShareRepository.existsByUser_IdAndPost_Id(user.getId(), postId)) {
            PostShare share = new PostShare();
            share.setUser(user);
            share.setPost(post);
            postShareRepository.save(share);
        }
        return interactions(postId, user.getId());
    }

    private List<PostResponse> toResponses(List<Post> posts, UUID viewerId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        Map<UUID, List<String>> images = postImageRepository.findByPost_IdInOrderByIdAsc(postIds).stream()
                .collect(Collectors.groupingBy(
                        image -> image.getPost().getId(),
                        Collectors.mapping(PostImage::getImageUrl, Collectors.toList())));
        Map<UUID, Long> likes = toCountMap(postLikeRepository.countByPostIds(postIds));
        Map<UUID, Long> comments = toCountMap(commentRepository.countByPostIds(postIds));
        Map<UUID, Long> shares = toCountMap(postShareRepository.countByPostIds(postIds));
        Set<UUID> likedPostIds = viewerId == null
                ? Set.of()
                : postLikeRepository.findLikedPostIds(viewerId, postIds);

        return posts.stream()
                .map(post -> toResponse(post, images, likes, comments, shares, likedPostIds))
                .toList();
    }

    private PostResponse toResponse(
            Post post,
            Map<UUID, List<String>> images,
            Map<UUID, Long> likes,
            Map<UUID, Long> comments,
            Map<UUID, Long> shares,
            Set<UUID> likedPostIds) {
        User author = post.getAuthor();
        String authorName = author == null ? "Otech member" : author.getFullName();
        String handle = author == null ? "" : "@" + author.getEmail().split("@", 2)[0];
        String avatarUrl = author == null || author.getAvatarUrl() == null ? "" : author.getAvatarUrl();
        String imageUrl = images.getOrDefault(post.getId(), List.of()).stream().findFirst().orElse("");
        return new PostResponse(
                post.getId(),
                author == null ? null : author.getId(),
                authorName,
                handle,
                formatAge(post.getCreatedAt()),
                avatarUrl,
                imageUrl,
                titleFrom(post.getContent()),
                post.getContent(),
                "",
                likes.getOrDefault(post.getId(), 0L),
                comments.getOrDefault(post.getId(), 0L),
                "Community post",
                shares.getOrDefault(post.getId(), 0L),
                author != null && likedPostIds.contains(post.getId()));
    }

    private Map<UUID, Long> toCountMap(List<PostCount> counts) {
        return counts.stream().collect(Collectors.toMap(PostCount::postId, PostCount::count));
    }

    private PostInteractionResponse interactions(UUID postId, UUID userId) {
        return new PostInteractionResponse(
                postLikeRepository.countByPost_Id(postId),
                commentRepository.countByPost_Id(postId),
                postShareRepository.countByPost_Id(postId),
                postLikeRepository.existsByUser_IdAndPost_Id(userId, postId));
    }

    private void replaceImage(Post post, String imageUrl) {
        postImageRepository.deleteByPost_Id(post.getId());
        if (imageUrl != null && !imageUrl.isBlank()) {
            PostImage image = new PostImage();
            image.setPost(post);
            image.setImageUrl(imageUrl.trim());
            postImageRepository.save(image);
        }
    }

    private CommentResponse toCommentResponse(Comment comment) {
        User author = comment.getUser();
        return new CommentResponse(
                comment.getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                author == null ? null : author.getId(),
                author == null ? "Otech member" : author.getFullName(),
                author == null || author.getAvatarUrl() == null ? "" : author.getAvatarUrl(),
                comment.getContent(),
                comment.getCreatedAt());
    }

    private Post findVisiblePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (post.isHidden()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        return post;
    }

    private Post requireOwner(Post post, User user) {
        if (post.getAuthor() == null || !post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this post");
        }
        return post;
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is not active");
        }
        return user;
    }

    private String titleFrom(String content) {
        if (content == null || content.isBlank()) {
            return "Community post";
        }
        return content.length() > 56 ? content.substring(0, 56) + "..." : content;
    }

    private String formatAge(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Recently";
        }
        long minutes = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toMinutes());
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hr ago";
        }
        return (hours / 24) + " days ago";
    }
}
