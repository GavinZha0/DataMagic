package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.AiVideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AiVideoRepository extends JpaRepository<AiVideoEntity, Integer>, JpaSpecificationExecutor<AiVideoEntity> {
    public List<AiVideoEntity> findByNameAndGroup(String name, String group);
    public List<AiVideoEntity> findByNameContainingOrderByIdDesc(String name);

    public List<AiVideoEntity> findByModelId(Integer modelId);
    @Query(value = "select distinct group from ai_image",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
