package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import javax.persistence.*;
import java.sql.Timestamp;

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
    @Column(name = "category", nullable = false, length = 16)
    private String category;

    @Basic
    @Column(name = "from_id", nullable = true)
    private Integer fromId;

    @Basic
    @Column(name = "to_id", nullable = false)
    private Integer toId;

    @Basic
    @Column(name = "content", nullable = false, length = -1)
    private String content;

    @Basic
    @Column(name = "tid", nullable = true)
    private Integer tid;

    @Basic
    @Column(name = "read_users", nullable = true, length = -1)
    private String readUsers;

    @Transient
    private String from;

    @Transient
    private String to;
}
