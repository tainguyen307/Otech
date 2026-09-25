package com.vn.otech.post.controller;

import com.vn.otech.post.dto.PostResponse;
import com.vn.otech.post.service.PostService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
    public List<PostResponse> listPosts() {
        return postService.listPosts();
    }
}