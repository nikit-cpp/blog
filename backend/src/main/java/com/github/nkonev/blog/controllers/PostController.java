package com.github.nkonev.blog.controllers;

import com.github.nkonev.blog.Constants;
import com.github.nkonev.blog.converter.PostConverter;
import com.github.nkonev.blog.dto.*;
import com.github.nkonev.blog.exception.DataNotFoundException;
import com.github.nkonev.blog.repo.jpa.CommentRepository;
import com.github.nkonev.blog.repo.jpa.PostRepository;
import com.github.nkonev.blog.repo.jpa.UserAccountRepository;
import com.github.nkonev.blog.services.PostService;
import com.github.nkonev.blog.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@RestController
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PostConverter postConverter;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;


    @Autowired
    private PostService postService;


    @GetMapping(Constants.Urls.API + Constants.Urls.POST)
    public Wrapper<PostDTO> getPosts(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @RequestParam(value = "searchString", required = false, defaultValue = "") String searchString,
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount // null if not authenticated
    ) {
        return postService.getPosts(page, size, searchString, userAccount);
    }

    public static class RandomPostDTO {
        private long id;
        private String title;

        public RandomPostDTO(long id, String title) {
            this.id = id;
            this.title = title;
        }

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    @GetMapping(Constants.Urls.API + Constants.Urls.POST + Constants.Urls.RANDOM)
    public List<RandomPostDTO> getRandomPosts() {

        return jdbcTemplate.query(
                "select id, title from posts.post order by random() limit 10",
                (resultSet, i) -> new RandomPostDTO(resultSet.getLong("id"), resultSet.getString("title"))
        );
    }


    @GetMapping(Constants.Urls.API + Constants.Urls.POST + Constants.Urls.POST_ID)
    public PostDTOExtended getPost(
            @PathVariable(Constants.PathVariables.POST_ID) long id,
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount // null if not authenticated
    ) {
        return postRepository
                .findById(id)
                .map(post -> postConverter.convertToDtoExtended(post, userAccount))
                .orElseThrow(() -> new DataNotFoundException("Post " + id + " not found"));
    }


    // ================================================= secured

    @PreAuthorize("@blogSecurityService.hasPostPermission(#userAccount, T(com.github.nkonev.blog.security.permissions.PostPermissions).READ_MY)")
    @GetMapping(Constants.Urls.API + Constants.Urls.POST + Constants.Urls.MY)
    public List<PostDTO> getMyPosts(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size,
            @RequestParam(value = "searchString", required = false, defaultValue = "") String searchString // TODO implement
    ) {

        PageRequest springDataPage = PageRequest.of(PageUtils.fixPage(page), PageUtils.fixSize(size));

        return postRepository
                .findMyPosts(springDataPage).getContent()
                .stream()
                .map(postService::convertToPostDTOWithCleanTags)
                .collect(Collectors.toList());
    }

    // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#el-common-built-in
    @PreAuthorize("@blogSecurityService.hasPostPermission(#userAccount, T(com.github.nkonev.blog.security.permissions.PostPermissions).CREATE)")
    @PostMapping(Constants.Urls.API + Constants.Urls.POST)
    public PostDTOWithAuthorization addPost(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // null if not authenticated
            @RequestBody @NotNull PostDTO postDTO
    ) {
        return postService.addPost(userAccount, postDTO);
    }

    @PreAuthorize("@blogSecurityService.hasPostPermission(#postDTO, #userAccount, T(com.github.nkonev.blog.security.permissions.PostPermissions).EDIT)")
    @PutMapping(Constants.Urls.API + Constants.Urls.POST)
    public PostDTOWithAuthorization updatePost(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // null if not authenticated
            @RequestBody @NotNull PostDTO postDTO
    ) {
        return postService.updatePost(userAccount, postDTO);
    }

    @PreAuthorize("@blogSecurityService.hasPostPermission(#postId, #userAccount, T(com.github.nkonev.blog.security.permissions.PostPermissions).DELETE)")
    @DeleteMapping(Constants.Urls.API + Constants.Urls.POST + Constants.Urls.POST_ID)
    public void deletePost(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // null if not authenticated
            @PathVariable(Constants.PathVariables.POST_ID) long postId
    ) {
        postService.deletePost(userAccount, postId);
    }
}
