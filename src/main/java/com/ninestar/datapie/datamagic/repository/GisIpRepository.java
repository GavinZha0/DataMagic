package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.GisIpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GisIpRepository extends JpaRepository<GisIpEntity, Integer>, JpaSpecificationExecutor<GisIpEntity> {
    public GisIpEntity findByIpFromLessThanEqualAndIpToGreaterThanEqual(Long ipFrom, Long ipTo);

}
