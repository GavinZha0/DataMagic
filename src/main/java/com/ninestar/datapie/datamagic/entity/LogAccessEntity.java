package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.sql.Timestamp;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "log_access", schema = "datapie", catalog = "")
@Schema(description="LogAccess")
public class LogAccessEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "ts_utc")
    // define the date format to return to frontend
    //@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    private Timestamp tsUtc;

    @Basic
    @Column(name = "username")
    private String username;

    @Basic
    @Column(name = "user_id")
    private Integer userId;

    @Basic
    @Column(name = "login", nullable = false)
    private Boolean login = true;

    @Basic
    @Column(name = "ip")
    private String ip;

    @Basic
    @Column(name = "browser")
    private String browser;

    @Basic
    @Column(name = "os")
    private String os;

    @Basic
    @Column(name = "lang")
    private String lang;

    @Basic
    @Column(name = "time_zone")
    private String timeZone;

    @Basic
    @Column(name = "location")
    private String location;

    @Basic
    @Column(name = "result")
    private String result;
}
