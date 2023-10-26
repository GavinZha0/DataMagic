package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.AiModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AiModelRepository extends JpaRepository<AiModelEntity, Integer>, JpaSpecificationExecutor<AiModelEntity> {
    public List<AiModelEntity> findByNameAndCategoryAndType(String name, String category, String type);
    public List<AiModelEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct category from ai_image",nativeQuery = true)
    public Set<Object> findDistinctCategory();
}
