package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.GisAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GisAreaRepository extends JpaRepository<GisAreaEntity, Integer>, JpaSpecificationExecutor<GisAreaEntity> {

}
