package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.VizReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VizDatareportRepository extends JpaRepository<VizReportEntity, Integer>, JpaSpecificationExecutor<VizReportEntity> {
    public List<VizReportEntity> findByNameAndGroup(String name, String category);
    public List<VizReportEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct `group` from viz_report order by `group`",nativeQuery = true)
    public List<Object> findDistinctGroup();

    @Query(value = "select count(*) from viz_report where view_ids like ? or view_ids like %?% or view_ids like %?% or view_ids like %?%",nativeQuery = true)
    public Integer countByViewId(String id1, String id2, String id3, String id4);
}
