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
import java.time.Instant;

@Data
@EqualsAndHashCode
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ai_image", schema = "datapie", catalog = "")
@ApiModel(value="AiImage", description="")
public class AiImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "desc", length = 64)
    private String desc;

    @Column(name = "group", length = 64)
    private String group;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "area", length = 64)
    private String area;

    @Column(name = "platform", length = 32)
    private String platform;

    @Column(name = "platform_ver", length = 16)
    private String platformVer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private AiModelEntity model;

    @Column(name = "content")
    private String content;

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
