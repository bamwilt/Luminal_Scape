#version 330 core
layout(location = 0) in vec2 aPos;

uniform float screenWidth;
uniform float screenHeight;

void main() {
    float x = aPos.x / screenWidth * 2.0 - 1.0;
    float y = 1.0 - (aPos.y / screenHeight * 2.0);
    gl_Position = vec4(x, y, 0.0, 1.0);
}
