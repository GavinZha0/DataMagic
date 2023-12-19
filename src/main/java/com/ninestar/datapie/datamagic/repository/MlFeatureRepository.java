package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface MlFeatureRepository extends JpaRepository<MlFeatureEntity, Integer>, JpaSpecificationExecutor<MlFeatureEntity> {
    public List<MlFeatureEntity> findByNameAndGroup(String name, String group);
    public List<MlFeatureEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct `group` from ml_feature",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
