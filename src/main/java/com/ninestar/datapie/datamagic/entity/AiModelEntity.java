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
@Table(name = "ai_model", schema = "datapie", catalog = "")
@ApiModel(value="AiModel", description="")
public class AiModelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "sid")
    private Integer sid;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "desc", length = 64)
    private String desc;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "tags", length = 64)
    private String tags;

    @Column(name = "version", nullable = false, length = 16)
    private String version;

    @Column(name = "network", length = 16)
    private String network;

    @Column(name = "framework", nullable = false, length = 16)
    private String framework;

    @Column(name = "frame_ver", length = 16)
    private String frameVer;

    @Column(name = "trainset", length = 64)
    private String trainset;

    @Column(name = "files", nullable = false)
    private String files;

    @Column(name = "input", nullable = false)
    private String input;

    @Column(name = "output", nullable = false)
    private String output;

    @Column(name = "eval")
    private String eval;

    @Column(name = "score")
    private Integer score;

    @Column(name = "price", length = 16)
    private String price;

    @Column(name = "detail")
    private String detail;

    @Column(name = "weblink", length = 64)
    private String weblink;

    @Column(name = "model_id")
    private Integer modelId;

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
