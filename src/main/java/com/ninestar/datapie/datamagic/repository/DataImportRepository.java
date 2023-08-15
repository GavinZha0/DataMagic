package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.DataImportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataImportRepository extends JpaRepository<DataImportEntity, Integer>, JpaSpecificationExecutor<DataImportEntity> {


}
