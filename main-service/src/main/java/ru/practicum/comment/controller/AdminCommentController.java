package ru.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.service.CommentService;

@RestController
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @PatchMapping("/admin/events/{eventId}/comments/{commentId}")
    public CommentFullDto patchComment(
            @PathVariable Long eventId,
            @PathVariable Long commentId,
            @RequestParam boolean published) {

        return commentService.updateCommentStatusByAdmin(eventId, commentId, published);
    }
}