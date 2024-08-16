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
        // receive msg from redis UP-STREAM
        logger.info(String.format("Recv %s from %s", message.getId(), message.getStream()));
        try {
            JSONObject jsonMsg = new JSONObject(message.getValue().get("msg"));
            logger.info(jsonMsg.toString());
            redisTemplate.opsForStream().acknowledge(message.getStream(),redisConfig.getConsumerGroup(), message.getId());
            QmsgType qMsg = jsonMsg.toBean(QmsgType.class);
            UniformResponse uniformMsg = qMsg.getPayload().toBean(UniformResponse.class);
            if(uniformMsg.getCode() == QmsgCode.RAY_EXPERIMENT_REPORT.getCode()){
                JSONObject jsonData = new JSONObject(uniformMsg.getData());
                if(jsonData.get("progress").toString().equals("1")){
                    SysMsgEntity newEntity = new SysMsgEntity();
                    newEntity.setType("msg");
                    newEntity.setCategory("ml");
                    newEntity.setFromId(0); // system
                    newEntity.setToId(qMsg.getUserId());
                    newEntity.setCode(QmsgCode.RAY_EXPERIMENT_REPORT.getCode()+"_"+jsonData.get("experId").toString());
                    newEntity.setContent(uniformMsg.getData().toString());
                    newEntity.setTid(Integer.parseInt(jsonData.get("algoId").toString()));
                    sysMsgRepository.save(newEntity);
                } else if(jsonData.get("progress").toString().equals("100")){
                    SysMsgEntity targetEntity = sysMsgRepository.findByCode(QmsgCode.RAY_EXPERIMENT_REPORT.getCode()+"_"+jsonData.get("experId").toString());
                    targetEntity.setContent(uniformMsg.getData().toString());
                    sysMsgRepository.save(targetEntity);
                }
            }
            // PendingMessagesSummary ff = redisTemplate.opsForStream().pending(message.getStream(), redisConfig.getConsumerGroup());
            // logger.info(ff.toString());
        } catch (Exception e){
            logger.error(e.getMessage());
        }
    }
}
