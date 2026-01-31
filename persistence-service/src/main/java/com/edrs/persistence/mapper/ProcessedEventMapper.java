package com.edrs.persistence.mapper;

import com.edrs.persistence.entity.ProcessedEvent;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ProcessedEventMapper {
    
    void insert(ProcessedEvent processedEvent);
    
    Optional<ProcessedEvent> findByEventId(UUID eventId);
    
    boolean existsByEventId(UUID eventId);
}
