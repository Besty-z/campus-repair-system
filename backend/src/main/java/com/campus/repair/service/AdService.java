package com.campus.repair.service;

import com.campus.repair.vo.AdBannerVO;

import java.util.List;

/**
 * 首页广告位服务：查询启用状态的广告。
 *
 * @author campus
 */
public interface AdService {

    /**
     * 查询启用状态的广告列表，按排序号升序。
     *
     * @return 启用广告列表
     */
    List<AdBannerVO> listEnabled();
}
