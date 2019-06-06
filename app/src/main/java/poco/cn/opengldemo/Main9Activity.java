package poco.cn.opengldemo;

import android.os.Bundle;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import poco.cn.opengldemo.media.GlRenderView;

public class Main9Activity extends AppCompatActivity
{

    protected GlRenderView glRenderView;
    protected SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main9);
        initView();
    }

    private void initView()
    {
        glRenderView = (GlRenderView) findViewById(R.id.glRenderView);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                glRenderView.setF(progress / (float)seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });
    }
}
