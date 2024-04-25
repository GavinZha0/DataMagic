package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MlDatasetRepository extends JpaRepository<MlDatasetEntity, Integer>, JpaSpecificationExecutor<MlDatasetEntity> {
    public List<MlDatasetEntity> findByNameAndGroup(String name, String category);
    public List<MlDatasetEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct `group` from ml_dataset where 'group' is not null order by `group`",nativeQuery = true)
    public List<Object> findDistinctGroup();

    @Query(value = "select count(id) from ml_dataset where source_id=?",nativeQuery = true)
    public Integer countBySourceId(Integer id);
}
