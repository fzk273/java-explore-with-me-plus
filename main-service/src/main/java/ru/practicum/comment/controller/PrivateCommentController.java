package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/comments")
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentFullDto> addComment(@RequestBody @Valid NewCommentDto dto,
                                                     @PathVariable Long eventId,
                                                     @PathVariable Long userId) {
        return ResponseEntity.ok(commentService.addComment(dto, eventId, userId));
    }

    @GetMapping
    public ResponseEntity<List<CommentFullDto>> getAllCommentsBy(@PathVariable Long userId,
                                                                 @PathVariable Long eventId) {
        return ResponseEntity.ok(commentService.getAllCommentsBy(userId, eventId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long userId,
                                              @PathVariable Long eventId,
                                              @PathVariable Long commentId) {
        commentService.deleteComment(userId, eventId, commentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentFullDto> updateComment(@PathVariable Long userId,
                                                        @PathVariable Long eventId,
                                                        @PathVariable Long commentId,
                                                        @Valid @RequestBody UpdCommentDto updDto) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(commentService.updateComment(userId, eventId, commentId, updDto));
    }

}
