package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;
import java.time.LocalDateTime;

@Data
public class EventShortDto {

    private String annotation;

    private CategoryDto category;

    private Long confirmedRequests;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private Long id;

    private UserShortDto initiator;

    private Boolean paid;

    private String title;

    private Long views;
}