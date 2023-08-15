package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SysUserRepository extends JpaRepository<SysUserEntity, Integer>, JpaSpecificationExecutor<SysUserEntity> {
    SysUserEntity findByName(String name);
    SysUserEntity findByRealname(String realname);
    SysUserEntity findByPhone(String phone);
    SysUserEntity findByEmail(String email);
    SysUserEntity findByNameAndOrgIdAndDeleted(String name, Integer orgId, Boolean deleted);
    List<SysUserEntity> findByOrgId(Integer orgId);

    List<SysUserEntity> findByOrgIdOrOrgIdIsNull(Integer orgId);

}
