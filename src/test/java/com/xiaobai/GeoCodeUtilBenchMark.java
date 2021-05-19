package com.xiaobai;

import com.xiaobai.geo.GeoCodeUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Description: 性能测试
 * author: xiaobai
 * Date: 2021/5/19 4:24 下午
 * Version 1.0
 */
public class GeoCodeUtilBenchMark {
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 5)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void getDistrictByPointBenchmark() {
        GeoCodeUtil.getDistrictByPoint(118.234,27.123);
    }

    /**
     * Benchmark                                         Mode  Cnt  Score   Error  Units
     * GeoCodeUtilBenchMark.getDistrictByPointBenchmark  avgt    5  0.017 ± 0.001  ms/op
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(GeoCodeUtilBenchMark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
