package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationDto;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.event.dao.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationService {


    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;

    private final CompilationMapper compilationMapper;

    @Transactional
    public CompilationDto create(NewCompilationDto newDto) {

        List<Event> events = this.findEventsBy(newDto.getEvents());


        Compilation compilation = compilationMapper.toEntity(newDto);
        compilation.setEvents(events);
        compilation = compilationRepository.save(compilation);


        return compilationMapper.toDto(compilation);
    }

    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationDto updDto) {

        Compilation compilation = this.findCompilationBy(compId);
        compilation = compilationMapper.updateFromDto(updDto, compilation);

        if (updDto.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(updDto.getEvents());
            compilation.setEvents(events);
        }
        compilation = compilationRepository.save(compilation);


        return compilationMapper.toDto(compilation);
    }

    @Transactional
    public void delete(Long compId) {

        compilationRepository.deleteById(compId);
    }

    public List<CompilationDto> getAllBy(Boolean pinned, Integer from, Integer size) {

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        Page<Compilation> compilations = pinned != null
                ? compilationRepository.findByPinned(pinned, pageable) : compilationRepository.findAll(pageable);

        return compilations.getContent()
                .stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    public CompilationDto getBy(Long compId) {
        Compilation compilation = this.findCompilationBy(compId);

        return compilationMapper.toDto(compilation);
    }


    private Compilation findCompilationBy(Long compId) {
        return compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Подборка не найдена"));
    }

    private List<Event> findEventsBy(Set<Long> eventsIds) {
        List<Event> events = eventRepository.findEventsByIdIn(eventsIds);

        if (events.size() != eventsIds.size()) {
            throw new NotFoundException("Некоторые события не найдены");
        }
        return events;
    }
}
