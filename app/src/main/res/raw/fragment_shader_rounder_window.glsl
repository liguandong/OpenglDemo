//precision highp float;
//
//uniform sampler2D sourceImage;
//varying vec2 vTextureCoord;
//uniform float radius;// 短边的0到0.5值
//uniform float ratio;
//uniform vec2 screenSize;
//void main() {
//    vec4 color = texture2D(sourceImage, vTextureCoord.xy);
//    vec2 npos = gl_FragCoord.xy / screenSize.xy;// 0.0 .. 1.0
//    float aspect = screenSize.x / screenSize.y;// aspect ratio x/y
//    vec2 ratio = vec2(aspect, 1.0);// aspect ratio (x/y,1)
//    vec2 uv = (2.0 * npos - 1.0) * ratio;// -1.0 .. 1.0
//    vec2 size = ratio - vec2(radius, radius);
//    float d = length(max(abs(uv), size) - size) - radius;
//    float alpha = 1.0 - step(0.0, d);
//
//    vec4 bk = vec4(0.0, 0.0, 0.0, 0.0);
//    gl_FragColor = mix(bk,color,alpha);
//}
precision highp float;

uniform sampler2D sourceImage;
varying vec2 vTextureCoord;
uniform float radius;
uniform vec2 screenSize;
void main() {
//    vec4 color = texture2D(sourceImage, vTextureCoord.xy);
//    vec2 npos = gl_FragCoord.xy / screenSize.xy;// 0.0 .. 1.0
//    float aspect = screenSize.x / screenSize.y;// aspect ratio x/y
//    vec2 ratio = vec2(aspect, 1.0);// aspect ratio (x/y,1)
//    vec2 uv = (2.0 * npos - 1.0) * ratio;// -1.0 .. 1.0
//    vec2 size = ratio - vec2(radius, radius);
//    float d = length(max(abs(uv), size) - size) - radius;
//    float alpha = 1.0 - step(0.0, d);
//
//    vec4 bk = vec4(0.0, 0.0, 0.0, 0.0);
//    gl_FragColor = mix(bk,color,alpha);
    vec4 color = texture2D(sourceImage, vTextureCoord.xy);
    vec2 npos = gl_FragCoord.xy / screenSize.xy;// 0.0 .. 1.0
    float aspect = screenSize.x / screenSize.y;// aspect ratio x/y
    vec2 ratio = vec2(aspect, 1.0);// aspect ratio (x/y,1)
    vec2 uv = (npos - 0.5) * ratio;// -0.5 .. 0.5
    vec2 size = ratio - vec2(radius, radius);
    float d = length(max(abs(uv), size) - size) - radius;
    float alpha = 1.0 - step(0.0, d);

    vec4 bk = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragColor = mix(bk,color,alpha);
}

//fmod(x,y)：返回 x/y 的余数。如果 y 为 0，结果不可预料。

//step(a,x)：如果 x<a，返回 0；否则，返回 1。