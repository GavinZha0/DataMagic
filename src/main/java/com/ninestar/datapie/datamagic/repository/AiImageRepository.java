package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.AiImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AiImageRepository extends JpaRepository<AiImageEntity, Integer>, JpaSpecificationExecutor<AiImageEntity> {
    public List<AiImageEntity> findByNameAndGroup(String name, String group);
    public List<AiImageEntity> findByNameContainingOrderByIdDesc(String name);

    public List<AiImageEntity> findByModelId(Integer modelId);
    @Query(value = "select distinct `group` from ai_image",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
