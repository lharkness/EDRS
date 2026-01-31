package com.edrs.persistence.mapper;

import com.edrs.persistence.entity.EventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface EventLogMapper {
    
    void insert(EventLog eventLog);
    
    Optional<EventLog> findByEventId(UUID eventId);
    
    boolean existsByEventId(UUID eventId);
    
    void updateProcessed(@Param("eventId") UUID eventId, @Param("processedAt") java.time.LocalDateTime processedAt);
}
