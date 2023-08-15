package com.ninestar.datapie.datamagic.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;


@Data
@EqualsAndHashCode
@Entity
@Table(name = "gis_point", schema = "datapie", catalog = "")
@ApiModel(value="GisPoint", description="")
public class GisPointEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "pid", nullable = true)
    private Integer pid;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "type", nullable = true, length = 16)
    private String type;

    @Basic
    @Column(name = "code", nullable = true)
    private Integer code;

    @Basic
    @Column(name = "lng", nullable = false)
    private Float lng;

    @Basic
    @Column(name = "lat", nullable = false)
    private Float lat;
}
