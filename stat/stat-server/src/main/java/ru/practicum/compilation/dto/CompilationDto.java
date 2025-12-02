package ru.practicum.compilation.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    private Long id;
    private List<ru.practicum.event.dto.EventShortDto> events;
    private Boolean pinned;
    private String title;
}