precision mediump float;

uniform sampler2D firstImage;
varying vec2 vTextureCoord;

void main() {
    gl_FragColor = texture2D(firstImage, vTextureCoord);
}
