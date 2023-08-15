package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.LogAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface LogAccessRepository extends JpaRepository<LogAccessEntity, Integer>, JpaSpecificationExecutor<LogAccessEntity> {
    List<LogAccessEntity> findByIpAndLocationNotNull(String ip);

}
