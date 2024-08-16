package com.ninestar.datapie.datamagic.consts;

public enum QmsgCode {

    RAY_STEP_REPORT(1),
    RAY_EPOCH_REPORT(2),
    RAY_TRIAL_REPORT(3),
    RAY_EXPERIMENT_REPORT(4),
    RAY_JOB_EXCEPTION(5);

    private Integer code;

    QmsgCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return this.code;
    }
}
