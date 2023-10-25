package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.AiImageEntity;
import com.ninestar.datapie.datamagic.entity.AiSecurityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AiSecurityRepository extends JpaRepository<AiSecurityEntity, Integer>, JpaSpecificationExecutor<AiSecurityEntity> {
    public List<AiSecurityEntity> findByNameAndGroup(String name, String group);
    public List<AiSecurityEntity> findByNameContainingOrderByIdDesc(String name);

    public List<AiSecurityEntity> findByModelId(Integer modelId);
    @Query(value = "select distinct group from ai_image",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
