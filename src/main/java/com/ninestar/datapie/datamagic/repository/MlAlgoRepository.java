package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.MlAlgoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface MlAlgoRepository extends JpaRepository<MlAlgoEntity, Integer>, JpaSpecificationExecutor<MlAlgoEntity> {
    public List<MlAlgoEntity> findByNameAndGroup(String name, String category);
    public List<MlAlgoEntity> findByNameContainingOrderByIdDesc(String name);

    @Query(value = "select distinct category from ml_algorithm",nativeQuery = true)
    public Set<Object> findDistinctGroup();
}
