precision highp float;
uniform sampler2D sourceImage;
varying vec2 vTextureCoord;
uniform float radius;// 短边的0到1值
uniform float ratio;
void main() {
    vec4 color = texture2D(sourceImage, vTextureCoord.xy);
    vec2 npos =  vTextureCoord.xy;// 0.0 .. 1.0
    float aspect = ratio;// aspect ratio x/y
    vec2 ratio = vec2(aspect, 1.0);// aspect ratio (x/y,1)
    vec2 uv = (npos - 0.5f) * ratio;// -1.0 .. 1.0
    // size是剔除圆角的内矩形
    vec2 size = ratio/2.0 - vec2(radius, radius);
    // 判断点到内矩形的距离
    float d = length(max(abs(uv), size) - size) - radius;
    float alpha = step(d, 0.0);
    vec4 bk = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragColor = mix(bk, color, alpha);
}

//fmod(x,y)：返回 x/y 的余数。如果 y 为 0，结果不可预料。

//step(a,x)：如果 x<a，返  回 0；否则，返回 1。