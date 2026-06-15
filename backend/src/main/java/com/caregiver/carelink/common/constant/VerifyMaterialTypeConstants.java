package com.caregiver.carelink.common.constant;

/**
 * 护工审核材料类型
 *
 * @author CareLink
 * @since 2026-02-24
 */
public final class VerifyMaterialTypeConstants {

    /** 身份证正面 */
    public static final int TYPE_ID_CARD_FRONT = 1;
    /** 身份证反面 */
    public static final int TYPE_ID_CARD_BACK = 2;
    /** 护工资格证 */
    public static final int TYPE_QUALIFICATION_CERT = 3;
    /** 其他证明材料（可多条） */
    public static final int TYPE_OTHER = 4;

    private VerifyMaterialTypeConstants() {
    }
}
