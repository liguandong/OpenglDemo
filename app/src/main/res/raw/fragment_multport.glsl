precision mediump float;

uniform sampler2D firstImage;
uniform sampler2D secondImage;
varying vec2 vTextureCoord;
varying vec4 vPosition;

uniform float progress;
uniform float scale;
uniform float transitionX;
uniform float transitionY;

void main() {

    //    vec4 color1 = texture2D(firstImage, vTextureCoord);
    //    vec4 color2;
    //    float alpha = 0.0f;
    //    float dx = scale * 0.5f;
    //    float dy = dx;
    //    if (vTextureCoord.x > dx && vTextureCoord.x < 0.5f+dx && vTextureCoord.y > dy && vTextureCoord.y < 0.5f+dy){
    //        alpha = 1.0f;
    //        float centerX = 0.5f;
    //        float centerY = 0.5f;
    //        color2 = texture2D(secondImage, vec2(centerX + (vTextureCoord.x - centerX) / scale, centerY + (vTextureCoord.y - centerY) / scale));
    //    }
    //
    //    gl_FragColor = mix(color1, color2, alpha);


//    vec4 color1 = texture2D(firstImage, vTextureCoord);
//    vec4 color2;
//    float alpha = 0.0f;
//    if (vPosition.x > -scale && vPosition.x < scale && vPosition.y > -scale && vPosition.y < scale){
//        alpha = 1.0f;
//        float centerX = 0.5f;
//        float centerY = 0.5f;
//        color2 = texture2D(secondImage, vec2(centerX + (vTextureCoord.x - centerX) / scale, centerY + (vTextureCoord.y - centerY) / scale));
//    }
//    gl_FragColor = mix(color1, color2, alpha);

    vec4 color1 = texture2D(firstImage, vTextureCoord);
    vec4 color2;
    float absX = abs(vPosition.x);
    float absY = abs(vPosition.y);
    float alpha = step(absX,scale) * step(absY,scale);
    float centerX = 0.5f;
    float centerY = 0.5f;
    color2 = texture2D(secondImage, vec2(centerX + (vTextureCoord.x - centerX) / scale, centerY + (vTextureCoord.y - centerY) / scale));
    gl_FragColor = mix(color1, color2, alpha);
}

