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
@Table(name = "viz_dataset", schema = "datapie", catalog = "")
@Schema(description="VizDataset")
public class VizDatasetEntity {

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
    @Column(name = "variable", nullable = true, length = -1)
    private String variable;

    @Basic
    @Column(name = "query", nullable = false, length = -1)
    private String query;

    @Basic
    @Column(name = "final_query", nullable = true, length = -1)
    private String finalQuery;

    @Basic
    @Column(name = "field", nullable = true, length = -1)
    private String field;

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

    // foreign key datasource
    @ManyToOne
    @JoinColumn(name = "source_id") // foreign key
    private DataSourceEntity datasource;

    // foreign key org_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;

    @Transient
    private Integer useage;
}
