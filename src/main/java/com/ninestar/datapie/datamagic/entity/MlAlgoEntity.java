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
@Table(name = "ml_algo", schema = "datapie", catalog = "")
@Schema(description="MlAlgorithm")
public class MlAlgoEntity {
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
    @Column(name = "framework", length = 16)
    private String framework;

    @Basic
    @Column(name = "frame_ver", length = 8)
    private String frameVer;

    @Basic
    @Column(name = "category", nullable = true, length = 16)
    private String category;

    @Basic
    @Column(name = "algo_name", nullable = true, length = 64)
    private String algoName;

    @Basic
    @Column(name = "data_cfg")
    private String dataCfg;

    @Basic
    @Column(name = "train_cfg")
    private String trainCfg;

    @Basic
    @Column(name = "src_code")
    private String srcCode;

    @Basic
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
