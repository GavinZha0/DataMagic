package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlFlowHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MlFlowHistoryRepository extends JpaRepository<MlFlowHistoryEntity, Integer>, JpaSpecificationExecutor<MlFlowHistoryEntity> {
    public List<MlFlowHistoryEntity> findByFlowIdAndCreatedByOrderByCreatedAtDesc(Integer flowId, String createdBy);

    public List<MlFlowHistoryEntity> findByFlowIdAndCreatedByAndStatusOrderByCreatedAtDesc(Integer flowId, String createdBy, Integer status);
}
