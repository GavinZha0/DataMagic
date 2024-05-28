package com.ninestar.datapie.datamagic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "sys_msg", schema = "datapie", catalog = "")
@Schema(description="SysMsg")
public class SysMsgEntity {

    @Id // primary key
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @CreationTimestamp
    @Column(name = "ts_utc", nullable = false)
    private Timestamp tsUtc;

    @Basic
    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Basic
    @Column(name = "msg", nullable = true, length = -1)
    private String msg;

    @Basic
    @Column(name = "tid", nullable = true)
    private Integer tid;

    @Basic
    @Column(name = "`read`", nullable = true)
    private Boolean read;


    // foreign key from_id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "from_id", referencedColumnName = "id")
    //@JsonIgnore
    private SysUserEntity fromUser;

    // foreign key user_id
    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private SysUserEntity toUser;

    // foreign key org_id
    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "org_id", referencedColumnName = "id")
    private SysOrgEntity toOrg;

    @Transient
    private String from;

    @Transient
    private String to;
}
