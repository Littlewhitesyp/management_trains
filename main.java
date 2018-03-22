package com.example.hasee.trainsadmin.Activity;



import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.hasee.trainsadmin.Adapter.FragmentAdapter;
import com.example.hasee.trainsadmin.Fragement.Adminself;
import com.example.hasee.trainsadmin.Fragement.management_stations;
import com.example.hasee.trainsadmin.Fragement.management_trains;
import com.example.hasee.trainsadmin.Fragement.search;
import com.example.hasee.trainsadmin.R;

import java.util.ArrayList;
import java.util.List;

import static com.example.hasee.trainsadmin.R.id.textView;


public class main extends AppCompatActivity {

    ViewPager mviewPager = null;
    TabLayout mtablayout = null;

    //标签页的图片和文字
    static int [] image = {
            R.drawable.search_red,
            R.drawable.train,
            R.drawable.station,
            R.drawable.management
    };
    List<String> tabs ;

    //head(title)
    TextView head =null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        initview();
    }

    public void initview(){
       head = (TextView) findViewById(R.id.head);
       mviewPager = (ViewPager) findViewById(R.id.viewpager);
       mtablayout = (TabLayout) findViewById(R.id.tab);
      FragmentManager fm = getSupportFragmentManager();

        /**
         * 构造fragment源
         */
        List<Fragment> fragments = new ArrayList<Fragment>();
        fragments.add(new search());
        fragments.add(new management_trains());
        fragments.add(new management_stations());
        fragments.add(new Adminself());
        /**
         * 构造标签页
         */
        tabs= new ArrayList<>();
        tabs.add("搜索");
        tabs.add("列车");
        tabs.add("车站");
        tabs.add("个人");
        FragmentAdapter madapter = new FragmentAdapter(fm,fragments,tabs);
        mviewPager.setAdapter(madapter);

        mtablayout.setupWithViewPager(mviewPager);

        resetTabs();
        //初始化默认选择第一个tab
        head.setText(tabs.get(0));

        /**
         * 标签被选中和不被选中的状态
         */
        mtablayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String mtext = tab.getText().toString();
                head.setText(mtext);
                TextView textView =(TextView)
                tab.getCustomView().findViewById(R.id.text);
                textView.setTextColor(getResources().getColor(R.color.red));
                ImageView imageView = (ImageView)
                        tab.getCustomView().findViewById(R.id.image);
                switch (mtext){
                    case "搜索":
                        imageView.setImageResource(R.drawable.search_red);
                        break;
                    case "列车":
                        imageView.setImageResource(R.drawable.train_red);
                        break;
                    case "车站":
                        imageView.setImageResource(R.drawable.station_red);
                        break;
                    case "个人":
                        imageView.setImageResource(R.drawable.people_red);
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                String mtext = tab.getText().toString();
                TextView textView =(TextView)
                        tab.getCustomView().findViewById(R.id.text);
                textView.setTextColor(getResources().getColor(R.color.gray));
                ImageView imageView = (ImageView)
                        tab.getCustomView().findViewById(R.id.image);
                switch (mtext){
                    case "搜索":
                        imageView.setImageResource(R.drawable.search);
                        break;
                    case "列车":
                        imageView.setImageResource(R.drawable.train);
                        break;
                    case "车站":
                        imageView.setImageResource(R.drawable.station);
                        break;
                    case "个人":
                        imageView.setImageResource(R.drawable.management);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //初始化默认选择第一个tab
        mtablayout.getTabAt(0).select();
        mtablayout.setSelected(true);
        TextView v1 = (TextView) mtablayout.getTabAt(0).getCustomView().findViewById(R.id.text);
        TextView v2 = (TextView) mtablayout.getTabAt(1).getCustomView().findViewById(R.id.text);
        TextView v3 = (TextView) mtablayout.getTabAt(2).getCustomView().findViewById(R.id.text);
        TextView v4 = (TextView) mtablayout.getTabAt(3).getCustomView().findViewById(R.id.text);
        v1.setTextColor(getResources().getColor(R.color.red));
        v2.setTextColor(getResources().getColor(R.color.gray));
        v3.setTextColor(getResources().getColor(R.color.gray));
        v4.setTextColor(getResources().getColor(R.color.gray));

    }

    public void resetTabs(){
          for(int i=0;i<tabs.size();i++){
              TabLayout.Tab tab = mtablayout.getTabAt(i);
              tab.setCustomView(gettabView(i));
          }
    }
    public View gettabView(int i){
        View view = getLayoutInflater().inflate(R.layout.custom_tab,null);
        TextView text = (TextView) view.findViewById(R.id.text);
        ImageView imageview = (ImageView) view.findViewById(R.id.image);
        text.setText(tabs.get(i));
        imageview.setImageResource(image[i]);
        return view;
    }

}
