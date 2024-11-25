package com.ninestar.datapie.datamagic.service;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.bridge.QmsgType;
import com.ninestar.datapie.datamagic.config.RedisConfig;
import com.ninestar.datapie.datamagic.consts.QmsgCode;
import com.ninestar.datapie.datamagic.entity.SysMsgEntity;
import com.ninestar.datapie.datamagic.repository.SysMsgRepository;
import com.ninestar.datapie.framework.utils.UniformResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;


@Component
public class RedisStreamListener implements StreamListener<String, MapRecord<String, String, String>> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    RedisConfig redisConfig;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SysMsgRepository sysMsgRepository;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        // receive k-value msg from redis UP-STREAM
        // msg:"{uid:3, code:4, msg: '', data:{algoId:17, experId:56, progress:88}}"
        try {
            // ack to mark done
            redisTemplate.opsForStream().acknowledge(message.getStream(),redisConfig.getConsumerGroup(), message.getId());
            JSONObject jsonMsg = new JSONObject(message.getValue().get("msg"));
            QmsgType qMsg = jsonMsg.toBean(QmsgType.class);
            logger.info(String.format("Recv msg %s from %s", qMsg.getCode(), message.getStream()));
            if(qMsg.getCode() == QmsgCode.RAY_EXPERIMENT_REPORT.getCode()){
                // experiment progress report
                JSONObject jsonData = new JSONObject(qMsg.getData());
                if(jsonData.get("progress").toString().equals("1")){
                    // experiment start
                    SysMsgEntity newEntity = new SysMsgEntity();
                    newEntity.setType("msg");
                    newEntity.setCategory("ml");
                    newEntity.setFromId(0); // background job
                    newEntity.setToId(qMsg.getUid());
                    // unique code is MsgId_ExperimentId
                    String unique_code = QmsgCode.RAY_EXPERIMENT_REPORT.getCode()+"_"+jsonData.get("experId").toString();
                    newEntity.setCode(unique_code);
                    newEntity.setContent(qMsg.getData().toString());
                    newEntity.setTid(Integer.parseInt(jsonData.get("algoId").toString()));
                    sysMsgRepository.save(newEntity);
                } else if(jsonData.get("progress").toString().equals("100")){
                    // experiment end
                    // unique code is MsgId_ExperimentId
                    String unique_code = QmsgCode.RAY_EXPERIMENT_REPORT.getCode()+"_"+jsonData.get("experId").toString();
                    // should get only one record
                    // duplicate issue should NOT happen
                    List<SysMsgEntity> targetEntity = sysMsgRepository.findByCodeOrderByTsDesc(unique_code);
                    if(!targetEntity.isEmpty()){
                        SysMsgEntity existedEntity = targetEntity.get(0);
                        existedEntity.setContent(qMsg.getData().toString());
                        sysMsgRepository.save(existedEntity);
                    }
                }
            }
            // PendingMessagesSummary ff = redisTemplate.opsForStream().pending(message.getStream(), redisConfig.getConsumerGroup());
            // logger.info(ff.toString());
        } catch (Exception e){
            logger.error(e.getMessage());
        }
    }
}
