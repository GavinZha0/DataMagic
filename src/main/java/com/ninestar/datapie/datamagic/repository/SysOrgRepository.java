package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.SysMenuEntity;
import com.ninestar.datapie.datamagic.entity.SysOrgEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SysOrgRepository extends JpaRepository<SysOrgEntity, Integer>, JpaSpecificationExecutor<SysOrgEntity> {
    SysOrgEntity findByName(String name);
    List<SysOrgEntity> findByPid(Integer pid);
    List<SysOrgEntity> findByActive(Boolean active);
    List<SysOrgEntity> findByPidAndActive(Integer pid, Boolean active);
    List<SysOrgEntity> findByDeleted(Boolean deleted, Sort sortable);
    List<SysMenuEntity> findByActiveAndDeleted(Boolean active, Boolean deleted);
    List<SysOrgEntity> findByActiveAndDeleted(Boolean active, Boolean deleted, Sort sortable);
}
