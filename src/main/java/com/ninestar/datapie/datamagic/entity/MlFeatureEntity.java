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
@Table(name = "ml_feature", schema = "datapie", catalog = "")
@Schema(description="MlFeature")
public class MlFeatureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "`desc`", length = 128)
    private String desc;

    @Basic
    @Column(name = "`group`", length = 64)
    private String group;

    @Basic
    @Column(name = "type", nullable = true, length = 16)
    private String type;

    @Basic
    @Column(name = "dataset_name", nullable = true, length = 32)
    private String datasetName;

    @Basic
    @Column(name = "query", nullable = true, length = -1)
    private String query;

    @Basic
    @Column(name = "fields", nullable = false, length = -1)
    private String fields;

    @Basic
    @Column(name = "features")
    private Integer features;

    @Basic
    @Column(name = "target", nullable = true, length = 16)
    private String target;

    @Basic
    @Column(name = "rel_pair", nullable = true, length = -1)
    private String relPair;

    @Basic
    @Column(name = "`public`", nullable = false)
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

    // foreign key datasource
    @ManyToOne
    @JoinColumn(name = "source_id") // foreign key
    private DataSourceEntity datasource;

    // foreign key org_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;
}
