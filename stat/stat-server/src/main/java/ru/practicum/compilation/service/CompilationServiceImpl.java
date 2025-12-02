package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.EventMapper;
import ru.practicum.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание подборки: {}", newCompilationDto);

        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
        }

        Compilation compilation = Compilation.builder()
                .events(events)
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .title(newCompilationDto.getTitle())
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Создана подборка с id={}", savedCompilation.getId());

        return toDto(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        log.info("Обновление подборки с id={}: {}", compId, updateRequest);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateRequest.getEvents()));
            compilation.setEvents(events);
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            compilation.setTitle(updateRequest.getTitle());
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка с id={} обновлена", compId);

        return toDto(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с id={}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка с id={} удалена", compId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        log.info("Получение подборок: pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = compilationRepository.findAllByPinned(pinned, pageable);

        return compilations.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки по id={}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        return toDto(compilation);
    }

    private CompilationDto toDto(Compilation compilation) {
        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventDtos)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}