package com.ninestar.datapie.datamagic.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "gis_area", schema = "datapie", catalog = "")
@ApiModel(value="GisArea", description="")
public class GisAreaEntity {
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
    @Column(name = "loc", nullable = false, length = -1)
    private String loc;
}
