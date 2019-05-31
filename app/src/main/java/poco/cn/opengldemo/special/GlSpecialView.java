package poco.cn.opengldemo.special;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import poco.cn.opengldemo.R;

/**
 * Created by lgd on 2019/5/23.
 */
public class GlSpecialView extends FrameLayout
{

    private GLSurfaceView glSurfaceView;
    private MultiWindowRender frameRender;

    int[] resId = new int[]{R.drawable.img1_1, R.drawable.img16_9, R.drawable.img9_16, R.drawable.img3_4, R.drawable.img_test};
    int index = 0;
    int index2 = 0;
    private View mask;


    public GlSpecialView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    private void init()
    {
        LayoutParams fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        fl.gravity = Gravity.CENTER;
        glSurfaceView = new GLSurfaceView(getContext());
        glSurfaceView.setEGLConfigChooser(8,8,8,8,16,8);
        glSurfaceView.setEGLContextClientVersion(2);
        addView(glSurfaceView, fl);

        frameRender = new MultiWindowRender(getContext());
        glSurfaceView.setRenderer(frameRender);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mask = new View(getContext());
        mask.setScaleX(0.5f);
        mask.setScaleY(0.5f);
        mask.setBackgroundColor(Color.GREEN);
        mask.setAlpha(0.15f);
        fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        fl.gravity = Gravity.CENTER;
        addView(mask,fl);
    }

    public void setScale(float scaleX,float scaleY)
    {
        mask.setScaleX(scaleX);
        mask.setScaleY(scaleY);
//        LayoutParams fl = (LayoutParams) mask.getLayoutParams();
//        fl.leftMargin = (int) ((mask.getWidth() - mask.getWidth() * scaleX )/ 2);
//        fl.topMargin = (int) ((mask.getHeight() - mask.getHeight() * scaleY )/ 2);
//        mask.setLayoutParams(fl);

        glSurfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                frameRender.setScale(scaleX,scaleY);
            }
        });
        glSurfaceView.requestRender();
    }


    public void fixCenter()
    {
        frameRender.fixCenter();
        glSurfaceView.requestRender();
    }

    public void centerCrop()
    {
        frameRender.centerCrop();
        glSurfaceView.requestRender();
    }

    public void setBitmap(Bitmap bitmap)
    {
        glSurfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                frameRender.setBitmap(bitmap);
            }
        });
        glSurfaceView.requestRender();
    }

    public void setBitmap2(Bitmap bitmap)
    {
        glSurfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                frameRender.setBitmap2(bitmap);
            }
        });
        glSurfaceView.requestRender();
    }

    public void setType(int i)
    {
        glSurfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                frameRender.setType(i);
            }
        });
        glSurfaceView.requestRender();
    }

    public void setRadius(float r)
    {
        glSurfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                frameRender.setRadius(r);
            }
        });
        glSurfaceView.requestRender();
    }

    // 0.01f

//    float radiusOffset = 0.5 - uRadius;
//    float radiusYOffset = radiusOffset * uRatio;
//    vec2 uv = textureCoord.xy - vec2(0.5, 0.5);
//    float rx = mod(abs(uv.x), radiusOffset);
//    float ry = mod(abs(uv.y), radiusYOffset) / uRatio;
//    //                        float mx = step(radiusOffset, abs(uv.x));
////                        float my = step(radiusYOffset, abs(uv.y));
////                        float mr = step(uRadius, length(vec2(rx, ry)));
//    float mx = (radiusOffset >= abs(uv.x) ? 0.0 : 1.0);
//    float my = (radiusYOffset >= abs(uv.y) ? 0.0 : 1.0);
//    float mr = (uRadius >= length(vec2(rx, ry)) ? 0.0 : 1.0);
//    float alpha = 1.0 - mx * my * mr;
//    vec4 color = texture2D(uTexture, textureCoord);
//            if (alpha < 1.0 || (abs(uv.x) > 0.5) || (abs(uv.y) > 0.5 * uRatio)) {
//    color = vec4(0.0, 0.0, 0.0, 0.0);
//}
//    gl_FragColor = color;
}
