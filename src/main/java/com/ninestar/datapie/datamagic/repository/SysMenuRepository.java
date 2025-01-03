package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.SysMenuEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SysMenuRepository extends JpaRepository<SysMenuEntity, Integer>, JpaSpecificationExecutor<SysMenuEntity> {
    SysMenuEntity findByName(String name);
    List<SysMenuEntity> findByActiveAndDeleted(Boolean active, Boolean deleted);
    List<SysMenuEntity> findByDeleted(Boolean deleted, Sort sortable);

    List<SysMenuEntity> findByActiveAndDeleted(Boolean active, Boolean deleted, Sort sortable);

    @Query(value = "select * from sys_menu where (org_id is null or org_id=?) and active is true and deleted is false order by pos",nativeQuery = true)
    List<SysMenuEntity> findByOrgIdActiveAndDeleted(Integer orgId, Boolean active, Boolean deleted);
}
