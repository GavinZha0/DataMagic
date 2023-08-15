package com.ninestar.datapie.datamagic.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;


@Data
@EqualsAndHashCode
@Entity
@Table(name = "gis_ip", schema = "datapie", catalog = "")
@ApiModel(value="GisIp", description="")
public class GisIpEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    @Column(name = "ip_from")
    private Long ipFrom;

    @Basic
    @Column(name = "ip_to")
    private Long ipTo;

    @Basic
    @Column(name = "code")
    private String code;

    @Basic
    @Column(name = "country")
    private String country;
}
