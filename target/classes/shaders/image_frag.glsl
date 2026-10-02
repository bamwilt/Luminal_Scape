#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform float uAlpha;

void main() {
    vec4 texel = texture(uTexture, vUv);
    // Se descarta lo totalmente transparente para que el logo no deje un
    // rectángulo fantasma al moverse sobre el fondo.
    if (texel.a <= 0.001) {
        discard;
    }
    FragColor = vec4(texel.rgb, texel.a * uAlpha);
}
