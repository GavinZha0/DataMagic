package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.sql.Timestamp;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "log_action", schema = "datapie", catalog = "")
@Schema(description="LogAction")
public class LogActionEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Basic
    @Column(name = "ts_utc")
    //@JsonFormat(pattern="yyyy/MM/dd HH:mm:ss")
    private Timestamp tsUtc;
    @Basic
    @Column(name = "username")
    private String username;
    @Basic
    @Column(name = "user_id")
    private Integer userId;
    @Basic
    @Column(name = "url")
    private String url;
    @Basic
    @Column(name = "type")
    private String type;
    @Basic
    @Column(name = "module")
    private String module;
    @Basic
    @Column(name = "method")
    private String method;
    @Basic
    @Column(name = "tid")
    private Integer tid;
    @Basic
    @Column(name = "param")
    private String param;
    @Basic
    @Column(name = "result")
    private String result;
}
