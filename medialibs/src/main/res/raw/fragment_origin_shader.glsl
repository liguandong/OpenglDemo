precision highp float;

uniform sampler2D sourceImage;
varying vec2 vTextureCoord;
void main() {
//    gl_FragColor = vec4(1f,1f,0f,1f);
    gl_FragColor = texture2D(sourceImage,vTextureCoord);
}