package com.example.hasee.trainsadmin.Activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.hasee.trainsadmin.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class add_station_of_train extends AppCompatActivity {
    /**
     * 控件
     */
    TextView TI_num;
    AutoCompleteTextView station_name;
    static TextView arrive_time;
    static TextView left_time;

    //是否已保存修改
    boolean ismodified = false;
    //是否修改过
    boolean modified = false;

    //接收的数据
    String TI_numstr;
    //插在该站点之后
    String station_namestr;
    //增加的站点名
    String this_station_namestr;
    static String arrive_timestr;
    static String left_timestr;

    //处理增加站点操作返回消息的句柄
    Handler handler_add;
    //处理日期选择完毕的句柄
    static Handler handler ;

    /**
     * 查询不在该列车的站点的名字
     */
    Handler handler_search;

    int position = 0;

    //判断弹出的是日期选择框还是时间选择框
    final static int DATE_DIALOG = 0,TIME_DIALOG = 1;

    //判断填写的是到达时间还是发车时间
    static boolean isarrive = true;

    //保存的停留时间
    String wait_timestr = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_station_of_train);
        init();

        /**
         * 处理查询不在该列车的站点的信息
         */
        handler_search = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.obj!=null){
                    try {
                        JSONObject result = new JSONObject(msg.obj.toString());
                        int status = result.getInt("status");
                        if(status==0){
                            JSONArray stations = result.getJSONArray("stations");
                            List<String> mstationslist = new ArrayList<>();
                            for(int i=0;i<stations.length();i++){
                                mstationslist.add(stations.getString(i));
                            }
                            ArrayAdapter<String> madapter = new ArrayAdapter<String>(add_station_of_train.this,android.R.layout.simple_dropdown_item_1line,mstationslist);
                            station_name.setAdapter(madapter);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(search_stations_not_in_this_Train).start();

        /**
         * 显示日期选择框完毕则选择时间选择框
         */
        handler = new Handler(){
            public void handleMessage(Message msg){
                if(msg.obj!=null){
                    String result = msg.obj.toString();
                    if(result.equals("1"))
                        myshowDialog(TIME_DIALOG);
                }
            }
        };

        handler_add = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.obj!=null){
                    try {
                        JSONObject result = new JSONObject(msg.obj.toString());
                        int status = result.getInt("status");
                        if(status==0) {
                            wait_timestr = result.getString("wait_time");
                            /**
                             * 表示修改成功
                             */
                            modified = true;
                            Toast.makeText(add_station_of_train.this, "添加成功", Toast.LENGTH_SHORT).show();
                        }
                        else if(status==1){
                            Toast.makeText(add_station_of_train.this, "添加失败", Toast.LENGTH_SHORT).show();
                            Toast.makeText(add_station_of_train.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        }
                        else Toast.makeText(add_station_of_train.this, "发车时间不能早于到达时间", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private void init(){
        /**
         * 控件初始化
         */
        TI_num = (TextView)findViewById(R.id.head_modify_station_of_train);
        station_name = (AutoCompleteTextView) findViewById(R.id.station_name);
        arrive_time = (TextView) findViewById(R.id.arrive_time);
        left_time = (TextView) findViewById(R.id.left_time);



        /**
         * 从上一个activity得到数据
         */
        Intent lastintent = this.getIntent();
        Bundle bundle = lastintent.getExtras();
        TI_numstr = bundle.getString("TI_num");
        position = bundle.getInt("position");

        try {

            JSONObject station = new JSONObject(bundle.getString("station"));
            station_namestr = station.getString("TS_name");
            TI_num.setText(TI_numstr+"增加停靠站点");


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 绑架返回键
     */
    @Override
    public void onBackPressed() {
        if(ismodified) Toast.makeText(this, "您有修改未保存，请填写完整信息，保存后再退出", Toast.LENGTH_SHORT).show();
        else  {
            if(modified) {
                this_station_namestr = station_name.getText().toString();
                arrive_timestr = arrive_time.getText().toString();
                left_timestr = left_time.getText().toString();

                Bundle bundle = new Bundle();
                bundle.putInt("position", position);
                bundle.putString("station_name",this_station_namestr);
                bundle.putString("arrive_time", arrive_timestr);
                bundle.putString("start_time", left_timestr);
                bundle.putString("wait_time", wait_timestr);
                Intent intent = new Intent(add_station_of_train.this, trains_management.class);
                intent.putExtras(bundle);
                setResult(300, intent);
            }
            super.onBackPressed();
        }
    }

    //保存修改按钮
    public void save_add(View v){
        /**
         * 标志修改已经保存
         */
        ismodified = false;
        /**
         * 得到最新的填写的内容
         */
        this_station_namestr = station_name.getText().toString();
        arrive_timestr = arrive_time.getText().toString();
        left_timestr = left_time.getText().toString();
        new Thread(modify).start();
    }

    //修改发车时间
    public void modify_left_time(View v){
        isarrive = false;
        myshowDialog(DATE_DIALOG);
        //表示信息已经修改
        ismodified = true;
    }

    //修改到达时间
    public void modify_arrive_time(View v){
        isarrive = true;
        myshowDialog(DATE_DIALOG);
        //表示信息已经修改
        ismodified = true;
    }


    /**
     * 修改操作句柄
     */
    Runnable modify = new Runnable() {
        String result1 = "";
        String urlstr = "";
        String params = "";
        @Override
        public void run() {
            urlstr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
            try {
                URL url = new URL(urlstr);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                params = "head=7&TI_num="+TI_numstr+"&TS_name="+station_namestr+
                        "&this_TS_name="+this_station_namestr
                        +"&arrive_time="+arrive_timestr+"&start_time="+left_timestr;
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
                Message m = handler_add.obtainMessage();
                m.obj = result1;
                handler_add.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    /**
     * 查出不在该列车的车站的名称
     */
    Runnable search_stations_not_in_this_Train = new Runnable() {
        String result1 = "";
        String urlstr = "";
        String params = "";
        @Override
        public void run() {
            urlstr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
            try {
                URL url = new URL(urlstr);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                params = "head=8&TI_num="+TI_numstr;
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
                Message m = handler_search.obtainMessage();
                m.obj = result1;
                handler_search.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };
    /**
     * 显示选择时间的对话框
     * @param dateDialog
     */
    private void myshowDialog(int dateDialog){
        MyDialogFragment myDialogFragment = new MyDialogFragment().newInstance(dateDialog);
        myDialogFragment.show(getFragmentManager(),"警告");
    }

    /**
     * 选择时间的对话框
     */
    public static class MyDialogFragment extends DialogFragment {
        private java.util.Calendar calendar;
        public MyDialogFragment newInstance(int title){
            MyDialogFragment myDialogFragment = new MyDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("cmd",title);
            myDialogFragment.setArguments(bundle);
            return myDialogFragment;
        }
        public Dialog onCreateDialog(Bundle savedInstanceState){
            int id = getArguments().getInt("cmd");

            Dialog dialog = null;
            switch (id){
                //日期选择框
                case DATE_DIALOG:
                    calendar= java.util.Calendar.getInstance();
                    dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            String monthStr,dayStr;
                            monthStr = (month+1)+"";
                            dayStr = dayOfMonth+"";
                            if(month+1<10) monthStr = "0"+monthStr;
                            if (dayOfMonth<10) dayStr = "0"+dayStr;
                            if(isarrive) {

                                arrive_timestr=year + "/" + monthStr + "/" + dayStr+ " ";
                                arrive_time.setText(arrive_timestr);
                            }
                            else {
                                left_timestr = year + "/" + monthStr + "/" + dayStr + " ";
                                left_time.setText(left_timestr);
                            }

                            /**
                             * 发送消息，触发时间选择框
                             */
                            Message m = handler.obtainMessage();
                            m.obj = "1";
                            handler.sendMessage(m);
                        }
                    },calendar.get(java.util.Calendar.YEAR),calendar.get(java.util.Calendar.MONTH),calendar.get(java.util.Calendar.DAY_OF_MONTH));
                    break;
                case TIME_DIALOG:
                    calendar = java.util.Calendar.getInstance();
                    dialog = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            String hourofdayStr,minuteStr;
                            hourofdayStr = hourOfDay<10?"0"+hourOfDay:hourOfDay+"";
                            minuteStr = minute<10?"0"+minute:minute+"";
                            if (isarrive) {
                                arrive_timestr =arrive_timestr+hourofdayStr + ":"+minuteStr;
                                arrive_time.append(" "+hourofdayStr + ":"+minuteStr);
                            }
                            else {
                                left_time.append(" "+hourofdayStr + ":"+minuteStr);
                                left_timestr = left_timestr+hourofdayStr + ":"+minuteStr;
                            }
                        }
                    },calendar.get(java.util.Calendar.HOUR_OF_DAY),calendar.get(java.util.Calendar.MINUTE),false);
                    break;
            }
            return dialog;
        }
    }
}
