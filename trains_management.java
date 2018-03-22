package com.example.hasee.trainsadmin.Activity;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hasee.trainsadmin.Adapter.stationsAdapter;
import com.example.hasee.trainsadmin.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;



public class trains_management extends AppCompatActivity  {
    /**
     * 控件
     */
    //列车整体
    LinearLayout train ;
    //列车各部分
    TextView trainsname,starttime,startstation,runtime,
            stationsbutton,endtime,endstation;

    ImageView starttype,endtype;
    ListView stations;
    String trainnamestr,starttimestr,starttypestr,startstationstr,runtimestr;
    String endtimestr,endtypestr,endstationstr;
    String result = "";

    //处理查询结果的句柄
    Handler handler ;

    /**
     * 车站listview数据源
     */
    JSONArray station = null;
    /**
     * 车站listview适配器
     */
    stationsAdapter madapter = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trains_management);
        init();

        /**
         * 处理返回结果
         */
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.obj!=null){
                    try {
                        JSONObject result = new JSONObject(msg.obj.toString());
                        int status = result.getInt("status");
                        if(status==0){
                             station= result.getJSONArray("stations");
                             madapter = new stationsAdapter(station,getLayoutInflater());
                             stations.setAdapter(madapter);
                             stations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    parent.getAdapter().getView(position,view,parent).setBackgroundColor(Color.RED);
                                    Bundle bundle = new Bundle();
                                    bundle.putString("TI_num",trainnamestr);
                                    bundle.putInt("position",position);
                                    bundle.putString("station",parent.getAdapter().getItem(position).toString());
                                    bundle.putInt("jsonarraylength",station.length());
                                    Intent mIntent = new Intent();
                                    mIntent.setClass(trains_management.this,modify_stations_of_train.class);
                                    mIntent.putExtras(bundle);
                                    startActivityForResult(mIntent,100);
                                }
                            });
                            stations.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                                @Override
                                public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {

                                        /**
                                         * 创建对话框
                                         */
                                        AlertDialog.Builder builder = new AlertDialog.Builder(trains_management.this);
                                        builder.setTitle("为本次列车增加停靠站点").setMessage("在本站的下一站增加站点").
                                                //取消按钮
                                                        setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                    }
                                                }).     //确定按钮
                                                setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Bundle bundle = new Bundle();
                                                bundle.putString("TI_num", trainnamestr);
                                                bundle.putInt("position", position);
                                                bundle.putString("station", parent.getAdapter().getItem(position).toString());
                                                Intent intent = new Intent(trains_management.this, add_station_of_train.class);
                                                intent.putExtras(bundle);
                                                startActivityForResult(intent, 200);
                                            }
                                        });
                                        builder.create().show();


                                    return  true;
                                }
                            });
                        }else
                            Toast.makeText(trains_management.this, "车站信息显示失败", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public void init(){
        /**
         * 初始化
         */
        train = (LinearLayout)findViewById(R.id.train);
        stations = (ListView)findViewById(R.id.stations);
        //从Fragment management_trains传来的列车信息
        result = this.getIntent().getExtras().getString("train");

        trainsname = (TextView)findViewById(R.id.trainsname);
        starttime = (TextView)findViewById(R.id.starttime);
        starttype = (ImageView) findViewById(R.id.starttype);
        startstation = (TextView)findViewById(R.id.startstation);
        runtime = (TextView)findViewById(R.id.runtime);
        stationsbutton = (TextView)findViewById(R.id.stationsbutton);
        endtime = (TextView)findViewById(R.id.endtime);
        endtype = (ImageView) findViewById(R.id.endtype);
        endstation = (TextView)findViewById(R.id.endstation);
        try {
            /**
             * 解析数据
             */
            JSONObject firstresult = new JSONObject(result);
            JSONObject Onetrain = firstresult.getJSONObject("trains");
            trainnamestr = Onetrain.getString("trainname");
            starttimestr = Onetrain.getString("starttime");
            starttypestr = Onetrain.getString("startstationtype");
            startstationstr = Onetrain.getString("startstation");
            runtimestr = Onetrain.getString("runtime");
            endtimestr = Onetrain.getString("endtime");
            endtypestr = Onetrain.getString("endstationtype");
            endstationstr = Onetrain.getString("endstation");
            setTextcolor(starttype,starttypestr);
            setTextcolor(endtype,endtypestr);
            trainsname.setText(trainnamestr);
            starttime.setText(starttimestr);
            startstation.setText(startstationstr);
            runtime.setText(runtimestr);
            endtime.setText(endtimestr);
            endstation.setText(endstationstr);

            new Thread(search_stations_by_TI_num).start();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    /**
     * 通过列车经过站点的状态，设置字体颜色
     * @param v
     * @param type
     */
    private void setTextcolor(ImageView v,String type){
        switch (type){
            case "始" :
                v.setImageResource(R.drawable.start);
                break;
            case "过":
                v.setImageResource(R.drawable.pass);
                break;
            case "终":
                v.setImageResource(R.drawable.end);
                break;
            default:
                break;
        }
    }

    Runnable search_stations_by_TI_num = new Runnable() {
        String result1 = "";
        String urlstr = "";
        String params = "";
        @Override
        public void run() {
            urlstr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
            try {
                URL url = new URL(urlstr);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                params = "head=4&TI_num="+trainnamestr;
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                //写参数
                DataOutputStream out = new DataOutputStream(http.getOutputStream());
                out.write(params.getBytes());
                //从输入流接收信息
                InputStreamReader in = new InputStreamReader(http.getInputStream());
                BufferedReader buffer = new BufferedReader(in);
                String inputline = "";
                result1 = "";
                while((inputline=buffer.readLine())!=null){
                    result1 += inputline;
                }
                in.close();
                http.disconnect();
                //发送返回的信息
                Message m = handler.obtainMessage();
                m.obj = result1;
                handler.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle;
        int position = 0;
        String arrive_time,start_time,wait_time,TS_name;
        switch (resultCode){
            case 100:
                bundle = data.getExtras();
                position = bundle.getInt("position");
                station.remove(position);
                madapter.setData(station);
                madapter.notifyDataSetChanged();
                break;
            case 200:
                bundle = data.getExtras();
                position = bundle.getInt("position");
                arrive_time = bundle.getString("arrive_time","--");
                start_time = bundle.getString("start_time","--");
                wait_time = bundle.getString("wait_time","--");
                try {
                    TS_name = station.getJSONObject(position).getString("TS_name");
                    JSONObject thisstation = new JSONObject();
                    thisstation.put("TS_name",TS_name);
                    thisstation.put("arrive_time",arrive_time);
                    thisstation.put("start_time",start_time);
                    if(wait_time.equals("null")||wait_time.equals("")||wait_time==null)
                        wait_time = "--";
                    thisstation.put("wait_time",wait_time);
                    station.put(position,thisstation);
                    madapter.setData(station);
                    madapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 300:
                bundle = data.getExtras();
                position = bundle.getInt("position");
                arrive_time = bundle.getString("arrive_time","--");
                start_time = bundle.getString("start_time","--");
                wait_time = bundle.getString("wait_time","--");

                TS_name = bundle.getString("station_name");
                try {
                    JSONObject thisstation = new JSONObject();
                    thisstation.put("TS_name",TS_name);
                    thisstation.put("arrive_time",arrive_time);
                    thisstation.put("start_time",start_time);
                    thisstation.put("wait_time",wait_time);
                    madapter.setData(station);
                    madapter.addItemAtPosition(position,thisstation);
                    madapter.notifyDataSetChanged();
                    if(position==station.length()-2){
                        Bundle mybundle = new Bundle();
                        mybundle.putString("TI_num",trainnamestr);
                        mybundle.putInt("position",position);
                        mybundle.putString("station",station.get(position).toString());
                        Log.e("station",station.get(position).toString());
                        mybundle.putInt("jsonarraylength",station.length());
                        Intent mIntent = new Intent();
                        mIntent.setClass(trains_management.this,modify_stations_of_train.class);
                        mIntent.putExtras(mybundle);
                        Toast.makeText(trains_management.this, "已增加了一个新终点站，请为上一个终点站设置出发时间", Toast.LENGTH_SHORT).show();
                        startActivity(mIntent);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                 break;
            case 400:
                finish();
                break;
            case 500:
                bundle = data.getExtras();
                String TI_num = bundle.getString("TI_num");
                trainsname.setText(TI_num);
                break;
            default :
                break;
        }
    }

    public void modify_trainInfo(View v){
        Intent intent = new Intent(trains_management.this,modifytrainsInfo.class);
        Bundle bundle = new Bundle();
        bundle.putString("TI_num",trainnamestr);
        intent.putExtras(bundle);
        startActivityForResult(intent,100);
    }

}
