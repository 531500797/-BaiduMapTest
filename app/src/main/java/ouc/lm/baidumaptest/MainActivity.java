package ouc.lm.baidumaptest;

import android.app.Activity;
import android.location.Geocoder;
import android.os.Bundle;
import android.transition.CircularPropagation;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.ArcOptions;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener{
    //百度地图控件
    private MapView mMapView = null;
    //百度地图对象
    private BaiduMap bdMap;
    private Button normalMapBtn;
    private Button satelliteMapBtn;
    private Button trafficMapBtn;
    private Button heatMapBtn;
    private Button overlayBtn;
    private Button locateBtn;
    //定位模式
    private LocationMode currentMode;
    //定位图标描述
    private BitmapDescriptor currentMarker=null;
    //
    private LocationClient locClient;
    //记录是否第一次定位
    private boolean isFirstLoc=true;

    //经纬度
    private double latitude,longitude;
//    private double latitude=39.963175,longitude=116.400244;

    private float currentX;
    private Marker marker1;
    //第几个覆盖物
    private int overlayIndex=7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext,注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
       setContentView(R.layout.activity_main);
       init();
    }
    private void init() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);  //初始化放大倍数
        bdMap = mMapView.getMap();
        bdMap.setMapStatus(msu);   //设置放大倍数
        normalMapBtn = (Button) findViewById(R.id.normal_map_btn);
        satelliteMapBtn = (Button) findViewById(R.id.satellite_map_btn);
        trafficMapBtn = (Button) findViewById(R.id.traffic_map_btn);
        heatMapBtn = (Button) findViewById(R.id.heat_map_btn);
        overlayBtn = (Button) findViewById(R.id.overlay_btn);
        locateBtn=(Button)findViewById(R.id.locate_btn);

        normalMapBtn.setOnClickListener(this);
        satelliteMapBtn.setOnClickListener(this);
        trafficMapBtn.setOnClickListener(this);
        heatMapBtn.setOnClickListener(this);
        overlayBtn.setOnClickListener(this);
        locateBtn.setOnClickListener(this);

        //
        normalMapBtn.setEnabled(false);
        currentMode=LocationMode.NORMAL;
        locateBtn.setText("普通");

        //开启定位图层
        bdMap.setMyLocationEnabled(true);
        locClient=new LocationClient(this);
        locClient.registerLocationListener(locationListener);   //注册定位监听器
        LocationClientOption option=new LocationClientOption();

        option.setOpenGps(true);  //打开GPS
    //    option.setLocationMode(LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//设置定位结果类型
        option.setIsNeedAddress(true);  //返回定位结果包含地址信息
        option.setScanSpan(1000);    //设置发起请求的定位间隔
        locClient.setLocOption(option);
        locClient.start();
        //对marker覆盖物添加点击事件
        bdMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker == marker1) {
                    final LatLng latLng = marker.getPosition();
                    Toast.makeText(MainActivity.this, latLng.toString(), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        //添加点击事件
        bdMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                displayInfoWindow(latLng);
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });

        //拖拽事件
        bdMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Toast.makeText(MainActivity.this, "拖拽结束，新位置：" + marker.getPosition().latitude + "," + marker.getPosition().longitude, Toast.LENGTH_LONG).show();
                reverseGeoCode(marker.getPosition());
            }

            @Override
            public void onMarkerDragStart(Marker marker) {

            }
        });
    }
    //定位监听器
    BDLocationListener locationListener=new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation==null||bdMap==null){
                return;
            }
            //构造定位数据
            MyLocationData locationData=new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())  //accuracy精度
                    .direction(100)   //方向
                    .latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude())
                    .build();
            //设置定位数据
            bdMap.setMyLocationData(locationData);
            latitude=bdLocation.getLatitude();
            longitude=bdLocation.getLongitude();
            //第一次定位时，地图中心点显示定位的位置
            if (isFirstLoc){
                isFirstLoc=false;
                LatLng ll=new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
                //MapStatusUpdate描述地图将要发生的变化
                //MapStatusUpdateFactory生成地图将要发生的变化
                MapStatusUpdate msu=MapStatusUpdateFactory.newLatLng(ll);
                bdMap.animateMapStatus(msu);  //驱动地图情景
                Toast.makeText(getApplicationContext(),bdLocation.getAddrStr(),Toast.LENGTH_LONG).show();
            }
        }
    };
    //反地理编码得到地址信息
    private void reverseGeoCode(LatLng latLng){
        //创建地理编码检索条例
        GeoCoder geoCoder=GeoCoder.newInstance();

        OnGetGeoCoderResultListener listener=new OnGetGeoCoderResultListener() {
            //地理编码查询结果回调函数
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                if (geoCodeResult==null||geoCodeResult.error!= SearchResult.ERRORNO.NO_ERROR);{
                    //没有检测到结果
                }
            }
            //反地理编码查询结果回调函数
            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                if (reverseGeoCodeResult==null||reverseGeoCodeResult.error!=SearchResult.ERRORNO.NO_ERROR){
                    //没有检测到结果
                    Toast.makeText(MainActivity.this,"抱歉，未能查找到结果",Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(MainActivity.this,"位置："+reverseGeoCodeResult.getAddress(),Toast.LENGTH_SHORT).show();
            }
            //地理编码查询结果回调函数
        };
        //设置地理编码检索监听者
        geoCoder.setOnGetGeoCodeResultListener(listener);
        geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));
  }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.normal_map_btn:
                bdMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                normalMapBtn.setEnabled(false);
                satelliteMapBtn.setEnabled(true);
                break;
            case R.id.satellite_map_btn:
                bdMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                normalMapBtn.setEnabled(true);
                satelliteMapBtn.setEnabled(false);
                break;
            case R.id.traffic_map_btn:
                if (!bdMap.isTrafficEnabled()) {
                    bdMap.setTrafficEnabled(true);
                    trafficMapBtn.setText("关闭实时路况");
                } else {
                    bdMap.setTrafficEnabled(false);
                    trafficMapBtn.setText("打开实时路况");
                }
                break;
            case R.id.heat_map_btn:
                if (!bdMap.isBaiduHeatMapEnabled()){
                    bdMap.setBaiduHeatMapEnabled(true);
                    heatMapBtn.setText("打开热力图");
                }else {
                    bdMap.setBaiduHeatMapEnabled(false);
                    heatMapBtn.setText("关闭热力图");
                }
                break;
            case R.id.locate_btn:
                switch (currentMode){
                    case NORMAL:
                        locateBtn.setText("跟随");
                        currentMode=LocationMode.FOLLOWING;
                        break;
                    case FOLLOWING:
                        locateBtn.setText("罗盘");
                        currentMode=LocationMode.COMPASS;
                        break;
                    case COMPASS:
                        locateBtn.setText("普通");
                        currentMode=LocationMode.NORMAL;
                        break;
                }
                bdMap.setMyLocationConfigeration(new MyLocationConfiguration(currentMode,true,currentMarker));
                break;
            case R.id.overlay_btn:
               switch (overlayIndex){
                    case 0:
                        overlayBtn.setText("显示多边形覆盖物");
                        addPolygonOptions();
                      //  addMarkOverlay();
                        break;
                   case 1:
                       overlayBtn.setText("显示文字覆盖物");
                       addTextOptions();
                       break;
                   case 2:
                       overlayBtn.setText("显示地形图图层覆盖物");
                       addGroundOverlayOptions();
                       break;
                   case 3:
                       overlayBtn.setText("显示折线覆盖物");
                       addPolylineOptions();
                       break;
                   case 4:
                       overlayBtn.setText("显示圆点覆盖物");
                       addDotOptions();
                       break;
                   case 5:
                       overlayBtn.setText("显示圆(空心)覆盖物");
                       addCircleOptions();
                       break;
                   case 6:
                       overlayBtn.setText("显示弧线覆盖物");
                       addArcOptions();
                       break;
                   case 7:
                       overlayBtn.setText("显示marker覆盖物");
                       addMarkOverlay();
                       break;
                }
                overlayIndex=(overlayIndex+1)%8;
                break;
        }
    }
//    添加标注覆盖物
    private void addMarkOverlay(){
        bdMap.clear();
        //定义marker坐标点
        LatLng point=new LatLng(latitude,longitude);
        //初始化全局bitmap信息，不用时及时recycle
        //初建marker图标
        BitmapDescriptor bitmap= BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);
        //构建markerOption,用于在地图上添加marker
        OverlayOptions options=new MarkerOptions()
                .position(point)
                .icon(bitmap)
                .zIndex(9)   //设置marker所在层级
                .draggable(true);  //设置手势拖拽
        //在地图上设置marker，并显示
        marker1=(Marker)bdMap.addOverlay(options);
        //回收bitmap资源
        bitmap.recycle();
    }
    //添加多边形覆盖物
    private void addPolygonOptions(){
        bdMap.clear();
        //多边形的五个顶点
        LatLng pt1=new LatLng(latitude+0.02,longitude);
        LatLng pt2=new LatLng(latitude,longitude-0.03);
        LatLng pt3=new LatLng(latitude-0.02,longitude-0.01);
        LatLng pt4=new LatLng(latitude-0.02,longitude+0.01);
        LatLng pt5=new LatLng(latitude,longitude+0.03);
        List<LatLng> points=new ArrayList<LatLng>();
        points.add(pt1);
        points.add(pt2);
        points.add(pt3);
        points.add(pt4);
        points.add(pt5);

        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.points(points);
        polygonOptions.fillColor(0xAAFFFF00);
        polygonOptions.stroke(new Stroke(2, 0xAA00FF00));
        Overlay polygon = bdMap.addOverlay(polygonOptions);
    }
    //添加文字覆盖物
    private void addTextOptions(){
        bdMap.clear();
        LatLng latLng=new LatLng(latitude,longitude);
        TextOptions textOptions=new TextOptions();
        textOptions.bgColor(0xAAFFFF00)
                .fontSize(38)
                .fontColor(0xFFFF00FF)
                .text("我在这里！")
                .rotate(-30)
                .position(latLng);
        bdMap.addOverlay(textOptions);
    }
    //添加地形图图层覆盖物
    private void addGroundOverlayOptions(){
        bdMap.clear();
        LatLng southwest=new LatLng(latitude-0.01,longitude-0.012);//西南
        LatLng northeast=new LatLng(latitude+0.01,longitude+0.012);//东北
        LatLngBounds bounds=new LatLngBounds.Builder().include(southwest).include(northeast).build();//得到一个地理范围对象
        GroundOverlayOptions groundOverlayOptions=new GroundOverlayOptions();
        //GroundOptions
        BitmapDescriptor bitmap2=BitmapDescriptorFactory.fromResource(R.drawable.csdn_blog);
        //显示的图片
        groundOverlayOptions.image(bitmap2);
        groundOverlayOptions.positionFromBounds(bounds);//将图片显示在矩形位置上
        groundOverlayOptions.transparency(0.7f);//显示透明度
        bdMap.addOverlay(groundOverlayOptions);
    }
    //添加折线覆盖物
    private void addPolylineOptions(){
        bdMap.clear();
        //点
        LatLng pt1=new LatLng(latitude+0.01,longitude);
        LatLng pt2=new LatLng(latitude,longitude);
        LatLng pt3=new LatLng(latitude,longitude+0.01);
        LatLng pt4=new LatLng(latitude+0.01,longitude+0.01);
        List<LatLng> points=new ArrayList<LatLng>();
        points.add(pt1);
        points.add(pt2);
        points.add(pt3);
        points.add(pt4);

        PolylineOptions polylineOptions=new PolylineOptions();
        polylineOptions.points(points);
        polylineOptions.color(0xFF000000);   //折线颜色
        polylineOptions.width(4);  //折线宽
        bdMap.addOverlay(polylineOptions);
    }
    //添加圆点覆盖物
    private void addDotOptions(){
        bdMap.clear();
        DotOptions dotOptions=new DotOptions();
        dotOptions.center(new LatLng(latitude, longitude));  //设置圆心坐标
        dotOptions.color(0xFFfaa755);
        dotOptions.radius(25);
        bdMap.addOverlay(dotOptions);
    }
    //添加圆(空心)覆盖物
    private  void addCircleOptions(){
        bdMap.clear();
        CircleOptions circleOptions=new CircleOptions();
        circleOptions.center(new LatLng(latitude,longitude));//设置圆心坐标
        circleOptions.fillColor(0xfffaa775);
        circleOptions.radius(50);
        circleOptions.stroke(new Stroke(5, 0xaa00ff00));//设置边框
        bdMap.addOverlay(circleOptions);
    }
    //添加弧线覆盖物
    private void addArcOptions(){
        bdMap.clear();
        LatLng pt1=new LatLng(latitude,longitude-0.01);
        LatLng pt2=new LatLng(latitude-0.01,longitude-0.01);
        LatLng pt3=new LatLng(latitude,longitude+0.01);
        ArcOptions arcOptions=new ArcOptions();
        arcOptions.points(pt1,pt2,pt3);  //设置弧线的起点，中点，终点坐标
        arcOptions.width(5);
        arcOptions.color(0xff000000);
        bdMap.addOverlay(arcOptions);
    }
    //显示弹出窗口覆盖物
    private void displayInfoWindow(final LatLng latLng){
        //创建infowindow展示view
        Button btn=new Button(getApplicationContext());
        btn.setBackgroundResource(R.drawable.popup);
        btn.setText("点我点我");
        BitmapDescriptor bitmapDescriptor=BitmapDescriptorFactory.fromView(btn);

        //infowindow点击事件
        InfoWindow.OnInfoWindowClickListener infoWindowClickListener=new InfoWindow.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick() {
                reverseGeoCode(latLng);
                //隐藏InfoWindow
                bdMap.hideInfoWindow();
            }
        };
        //创建infoWindow
        InfoWindow infoWindow=new InfoWindow(bitmapDescriptor,latLng,-47,infoWindowClickListener);
        //显示InfoWindow
        bdMap.showInfoWindow(infoWindow);
    }
    @Override
    protected void onDestroy() {
        //退出时销毁定位
        locClient.stop();
        bdMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView=null;
        super.onDestroy();

    }
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }
}
