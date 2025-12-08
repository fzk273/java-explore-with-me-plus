package ru.practicum.compilations.mapper;

import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationDto;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class CompilationMapper {

    public static Compilation toEntity(NewCompilationDto newDto) {
        if (newDto == null) {
            return null;
        }

        // id оставляем null — БД сгенерирует
        // events игнорируем — их добавляет сервис по newDto.getEvents()
        return Compilation.builder()
                .pinned(newDto.getPinned())
                .title(newDto.getTitle())
                .build();
    }

    public static Compilation updateFromDto(UpdateCompilationDto updDto, Compilation compilation) {
        if (updDto == null || compilation == null) {
            return compilation;
        }
        if (updDto.getPinned() != null) {
            compilation.setPinned(updDto.getPinned());
        }
        if (updDto.getTitle() != null) {
            compilation.setTitle(updDto.getTitle());
        }
        return compilation;
    }

    public static CompilationDto toDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(mapEvents(compilation.getEvents()))
                .build();
    }

    public static List<EventShortDto> mapEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        return events.stream()
                .map(EventMapper::mapToShortDto)
                .collect(Collectors.toList());
    }
}
