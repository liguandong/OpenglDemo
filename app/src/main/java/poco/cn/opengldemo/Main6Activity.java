package poco.cn.opengldemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import poco.cn.opengldemo.special.GlSpecialView;

public class Main6Activity extends AppCompatActivity implements View.OnClickListener
{

    protected GlSpecialView glSurfaceView;
    protected Button changeImg;
    protected SeekBar seekBar1;
    protected Button fixCenter;
    protected Button centerCrop;
    protected SeekBar seekBar2;
    protected Button changeImg2;
    protected Button square;
    protected Button circle;
    protected Button roundedRectangle;
    protected Button roundedT;

    int[] resId = new int[]{R.drawable.img1_1, R.drawable.img16_9, R.drawable.img9_16, R.drawable.img3_4, R.drawable.img_test};
    int index = 0;
    int index2 = 0;
    private SeekBar seekBar3;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main6);
        initView();
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.changeImg)
        {
            index = (index + 1) % resId.length;
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId[index]);
            glSurfaceView.setBitmap(bitmap);
        } else if (view.getId() == R.id.fixCenter)
        {
            glSurfaceView.fixCenter();
        } else if (view.getId() == R.id.centerCrop)
        {
            glSurfaceView.centerCrop();
        } else if (view.getId() == R.id.changeImg2)
        {
            index2 = (index2 + 1) % resId.length;
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId[index2]);
            glSurfaceView.setBitmap2(bitmap);
        } else if (view.getId() == R.id.square)
        {
            glSurfaceView.setType(1);
        } else if (view.getId() == R.id.circle)
        {
            glSurfaceView.setType(2);

        } else if (view.getId() == R.id.rounded)
        {
            glSurfaceView.setType(3);

        } else if (view.getId() == R.id.roundedT)
        {
            glSurfaceView.setType(4);
        }
    }

    private void initView()
    {
        glSurfaceView = (GlSpecialView) findViewById(R.id.glSurfaceView);
        changeImg = (Button) findViewById(R.id.changeImg);
        changeImg.setOnClickListener(Main6Activity.this);
        seekBar1 = (SeekBar) findViewById(R.id.seekBar);
        fixCenter = (Button) findViewById(R.id.fixCenter);
        fixCenter.setOnClickListener(Main6Activity.this);
        centerCrop = (Button) findViewById(R.id.centerCrop);
        centerCrop.setOnClickListener(Main6Activity.this);
        seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
        seekBar3 = (SeekBar) findViewById(R.id.seekBar3);
        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                float a = progress / (float) seekBar.getMax();
                glSurfaceView.setScale(a, seekBar2.getProgress() / (float) seekBar2.getMax());
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

        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                float a = progress / (float) seekBar.getMax();
                glSurfaceView.setScale(seekBar1.getProgress() / (float) seekBar1.getMax(), a);
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
        seekBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                float a = progress / (float) seekBar.getMax();
                glSurfaceView.setRadius(a);
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
        changeImg2 = (Button) findViewById(R.id.changeImg2);
        changeImg2.setOnClickListener(Main6Activity.this);
        square = (Button) findViewById(R.id.square);
        square.setOnClickListener(Main6Activity.this);
        circle = (Button) findViewById(R.id.circle);
        circle.setOnClickListener(Main6Activity.this);
        roundedRectangle = (Button) findViewById(R.id.rounded);
        roundedRectangle.setOnClickListener(Main6Activity.this);
        roundedT = (Button) findViewById(R.id.roundedT);
        roundedT.setOnClickListener(Main6Activity.this);

    }
}
