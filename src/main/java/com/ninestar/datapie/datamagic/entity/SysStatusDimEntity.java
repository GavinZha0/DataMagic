package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Objects;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "sys_status_dim", schema = "datapie", catalog = "")
@Schema(description="SysStatusDim Object")
public class SysStatusDimEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "category", nullable = false, length = 16)
    private String category;

    @Basic
    @Column(name = "status", nullable = false, length = 16)
    private String status;
}
