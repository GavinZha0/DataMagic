package com.ninestar.datapie.datamagic.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@EqualsAndHashCode
@Entity
@Table(name = "sys_site", schema = "datapie", catalog = "")
@ApiModel(value="SysSite", description="")
public class SysSiteEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Integer id;

    @Basic
    @Column(name = "name", nullable = true, length = 64)
    public String name;

    @Basic
    @Column(name = "owner", nullable = true, length = 64)
    public String owner;

    @Basic
    @Column(name = "partner", nullable = true, length = 64)
    public String partner;

    @Basic
    @Column(name = "about", nullable = true, length = 255)
    public String about;

    @Basic
    @Column(name = "logo", nullable = true, length = 255)
    public String logo;

    @Basic
    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 64)
    public String createdBy;

    @Basic
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public Timestamp createdAt;

    @Basic
    @LastModifiedBy
    @Column(name = "updated_by", nullable = true, length = 64)
    public String updatedBy;

    @Basic
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    public Timestamp updatedAt;
}
