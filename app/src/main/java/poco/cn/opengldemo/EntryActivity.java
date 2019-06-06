package poco.cn.opengldemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import poco.cn.opengldemo.utils.ShareData;

public class EntryActivity extends AppCompatActivity
{

    private RecyclerView mList;
    private ArrayList<MenuBean> data;
    private String[] must;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ShareData.InitData(this);
        setContentView(R.layout.activity_entry);
        mList = (RecyclerView) findViewById(R.id.recyclerView);
        mList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        data = new ArrayList<>();
        add("视频播放", MainActivity.class);
        add("画幅测试1", Main2Activity.class);
        add("画幅测试2（改善版）", Main3Activity.class);
        add("多窗口", Main6Activity.class);
        add("录音", Main7Activity.class);
        add("多音频播放", Main8Activity.class);
        add("斜分屏", Main9Activity.class);
//        add("颜色混合",BlendActivity.class);
        mList.setAdapter(new MenuAdapter());

        must = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        boolean hasPerminsion = true;
        for (String p : must)
        {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
            {
                hasPerminsion = false;
                //请求权限

            }
        }
        if (!hasPerminsion)
        {
            ActivityCompat.requestPermissions(this, must, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private void add(String name, Class<?> clazz)
    {
        MenuBean bean = new MenuBean();
        bean.name = name;
        bean.clazz = clazz;
        data.add(bean);
    }

    private class MenuBean
    {

        String name;
        Class<?> clazz;

    }

    private class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuHolder>
    {


        @Override
        public MenuHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            return new MenuHolder(getLayoutInflater().inflate(R.layout.item_button, parent, false));
        }

        @Override
        public void onBindViewHolder(MenuHolder holder, int position)
        {
            holder.setPosition(position);
        }

        @Override
        public int getItemCount()
        {
            return data.size();
        }

        class MenuHolder extends RecyclerView.ViewHolder
        {

            private Button mBtn;

            MenuHolder(View itemView)
            {
                super(itemView);
                mBtn = (Button) itemView.findViewById(R.id.mBtn);
                mBtn.setOnClickListener(onClickListener);
            }

            public void setPosition(int position)
            {
                MenuBean bean = data.get(position);
                mBtn.setText(bean.name);
                mBtn.setTag(position);
            }
        }

        private View.OnClickListener onClickListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                boolean hasPerminsion = true;
                for (String p : must)
                {
                    if (ContextCompat.checkSelfPermission(EntryActivity.this, p) != PackageManager.PERMISSION_GRANTED){
                        hasPerminsion = false;
                        //请求权限
                    }
                }
                if(hasPerminsion)
                {
                    int position = (int) v.getTag();
                    MenuBean bean = data.get(position);
                    startActivity(new Intent(EntryActivity.this, bean.clazz));
                }else{
                    Toast.makeText(EntryActivity.this, "请开启权限", Toast.LENGTH_SHORT).show();
                }
            }
        };

    }
}
