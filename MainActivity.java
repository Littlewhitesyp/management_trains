package com.example.hasee.trainsadmin.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.hasee.trainsadmin.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

import static com.example.hasee.trainsadmin.R.drawable.pass;

public class MainActivity extends AppCompatActivity{
    /**
     * 控件
     */
    ProgressBar mProgressbar;
    AutoCompleteTextView maccount;
    EditText mpassword;

    /**
     * 账号/密码
     */
    String accountstr = "";
    String passwordstr = "";

    String countystr = "";   //电话国家码
    
    //处理返回结果
    Handler handler = null;

    /**
     * 修改密码
     */
    Handler handle_modify_password;

    TextInputLayout account,password_layout;

    /**
     * 找回密码对话框控件
     */

    Dialog dialog_modify_passwd;

    TextInputLayout yanzhengma;

    EditText phoneEditText,
             newpasswd,
             code;

    Button getcode,
           modify_passwd;

    String code_str;   //验证码



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        handle_modify_password = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.obj!=null){
                    try {
                        JSONObject result = new JSONObject(msg.obj.toString());
                        int status = result.getInt("status");
                        if(status==0)
                        {
                            Toast.makeText(MainActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                            dialog_modify_passwd.dismiss();
                        }

                        else
                            Toast.makeText(MainActivity.this, "修改失败", Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * 初始化
     */
    private void init(){

        countystr = "86";   //默认中国
        /**
         * 从account中读取登陆过的账号
         */
        SharedPreferences readaccount = getSharedPreferences("account",MODE_PRIVATE);
        HashMap<String,String> mhashmap = (HashMap<String, String>)readaccount.getAll();
        List<String> mlist = new ArrayList<>();
        for(String key:mhashmap.keySet()){
            mlist.add((String) mhashmap.get(key));
        }
        ArrayAdapter madapter = new ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line,mlist);
        mProgressbar = (ProgressBar) findViewById(R.id.login_progress);

        maccount = (AutoCompleteTextView) findViewById(R.id.email);
        maccount.setAdapter(madapter);

        mpassword = (EditText) findViewById(R.id.password);
        account = (TextInputLayout)findViewById(R.id.account);
        password_layout = (TextInputLayout)findViewById(R.id.password_layout);
        //查看是否已经登陆
        SharedPreferences read = getSharedPreferences("admin",MODE_PRIVATE);
        accountstr =  read.getString("account","");

        if(!accountstr.equals("")) load_signup();
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.obj!=null){
                    //隐藏进度条
                    showProgressBar(false);
                    try {
                        JSONObject result = new JSONObject(msg.obj.toString());
                        int status = result.getInt("status");
                        if(status==0) {
                            Toast.makeText(MainActivity.this, R.string.successfulsign_up, Toast.LENGTH_SHORT).show();
                            setdata();
                            load_signup();
                        }
                        else if(status==1)
                            password_layout.setError("密码错误");
                        else account.setError("账号不存在");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else Toast.makeText(MainActivity.this, "未知错误", Toast.LENGTH_SHORT).show();
            }
        };

        /**
         * 给账号输入增加监听
         */
        maccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
               account.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                  String accountstring = maccount.getText().toString();
                  if(!is_valid_account(accountstring)){
                      account.setErrorEnabled(true);
                      account.setError("账号必须是11位的手机号码");
                  }
            }
    });

        /**
         * 给密码输入增加监听
         */
        mpassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                password_layout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String accountstring = mpassword.getText().toString();
                if(!is_valid_password(accountstring)){
                    password_layout.setErrorEnabled(true);
                    password_layout.setError("密码必须是1-12位数字或字母的组合");
                }
            }
        });

        init_modify_passwd();
    }

    /**
     * 保存数据
     */
    public void setdata(){
        //将账号保存在本地，以便下次输入时提示
        SharedPreferences.Editor editor_account = getSharedPreferences("account",MODE_PRIVATE).edit();
        editor_account.putString(accountstr,accountstr);
        editor_account.commit();
        //将账号保存本地，以便验证登录状态
        SharedPreferences.Editor editor_admin = getSharedPreferences("admin",MODE_PRIVATE).edit();
        editor_admin.putString("account",accountstr);
        editor_admin.commit();
    }

    /**
     * 登录按钮点击事件
     * @param v
     */
    public void sign_up(View v){
        //点击登录按钮后显示进度条
        showProgressBar(true);
        /**
         * 得到账号和密码
         */
        accountstr = maccount.getText().toString();
        passwordstr = mpassword.getText().toString();
        if(!is_valid_account(accountstr)||!is_valid_password(passwordstr)){
            showProgressBar(false);
            account.setErrorEnabled(true);
            account.setError("账号必须是11位的手机号码");
            password_layout.setErrorEnabled(true);
            password_layout.setError("密码必须是1-12位数字或字母的组合");
            Toast.makeText(this, "上述输入有错误，请根据提示改正后重新登录", Toast.LENGTH_SHORT).show();
        }else
        new Thread(sign).start();
    }

    /**
     * 进度条显示逻辑控制
     * @param ishowing
     */
    private void showProgressBar(boolean ishowing){
        if(ishowing) mProgressbar.setVisibility(View.VISIBLE);
        else mProgressbar.setVisibility(View.GONE);
    }
    

    /**
     * 登陆的线程
     */
    Runnable sign = new Runnable() {
        //返回结果
        String result = "";
        //url
        String urlStr = "";
        URL url = null;
        //参数
        String params = "";
        @Override
        public void run() {
            urlStr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
            try {
                url = new URL(urlStr);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                params = "head=1&account=" + accountstr + "&password=" + passwordstr;
                DataOutputStream out = new DataOutputStream(http.getOutputStream());
                out.write(params.getBytes());
                InputStreamReader in = new InputStreamReader(http.getInputStream());
                BufferedReader buffer = new BufferedReader(in);
                String inputline = "";
                result = "";
                while((inputline = buffer.readLine()) != null){
                    result += inputline;
                }
                in.close();
                http.disconnect();
                Message m = handler.obtainMessage();
                m.obj = result;
                handler.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**登陆成功后跳转
     */
    private void load_signup(){
        Intent mintent = new Intent();
        mintent.setClass(MainActivity.this,main.class);
        startActivity(mintent);
        finish();
    }

    /**
     * 判断账号是否符合要求
     * @param account
     * @return
     */
    public boolean is_valid_account(String account){
        Pattern p = Pattern.compile("(\\d){11}");
        Matcher m = p.matcher(account);
        if(m.matches()) return true;
        else return false;
    }
    public boolean is_valid_password(String account){
        Pattern p = Pattern.compile("(\\d|\\w){1,12}");
        Matcher m = p.matcher(account);
        if(m.matches()) return true;
        else return false;
    }


    /** 请求验证码，其中country表示国家代码，如“86”；phone表示手机号码，如“13800138000”
     * @param country
     * @param phone
     */
    public void sendCode(String country, String phone) {
        // 注册一个事件回调，用于处理发送验证码操作的结果
        SMSSDK.registerEventHandler(new EventHandler() {
            public void afterEvent(int event, int result, Object data) {
                if (result == SMSSDK.RESULT_COMPLETE) {
                    Log.e("SMS","获取验证码成功");
                } else{
                    Log.e("SMS","获取验证码失败");
                }

            }
        });
        // 触发操作
        SMSSDK.getVerificationCode(country, phone);
    }

    /** 提交验证码，其中的code表示验证码，如“1357”
     * @param country
     * @param phone
     * @param code
     */
    public void submitCode(String country, final String phone, String code) {
        // 注册一个事件回调，用于处理提交验证码操作的结果
        SMSSDK.registerEventHandler(new EventHandler() {
            public void afterEvent(int event, int result, Object data) {
                if (result == SMSSDK.RESULT_COMPLETE) {
                    // TODO 处理验证成功的结果
                    accountstr = phoneEditText.getText().toString();
                    passwordstr = newpasswd.getText().toString();
                    new Thread(modify_password).start();
                } else{
                    // TODO 处理错误的结果
                }

            }
        });
        // 触发操作
        SMSSDK.submitVerificationCode(country, phone, code);
    }

    protected void onDestroy() {
        super.onDestroy();
        //用完回调要注销掉，否则可能会出现内存泄露
        SMSSDK.unregisterAllEventHandler();
    }

    /**
     * 初始化忘记密码对话框
     */
    public void init_modify_passwd(){
        accountstr = maccount.getText().toString();
        View mview = getLayoutInflater().inflate(R.layout.my_find_passwd_dialog,null);
        phoneEditText = (EditText) mview.findViewById(R.id.phone);
        newpasswd = (EditText) mview.findViewById(R.id.newPassqord);
        code = (EditText)mview.findViewById(R.id.code);
        getcode = (Button)mview.findViewById(R.id.getcode);
        modify_passwd = (Button)mview.findViewById(R.id.modify_passwd);
        yanzhengma = (TextInputLayout)mview.findViewById(R.id.yanzhengma);

        phoneEditText.setText(accountstr);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("更改密码")
                .setView(mview);


        /**
         * 获取验证码
         */
        getcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accountstr = phoneEditText.getText().toString();
                sendCode(countystr,accountstr);
            }
        });
        /**
         * 修改密码
         */
        modify_passwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accountstr = phoneEditText.getText().toString();
                code_str = code.getText().toString();
                if(code_str==null||code_str.equals(""))
                    yanzhengma.setError("验证码不能为空");
                else submitCode(countystr,accountstr,code_str);
            }
        });

        dialog_modify_passwd = builder.create();

        code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                 yanzhengma.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
               if(code.getText().toString()==null||code.getText().toString().equals("")) {
                   yanzhengma.setErrorEnabled(true);
                   yanzhengma.setError("验证码不能为空");
               }
            }
        });


    }

    Runnable modify_password = new Runnable() {
        String result = "",
                urlstr = "",
                params = "";
        @Override
        public void run() {
            urlstr = "http://172.18.159.1:8080/TrainInfoManagement/index.jsp";
            try {
                URL url = new URL(urlstr);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(http.getOutputStream());
                params = "head=21&phone="+accountstr
                        +"&passwd="+passwordstr;
                ;
                out.write(params.getBytes());
                InputStreamReader in = new InputStreamReader(http.getInputStream());
                BufferedReader buffer = new BufferedReader(in);
                String inputline = "";
                result = "";
                while ((inputline=buffer.readLine())!=null){
                    result += inputline;
                }
                in.close();
                http.disconnect();
                Message m  = handle_modify_password.obtainMessage();
                m.obj = result;
                handle_modify_password.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 忘记密码按钮
     * @param v
     */
    public void find_passwd(View v){
        dialog_modify_passwd.show();
    }
}
