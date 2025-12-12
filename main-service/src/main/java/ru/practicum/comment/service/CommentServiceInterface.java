package ru.practicum.comment.service;

import jakarta.validation.Valid;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdCommentDto;

import java.util.List;

public interface CommentServiceInterface {
    CommentFullDto addComment(@Valid NewCommentDto dto, Long eventId, Long userId);

    CommentFullDto updateCommentStatusByAdmin(Long eventId, Long commentId, boolean published);

    List<CommentFullDto> getPublicCommentsByEvent(Long eventId);

    CommentFullDto updateComment(Long userId, Long eventId, Long commentId, @Valid UpdCommentDto updDto);

    void deleteComment(Long userId, Long eventId, Long commentId);

    List<CommentFullDto> getAllCommentsBy(Long userId, Long eventId);
}
