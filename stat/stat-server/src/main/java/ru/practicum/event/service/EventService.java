package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    // Административные методы

    //Получение событий администратором с фильтрацией

    List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size);

    //Обновление события администратором

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest updateEvent);


    //Публикация события администратором

    EventFullDto publishEvent(Long eventId);


    //Отклонение события администратором

    EventFullDto rejectEvent(Long eventId);


    //Получение события администратором по ID

    EventFullDto getEventByAdmin(Long eventId);

    // Публичные методы

    //Получение опубликованных событий с фильтрацией

    List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                        String sort, Integer from, Integer size, HttpServletRequest request);


    //Получение опубликованного события по ID

    EventFullDto getEventByIdPublic(Long id, HttpServletRequest request);

    // Приватные методы (для авторизованных пользователей)

    //Создание нового события пользователем

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);


    //Получение событий пользователя

    List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size);


    //Получение конкретного события пользователя

    EventFullDto getEventByUser(Long userId, Long eventId);


    //Обновление события пользователем

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventRequest updateEvent);


    //Отмена события пользователем

    EventFullDto cancelEventByUser(Long userId, Long eventId);

    // Вспомогательные методы (для внутреннего использования)

    //Получение модели события по ID (для внутреннего использования)

    Event getEventById(Long eventId);


    //Получение модели события пользователя по ID (для внутреннего использования)

    Event getEventByUserId(Long userId, Long eventId);


    //Проверка возможности участия в событии

    boolean isEventAvailableForParticipation(Event event);


    //Получение количества подтвержденных запросов на участи
    Integer getConfirmedRequestsCount(Long eventId);
}