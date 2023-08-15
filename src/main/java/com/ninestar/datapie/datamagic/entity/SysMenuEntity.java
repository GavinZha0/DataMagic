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
import java.util.*;

@Data
@ToString(exclude = {"reports"})
@EqualsAndHashCode(exclude = {"reports"})
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sys_menu", schema = "datapie", catalog = "")
@ApiModel(value="SysMenu", description="")
public class SysMenuEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "pid", nullable = true)
    private Integer pid;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "title", nullable = false, length = 64)
    private String title;

    @Basic
    @Column(name = "icon", nullable = true, length = 64)
    private String icon;

    @Basic
    @Column(name = "path", nullable = true, length = 64)
    private String path;
    @Basic
    @Column(name = "redirect", nullable = true, length = 64)
    private String redirect;

    @Basic
    @Column(name = "component", nullable = true, length = 64)
    private String component;

    @Basic
    @Column(name = "subreport", nullable = true)
    private Boolean subReport;

    @Basic
    @Column(name = "pos", nullable = true)
    private Integer pos;

    @Basic
    @Column(name = "active", nullable = false)
    private Boolean active;

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

    // one (menu) to many (reports)
    // mappedBy points to private field 'menu' in VizDatareportEntity
    @OneToMany(mappedBy = "menu", fetch = FetchType.EAGER)
    private Set<VizReportEntity> reports = new HashSet<VizReportEntity>();

    // for tree build
    @Transient
    private List<SysMenuEntity> children = new ArrayList<SysMenuEntity>();

    @Transient
    private Byte permit;
}
