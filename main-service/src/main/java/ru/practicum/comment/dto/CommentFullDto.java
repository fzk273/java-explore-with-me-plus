package ru.practicum.comment.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.comment.model.CommentState;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentFullDto {

    private Long id;

    private String text;

    private Long authorId;

    private Long eventId;

    private LocalDateTime publishedOn;

    private CommentState state;
}