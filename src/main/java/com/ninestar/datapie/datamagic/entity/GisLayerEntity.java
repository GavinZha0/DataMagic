package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;


@Data
@EqualsAndHashCode
@Entity
@Table(name = "gis_layer", schema = "datapie", catalog = "")
@Schema(description="GisLayer")
public class GisLayerEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Basic
    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Basic
    @Column(name = "`group`", nullable = false, length = 64)
    private String group;

    @Basic
    @Column(name = "icon", nullable = true, length = 255)
    private String icon;

    @Basic
    @Column(name = "args", nullable = false, length = 255)
    private String args;

    @Basic
    @Column(name = "options", nullable = true, length = -1)
    private String options;
}
