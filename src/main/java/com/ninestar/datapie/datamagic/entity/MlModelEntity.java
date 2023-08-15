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
@Table(name = "ml_model", schema = "datapie", catalog = "")
@ApiModel(value="MLModel", description="")
public class MlModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "pid")
    private Integer pid;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "desc", nullable = true, length = 128)
    private String desc;

    @Column(name = "group", length = 64)
    private String group;

    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Column(name = "framework", length = 16)
    private String framework;

    @Column(name = "frame_ver", length = 16)
    private String frameVer;

    @Column(name = "version", length = 16)
    private String version;

    @Column(name = "content")
    private String content;

    @Column(name = "config")
    private String config;

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
}
