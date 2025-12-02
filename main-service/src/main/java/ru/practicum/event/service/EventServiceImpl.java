package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.ParticipationRequestRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestRepository requestRepository;
    // private final StatClient statClient; // Временно закомментировано

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события для пользователя с id={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + newEventDto.getCategory() + " не найдена"));

        // Проверка даты события
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть минимум на 2 часа позже текущего времени");
        }

        Event event = Event.builder()
                .title(newEventDto.getTitle())
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiator(user)
                .createdOn(LocalDateTime.now())
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .state(Event.EventState.PENDING)
                .lat(newEventDto.getLocation().getLat())
                .lon(newEventDto.getLocation().getLon())
                .views(0L)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Создано событие с id={}", savedEvent.getId());

        return toEventFullDto(savedEvent);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        log.info("Поиск событий администратором: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<Event.EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(Event.EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.findEventsWithFilters(users, eventStates, categories, rangeStart, rangeEnd, pageable);

        return events.stream()
                .map(this::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest updateEvent) {
        log.info("Обновление события с id={} администратором", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Проверка на отрицательный лимит участников
        if (updateEvent.getParticipantLimit() != null && updateEvent.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit cannot be negative");
        }

        // Проверка даты события
        if (updateEvent.getEventDate() != null) {
            if (updateEvent.getEventDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Дата события не может быть в прошлом");
            }
        }

        updateEventFields(event, updateEvent);

        if (updateEvent.getStateAction() != null) {
            switch (updateEvent.getStateAction()) {
                case PUBLISH_EVENT:
                    if (event.getState() != Event.EventState.PENDING) {
                        throw new ConflictException("Можно публиковать только события в состоянии PENDING");
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Дата события должна быть минимум на 1 час позже текущего времени");
                    }
                    event.setState(Event.EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == Event.EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(Event.EventState.CANCELED);
                    break;
                default:
                    // Для администратора другие действия не нужны
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с id={} обновлено администратором", eventId);

        return toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventFullDto publishEvent(Long eventId) {
        log.info("Публикация события с id={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != Event.EventState.PENDING) {
            throw new ConflictException("Можно публиковать только события в состоянии PENDING");
        }

        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата события должна быть минимум на 1 час позже текущего времени");
        }

        event.setState(Event.EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());

        Event publishedEvent = eventRepository.save(event);
        log.info("Событие с id={} опубликовано", eventId);

        return toEventFullDto(publishedEvent);
    }

    @Override
    @Transactional
    public EventFullDto rejectEvent(Long eventId) {
        log.info("Отклонение события с id={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() == Event.EventState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить опубликованное событие");
        }

        event.setState(Event.EventState.CANCELED);

        Event rejectedEvent = eventRepository.save(event);
        log.info("Событие с id={} отклонено", eventId);

        return toEventFullDto(rejectedEvent);
    }

    @Override
    public EventFullDto getEventByAdmin(Long eventId) {
        log.info("Получение события с id={} администратором", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return toEventFullDto(event);
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               String sort, Integer from, Integer size, HttpServletRequest request) {
        log.info("Публичный поиск событий: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        // Валидация дат
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("End date must be after start date");
        }

        Pageable pageable;
        if ("EVENT_DATE".equals(sort)) {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").descending());
        } else if ("VIEWS".equals(sort)) {
            pageable = PageRequest.of(from / size, size, Sort.by("views").descending());
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        List<Event> events = eventRepository.findPublishedEventsWithFilters(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);

        // Отправляем статистику о запросе (для аналитики)
        // Временно закомментировано
        // if (request != null) {
        //     try {
        //         statClient.hit(request);
        //     } catch (Exception e) {
        //         log.error("Ошибка при отправке статистики: {}", e.getMessage());
        //     }
        // }

        return events.stream()
                .map(this::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto getEventByIdPublic(Long id, HttpServletRequest request) {
        log.info("Получение публичного события с id={}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + id + " не найдено");
        }

        // УВЕЛИЧИВАЕМ СЧЕТЧИК ПРОСМОТРОВ В БАЗЕ ДАННЫХ
        event.setViews(event.getViews() + 1);
        Event savedEvent = eventRepository.save(event);
        log.info("Событие с id={} просмотрено. Количество просмотров: {}", id, savedEvent.getViews());

        // Отправляем статистику о просмотре (для аналитики, но не для подсчета views)
        // Временно закомментировано
        // if (request != null) {
        //     try {
        //         statClient.hit(request);
        //     } catch (Exception e) {
        //         log.error("Ошибка при отправке статистики: {}", e.getMessage());
        //     }
        // }

        return toEventFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        log.info("Получение событий пользователя с id={}, from={}, size={}", userId, from, size);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        return events.stream()
                .map(this::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        log.info("Получение события с id={} пользователем с id={}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventRequest updateEvent) {
        log.info("Обновление события с id={} пользователем с id={}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Проверка на отрицательный лимит участников
        if (updateEvent.getParticipantLimit() != null && updateEvent.getParticipantLimit() < 0) {
            throw new BadRequestException("Participant limit cannot be negative");
        }

        if (event.getState() == Event.EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменять опубликованные события");
        }

        // Проверка даты события
        if (updateEvent.getEventDate() != null) {
            if (updateEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new BadRequestException("Дата события должна быть минимум на 2 часа позже текущего времени");
            }
        }

        updateEventFields(event, updateEvent);

        if (updateEvent.getStateAction() != null) {
            switch (updateEvent.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(Event.EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(Event.EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с id={} обновлено пользователем с id={}", eventId, userId);

        return toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional
    public EventFullDto cancelEventByUser(Long userId, Long eventId) {
        log.info("Отмена события с id={} пользователем с id={}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != Event.EventState.PENDING) {
            throw new ConflictException("Можно отменять только события в состоянии PENDING");
        }

        event.setState(Event.EventState.CANCELED);

        Event canceledEvent = eventRepository.save(event);
        log.info("Событие с id={} отменено пользователем с id={}", eventId, userId);

        return toEventFullDto(canceledEvent);
    }

    @Override
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    @Override
    public Event getEventByUserId(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    @Override
    public boolean isEventAvailableForParticipation(Event event) {
        if (event.getState() != Event.EventState.PUBLISHED) {
            return false;
        }

        if (event.getParticipantLimit() > 0) {
            Integer confirmedRequests = getConfirmedRequestsCount(event.getId());
            return confirmedRequests < event.getParticipantLimit();
        }

        return true;
    }

    @Override
    public Integer getConfirmedRequestsCount(Long eventId) {
        return requestRepository.countConfirmedRequestsByEventId(eventId);
    }

    private void updateEventFields(Event event, UpdateEventRequest updateEvent) {
        if (updateEvent.getTitle() != null && !updateEvent.getTitle().isBlank()) {
            event.setTitle(updateEvent.getTitle());
        }
        if (updateEvent.getAnnotation() != null && !updateEvent.getAnnotation().isBlank()) {
            event.setAnnotation(updateEvent.getAnnotation());
        }
        if (updateEvent.getDescription() != null && !updateEvent.getDescription().isBlank()) {
            event.setDescription(updateEvent.getDescription());
        }
        if (updateEvent.getCategory() != null) {
            Category category = categoryRepository.findById(updateEvent.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (updateEvent.getEventDate() != null) {
            event.setEventDate(updateEvent.getEventDate());
        }
        if (updateEvent.getPaid() != null) {
            event.setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (updateEvent.getRequestModeration() != null) {
            event.setRequestModeration(updateEvent.getRequestModeration());
        }
        if (updateEvent.getLocation() != null) {
            event.setLat(updateEvent.getLocation().getLat());
            event.setLon(updateEvent.getLocation().getLon());
        }
    }

    private EventFullDto toEventFullDto(Event event) {
        Integer confirmedRequests = getConfirmedRequestsCount(event.getId());

        CategoryDto categoryDto = CategoryDto.builder()
                .id(event.getCategory().getId())
                .name(event.getCategory().getName())
                .build();

        UserShortDto userShortDto = UserShortDto.builder()
                .id(event.getInitiator().getId())
                .name(event.getInitiator().getName())
                .build();

        EventFullDto.LocationDto locationDto = EventFullDto.LocationDto.builder()
                .lat(event.getLat())
                .lon(event.getLon())
                .build();

        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(categoryDto)
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .initiator(userShortDto)
                .description(event.getDescription())
                .participantLimit(event.getParticipantLimit())
                .state(event.getState())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .location(locationDto)
                .requestModeration(event.getRequestModeration())
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0)
                .views(event.getViews())
                .build();
    }

    private EventShortDto toEventShortDto(Event event) {
        Integer confirmedRequests = getConfirmedRequestsCount(event.getId());

        CategoryDto categoryDto = CategoryDto.builder()
                .id(event.getCategory().getId())
                .name(event.getCategory().getName())
                .build();

        UserShortDto userShortDto = UserShortDto.builder()
                .id(event.getInitiator().getId())
                .name(event.getInitiator().getName())
                .build();

        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(categoryDto)
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .initiator(userShortDto)
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0)
                .views(event.getViews())
                .build();
    }
}