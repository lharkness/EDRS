package com.edrs.persistence.mapper;

import com.edrs.persistence.entity.InventoryItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryItemMapper {
    
    void insert(InventoryItem item);
    
    InventoryItem findById(String id);
    
    void update(InventoryItem item);
    
    boolean existsById(String id);
}
