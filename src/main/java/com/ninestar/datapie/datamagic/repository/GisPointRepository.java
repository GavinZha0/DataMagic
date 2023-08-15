package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.GisIpEntity;
import com.ninestar.datapie.datamagic.entity.GisPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GisPointRepository extends JpaRepository<GisPointEntity, Integer>, JpaSpecificationExecutor<GisPointEntity> {

}
