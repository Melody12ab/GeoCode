# GeoCode

> 提供基于高德行政区数据的地理位置逆编码服务

## 使用方法

```java
/**
*region 格式为 行政区名|行政区adCode
*/
String region=GeoCodeUtil.getDistrictByPoint(lon,lat);
```

### TDOO
1. 支持更完整的行政区信息
2. 支持根据经纬度查询省、市、区县信息