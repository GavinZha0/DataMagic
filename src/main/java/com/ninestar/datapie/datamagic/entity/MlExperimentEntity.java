package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import javax.persistence.*;
import java.sql.Timestamp;

@Data
@EqualsAndHashCode
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ml_experiment", schema = "datapie", catalog = "")
@Schema(description="MlExperiment")
public class MlExperimentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "ml_id", nullable = false)
    private Integer mlId;

    @Basic
    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "`desc`", nullable = true, length = 128)
    private String desc;

    @Basic
    @Column(name = "dataset", nullable = false)
    private String dataset;

    @Basic
    @Column(name = "algo", nullable = false)
    private String algo;

    @Basic
    @Column(name = "train", nullable = false)
    private String train;

    @Basic
    @Column(name = "trials", nullable = true)
    private String trials;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "org_id", nullable = false)
    private Integer orgId;

    @Basic
    @CreationTimestamp
    @Column(name = "start_at", nullable = false)
    private Timestamp startAt;

    @Basic
    @UpdateTimestamp
    @Column(name = "end_at", nullable = true)
    private Timestamp endAt;

    // foreign key org_id
    //@ManyToOne(optional = false, fetch = FetchType.EAGER)
    //@JoinColumn(name = "org_id", referencedColumnName = "id")
    //private SysOrgEntity org;
}
