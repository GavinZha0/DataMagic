package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.AiAudioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface AiAudioRepository extends JpaRepository<AiAudioEntity, Integer>, JpaSpecificationExecutor<AiAudioEntity> {
    public List<AiAudioEntity> findByNameAndGroup(String name, String group);
    public List<AiAudioEntity> findByNameContainingOrderByIdDesc(String name);

    public List<AiAudioEntity> findByModelId(Integer modelId);
    @Query(value = "select distinct group from ai_image",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
