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
@Table(name = "data_source", schema = "datapie", catalog = "")
@Schema(description="DataSource")
public class DataSourceEntity {

    @Id // primary key
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "`desc`", nullable = false, length = 128)
    private String desc;

    @Basic
    @Column(name = "`group`", nullable = true, length = 64)
    private String group = "default";

    @Basic
    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Basic
    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Basic
    @Column(name = "params", nullable = true, length = 255)
    private String params;

    @Basic
    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Basic
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Basic
    @Column(name = "version", nullable = true, length = 64)
    private String version;

    @Basic
    @Column(name = "`public`", nullable = false)
    private Boolean pubFlag;

    @Basic
    @Column(name = "locked_table", nullable = true, length = -1)
    private String lockedTable;

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
