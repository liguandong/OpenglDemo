package poco.cn.opengldemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import poco.cn.opengldemo.ratio.Main2Activity;
import poco.cn.opengldemo.ratio2.Main3Activity;
import poco.cn.opengldemo.utils.ShareData;
import poco.cn.opengldemo.video.MainActivity;

public class EntryActivity extends AppCompatActivity
{

    private RecyclerView mList;
    private ArrayList<MenuBean> data;

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
        add("画幅测试", Main2Activity.class);
        add("画幅测试2", Main3Activity.class);
//        add("颜色混合",BlendActivity.class);
        mList.setAdapter(new MenuAdapter());
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
                int position = (int) v.getTag();
                MenuBean bean = data.get(position);
                startActivity(new Intent(EntryActivity.this, bean.clazz));
            }
        };

    }
}
