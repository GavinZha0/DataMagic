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
@Table(name = "viz_report", schema = "datapie", catalog = "")
@ApiModel(value="VizReport", description="")
public class VizReportEntity {
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
    @Column(name = "pages", nullable = false, length = -1)
    private String pages;

    @Basic
    @Column(name = "`public`", nullable = false)
    private Boolean pubFlag;

    @Basic
    @Column(name = "pub_pub", nullable = false)
    private Boolean publishPub;

    @Basic
    @Column(name = "view_ids", nullable = false, length = -1)
    private String viewIds;

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

    // setting optional to true in order to allow menu to be null
    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_id", referencedColumnName = "id") // foreign key
    private SysMenuEntity menu;

    // foreign key org_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;
}
