package poco.cn.medialibs.media;

public class AVTracer
{
    public interface AVErrorReportHandler
    {
        void report(String log);
    }

    private static AVTracer sAVTracer;
    public static AVTracer getInstance()
    {
        if(sAVTracer == null)
        {
            sAVTracer = new AVTracer();
        }
        return sAVTracer;
    }

    private static final int MAX = 10240;
    private StringBuffer mTracer = new StringBuffer();
    private AVErrorReportHandler mAVErrorReportHandler;
    private int mReportCount = 0;
    private int mMaxReportCount = 2;

    private AVTracer()
    {}

    /**
     * 添加方法回调的LOG
     */
    protected void addMethod(String method, Object result, String[] inputs, Object... args)
    {
        if(mAVErrorReportHandler == null)
            return;

        if(mTracer.length() > MAX)
        {
            mTracer.delete(MAX, mTracer.length());
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(method);
        buffer.append("(");
        int i = 0;
        for(Object arg : args)
        {
            if(i > 0) {
                buffer.append(", ");
            }
            buffer.append(arg);
            i++;
        }
        buffer.append(")");
        buffer.append(" -> ");
        buffer.append(result);

        if(result instanceof Boolean && (Boolean)result == false && inputs != null)
        {
            int c = 1;
            for(String input : inputs) {
                buffer.append(" -> f"+c);
                AVInfo info = new AVInfo();
                if(AVUtils.avInfo(input, info, true)) {
                    buffer.append(info);
                } else {
                    buffer.append("call avInfo fail");
                }
                c++;
            }
        }

        if(mTracer.length() > 0)
        {
            buffer.append(" | ");
        }
        mTracer.insert(0, buffer);

        if(result instanceof Boolean)
        {
            if((Boolean)result == false)
            {
                report();
            }
        }
    }

    /**
     * 设置最大提交出错报告的次数
     * @param count 次数
     */
    public void setMaxReportCount(int count)
    {
        mMaxReportCount = count;
    }

    /**
     * 设置提交报告的回调，请在回调中上传或保存LOG
     * @param handler 回调接口
     */
    public void setReportHandler(AVErrorReportHandler handler)
    {
        mAVErrorReportHandler = handler;
    }

    private void report()
    {
        if(mReportCount < mMaxReportCount) {
            mAVErrorReportHandler.report(mTracer.toString());
            mReportCount++;
        }
    }
}
