package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.AiDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AiDataRepository extends JpaRepository<AiDataEntity, Integer>, JpaSpecificationExecutor<AiDataEntity> {
    public List<AiDataEntity> findByNameAndGroup(String name, String group);
    public List<AiDataEntity> findByNameContainingOrderByIdDesc(String name);

    public List<AiDataEntity> findByModelId(Integer modelId);
    @Query(value = "select distinct group from ai_image",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
