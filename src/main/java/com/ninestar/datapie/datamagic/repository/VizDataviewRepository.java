package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.VizViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface VizDataviewRepository extends JpaRepository<VizViewEntity, Integer>, JpaSpecificationExecutor<VizViewEntity> {
    public List<VizViewEntity> findByNameAndGroup(String name, String category);
    public List<VizViewEntity> findByNameContainingOrderByIdDesc(String name);
    public List<VizViewEntity> findByGroupOrderByName(String group);

    @Query(value = "select distinct `group` from viz_view where org_id = ? and (`public` is true or created_by = ?) order by `group`",nativeQuery = true)
    public List<Object> findDistinctGroupByOrg(Integer orgId, String createdBy);

    @Query(value = "select distinct `group` from viz_view where `public` is true or created_by = ? order by `group`",nativeQuery = true)
    public List<Object> findAllDistinctGroup(String createdBy);

    @Query(value = "select count(id) from viz_view where dataset_id=?",nativeQuery = true)
    public Integer countByDatasetId(Integer id);
}
