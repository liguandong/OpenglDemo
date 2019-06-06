#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sourceImage;
uniform vec2 screenSize;
uniform float diagonal;
void main() {
    vec4 color = texture2D(sourceImage, vTextureCoord.xy);
    vec2 npos = gl_FragCoord.xy / screenSize.xy;// 0.0 .. 1.0
    float aspect = screenSize.x / screenSize.y;// aspect ratio x/y
    vec2 ratio = vec2(aspect, 1.0);// aspect ratio (x/y,1)
    vec2 uv = (npos * 2.0 - 1.0) * ratio;// -1 .. -1
    float r1 = (1.0 - (1.0 - diagonal)) / (aspect * 2.0);
//    float r2 = 1.0f;
//    if (uv.y > 0.0){
//        r2 = (abs(uv.y) - diagonal) / (aspect + uv.x);
//    } else {
//        r2 = (abs(uv.y) - diagonal) / (aspect - uv.x);
//    }
    float r2 = (abs(uv.y)- (1.0 - diagonal)) / (aspect + uv.x * (-1.0 + 2.0 * step(0.0,uv.y)));
    float alpha = step(r2, r1);
    vec4 bk = vec4(0.0, 0.0, 0.0, 0.0);
    gl_FragColor = mix(bk, color, alpha);
}
