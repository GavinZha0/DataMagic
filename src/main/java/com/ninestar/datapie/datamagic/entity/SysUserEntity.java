package com.ninestar.datapie.datamagic.entity;

import io.swagger.annotations.ApiModel;
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
import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@ToString(exclude = {"roles"})
@EqualsAndHashCode(exclude = {"roles"})
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sys_user", schema = "datapie", catalog = "")
@ApiModel(value="SysUser", description="")
public class SysUserEntity {

    @Id // primary key
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "password", nullable = false, length = 128)
    private String password;

    @Basic
    @Column(name = "`desc`", nullable = true, length = 64)
    private String desc;

    @Basic
    @Column(name = "realname", nullable = false, length = 64)
    private String realname;

    @Basic
    @Column(name = "email", nullable = true, length = 64)
    private String email;

    @Basic
    @Column(name = "phone", nullable = true, length = 16)
    private String phone;

    @Basic
    @Column(name = "social", nullable = true, length = 255)
    private String social;

    @Basic
    @Column(name = "avatar", nullable = true, length = 255)
    private String avatar;

    @Basic
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Basic
    @Column(name = "sms_code", nullable = true)
    private Boolean smsCode;

    @Basic
    @Column(name = "exp_date", nullable = true)
    private Date expDate;

    @Basic
    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

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
    // many (users) to one (org)
    // optional: true(default) -- org is optional, it means to left join; false -- inner join
    // FetchType: EAGER(default) -- info is loaded when initialize; LAZY -- info is loaded when query
    // CascadeType: PERSIST -- save/update recursively; REMOVE -- delete recursively
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id") // foreign key org_id
    private SysOrgEntity org;

    // link to role via the third table sys_user_role
    //maybe you will meet 'detach entity' error when CascadeType.PERSIST
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "sys_user_role", // the third table
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<SysRoleEntity> roles = new HashSet<SysRoleEntity>();


    @Transient
    private Integer oId;

    @Transient
    private String orgName;

    @Transient
    private String[] roleNames;
}
