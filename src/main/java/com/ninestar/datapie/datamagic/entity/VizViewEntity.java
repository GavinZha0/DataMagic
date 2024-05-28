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
@Table(name = "viz_view", schema = "datapie", catalog = "")
@Schema(description="VizView")
public class VizViewEntity {

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
    @Column(name = "`group`", nullable = true, length = 64)
    private String group;

    @Basic
    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Basic
    @Column(name = "dim", nullable = false, length = -1)
    private String dim;

    @Basic
    @Column(name = "relation", nullable = true, length = -1)
    private String relation;

    @Basic
    @Column(name = "location", nullable = true, length = -1)
    private String location;

    @Basic
    @Column(name = "metrics", nullable = false, length = -1)
    private String metrics;

    @Basic
    @Column(name = "agg", nullable = true, length = 16)
    private String agg;

    @Basic
    @Column(name = "prec", nullable = true)
    private Integer prec;

    @Basic
    @Column(name = "filter", nullable = true, length = -1)
    private String filter;

    @Basic
    @Column(name = "sorter", nullable = true, length = -1)
    private String sorter;

    @Basic
    @Column(name = "variable", nullable = true, length = -1)
    private String variable;

    @Basic
    @Column(name = "calculation", nullable = true, length = -1)
    private String calculation;

    @Basic
    @Column(name = "model", nullable = false, length = -1)
    private String model;

    @Basic
    @Column(name = "lib_name", nullable = false, length = 16)
    private String libName;

    @Basic
    @Column(name = "lib_ver", nullable = true, length = 16)
    private String libVer;

    @Basic
    @Column(name = "lib_cfg", nullable = true, length = -1)
    private String libCfg;

    @Basic
    @Column(name = "`public`", nullable = false)
    private Boolean pubFlag;

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

    @ManyToOne
    @JoinColumn(name = "dataset_id") // foreign key
    private VizDatasetEntity dataset;

    // foreign key org_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;

    @Transient
    private Integer useage;
}
