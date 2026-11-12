package com.campus.repair.controller;

import com.campus.repair.common.Result;
import com.campus.repair.service.AdService;
import com.campus.repair.vo.AdBannerVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页广告位接口：查询启用状态的广告（按排序号升序）。
 *
 * @author campus
 */
@RestController
@RequestMapping("/ads")
public class AdController {

    /** 广告位服务 */
    private final AdService adService;

    /**
     * 构造广告控制器。
     *
     * @param adService 广告位服务
     */
    public AdController(AdService adService) {
        this.adService = adService;
    }

    /**
     * 查询启用状态的广告列表（按排序号升序）。
     *
     * @return 启用广告列表
     */
    @GetMapping
    public Result<List<AdBannerVO>> listEnabled() {
        return Result.success(adService.listEnabled());
    }
}
