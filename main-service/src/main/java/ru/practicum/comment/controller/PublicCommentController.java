package ru.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/events/{eventId}/comments")
    public List<CommentFullDto> getComments(@PathVariable Long eventId) {
        return commentService.getPublicCommentsByEvent(eventId);
    }

    @GetMapping("/events/{eventId}/comments/{commentId}")
    public CommentFullDto getCommentById(@PathVariable Long eventId, @PathVariable Long commentId) {
        return commentService.getPublicCommentById(eventId, commentId);
    }

}