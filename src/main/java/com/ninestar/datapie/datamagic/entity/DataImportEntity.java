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
@Table(name = "data_import", schema = "datapie", catalog = "")
@ApiModel(value="DataImport", description="")
public class DataImportEntity {

    @Id // primary key
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "files", nullable = false, length = -1)
    private String files;

    @Basic
    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Basic
    @Column(name = "attrs", nullable = false, length = -1)
    private String attrs;

    @Basic
    @Column(name = "fields", nullable = false, length = -1)
    private String fields;

    @Basic
    @Column(name = "config", nullable = false, length = -1)
    private String config;

    @Basic
    @Column(name = "source_id", nullable = true)
    private Integer sourceId;

    @Basic
    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Basic
    @Column(name = "overwrite", nullable = true)
    private Boolean overwrite;

    @Basic
    @Column(name = "`rows`", nullable = true)
    private Integer rows;

    @Basic
    @Column(name = "records", nullable = true)
    private Integer records;

    @Basic
    @Column(name = "ftp_path", nullable = false, length = 255)
    private String ftpPath;

    @Basic
    @Column(name = "status", nullable = true, length = 16)
    private String status;

    @Basic
    @Column(name = "detail", nullable = true, length = -1)
    private String detail;

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
