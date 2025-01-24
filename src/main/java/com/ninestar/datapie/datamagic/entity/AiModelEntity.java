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
import java.sql.Timestamp;

@Data
@EqualsAndHashCode
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ai_model", schema = "datapie", catalog = "")
@Schema(description="AiModel")
public class AiModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;


    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "`desc`", length = 64)
    private String desc;

    @Column(name = "area", nullable = false, length = 64)
    private String area;

    @Column(name = "tags", length = 64)
    private String tags;

    @Column(name = "algo_id")
    private Integer algoId;

    @Column(name = "`schema`")
    private String schema;

    @Column(name = "transform")
    private String transform;

    @Column(name = "rate")
    private Integer rate;

    @Column(name = "price", length = 16)
    private String price;

    @Column(name = "`public`", nullable = false)
    private Boolean pubFlag = false;

    // // 0:idle; 1:serving; 2:exception; 3:unknown;
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "run_id", length = 32)
    private String runId;

    @Column(name = "version")
    private Integer version;

    @Column(name = "deploy_to", length = 64)
    private String deployTo;

    @Column(name = "endpoint", length = 64)
    private String endpoint;

    // registered by
    @Basic
    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    // registered at
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

    @Basic
    @Column(name = "deployed_by", nullable = true, length = 64)
    private String deployedBy;

    @Basic
    @Column(name = "deployed_at", nullable = true)
    private Timestamp deployedAt;

    // foreign key org_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;
}
