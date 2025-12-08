package ru.practicum.event.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndState(Long eventId, EventState state);

    List<Event> findEventsByIdIn(Collection<Long> ids);

    List<Event> findEventsByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByIdAndInitiatorId(Long eventId, Long userId);

    List<Event> findAllByInitiatorId(Long userId);

    Optional<Event> findByIdAndInitiator_Id(Long eventId, Long userId);

    @Query("""
            SELECT e FROM Event e
            WHERE
                (:userIds IS NULL OR e.initiator.id IN :userIds)
                AND (:states IS NULL OR e.state IN :states)
                AND (:categoryIds IS NULL OR e.category.id IN :categoryIds)
                AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
                AND e.eventDate <= COALESCE(:rangeEnd, e.eventDate)
            ORDER BY e.id
            """)
    List<Event> findEventsWithFilters(@Param("userIds") List<Long> userIds,
                                      @Param("states") List<EventState> states,
                                      @Param("categoryIds") List<Long> categoryIds,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE e.state = 'PUBLISHED'
                AND (
                    :text IS NULL OR LOWER(e.annotation) LIKE :text
                    OR LOWER(e.description) LIKE :text
                    )
                AND (:categories IS NULL OR e.category.id IN :categories)
                AND (:paid IS NULL OR e.paid = :paid)
                AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
                AND e.eventDate <= COALESCE(:rangeEnd, e.eventDate)
                AND (
                      :onlyAvailable IS NULL OR :onlyAvailable = false
                      OR COALESCE(e.participantLimit, 0) = 0
                      OR COALESCE(e.confirmedRequests, 0) < COALESCE(e.participantLimit, 0)
                    )
            ORDER BY e.id
            """)
    List<Event> findPublishedEventsWithFilters(@Param("text") String text,
                                               @Param("categories") List<Long> categories,
                                               @Param("paid") Boolean paid,
                                               @Param("rangeStart") LocalDateTime rangeStart,
                                               @Param("rangeEnd") LocalDateTime rangeEnd,
                                               @Param("onlyAvailable") Boolean onlyAvailable,
                                               Pageable pageable);
}