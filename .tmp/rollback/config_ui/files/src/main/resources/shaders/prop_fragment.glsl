#version 330 core
out vec4 FragColor;

// Tamaño del array de luces puntuales: tiene que coincidir con el de
// wall_fragment.glsl y con MapConfig.MAX_LIGHT_COUNT (ver alli el aviso).
#define MAX_LIGHT_COUNT 8

in vec3 FragPos;
in vec3 Normal;

uniform vec3 u_propColor;
uniform vec3 lightPos;
uniform vec3 lightColor;
uniform vec3 viewPos;
uniform float u_ambientLight;
uniform float u_viewDistance;
uniform float u_fogNear;
uniform vec3 u_fogColor;

// Luces puntuales: las placas '◉' y las llaves. Una planta al pie de una placa
// recibe su luz, y la llave se recortara contra ella sin encenderse ella misma.
uniform int u_lightCount;
uniform vec3 u_lightPos[MAX_LIGHT_COUNT];
uniform vec3 u_lightColor[MAX_LIGHT_COUNT];
uniform float u_attenLinear;
uniform float u_attenQuadratic;

void main()
{
    // Los modelos no traen textura: el color plano del material se pinta tal
    // cual, con la misma luz y la misma niebla que el escenario para que una
    // planta no parezca una pieza pegada de otro mundo.
    vec3 norm = normalize(Normal);

    float ambientStrength = u_ambientLight;
    vec3 ambient = ambientStrength * lightColor;

    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // Especifico suave: la madera y el metal del pack no son lisos, y un brillo
    // fuerte los haria parecer plastico.
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 24.0);
    vec3 specular = 0.18 * spec * lightColor;

    // Luces puntuales, sumadas al difuso global con la misma caida que en el
    // muro para que una planta y el suelo que pisan tengan la misma luz.
    for (int i = 0; i < MAX_LIGHT_COUNT; i++) {
        if (i >= u_lightCount) {
            break;
        }
        vec3 toLight = u_lightPos[i] - FragPos;
        float d = length(toLight);
        float atten = 1.0 / (1.0 + u_attenLinear * d + u_attenQuadratic * d * d);
        diffuse += max(dot(norm, toLight / max(d, 0.0001)), 0.0) * atten * u_lightColor[i];
    }

    vec3 result = (ambient + diffuse + specular) * u_propColor;
    FragColor = vec4(result, 1.0);

    // Misma niebla que el muro: mismo margen cercano y misma curva, para que
    // un mueble cercano no se vea mas nitido que el pasillo que lo rodea.
    float dist = length(viewPos - FragPos);
    float near = u_fogNear;
    float span = max(u_viewDistance - near, 0.001);
    float t = clamp((dist - near) / span, 0.0, 1.0);
    float fogFactor = t * t * t;
    FragColor.rgb = mix(FragColor.rgb, u_fogColor, fogFactor);
}
