package com.ninestar.datapie.datamagic.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@EqualsAndHashCode
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ml_flow", schema = "datapie", catalog = "")
@ApiModel(value="MlFlow", description="")
public class MlFlowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "pid")
    private Integer pid;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "`desc`", length = 128)
    private String desc;

    @Column(name = "`group`", length = 64)
    private String group;

    @Column(name = "config")
    private String config;

    @Column(name = "workflow")
    private String workflow;

    @Column(name = "canvas")
    private String canvas;

    @Column(name = "x6_ver", length = 8)
    private String x6Ver;

    @Column(name = "version", length = 8)
    private String version;

    @Column(name = "last_run")
    private Timestamp lastRun;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "error")
    private String error;

    @Column(name = "public", nullable = false)
    private Boolean pubFlag = false;

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
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;

}
