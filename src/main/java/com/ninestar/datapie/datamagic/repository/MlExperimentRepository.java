package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlExperimentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface MlExperimentRepository extends JpaRepository<MlExperimentEntity, Integer>, JpaSpecificationExecutor<MlExperimentEntity> {
    public List<MlExperimentEntity> findByMlIdAndUserIdOrderByStartAtDesc(Integer mlId, Integer userId);

}
