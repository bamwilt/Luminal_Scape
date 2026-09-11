#version 330 core

uniform vec4 buttonColor;
uniform vec2 uRectPos;
uniform vec2 uRectSize;
uniform float uCornerRadius;

in vec2 vPos;
out vec4 FragColor;

void main() {
    float radius = min(uCornerRadius, min(uRectSize.x, uRectSize.y) * 0.5);
    vec2 p = vPos - uRectPos - uRectSize * 0.5;
    vec2 d = abs(p) - (uRectSize * 0.5 - radius);
    float dist = length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
    float coverage = 1.0 - smoothstep(0.0, 1.5, dist);
    FragColor = vec4(buttonColor.rgb, buttonColor.a * coverage);
}