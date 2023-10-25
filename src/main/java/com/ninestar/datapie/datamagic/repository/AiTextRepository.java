package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.AiTextEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AiTextRepository extends JpaRepository<AiTextEntity, Integer>, JpaSpecificationExecutor<AiTextEntity> {
    public List<AiTextEntity> findByNameAndGroup(String name, String group);
    public List<AiTextEntity> findByNameContainingOrderByIdDesc(String name);

    public List<AiTextEntity> findByModelId(Integer modelId);
    @Query(value = "select distinct group from ai_image",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
