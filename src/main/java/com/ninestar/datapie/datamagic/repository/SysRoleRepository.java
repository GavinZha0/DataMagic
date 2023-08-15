package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.SysRoleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SysRoleRepository extends JpaRepository<SysRoleEntity, Integer>, JpaSpecificationExecutor<SysRoleEntity> {
    public Page<SysRoleEntity> findByOrgIdOrOrgIdIsNull(Integer orgId, Pageable pageable);
    public List<SysRoleEntity> findByOrgIdOrOrgIdIsNull(Integer orgId);
    public SysRoleEntity findByName(String name);
    public SysRoleEntity findByNameAndOrgId(String name, Integer orgId);
}
