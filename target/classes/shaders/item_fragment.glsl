#version 330 core
out vec4 FragColor;

// Tamaño del array de luces puntuales: tiene que coincidir con el de
// wall_fragment.glsl y con MapConfig.MAX_LIGHT_COUNT.
#define MAX_LIGHT_COUNT 8

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

uniform bool useTexture;
uniform bool useLighting;
uniform sampler2D textureSampler;
uniform vec3 objectColor;

uniform vec3 lightPos;
uniform vec3 lightColor;
uniform vec3 viewPos;
uniform float time;

// Luces puntuales (placas '◉' y otras llaves).
uniform int u_lightCount;
uniform vec3 u_lightPos[MAX_LIGHT_COUNT];
uniform vec3 u_lightColor[MAX_LIGHT_COUNT];
uniform float u_attenLinear;
uniform float u_attenQuadratic;

// Atmosfera del nivel: los items se biofjean igual que el escenario, o
// flotarian nitidos delante de un fondo ya desvanecido.
uniform float u_ambientLight;
uniform float u_viewDistance;
uniform float u_fogNear;
uniform vec3 u_fogColor;

void main()
{
    vec3 color;

    if (useTexture) {
        color = texture(textureSampler, TexCoords).rgb;
    } else {
        color = objectColor;
    }

    if (useLighting) {
        float ambientStrength = u_ambientLight + 0.15;
        vec3 ambient = ambientStrength * lightColor;

        vec3 norm = normalize(Normal);
        vec3 lightDir = normalize(lightPos - FragPos);
        float diff = max(dot(norm, lightDir), 0.0);
        vec3 diffuse = diff * lightColor;

        // Specular más marcado para que el item destaque.
        float specularStrength = 1.2;
        vec3 viewDir = normalize(viewPos - FragPos);
        vec3 reflectDir = reflect(-lightDir, norm);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), 48);
        vec3 specular = specularStrength * spec * lightColor;

        // Luces puntuales. El item NO se enciende a si mismo: deja la aureola
        // dorada alrededor, y como su foco esta en su mismo centro, la normal
        // hacia el apunta hacia dentro y el difuso le sale cero. Se ve la llave
        // recortada contra el suelo que ella ilumina, no un objeto flotando.
        for (int i = 0; i < MAX_LIGHT_COUNT; i++) {
            if (i >= u_lightCount) {
                break;
            }
            vec3 toLight = u_lightPos[i] - FragPos;
            float d = length(toLight);
            float atten = 1.0 / (1.0 + u_attenLinear * d + u_attenQuadratic * d * d);
            diffuse += max(dot(norm, toLight / max(d, 0.0001)), 0.0) * atten * u_lightColor[i];
        }

        vec3 result = (ambient + diffuse + specular) * color;
        FragColor = vec4(result, 1.0);
    } else {
        FragColor = vec4(color, 1.0);
    }

    // Mismo margen cercano y misma curva que el muro y la decoracion.
    float dist = length(viewPos - FragPos);
    float near = u_fogNear;
    float span = max(u_viewDistance - near, 0.001);
    float t = clamp((dist - near) / span, 0.0, 1.0);
    float fogFactor = t * t * t;
    FragColor.rgb = mix(FragColor.rgb, u_fogColor, fogFactor);
}