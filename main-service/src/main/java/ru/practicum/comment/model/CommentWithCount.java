package ru.practicum.comment.model;

import lombok.Data;

@Data
public class CommentWithCount {
    private Long commentId;
    private Long commentCount;
}
