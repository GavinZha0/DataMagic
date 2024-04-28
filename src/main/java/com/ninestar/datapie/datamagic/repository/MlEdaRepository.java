package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlEdaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MlEdaRepository extends JpaRepository<MlEdaEntity, Integer>, JpaSpecificationExecutor<MlEdaEntity> {
    public List<MlEdaEntity> findByNameAndGroup(String name, String group);
    public List<MlEdaEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct `group` from ml_dea where 'group' is not null order by `group`",nativeQuery = true)
    public List<Object> findDistinctGroup();
}
