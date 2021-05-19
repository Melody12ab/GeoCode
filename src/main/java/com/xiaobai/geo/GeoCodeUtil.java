package com.xiaobai.geo;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.davidmoten.rtree.*;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.internal.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * author: xiaobai
 * Date: 2021/5/19 3:51 下午
 * Version 1.0
 */
public class GeoCodeUtil {
    private static final int TIME_OUT = 3000;
    private static final String KEY = "1a04205ff755b2490cbfee133cf83c03";

    private static final Map<String, List<Point>> allDistrictMaps = new HashMap<>(4096);
    private static RTree<String, Rectangle> districtTree;

    private static final String rootPath = System.getProperty("user.dir");

    public static void init() {
        /**
         * 初始化R树
         */
        List<String> lines = new ArrayList<>();
        IoUtil.readUtf8Lines(GeoCodeUtil.class.getClassLoader().getResourceAsStream("districts.txt"), lines);
        for (String line : lines) {
            String adCode = line.split("--")[0];
            String district = line.split("--")[1];
            if (district.contains("|")) {
                String[] dis = district.split("\\|");
                for (String di : dis) {
                    allDistrictMaps.put(adCode, getDistricts(di));
                }
            } else {
                allDistrictMaps.put(adCode, getDistricts(district));
            }
        }
    }

    /**
     * 创建R树并序列化
     *
     * @throws IOException
     */
    private static void buildDistrictSearchRtree() throws IOException {
        RTree<String, Rectangle> cityTree = RTree.create();
        for (Map.Entry<String, List<Point>> entry : allDistrictMaps.entrySet()) {
            cityTree = cityTree.add(entry.getKey(), Util.mbr(entry.getValue()));
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Serializer<String, Rectangle> serializer = Serializers.flatBuffers().utf8();
        serializer.write(cityTree, outputStream);
        FileUtil.writeBytes(outputStream.toByteArray(), FileUtil.file(rootPath + "/src/main/resources/rtree.txt"));
    }

    /**
     * 通过经纬度查询行政区信息
     *
     * @param lon
     * @param lat
     * @return adcode|districtName
     */
    public static String getDistrictByPoint(Double lon, Double lat) {
        if (districtTree == null) {
            init();
            byte[] bytes = IoUtil.readBytes(GeoCodeUtil.class.getClassLoader().getResourceAsStream("rtree.txt"));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            Serializer<String, Rectangle> serializer = Serializers.flatBuffers().utf8();
            try {
                districtTree = serializer.read(inputStream, bytes.length, InternalStructure.DEFAULT);
            } catch (IOException e) {
            }
        }
        List<String> search = districtTree.search(Geometries.point(lon, lat)).map(Entry::value).toList().toBlocking().single();
        for (String name : search) {
            boolean inPolygon = PtInPolygon(Geometries.point(lon, lat), allDistrictMaps.get(name));
            if (inPolygon) {
                return name;
            }
        }
        return "oops! notFound";
    }

    // 功能：判断点是否在多边形内
    // 方法：求解通过该点的水平线与多边形各边的交点
    // 结论：单边交点为奇数，成立!
    // 参数：Apoints length>2
    // POINT p 指定的某个点
    // LPPOINT ptPolygon 多边形的各个顶点坐标（首末点可以不一致）
    private static boolean PtInPolygon(Point point, List<Point> APoints) {
        int nCross = 0;
        for (int i = 0; i < APoints.size(); i++) {
            Point p1 = APoints.get(i);
            Point p2 = APoints.get((i + 1) % APoints.size());
            // 是否落在某一个点上
            if (point.equals(p1) || point.equals(p2)) {
                return true;
            }
            // 求解 y=p.y 与 p1p2 的交点
            if (p1.x() == p2.x()) { // p1p2 与 y=p0.y平行
                continue;
            }
            if (point.x() < Math.min(p1.x(), p2.x())) // 交点在p1p2延长线上
                continue;
            if (point.x() >= Math.max(p1.x(), p2.x())) // 交点在p1p2延长线上
                continue;
            // 求交点的 X 坐标
            double x = (double) (point.x() - p1.x()) * (double) (p2.y() - p1.y()) / (double) (p2.x() - p1.x()) + p1.y();
            if (x > point.y())
                nCross++; // 只统计单边交点
        }
        // 单边交点为偶数，点在多边形之外 ---
        return (nCross % 2 == 1);
    }

    private static List<Point> getDistricts(String district) {
        List<Point> points = new ArrayList<>();
        String[] locations = district.split(";");
        for (String location : locations) {
            String[] parse = location.split(",");
            points.add(Geometries.point(Double.parseDouble(parse[0]), Double.parseDouble(parse[1])));
        }
        return points;
    }

    private static void driver() throws InterruptedException {
//        String fullUrl = String.format("http://restapi.amap.com/v3/config/district?keywords=%s&subdistrict=%d&key=%s&extensions=all", "中国", 3, KEY);
        String china = FileUtil.readString(FileUtil.file(rootPath + "/china.txt"), StandardCharsets.UTF_8);
        List<String> result = new ArrayList<>();
        JSONObject jsonObject = JSON.parseObject(china);
        List<String> districts = getAllAdCode(jsonObject.getJSONArray("districts"));
        int i = 1;
        for (String district : districts) {
            result.add(getDistrictByAdCode(district));
            System.out.println(i++);
            TimeUnit.MILLISECONDS.sleep(200);
        }
        FileUtil.writeLines(result, FileUtil.file(rootPath + "/districts.txt"), StandardCharsets.UTF_8);
    }

    private static List<String> getAllAdCode(JSONArray jsonArray) {
        List<String> districts = new ArrayList<>();
        if (jsonArray.size() == 0) {
            return districts;
        }
        if (!jsonArray.getJSONObject(0).getString("level").equals("district")) {
            for (int i = 0, len = jsonArray.size(); i < len; i++) {
                districts.addAll(getAllAdCode(jsonArray.getJSONObject(i).getJSONArray("districts")));
            }
        }
        for (int i = 0, len = jsonArray.size(); i < len; i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.getString("level").equals("district")) {
                districts.add(jsonObject.getString("adcode") + "|" + jsonObject.getString("name"));
            }
        }
        return districts;
    }

    private static String getDistrictByAdCode(String msg) {
        String adCode = msg.split("\\|")[0];
        String fullUrl = String.format("http://restapi.amap.com/v3/config/district?keywords=%s&subdistrict=%d&key=%s&extensions=all", adCode, 0, KEY);
        String response = HttpUtil.get(fullUrl, TIME_OUT);
        if (JSON.parseObject(response).getString("status").equals("1")) {
            return msg + "--" + JSON.parseObject(response).getJSONArray("districts").getJSONObject(0).getString("polyline");
        }
        return msg + "--失败";
    }

}

