package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(message = "Annotation cannot be blank")
    @Size(min = 20, max = 2000, message = "Annotation must be between 20 and 2000 characters")
    private String annotation;

    @NotNull(message = "Category cannot be null")
    @Positive(message = "Category ID must be positive")
    private Long category;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 20, max = 7000, message = "Description must be between 20 and 7000 characters")
    private String description;

    @NotNull(message = "Event date cannot be null")
    @Future(message = "Event date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "Location cannot be null")
    private LocationDto location;

    private Boolean paid;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    private Integer participantLimit;

    private Boolean requestModeration;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    private String title;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        @NotNull(message = "Latitude cannot be null")
        private Double lat;

        @NotNull(message = "Longitude cannot be null")
        private Double lon;
    }
}