package com.stockresearch.repository;

import com.stockresearch.domain.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCompanyIdOrderByEventDateDesc(Long companyId);

    List<Event> findAllByOrderByEventDateDesc(Pageable pageable);

    List<Event> findByTypeOrderByEventDateDesc(Event.EventType type, Pageable pageable);

    @Query("""
        SELECT e FROM Event e
        WHERE e.eventDate >= :since
        ORDER BY e.eventDate DESC
    """)
    List<Event> findSince(@Param("since") LocalDateTime since);

    @Query("""
        SELECT e FROM Event e
        WHERE e.company.id = :companyId AND e.eventDate >= :since
        ORDER BY e.eventDate DESC
    """)
    List<Event> findByCompanySince(@Param("companyId") Long companyId, @Param("since") LocalDateTime since);
}
