package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.SysParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysParamRepository extends JpaRepository<SysParamEntity, Integer>, JpaSpecificationExecutor<SysParamEntity> {
    public SysParamEntity findByName(String name);
    public SysParamEntity findByGroupAndName(String group, String name);
    public SysParamEntity findByModuleAndGroupAndName(String model, String group, String name);
}
