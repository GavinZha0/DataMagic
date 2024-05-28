package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;


@Data
@EqualsAndHashCode
@Entity
@Table(name = "sys_role_menu_permit", schema = "datapie", catalog = "")
@Schema(description="SysRoleMenuPermit")
public class SysRoleMenuPermitEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Basic
    @Column(name = "menu_id", nullable = false)
    private Integer menuId;

    @Basic
    @Column(name = "permit", nullable = true)
    private Byte permit;

    @Basic
    @Column(name = "view", nullable = false)
    private Boolean view;

    @Basic
    @Column(name = "edit", nullable = false)
    private Boolean edit;

    @Basic
    @Column(name = "publish", nullable = false)
    private Boolean publish;

    @Basic
    @Column(name = "subscribe", nullable = false)
    private Boolean subscribe;

    @Basic
    @Column(name = "import", nullable = false)
    private Boolean import1;

    @Basic
    @Column(name = "export", nullable = false)
    private Boolean export;
}
