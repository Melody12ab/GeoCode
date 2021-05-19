package com.xiaobai;

import com.xiaobai.geo.GeoCodeUtil;
import org.junit.Before;
import org.junit.Test;

/**
 * Description:test case
 * author: xiaobai
 * Date: 2021/5/19 4:13 下午
 * Version 1.0
 */
public class GeoCodeUtilTest {

    @Before
    public void before() {
        GeoCodeUtil.init();
    }

    @Test
    public void getDistrictByPointTest() {
        System.out.println(GeoCodeUtil.getDistrictByPoint(118.234, 27.123));
    }
}
