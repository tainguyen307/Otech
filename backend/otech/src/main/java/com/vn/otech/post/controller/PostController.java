package com.vn.otech.post.controller;

import com.vn.otech.post.dto.PostResponse;
import com.vn.otech.post.dto.CommentRequest;
import com.vn.otech.post.dto.CommentResponse;
import com.vn.otech.post.dto.PostInteractionResponse;
import com.vn.otech.post.dto.PostRequest;
import com.vn.otech.post.service.PostService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<PostResponse> listPosts(@AuthenticationPrincipal UserDetails principal) {
        return postService.listPosts(principal == null ? null : principal.getUsername());
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody PostRequest request) {
        PostResponse post = postService.create(principal.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/posts/" + post.id())).body(post);
    }

    @PutMapping("/{postId}")
    public PostResponse updatePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID postId,
            @Valid @RequestBody PostRequest request) {
        return postService.update(principal.getUsername(), postId, request);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID postId) {
        postService.delete(principal.getUsername(), postId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{postId}/likes")
    public PostInteractionResponse likePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID postId) {
        return postService.like(principal.getUsername(), postId);
    }

    @DeleteMapping("/{postId}/likes")
    public PostInteractionResponse unlikePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID postId) {
        return postService.unlike(principal.getUsername(), postId);
    }

    @GetMapping("/{postId}/comments")
    public List<CommentResponse> listComments(@PathVariable UUID postId) {
        return postService.listComments(postId);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse comment = postService.createComment(principal.getUsername(), postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PutMapping("/{postId}/shares")
    public PostInteractionResponse sharePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID postId) {
        return postService.share(principal.getUsername(), postId);
    }
}