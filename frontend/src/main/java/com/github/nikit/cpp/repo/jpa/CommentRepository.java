package com.github.nikit.cpp.repo.jpa;

import com.github.nikit.cpp.entity.jpa.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findCommentByPostIdOrderByIdAsc(Pageable page, long postId);

    void deleteByPostId(long postId);
}
