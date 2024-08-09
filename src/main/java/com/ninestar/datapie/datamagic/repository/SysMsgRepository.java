package com.ninestar.datapie.datamagic.repository;

import com.ninestar.datapie.datamagic.entity.SysMsgEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SysMsgRepository extends JpaRepository<SysMsgEntity, Integer>, JpaSpecificationExecutor<SysMsgEntity> {
    @Query(value = "select * from sys_msg where id = ?", nativeQuery = true)
    Optional<SysMsgEntity> findById(Integer id);
    @Query(value = "select * from sys_msg where (to_id = ? type = 'msg') or (to_id = ? and type = 'notice') order by ts_utc desc",nativeQuery = true)
    //@Query(value = "select msg.tsUtc, msg.type, msg.fromUser.realname, msg.toUser.realname, msg.msg from SysMsgEntity msg where msg.toUser.id = ?1 or msg.toOrg.id = ?2 order by msg.tsUtc desc")
    List<SysMsgEntity> findByUserIdOrOrgIdOrderByTsUtcDesc(Integer userId, Integer orgId);
    @Query(value = "select * from sys_msg where from_id = ? order by ts_utc desc",nativeQuery = true)
    List<SysMsgEntity> findByFromIdOrderByTsUtcDesc(Integer fromId);

    @Query(value = "select * from sys_msg where type = 'notice' and to_id = ? and read_users not like '[?]' " +
            "and read_users not like '[?,%' and read_users not like '%,?,%' and read_users not like '%,?]' " +
            "order by ts_utc desc",nativeQuery = true)
    List<SysMsgEntity> findUnreadNoticeById(Integer orgId, Integer userId1, Integer userId2, Integer userId3, Integer userId4);

    @Query(value = "select * from sys_msg where type = 'msg' and to_id = ? and (read_users is null or read_users = '' or read_users = '[]') order by ts_utc desc",nativeQuery = true)
    List<SysMsgEntity> findUnreadMsgById(Integer userId);
}
