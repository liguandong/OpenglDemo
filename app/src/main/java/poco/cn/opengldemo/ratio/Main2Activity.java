package poco.cn.opengldemo.ratio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import poco.cn.opengldemo.R;
import poco.cn.opengldemo.utils.ShareData;
import poco.cn.opengldemo.base.PlayRatio;

// 根据画幅绘制图片，
public class Main2Activity extends AppCompatActivity implements View.OnClickListener
{

    protected GlFrameView GLSurfaceView;
    protected Button textView11;
    protected Button textView169;
    protected Button textView916;
    protected Button textView34;
    protected Button textView2351;
    protected Group group;
    protected Button fullIn;
    protected Button fullOut;
    protected Button rotate;
    protected Button enterFrame;
    protected Button exitFrame;
    protected Button changeImg;

    int[] resId = new int[]{R.drawable.img1_1,R.drawable.img16_9,R.drawable.img9_16,R.drawable.img3_4,R.drawable.img_test};
    int index = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main2);
        initView();
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.textView1_1)
        {
            GLSurfaceView.setPlayRatio(PlayRatio.RATIO_1_1);
        } else if (view.getId() == R.id.textView16_9)
        {
            GLSurfaceView.setPlayRatio(PlayRatio.RATIO_16_9);

        } else if (view.getId() == R.id.textView9_16)
        {
            GLSurfaceView.setPlayRatio(PlayRatio.RATIO_9_16);

        } else if (view.getId() == R.id.textView3_4)
        {
            GLSurfaceView.setPlayRatio(PlayRatio.RATIO_3_4);

        } else if (view.getId() == R.id.textView235_1)
        {
            GLSurfaceView.setPlayRatio(PlayRatio.RATIO_235_1);

        } else if (view.getId() == R.id.fullIn)
        {
            GLSurfaceView.scaleToMin();
        } else if (view.getId() == R.id.fullOut)
        {
            GLSurfaceView.resetScale();

        } else if (view.getId() == R.id.rotate)
        {
            GLSurfaceView.rotateFrame(false);
        } else if (view.getId() == R.id.enterFrame)
        {
            GLSurfaceView.enterFrameMode();
        } else if (view.getId() == R.id.exitFrame)
        {
            GLSurfaceView.exitFrameMode();
        } else if (view.getId() == R.id.changeImg)
        {
            index = (index+1)%resId.length;
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId[index]);
            GLSurfaceView.setBitmap(bitmap);
        }
    }

    private void initView()
    {
        GLSurfaceView = (GlFrameView) findViewById(R.id.glSurfaceView);
        textView11 = (Button) findViewById(R.id.textView1_1);
        textView11.setOnClickListener(Main2Activity.this);
        textView169 = (Button) findViewById(R.id.textView16_9);
        textView169.setOnClickListener(Main2Activity.this);
        textView916 = (Button) findViewById(R.id.textView9_16);
        textView916.setOnClickListener(Main2Activity.this);
        textView34 = (Button) findViewById(R.id.textView3_4);
        textView34.setOnClickListener(Main2Activity.this);
        textView2351 = (Button) findViewById(R.id.textView235_1);
        textView2351.setOnClickListener(Main2Activity.this);
        group = (Group) findViewById(R.id.group);
        fullIn = (Button) findViewById(R.id.fullIn);
        fullIn.setOnClickListener(Main2Activity.this);
        fullOut = (Button) findViewById(R.id.fullOut);
        fullOut.setOnClickListener(Main2Activity.this);
        rotate = (Button) findViewById(R.id.rotate);
        rotate.setOnClickListener(Main2Activity.this);
        enterFrame = (Button) findViewById(R.id.enterFrame);
        enterFrame.setOnClickListener(Main2Activity.this);
        exitFrame = (Button) findViewById(R.id.exitFrame);
        exitFrame.setOnClickListener(Main2Activity.this);
        changeImg = (Button) findViewById(R.id.changeImg);
        changeImg.setOnClickListener(Main2Activity.this);


        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId[0]);
        GLSurfaceView.setBitmap(bitmap);
        GLSurfaceView.setViewSize(ShareData.m_screenWidth, (int) (ShareData.m_screenWidth / 1.3f));
        GLSurfaceView.setPlayRatio(PlayRatio.RATIO_16_9);
    }


}
