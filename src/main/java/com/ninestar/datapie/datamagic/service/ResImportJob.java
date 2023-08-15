package com.ninestar.datapie.datamagic.service;

import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// This is a Xxl-Job executor
// it can be scheduled by Xxl-Job Admin

@Component
public class ResImportJob extends IJobHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void execute() throws Exception {
        logger.info("********* ResImportJob is running......");
        // Check table res_importer to get a waiting task
        // Extract data files by reading file stream
        // transform records based on config
        // load records into database
    }
}
