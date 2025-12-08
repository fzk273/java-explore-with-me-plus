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
            LEFT JOIN FETCH e.category
            LEFT JOIN FETCH e.initiator
            WHERE
            (:userIds IS NULL OR e.initiator.id IN :userIds)
            AND (:states IS NULL OR e.state IN :states)
            AND (:categoryIds IS NULL OR e.category.id IN :categoryIds)
            AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart)
            AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)
            """)
    List<Event> findEventsWithFilters(@Param("userIds") List<Long> userIds,
                                      @Param("states") List<EventState> states,
                                      @Param("categoryIds") List<Long> categoryIds,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            LEFT JOIN FETCH e.category
            LEFT JOIN FETCH e.initiator
            WHERE e.state = 'PUBLISHED'
            AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%'))
            OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (:paid IS NULL OR e.paid = :paid)
            AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart)
            AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)
            AND (:onlyAvailable IS NULL OR :onlyAvailable = false OR
                e.participantLimit = 0 OR
                e.participantLimit > (
                  SELECT COUNT(pr) FROM Request pr
                  WHERE pr.event = e AND pr.status = 'CONFIRMED'
            ))
            """)
    List<Event> findPublishedEventsWithFilters(@Param("text") String text,
                                               @Param("categories") List<Long> categories,
                                               @Param("paid") Boolean paid,
                                               @Param("rangeStart") LocalDateTime rangeStart,
                                               @Param("rangeEnd") LocalDateTime rangeEnd,
                                               @Param("onlyAvailable") Boolean onlyAvailable,
                                               Pageable pageable);
}