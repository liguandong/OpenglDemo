
precision mediump float;

uniform sampler2D firstImage;
varying vec2 vTextureCoord;

uniform float progress;
uniform vec4 mask;

void main() {

    vec4 color = texture2D(firstImage, vTextureCoord);
//    float y = 0.5 + (vTextureCoord.y - 0.5) / (1.0 - progress);
//    float p = 1.0 - step(0.0, y) * step(y, 1.0);

    float y = abs(vTextureCoord.y - 0.5f) * 2.0;
    float p = step(1.0- progress,y);

    gl_FragColor = mix(color, mask, p);
}

