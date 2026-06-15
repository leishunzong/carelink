package com.caregiver.carelink.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.caregiver.carelink.dto.ServicePackageDTO;

import java.util.List;

import com.caregiver.carelink.entity.ServicePackage;
import com.caregiver.carelink.vo.CaregiverMyPackageVO;
import com.caregiver.carelink.vo.ServicePackageVO;
import com.caregiver.carelink.vo.ServicePackageWithStatusVO;

/**
 * 服务包服务接口
 *
 * @author CareLink
 * @since 2026-02-11
 */
public interface ServicePackageService extends IService<ServicePackage> {

    /**
     * 分页查询服务包（支持按分类、状态筛选）
     */
    IPage<ServicePackageVO> pageList(Integer category, Integer status, long current, long size);

    /**
     * 关键词搜索服务包（仅上架，FULLTEXT 名称+描述）
     * 关键词为空时按分类分页列出上架服务包
     */
    IPage<ServicePackageVO> search(String keyword, Integer category, long current, long size);

    /**
     * 热门/推荐搜索关键词（按销量取上架服务包名称，供搜索框推荐）
     */
    List<String> getHotKeywords(int limit);

    /**
     * 查询护工已准入的服务包列表（仅返回上架服务包）
     */
    List<ServicePackageVO> getCaregiverPackages(Long caregiverId);

    /**
     * 查询护工已开通的服务包列表（精简：基本信息+准入时间，供护工端「我的服务包」）
     */
    List<CaregiverMyPackageVO> getCaregiverMyPackages(Long caregiverId);

    /**
     * 根据ID查询详情（转VO）
     */
    ServicePackageVO getDetailById(Long id);

    /**
     * 新增服务包
     */
    void addPackage(ServicePackageDTO dto);

    /**
     * 更新服务包
     */
    void updatePackageById(Long id, ServicePackageDTO dto);

    /**
     * 删除服务包
     */
    boolean removePackageById(Long id);

    /**
     * 上架服务包（status=1）
     */
    void onShelf(Long id);

    /**
     * 下架服务包（status=0）
     */
    void offShelf(Long id);

    /**
     * 分页查询上架服务包，并标记当前护工的开通状态
     * <p>
     * 护工端使用：查看可开通的服务包列表时，已开通的服务包会携带开通时间
     *
     * @param caregiverId 护工ID
     * @param category    服务类型（可选，传null查全部）
     * @param current     页码
     * @param size        每页数量
     * @return 附带开通状态的服务包分页列表
     */
    IPage<ServicePackageWithStatusVO> pageListWithStatus(Long caregiverId, Integer category, long current, long size);
}
