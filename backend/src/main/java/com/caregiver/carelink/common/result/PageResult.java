package com.caregiver.carelink.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Data
@ApiModel(description = "分页返回结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("数据列表")
    private List<T> records;

    @ApiModelProperty("总记录数")
    private Long total;

    @ApiModelProperty("每页显示条数")
    private Long size;

    @ApiModelProperty("当前页")
    private Long current;

    @ApiModelProperty("总页数")
    private Long pages;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long size, Long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = (total + size - 1) / size;
    }

    /**
     * 从MyBatis Plus的IPage对象转换
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getSize(),
                page.getCurrent()
        );
    }

    /**
     * 自定义构建
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long size, Long current) {
        return new PageResult<>(records, total, size, current);
    }
}
