package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "sys_user_role", schema = "datapie", catalog = "")
@Schema(description="SysUserRole")
public class SysUserRoleEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Basic
    @Column(name = "role_id", nullable = false)
    private Integer roleId;
}
