package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlAlgoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MlAlgoRepository extends JpaRepository<MlAlgoEntity, Integer>, JpaSpecificationExecutor<MlAlgoEntity> {
    public List<MlAlgoEntity> findByNameAndGroup(String name, String category);
    public List<MlAlgoEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct `group` from ml_algo where org_id=? and `group` is not null order by `group`",nativeQuery = true)
    public List<Object> findGroupsInOrgId(Integer orgId);
}
