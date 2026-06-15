package com.caregiver.carelink.common.constant;

/**
 * 护工工作状态常量
 *
 * @author CareLink
 * @since 2026-01-29
 */
public class WorkStateConstants {

    /**
     * 接单中（空闲，可以接单）
     */
    public static final Integer WORK_STATE_AVAILABLE = 1;

    /**
     * 服务中（正在服务订单，不参与派单）
     */
    public static final Integer WORK_STATE_BUSY = 2;

    /**
     * 休息中（护工主动休息，不接单）
     */
    public static final Integer WORK_STATE_REST = 3;
}
