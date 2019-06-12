////////////////////////////////////////////////////////////////////////////////
///
/// Example class that invokes native SoundTouch routines through the JNI
/// interface.
///
/// Author        : Copyright (c) Olli Parviainen
/// Author e-mail : oparviai 'at' iki.fi
/// WWW           : http://www.surina.net
///
////////////////////////////////////////////////////////////////////////////////

package cn.poco.soundtouch;

public final class SoundTouch
{
    // Native interface function that returns SoundTouch version string.
    // This invokes the native c++ routine defined in "soundtouch-jni.cpp".
    public native final static String getVersionString();
    
    private native final void setTempo(long handle, float tempo);

    private native final void setPitchSemiTones(long handle, float pitch);
    
    private native final void setSpeed(long handle, float speed);

    private native final int processFile(long handle, String inputFile, String outputFile, String WavINpath, String WavOUTpath);

    public native final static String getErrorString();

    private native final static long newInstance();
    
    private native final void deleteInstance(long handle);
    
    long handle = 0;
    
    
    public SoundTouch()
    {
    	handle = newInstance();    	
    }
    
    
    public void close()
    {
    	deleteInstance(handle);
    	handle = 0;
    }

    //设置节奏参数 -50～100
    public void setTempo(float tempo)
    {
    	setTempo(handle, tempo);
    }

    //设置音高参数 -12～12
    public void setPitchSemiTones(float pitch)
    {
    	setPitchSemiTones(handle, pitch);
    }

    //设置速率参数 -50～100
    public void setSpeed(float speed)
    {
    	setSpeed(handle, speed);
    }

    //每次处理都需要执行这四个方法，三个设置参数方法，以及处理方法。
    //变声处理，以下四个路径都需要上层传入，Wav文件较大，建议处理完按照路径删除，
    //inputFile——AAC文件输入路径
    //outputFile--AAC文件输出路径
    //WavINpath——解码Wav入文件路径
    //WavOUTpath——解码Wav出文件路径
    public int processFile(String inputFile, String outputFile, String WavINpath, String WavOUTpath)
    {
    	return processFile(handle, inputFile, outputFile, WavINpath, WavOUTpath);
    }

    
    // Load the native library upon startup
    static
    {
        System.loadLibrary("Poco_ChangeVoice");
    }
}
