package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.LogActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogActionRepository extends JpaRepository<LogActionEntity, Integer>, JpaSpecificationExecutor<LogActionEntity> {


}
