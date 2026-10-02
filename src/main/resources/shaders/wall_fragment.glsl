#version 330 core
out vec4 FragColor;

// Tamaño del array de luces puntuales. GLSL exige que el tamaño de un array de
// uniforms sea una constante de compilacion, asi que este numero esta escrito
// aqui y NO se puede leer de MapConfig: hay que cambiarlo en los dos shaders a
// la vez que en MapConfig.MAX_LIGHT_COUNT. La comprobacion que lo vigila esta en
// src/test/java/DecorYTechoChecks.java (seccion luces).
#define MAX_LIGHT_COUNT 8

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

// Propiedades del material
uniform bool useTexture;
uniform bool useLighting;
uniform sampler2D textureSampler;
uniform vec3 objectColor;

// Propiedades de la luz
uniform vec3 lightPos;
uniform vec3 lightColor;
uniform vec3 viewPos;

// Atmosfera del nivel
uniform float u_ambientLight;   // luz ambiente base (dark..bright)
uniform float u_viewDistance;   // distancia a la que la niebla es total
uniform float u_fogNear;        // distancia a la que la niebla empieza a verse
uniform vec3 u_fogColor;        // color del horizonte: el destino de la niebla

// Emision propia: la lleva solo el techo con luz '◉' (ver u_emission). El
// resto del escenario la manda a cero.
uniform float u_emission;

// Luces puntuales: las placas '◉' y las llaves. Se acumulan al difuso global
// con caida por distancia, y solo llegan las u_lightCount mas cercanas a la
// camara (el mapa entero no cabe en el array).
uniform int u_lightCount;
uniform vec3 u_lightPos[MAX_LIGHT_COUNT];
uniform vec3 u_lightColor[MAX_LIGHT_COUNT];
uniform float u_attenLinear;     // caida: 1 / (1 + L*d + Q*d^2)
uniform float u_attenQuadratic;

void main()
{
    vec3 color;
    
    // Obtener color base
    if (useTexture) {
        color = texture(textureSampler, TexCoords).rgb;
    } else {
        color = objectColor;
    }
    
    if (useLighting) {
        // Ambient
        float ambientStrength = u_ambientLight;
        vec3 ambient = ambientStrength * lightColor;
        
        // Diffuse 
        vec3 norm = normalize(Normal);
        vec3 lightDir = normalize(lightPos - FragPos);
        float diff = max(dot(norm, lightDir), 0.0);
        vec3 diffuse = diff * lightColor;
        
        // Specular
        float specularStrength = 0.5;
        vec3 viewDir = normalize(viewPos - FragPos);
        vec3 reflectDir = reflect(-lightDir, norm);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
        vec3 specular = specularStrength * spec * lightColor;
        
        // Luces puntuales. Van al MISMO lado que el difuso global, no aparte:
        // si se sumaran despues de multiplicar por el color, el brillo de un
        // muro de color plano seria independiente de su tono.
        for (int i = 0; i < MAX_LIGHT_COUNT; i++) {
            if (i >= u_lightCount) {
                break;
            }
            vec3 toLight = u_lightPos[i] - FragPos;
            float d = length(toLight);
            float atten = 1.0 / (1.0 + u_attenLinear * d + u_attenQuadratic * d * d);
            float ndl = max(dot(norm, toLight / max(d, 0.0001)), 0.0);
            diffuse += ndl * atten * u_lightColor[i];
        }
        
        // Combinar resultados
        vec3 result = (ambient + diffuse + specular) * color;
        // Emision: el techo con luz brilla con independencia de la luz que
        // recibe, por eso va fuera de la multiplicacion por el color.
        result += color * u_emission;
        FragColor = vec4(result, 1.0);
    } else {
        FragColor = vec4(color + color * u_emission, 1.0);
    }

    // Niebla con margen cercano y curva cubica: 0 hasta u_fogNear, y de ahi
    // sube despacio para que el campo cercano (el suelo que pisas y el
    // arranque del pasillo) se vea limpio. La curva al cubo deja la mayor
    // parte del recorrido casi despejado y luego cierra de golpe, que es la
    // sensacion de pared de niebla de Silent Hill. Con la lineal de antes la
    // niebla ya valia 0,3 a tres metros y no se distinguia el suelo de la
    // pared de fondo.
    float dist = length(viewPos - FragPos);
    float near = u_fogNear;
    float span = max(u_viewDistance - near, 0.001);
    float t = clamp((dist - near) / span, 0.0, 1.0);
    float fogFactor = t * t * t;
    FragColor.rgb = mix(FragColor.rgb, u_fogColor, fogFactor);
}
