package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.GisLayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GisLayerRepository extends JpaRepository<GisLayerEntity, Integer>, JpaSpecificationExecutor<GisLayerEntity> {

}
