#version 330 core
out vec4 FragColor;

in vec2 vUv;

uniform float uTime;
uniform vec2 uResolution;

// Espirales doradas: brazos que giran desde el centro en tonos de dorado.
void main() {
    float aspect = uResolution.x / uResolution.y;
    vec2 p = (vUv - 0.5) * vec2(aspect, 1.0);
    float t = uTime;
    float ang = atan(p.y, p.x);
    float rad = length(p);

    // Espiral: el angulo crece con el radio; dos brazos contrarotando.
    float spiral1 = 0.5 + 0.5 * sin(rad * 9.0 - ang * 6.0 + t * 2.0);
    float spiral2 = 0.5 + 0.5 * sin(rad * 7.0 + ang * 7.0 - t * 1.4);
    float bands = clamp(spiral1 * 0.7 + spiral2 * 0.4, 0.0, 1.0);

    vec3 doradoOscuro = vec3(0.08, 0.05, 0.01);
    vec3 doradoMedio = vec3(0.75, 0.50, 0.08);
    vec3 doradoClaro = vec3(1.0, 0.90, 0.45);

    vec3 col = mix(doradoOscuro, doradoMedio, smoothstep(0.3, 0.65, bands));
    col = mix(col, doradoClaro, smoothstep(0.8, 1.0, bands) * 0.9);

    // Centro brillante que respira.
    float core = exp(-rad * 5.0);
    col += doradoClaro * core * (0.35 + 0.30 * sin(t * 1.3));

    col *= mix(1.0, 0.5, smoothstep(0.4, 1.6, rad));
    FragColor = vec4(col, 1.0);
}