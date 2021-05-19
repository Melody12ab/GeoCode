package com.xiaobai.geo;

import lombok.Data;

/**
 * Description: 高德区域实体
 * author: xiaobai
 * Date: 2021/5/19 3:41 下午
 * Version 1.0
 */
@Data
public class District {
    /**
     * 城市编码
     */
    private String cityCode;

    /**
     * 区域编码
     * 街道没有独有的adcode，均继承父类（区县）的adcode
     */
    private String adCode;

    /**
     * 行政区名称
     */
    private String name;

    /**
     * 行政区边界坐标点
     * 当一个行政区范围，由完全分隔两块或者多块的地块组
     * 成，每块地的 polyline 坐标串以 | 分隔 。
     * 如北京 的 朝阳区
     */
    private String polyline;

    /**
     * 区域中心点
     * eg：110.329,34.7452
     *
     */
    private String center;

    /**
     * 行政区划级别
     * country:国家
     * province:省份（直辖市会在province和city显示）
     * city:市（直辖市会在province和city显示）
     * district:区县
     * street:街道
     */
    private String level;

    /**
     * 下级行政区列表，包含district元素
     */
    private District[] districts;
}
