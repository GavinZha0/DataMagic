package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.DataSourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;

import java.util.List;

public interface DataSourceRepository extends JpaRepository<DataSourceEntity, Integer>, JpaSpecificationExecutor<DataSourceEntity> {
    DataSourceEntity findByName(String name);
    @Query(value = "select * from data_source where org_id = ? or org_id < 0",nativeQuery = true)
    List<DataSourceEntity> findByOrgId(Integer orgId);
    Page<DataSourceEntity> findByOrgId(Integer orgId, Pageable pageable);
    List<DataSourceEntity> findByNameContainingOrderByIdDesc(String name);

    Page<DataSourceEntity> findByIdOrPubFlag(Integer id, Boolean pubflag, @Nullable Specification<DataSourceEntity> spec, Pageable pageable);

    @Query(value = "select distinct `group` from data_source where org_id = ? and `group` is not null order by `group`",nativeQuery = true)
    List<Object> findDistinctGroup(Integer org);
}
