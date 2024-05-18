package com.ninestar.datapie.datamagic.entity;

import io.swagger.annotations.ApiModel;
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
@Table(name = "ml_flow_history", schema = "datapie", catalog = "")
@ApiModel(value="MlFlowHistory", description="")
public class MlFlowHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "config")
    private String config;

    @Column(name = "workflow")
    private String workflow;

    @Column(name = "x6_ver", length = 8)
    private String flowVer;

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
    @JoinColumn(name = "flow_id", referencedColumnName = "id")
    private MlFlowEntity flow;

    // foreign key org_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity org;

}
