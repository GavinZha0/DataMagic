package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlDatasetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MlDatasetRepository extends JpaRepository<MlDatasetEntity, Integer>, JpaSpecificationExecutor<MlDatasetEntity> {
    public List<MlDatasetEntity> findByNameAndGroup(String name, String category);
    public List<MlDatasetEntity> findByOrgId(Integer orgId);
    public List<MlDatasetEntity> findByNameContainingOrderByIdDesc(String name);
    @Query(value = "select distinct name from ml_dataset where (org_id = ? and 'group'=? and `public` is true) or (created_by=?) order by name",nativeQuery = true)
    public List<Object> findPublicDatasetInGroup(Integer org, String group, String createdBy);
    @Query(value = "select distinct name from ml_dataset where created_by=? and `group`=? order by name",nativeQuery = true)
    public List<Object> findCreatedMeInGroup(String createdBy, String group);

    @Query(value = "select distinct `group` from ml_dataset where `group` is not null order by `group`",nativeQuery = true)
    public List<Object> findDistinctGroup();

    @Query(value = "select count(id) from ml_dataset where source_id=?",nativeQuery = true)
    public Integer countBySourceId(Integer id);


}
