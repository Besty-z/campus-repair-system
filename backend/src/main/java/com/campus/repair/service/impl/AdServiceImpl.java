package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.repair.common.Constants;
import com.campus.repair.entity.AdBanner;
import com.campus.repair.mapper.AdBannerMapper;
import com.campus.repair.service.AdService;
import com.campus.repair.vo.AdBannerVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 首页广告位服务实现：查询启用状态的广告并按排序号升序返回。
 *
 * @author campus
 */
@Service
public class AdServiceImpl implements AdService {

    /** 广告位 Mapper */
    private final AdBannerMapper adBannerMapper;

    /**
     * 构造广告位服务。
     *
     * @param adBannerMapper 广告位 Mapper
     */
    public AdServiceImpl(AdBannerMapper adBannerMapper) {
        this.adBannerMapper = adBannerMapper;
    }

    /**
     * 查询启用状态的广告列表，按排序号升序。
     *
     * @return 启用广告列表
     */
    @Override
    public List<AdBannerVO> listEnabled() {
        List<AdBanner> list = adBannerMapper.selectList(new LambdaQueryWrapper<AdBanner>()
                .eq(AdBanner::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(AdBanner::getSort));
        return list.stream().map(AdBannerVO::from).collect(Collectors.toList());
    }
}
