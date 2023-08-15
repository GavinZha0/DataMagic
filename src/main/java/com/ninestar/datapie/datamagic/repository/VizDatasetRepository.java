package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.VizDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VizDatasetRepository extends JpaRepository<VizDatasetEntity, Integer>, JpaSpecificationExecutor<VizDatasetEntity> {
    public List<VizDatasetEntity> findByNameAndGroup(String name, String category);
    public List<VizDatasetEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct `group` from viz_dataset where 'group' is not null order by `group`",nativeQuery = true)
    public List<Object> findDistinctGroup();

    @Query(value = "select count(id) from viz_dataset where source_id=?",nativeQuery = true)
    public Integer countBySourceId(Integer id);
}
