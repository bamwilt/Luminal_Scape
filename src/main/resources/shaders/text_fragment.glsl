#version 330 core
in vec2 texCoords;
uniform sampler2D tex;
uniform vec3 color;
out vec4 FragColor;

void main() {
    float alpha = texture(tex, texCoords).r;
    FragColor = vec4(color, alpha);
}