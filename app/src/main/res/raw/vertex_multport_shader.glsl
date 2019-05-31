uniform mat4 uMVPMatrix;
uniform mat4 uTexMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 vTextureCoord;
varying vec4 vPosition;

void main () {
    gl_Position = uMVPMatrix * aPosition;
    vPosition = gl_Position;
    vTextureCoord = (uTexMatrix * aTextureCoord).xy;
}

