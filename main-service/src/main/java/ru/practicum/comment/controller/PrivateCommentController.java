package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping("/users/{userId}/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentFullDto addComment(@PathVariable Long userId,
                                     @PathVariable Long eventId,
                                     @Valid @RequestBody NewCommentDto dto) {
        return commentService.addComment(dto, eventId, userId);
    }

    @GetMapping("/users/{userId}/events/{eventId}/comments")
    public List<CommentFullDto> getAllCommentsBy(@PathVariable Long userId,
                                                 @PathVariable Long eventId) {
        return commentService.getAllCommentsBy(userId, eventId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}/comments/{commentId}")
    public CommentFullDto updateComment(@PathVariable Long userId,
                                        @PathVariable Long eventId,
                                        @PathVariable Long commentId,
                                        @Valid @RequestBody UpdCommentDto updDto) {
        return commentService.updateComment(userId, eventId, commentId, updDto);
    }

    @DeleteMapping("/users/{userId}/events/{eventId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long eventId,
                              @PathVariable Long commentId) {
        commentService.deleteComment(userId, eventId, commentId);
    }
}