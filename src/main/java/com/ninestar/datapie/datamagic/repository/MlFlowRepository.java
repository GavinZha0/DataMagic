package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MlFlowRepository extends JpaRepository<MlFlowEntity, Integer>, JpaSpecificationExecutor<MlFlowEntity> {
    List<MlFlowEntity> findByNameAndGroup(String name, String category);
    List<MlFlowEntity> findByNameContainingOrderByIdDesc(String name);
    List<MlFlowEntity> findByPidAndVersionStartingWith(Integer pid, String version);

    @Query(value = "select distinct `group` from ml_flow where 'group' is not null order by `group`",nativeQuery = true)
    List<Object> findDistinctGroup();
}
