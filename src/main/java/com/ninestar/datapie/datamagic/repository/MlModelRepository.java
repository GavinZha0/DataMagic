package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface MlModelRepository extends JpaRepository<MlModelEntity, Integer>, JpaSpecificationExecutor<MlModelEntity> {
    public List<MlModelEntity> findByNameAndGroup(String name, String category);
    public List<MlModelEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct category from ml_algorithm",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
