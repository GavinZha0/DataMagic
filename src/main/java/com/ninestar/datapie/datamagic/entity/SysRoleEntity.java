package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Set;

@Data
@ToString(exclude = {"users", "permits"})
@EqualsAndHashCode(exclude = {"users", "permits"})
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sys_role", schema = "datapie", catalog = "")
@Schema(description="SysRole")
public class SysRoleEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "`desc`", nullable = true, length = 128)
    private String desc;

    @Basic
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Basic
    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Basic
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Basic
    @LastModifiedBy
    @Column(name = "updated_by", nullable = true, length = 64)
    private String updatedBy;

    @Basic
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private Timestamp updatedAt;
    
    // foreign key org_id
    @ManyToOne
    @JoinColumn(name = "org_id")
    private SysOrgEntity org;

    // user to role, many to many
    //@ManyToMany(mappedBy = "roles", fetch = FetchType.EAGER)
    @ManyToMany(targetEntity=SysUserEntity.class, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "sys_user_role",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<SysUserEntity> users;

    // role to anchor permit, many to many
    @ManyToMany(targetEntity=SysMenuEntity.class, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "sys_role_menu_permit", // the third table
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id"))
    private Set<SysMenuEntity> permits;

}
