package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlWorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MlWorkflowRepository extends JpaRepository<MlWorkflowEntity, Integer>, JpaSpecificationExecutor<MlWorkflowEntity> {
    List<MlWorkflowEntity> findByNameAndGroup(String name, String category);
    List<MlWorkflowEntity> findByNameContainingOrderByIdDesc(String name);
    List<MlWorkflowEntity> findByVersionStartingWith(String version);

    @Query(value = "select distinct `group` from ml_workflow where 'group' is not null order by `group`",nativeQuery = true)
    List<Object> findDistinctGroup();
}
