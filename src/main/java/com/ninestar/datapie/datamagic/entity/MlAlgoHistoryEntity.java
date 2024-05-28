package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import javax.persistence.*;
import java.sql.Timestamp;

@Data
@EqualsAndHashCode
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ml_algo_history", schema = "datapie", catalog = "")
@Schema(description="MlAlgoHistory")
public class MlAlgoHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Basic
    @Column(name = "framework", length = 16)
    private String framework;

    @Basic
    @Column(name = "frame_ver", length = 8)
    private String frameVer;

    @Basic
    @Column(name = "content")
    private String content;

    @Basic
    @Column(name = "config")
    private String config;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "status")
    private Integer status;

    @Column(name = "result")
    private String result;

    @Basic
    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Basic
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    // foreign key flow_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "algo_id", referencedColumnName = "id")
    private MlAlgoEntity algo;

    // foreign key org_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;
}
