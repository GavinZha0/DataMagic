package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlAlgoHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MlAlgoHistoryRepository extends JpaRepository<MlAlgoHistoryEntity, Integer>, JpaSpecificationExecutor<MlAlgoHistoryEntity> {
    public List<MlAlgoHistoryEntity> findByAlgoIdAndCreatedByOrderByCreatedAtDesc(Integer alogId, String createdBy);

    public List<MlAlgoHistoryEntity> findByAlgoIdAndCreatedByAndStatusOrderByCreatedAtDesc(Integer alogId, String createdBy, Integer status);
}
