package com.example.hasee.trainsadmin.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hasee.trainsadmin.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class modifytrainsInfo extends AppCompatActivity {
    /**
     * 控件
     */
    TextView TI_num_head;
    EditText TI_num;
    static EditText captain_of_train;
    static EditText num_of_carriage;

    //是否已保存修改
    boolean ismodified = false;
    //是否修改过
    boolean modified = false;

    //接收的数据
    String TI_numstr;

    String this_TI_num;
    static String captain;
    static String num_of_carriagestr;

    //处理查询车次详细信息
    Handler handler_search;
    //处理删除操作返回消息的句柄
    Handler handler_delete;
    //处理修改操作返回消息的句柄
    Handler handler_modify;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modifytrains_info);

        /**
         * 查找列车详细信息
         */
        handler_search = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                   if(msg.obj!=null){
                       try {
                           JSONObject result = new JSONObject(msg.obj.toString());
                           int status = result.getInt("status");
                           //成功
                           if(status==0){
                                 captain = result.getString("captain_of_train");
                                 num_of_carriagestr = result.getString("num_of_carriage");
                                 TI_num.setText(TI_numstr);
                                 captain_of_train.setText(captain);
                                 num_of_carriage.setText(num_of_carriagestr);
                           }else {
                               Toast.makeText(modifytrainsInfo.this, "没有找到该列车的详细信息", Toast.LENGTH_SHORT).show();
                           }
                       } catch (JSONException e) {
                           e.printStackTrace();
                       }
                   }
            }
        };

        /**
         * 初始化
         */
        init();

        /**
         * 处理删除操作返回信息
         */
        handler_delete = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.obj!=null){
                    try {
                        JSONObject result = new JSONObject(msg.obj.toString());
                        int status = result.getInt("status");
                        if(status==0) {
                            Toast.makeText(modifytrainsInfo.this, "删除成功", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(modifytrainsInfo.this,trains_management.class);
                            setResult(400,intent);
                            finish();
                        }
                        else
                            Toast.makeText(modifytrainsInfo.this, "删除失败", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        //处理修改
        handler_modify = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.obj!=null){
                    try {
                        JSONObject result = new JSONObject(msg.obj.toString());
                        int status = result.getInt("status");
                        if(status==0) {

                            Toast.makeText(modifytrainsInfo.this, "修改成功", Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(modifytrainsInfo.this, "修改失败", Toast.LENGTH_SHORT).show();
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
        TI_num_head = (TextView)findViewById(R.id.head_modify_train);
        TI_num = (EditText)findViewById(R.id.TI_num);
        captain_of_train = (EditText)findViewById(R.id.captain_of_train);
        num_of_carriage = (EditText)findViewById(R.id.num_of_carriage);
        /**
         * 线程开启一定要在下面三个组件增加监听之前
         */
        new Thread(search_train_info).start();



        /**
         * 从上一个activity得到数据
         */
        Intent lastintent = this.getIntent();
        Bundle bundle = lastintent.getExtras();
        TI_numstr = bundle.getString("TI_num");

        TI_num_head.setText("列车"+TI_numstr+"信息管理");




    }

    /**
     * 绑架返回键
     */
    @Override
    public void onBackPressed() {
                this_TI_num = TI_num.getText().toString();
                Bundle bundle = new Bundle();
                bundle.putString("TI_num",this_TI_num);
                Intent intent = new Intent(modifytrainsInfo.this, trains_management.class);
                intent.putExtras(bundle);
                setResult(500, intent);
                super.onBackPressed();
        }

    //删除按钮
    public void delete(View v){
        this_TI_num = TI_num.getText().toString();
        // 删除列车信息
        AlertDialog.Builder build =new AlertDialog.Builder(this);
        build.setTitle("数据无价,谨慎选择此项!!!")
                .setMessage("是否确定删除列车"+this_TI_num+"的所有信息？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(delete).start();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        build.create().show();

    }
    //保存修改按钮
    public void modify(View v){
        /**
         * 标志修改已经保存
         */
        ismodified = false;
        /**
         * 得到最新的数据
         */
        this_TI_num = TI_num.getText().toString();
        captain = captain_of_train.getText().toString();
        num_of_carriagestr = num_of_carriage.getText().toString();
        new Thread(modify).start();
    }


    /**
     * 删除操作线程
     */
    Runnable delete = new Runnable() {
        String result1 = "";
        String urlstr = "";
        String params = "";
        @Override
        public void run() {
            urlstr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
            try {
                URL url = new URL(urlstr);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                params = "head=10&TI_num="+this_TI_num;
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
                Message m = handler_delete.obtainMessage();
                m.obj = result1;
                handler_delete.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };
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
                params = "head=11&TI_num="+this_TI_num+"&captain="+captain
                        +"&num_of_carriage="+num_of_carriagestr+"&old_TI_num="+TI_numstr;
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
                Message m = handler_modify.obtainMessage();
                m.obj = result1;
                handler_modify.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    Runnable search_train_info = new Runnable() {
        String result = "";
        String urlstr = "";
        String params = "";
        @Override
        public void run() {
            urlstr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
            try {
                URL url = new URL(urlstr);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                params = "head=9&TI_num="+TI_numstr;
                DataOutputStream out = new DataOutputStream(http.getOutputStream());
                out.write(params.getBytes());
                InputStreamReader in = new InputStreamReader(http.getInputStream());
                BufferedReader buff = new BufferedReader(in);
                String inputline = "";
                result = "";
                while ((inputline = buff.readLine())!=null){
                    result += inputline;
                }
                in.close();
                http.disconnect();
                Message m = handler_search.obtainMessage();
                m.obj = result;
                handler_search.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

}
