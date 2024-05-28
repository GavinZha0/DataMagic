package com.ninestar.datapie.datamagic.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;


@Data
@EqualsAndHashCode
@Entity
@Table(name = "gis_ip", schema = "datapie", catalog = "")
@Schema(description="GisIp")
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
