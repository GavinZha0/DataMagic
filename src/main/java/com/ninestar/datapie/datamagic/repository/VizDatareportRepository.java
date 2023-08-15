package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.VizReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VizDatareportRepository extends JpaRepository<VizReportEntity, Integer>, JpaSpecificationExecutor<VizReportEntity> {
    public List<VizReportEntity> findByNameAndGroup(String name, String category);
    public List<VizReportEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct `group` from viz_report order by `group`",nativeQuery = true)
    public List<Object> findDistinctGroup();
}
