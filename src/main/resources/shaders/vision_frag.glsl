#version 330 core

uniform vec4 uOverlayColor;
out vec4 FragColor;

void main() {
    FragColor = uOverlayColor;
}