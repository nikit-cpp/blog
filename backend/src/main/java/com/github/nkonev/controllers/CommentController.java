package com.github.nkonev.controllers;

import com.github.nkonev.Constants;
import com.github.nkonev.dto.*;
import com.github.nkonev.utils.PageUtils;
import com.github.nkonev.converter.CommentConverter;
import com.github.nkonev.entity.jpa.Comment;
import com.github.nkonev.entity.jpa.UserAccount;
import com.github.nkonev.exception.BadRequestException;
import com.github.nkonev.repo.jpa.CommentRepository;
import com.github.nkonev.repo.jpa.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@RestController
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentConverter commentConverter;

    @Autowired
    private UserAccountRepository userAccountRepository;

    /**
     * List post comments
     * @param userAccount
     * @param postId
     * @return
     */
    @GetMapping(Constants.Uls.API+Constants.Uls.POST+Constants.Uls.POST_ID +Constants.Uls.COMMENT)
    public Wrapper<CommentDTOWithAuthorization> getPostComments(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // nullable
            @PathVariable(Constants.PathVariables.POST_ID) long postId,
            @RequestParam(value = "page", required=false, defaultValue = "0") int page,
            @RequestParam(value = "size", required=false, defaultValue = "0") int size
            ) {

        PageRequest springDataPage = new PageRequest(PageUtils.fixPage(page), PageUtils.fixSize(size));

        long count = commentRepository.countByPostId(postId);
        List<Comment> comments = commentRepository.findCommentByPostIdOrderByIdAsc(springDataPage, postId);

        Collection<CommentDTOWithAuthorization> commentsCollection =  comments
                .stream()
                .map(comment -> commentConverter.convertToDto(comment, userAccount))
                .collect(Collectors.toList());

        return new Wrapper<CommentDTOWithAuthorization>(commentsCollection, count);
    }

    @PreAuthorize("@blogSecurityService.hasCommentPermission(#userAccount, T(com.github.nkonev.security.permissions.CommentPermissions).CREATE)")
    @PostMapping(Constants.Uls.API+Constants.Uls.POST+Constants.Uls.POST_ID +Constants.Uls.COMMENT)
    public CommentDTOExtended addComment(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // nullable
            @PathVariable(Constants.PathVariables.POST_ID) long postId,
            @RequestBody @NotNull CommentDTO commentDTO
    ){
        Assert.notNull(userAccount, "UserAccountDetailsDTO can't be null");
        if (commentDTO.getId()!=0){
            throw new BadRequestException("id cannot be set");
        }

        long count = commentRepository.countByPostId(postId);
        Comment comment = commentConverter.convertFromDto(commentDTO, postId, null);

        UserAccount ua = userAccountRepository.findOne(userAccount.getId()); // Hibernate caches it
        Assert.notNull(ua, "User account not found");
        comment.setOwner(ua);
        Comment saved = commentRepository.save(comment);

        return commentConverter.convertToDtoExtended(saved, userAccount, count);
    }

    @PreAuthorize("@blogSecurityService.hasCommentPermission(#commentDTO, #userAccount, T(com.github.nkonev.security.permissions.CommentPermissions).EDIT)")
    @PutMapping(Constants.Uls.API+Constants.Uls.POST+Constants.Uls.POST_ID +Constants.Uls.COMMENT)
    public CommentDTOExtended updateComment (
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // nullable
            @PathVariable(Constants.PathVariables.POST_ID) long postId,
            @RequestBody @NotNull CommentDTO commentDTO
    ){
        Assert.notNull(userAccount, "UserAccountDetailsDTO can't be null");

        long count = commentRepository.countByPostId(postId);

        Comment found = commentRepository.findOne(commentDTO.getId());
        Assert.notNull(found, "Comment with id " + commentDTO.getId() + " not found");

        Comment updatedEntity = commentConverter.convertFromDto(commentDTO, 0, found);
        Comment saved = commentRepository.save(updatedEntity);

        return commentConverter.convertToDtoExtended(saved, userAccount, count);
    }

    @PreAuthorize("@blogSecurityService.hasCommentPermission(#commentId, #userAccount, T(com.github.nkonev.security.permissions.CommentPermissions).DELETE)")
    @DeleteMapping(Constants.Uls.API+Constants.Uls.POST+Constants.Uls.POST_ID +Constants.Uls.COMMENT+Constants.Uls.COMMENT_ID)
    public long deleteComment(
            @AuthenticationPrincipal UserAccountDetailsDTO userAccount, // null if not authenticated
            @PathVariable(Constants.PathVariables.POST_ID) long postId,
            @PathVariable(Constants.PathVariables.COMMENT_ID) long commentId
    ) {
        Assert.notNull(userAccount, "UserAccountDetailsDTO can't be null");
        commentRepository.delete(commentId);

        return commentRepository.countByPostId(postId);
    }
}