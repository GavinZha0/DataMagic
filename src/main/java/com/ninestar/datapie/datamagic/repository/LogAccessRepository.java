package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.LogAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LogAccessRepository extends JpaRepository<LogAccessEntity, Integer>, JpaSpecificationExecutor<LogAccessEntity> {
    List<LogAccessEntity> findByIpAndLocationNotNull(String ip);
    @Query(value = "select a.* from log_access a join(select min(ts_utc) as ts_utc from (select * from log_access where user_id = 3 and login is true and result = 'ok' order by ts_utc desc limit 2)x)y using (ts_utc)",nativeQuery = true)
    LogAccessEntity findLatestByUserId(Integer userId);
}
