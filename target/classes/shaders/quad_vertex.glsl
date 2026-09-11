#version 330 core
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec4 aColor;

uniform vec2 uScreenSize;

out vec4 vColor;

void main() {
    float x = aPos.x / uScreenSize.x * 2.0 - 1.0;
    float y = 1.0 - (aPos.y / uScreenSize.y * 2.0);
    vColor = aColor;
    gl_Position = vec4(x, y, 0.0, 1.0);
}