#version 330 core
out vec4 FragColor;

in vec2 vUv;

uniform float uTime;
uniform vec2 uResolution;

// Paleta: negro, marrón, naranja, amarillo, blanco.
vec3 pal(float t) {
    vec3 negro = vec3(0.03, 0.02, 0.02);
    vec3 marron = vec3(0.38, 0.22, 0.08);
    vec3 naranja = vec3(0.95, 0.44, 0.06);
    vec3 amarillo = vec3(1.0, 0.82, 0.18);
    vec3 blanco = vec3(1.0, 0.97, 0.85);
    t = fract(t);
    if (t < 0.25) return mix(negro, marron, t * 4.0);
    if (t < 0.50) return mix(marron, naranja, (t - 0.25) * 4.0);
    if (t < 0.75) return mix(naranja, amarillo, (t - 0.50) * 4.0);
    return mix(amarillo, blanco, (t - 0.75) * 4.0);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main()
{
    float aspect = uResolution.x / uResolution.y;
    vec2 p = (vUv - 0.5) * vec2(aspect, 1.0);

    float angle = atan(p.y, p.x);
    float radius = length(p);

    // Movimiento psicodélico: espiral girando + oleaje + bandas onduladas.
    float waves = 0.0;
    waves += sin(angle * 6.0 + uTime * 1.6);
    waves += sin(radius * 14.0 - uTime * 2.2);
    waves += sin((p.x + p.y) * 8.0 + uTime * 1.2);
    waves += sin(length(p + vec2(sin(uTime * 0.4), cos(uTime * 0.35)) * 0.9) * 12.0 - uTime * 1.9);

    float idx = 0.5 + waves * 0.22 + uTime * 0.07;
    vec3 col = pal(idx);

    // Partículas flotando hacia arriba, resaltando en la paleta.
    float glow = 0.0;
    for (int i = 0; i < 12; i++) {
        float id = float(i);
        vec2 pos = vec2((hash(vec2(id, 1.7)) - 0.5) * aspect * 2.0,
                        fract(hash(vec2(id, 9.3)) + uTime * 0.07 + id * 0.04) * 2.0 - 1.0);
        vec2 delta = p - pos;
        float dist = sqrt(dot(delta, delta));
        glow += 0.05 / (dist * dist + 0.05);
    }
    col = mix(col, vec3(1.0, 0.95, 0.65), clamp(glow, 0.0, 1.0) * 0.9);

    // Vinetado suave para dar profundidad.
    float vig = 0.7 + 0.3 * smoothstep(1.8, 0.0, radius);
    col *= vig;

    FragColor = vec4(col, 1.0);
}