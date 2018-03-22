package com.example.hasee.trainsadmin.Activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class add_train extends AppCompatActivity {
    /**
     * 控件
     */
    EditText mTI_num,mcaptain_of_train,mnum_of_carriage;
    AutoCompleteTextView mstart_station,mend_station;
    static TextView mStart_time,mend_time;


    //判断弹出的是日期选择框还是时间选择框
    final static int DATE_DIALOG = 0,TIME_DIALOG = 1;

    //判断填写的是到达时间还是发车时间
    static boolean isarrive = true;
    static String arrive_timestr,left_timestr;

    //处理日期选择完毕的句柄
    static Handler handler ;
    //查询出数据库已有车站
    Handler handler_search;
    //处理添加信息的句柄
    Handler handler_add;

    String TI_numstr,captain_of_trainstr,num_of_carriagestr,start_stationstr,end_stationstr;

    TextInputLayout mTI_num_layout,msstation,mestation,m_num_of_carriage_layout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_train);
        init();

        /**
         * 日期输入完毕开始输入时间
         */
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.obj!=null){
                    String result = msg.obj.toString();
                    if(result.equals("1"))
                        myshowDialog(TIME_DIALOG);
                }
            }
        };

        /**
         * 处理查询不在该列车的站点的信息，本次查询所有站点
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
                            ArrayAdapter<String> madapter = new ArrayAdapter<String>(add_train.this,android.R.layout.simple_dropdown_item_1line,mstationslist);
                            mstart_station.setAdapter(madapter);
                            mend_station.setAdapter(madapter);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        //需要放在handler_search定义之后，不然会报handler 为空的错误
        new Thread(search_stations_not_in_this_Train).start();

        handler_add = new Handler(){
          public void handleMessage(Message msg){
              if(msg.obj!=null){
                  try {
                      JSONObject result = new JSONObject(msg.obj.toString());
                      int status = result.getInt("status");
                      if(status==0){
                          Toast.makeText(add_train.this, "添加车次信息成功", Toast.LENGTH_SHORT).show();
                          finish();
                      }
                      else if(status==1){
                          mTI_num_layout.setErrorEnabled(true);
                          mTI_num_layout.setError("该列车已存在");
                      }else Toast.makeText(add_train.this, "添加车次信息失败", Toast.LENGTH_SHORT).show();
                  } catch (JSONException e) {
                      e.printStackTrace();
                  }
              }
          }
        };
    }

    public void init(){
        /**
         * 初始化控件
         */
        mTI_num_layout = (TextInputLayout)findViewById(R.id.TI_num_layout);

        m_num_of_carriage_layout = (TextInputLayout)findViewById(R.id.num_of_carriages_layout);
        msstation = (TextInputLayout)findViewById(R.id.msstation);
        mestation = (TextInputLayout)findViewById(R.id.mestation);

        mTI_num = (EditText)findViewById(R.id.TI_num);
        mcaptain_of_train = (EditText)findViewById(R.id.captain);
        mnum_of_carriage = (EditText)findViewById(R.id.num_of_carriage);

        mstart_station = (AutoCompleteTextView)findViewById(R.id.startstation);
        mend_station = (AutoCompleteTextView)findViewById(R.id.endstation);

        mStart_time = (TextView)findViewById(R.id.start_time);
        mend_time = (TextView)findViewById(R.id.arrive_time);

        mTI_num.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                String m_TI_num = mTI_num.getText().toString();
                if(m_TI_num==null||m_TI_num.equals("")){
                    mTI_num_layout.setErrorEnabled(true);
                    mTI_num_layout.setError("列车编号不能为空");
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTI_num_layout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String m_TI_num = mTI_num.getText().toString();
                if(m_TI_num==null||m_TI_num.equals("")){
                    mTI_num_layout.setErrorEnabled(true);
                    mTI_num_layout.setError("列车编号不能为空");
                }
            }
        });

        mstart_station.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                msstation.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mend_station.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mestation.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mnum_of_carriage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                m_num_of_carriage_layout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }


    /**
     * 一些点击事件处理函数
     */
    //添加发车时间
    public void add_left_time(View v){
        isarrive = false;
        myshowDialog(DATE_DIALOG);
    }
    //添加到站时间
    public void add_arrive_time(View v){
        isarrive = true;
        myshowDialog(DATE_DIALOG);
    }
    //添加完成按钮处理事件
    public void finish(View v){
        /**
         * 得到最新的填写数据
         */
        TI_numstr = mTI_num.getText().toString();
        captain_of_trainstr = mcaptain_of_train.getText().toString();
        num_of_carriagestr = mnum_of_carriage.getText().toString();
        start_stationstr = mstart_station.getText().toString();
        end_stationstr = mend_station.getText().toString();
        arrive_timestr = mend_time.getText().toString();
        left_timestr = mStart_time.getText().toString();
        if(TI_numstr==null||TI_numstr.equals("")) {
            mTI_num_layout.setErrorEnabled(true);
            mTI_num_layout.setError("列车编号不能为空");
        }else if(start_stationstr==null||start_stationstr.equals("")){
            msstation.setErrorEnabled(true);
            msstation.setError("出发站不能为空");
        }else if(left_timestr==null||left_timestr.equals("")){
            Toast.makeText(this, "发车时间不能为空", Toast.LENGTH_SHORT).show();
        }else if(end_stationstr==null||end_stationstr.equals("")){
            mestation.setErrorEnabled(true);
            mestation.setError("到达站不能为空");
        }else if(arrive_timestr==null||arrive_timestr.equals("")){
            Toast.makeText(this, "到达时间不能为空", Toast.LENGTH_SHORT).show();
        }else if(!isnum_of_carriage_valid()){
            m_num_of_carriage_layout.setErrorEnabled(true);
            m_num_of_carriage_layout.setError("该项只能是少于四位的数字");
        }
        else new Thread(add_train).start();


    }


    /**
     * 查出不在该列车的车站的名称（本次就是查询出所有车站）
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

    Runnable add_train = new Runnable() {
            String result1 = "";
            String urlstr = "";
            String params = "";
            @Override
            public void run() {
                urlstr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
                try {
                    URL url = new URL(urlstr);
                    HttpURLConnection http = (HttpURLConnection)url.openConnection();
                    params = "head=12&TI_num=" +TI_numstr
                            +"&sstation_name=" +start_stationstr
                            +"&estation_name="+end_stationstr
                            +"&stime="+left_timestr
                            +"&etime="+arrive_timestr
                            +"&captain="+captain_of_trainstr
                            +"&num_of_carriage="+num_of_carriagestr;
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

    public boolean isnum_of_carriage_valid(){
        String num_of_carriagestr = mnum_of_carriage.getText().toString();
        Pattern p = Pattern.compile("\\d{0,3}");
        Matcher m = p.matcher(num_of_carriagestr);
        if(m.matches()) return true;
        else return false;
    }

    /**-----------------------------内部类工具，弹出日期事件选择框---------------------------------------/

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
                                mend_time.setText(arrive_timestr);
                            }
                            else {
                                left_timestr = year + "/" + monthStr + "/" + dayStr + " ";
                                mStart_time.setText(left_timestr);
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
                                mend_time.append(" "+hourofdayStr + ":"+minuteStr);
                            }
                            else {
                                mStart_time.append(" "+hourofdayStr + ":"+minuteStr);
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
