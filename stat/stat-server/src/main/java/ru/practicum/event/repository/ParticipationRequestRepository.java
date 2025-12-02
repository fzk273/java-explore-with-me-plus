package ru.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.event.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.requester.id = :requesterId")
    List<ParticipationRequest> findByRequesterId(@Param("requesterId") Long requesterId);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.event.id = :eventId")
    List<ParticipationRequest> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.id = :id AND pr.requester.id = :requesterId")
    Optional<ParticipationRequest> findByIdAndRequesterId(@Param("id") Long id,
                                                          @Param("requesterId") Long requesterId);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId " +
            "AND pr.status = 'CONFIRMED'")
    Integer countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT pr FROM ParticipationRequest pr " +
            "LEFT JOIN FETCH pr.event e " +
            "LEFT JOIN FETCH e.category " +
            "LEFT JOIN FETCH e.initiator " +
            "LEFT JOIN FETCH pr.requester " +
            "WHERE pr.event.id = :eventId " +
            "AND pr.id IN :requestIds")
    List<ParticipationRequest> findAllByEventIdAndIdIn(@Param("eventId") Long eventId,
                                                       @Param("requestIds") List<Long> requestIds);

    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN TRUE ELSE FALSE END " +
            "FROM ParticipationRequest pr " +
            "WHERE pr.requester.id = :requesterId " +
            "AND pr.event.id = :eventId")
    boolean existsByRequesterIdAndEventId(@Param("requesterId") Long requesterId,
                                          @Param("eventId") Long eventId);


}