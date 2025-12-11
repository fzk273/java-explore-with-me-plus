package ru.practicum.comment.mapper;

import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdCommentDto;
import ru.practicum.comment.model.Comment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static Comment toEntity(NewCommentDto dto) {
        if (dto == null) {
            return null;
        }

        return Comment.builder()
                .text(dto.getText())
                .build();
    }

    public static CommentFullDto toFullDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        return CommentFullDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .eventId(comment.getEvent() != null ? comment.getEvent().getId() : null)
                .publishedOn(toLocalDateTime(comment.getPublishedOn()))
                .state(comment.getState())
                .build();
    }

    public static List<CommentFullDto> toFullDto(List<Comment> comments) {
        if (comments == null) {
            return List.of();
        }
        return comments.stream()
                .map(CommentMapper::toFullDto)
                .collect(Collectors.toList());
    }

    public static void updateFromDto(UpdCommentDto updDto, Comment comment) {
        if (updDto == null || comment == null) {
            return;
        }

        if (updDto.getText() != null) {
            comment.setText(updDto.getText());
        }
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
                : null;
    }
}

