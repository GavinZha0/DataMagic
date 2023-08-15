package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.bridge.MenuPermitType;
import com.ninestar.datapie.datamagic.entity.SysRoleEntity;
import com.ninestar.datapie.datamagic.entity.SysRoleMenuPermitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SysRoleMenuPermitRepository extends JpaRepository<SysRoleMenuPermitEntity, Integer>, JpaSpecificationExecutor<SysRoleMenuPermitEntity> {
    public Set<SysRoleMenuPermitEntity> findByRoleId(Integer id);

    @Query(value = "select title, path, permit\n" +
            "       from sys_role_menu_permit p\n" +
            "       join sys_user_role x using(role_id)\n" +
            "       join sys_menu a on(p.menu_id=a.id)\n" +
            "       where user_id=? and active is true and deleted is false",nativeQuery = true)
    public List<Object[]> findPermitByUser(Integer id);

    @Query(value = "select m.id, permit\n" +
            "       from sys_role_menu_permit p\n" +
            "       join sys_user_role x using(role_id)\n" +
            "       join sys_menu m on(p.menu_id=m.id)\n" +
            "       where user_id=? and active is true and deleted is false",nativeQuery = true)
    public List<Map> findPermitByUserId(Integer id);
}
