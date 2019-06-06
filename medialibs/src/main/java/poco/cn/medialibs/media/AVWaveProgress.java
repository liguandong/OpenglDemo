package poco.cn.medialibs.media;

public interface AVWaveProgress {
    /**
     * 波形数据更新回调
     * @param position         波形当前更新到的位置
     */
    void onBufferUpdated(int position);

    /**
     * 波形数据生成回调
     * @param buffer         波形数据，之后的更新直接在这个数组上进行更新
     */
    void onBufferCreated(byte[] buffer);
}
