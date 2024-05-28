package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sys_org", schema = "datapie", catalog = "")
@Schema(description="SysOrg")
public class SysOrgEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Schema(description = "org unique id")
    private Integer id;

    @Basic
    @Column(name = "pid", nullable = true)
    @Schema(description = "parent id")
    private Integer pid;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    @Schema(description = "org name")
    private String name;

    @Basic
    @Column(name = "`desc`", nullable = true, length = 128)
    @Schema(description = "description")
    private String desc;

    @Basic
    @Column(name = "logo", nullable = true, length = 255)
    @Schema(description = "org logo")
    private String logo;

    @Basic
    @Column(name = "active", nullable = false)
    @Schema(description = "active or disabled")
    private Boolean active;

    @Basic
    @Column(name = "exp_date", nullable = true)
    @Schema(description = "expiration date")
    private Date expDate;

    @Basic
    @Column(name = "deleted", nullable = false)
    @Schema(description = "org is deleted")
    private Boolean deleted;

    @Basic
    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 64)
    @Schema(description = "created by")
    private String createdBy;

    @Basic
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    @Schema(description = "created at")
    private Timestamp createdAt;

    @Basic
    @LastModifiedBy
    @Column(name = "updated_by", nullable = true, length = 64)
    @Schema(description = "updated by")
    private String updatedBy;

    @Basic
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    @Schema(description = "updated at")
    private Timestamp updatedAt;

    //comment these out because I don't want to maintain users in org
    // one (org) to many (users)
    // mappedBy points to private field 'org' in SysUserEntity
    //@OneToMany(mappedBy = "org", fetch = FetchType.EAGER)
    //private Set<SysUserEntity> users = new HashSet<SysUserEntity>();

    // for tree build
    @Transient
    private List<SysOrgEntity> children = new ArrayList<SysOrgEntity>();

    @Transient
    private Integer userCount;
}
