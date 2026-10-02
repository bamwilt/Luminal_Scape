#version 330 core

in vec2 TexCoords;
in vec3 Normal;
in vec3 FragPos;

out vec4 FragColor;

uniform sampler2D texture_diffuse1;
uniform vec3 lightPos;
uniform vec3 lightColor;
uniform vec3 viewPos;
uniform float emissionStrength;
uniform float glowIntensity;

void main() {
    // Propiedades del material
    vec3 diffuseColor = texture(texture_diffuse1, TexCoords).rgb;
    
    // Oscurecer las texturas base
    float darknessFactor = 0.3; // Reduce el brillo base
    diffuseColor *= darknessFactor;
    
    // Luz ambiental MUY tenue
    float ambientStrength = 0.05; // Muy baja
    vec3 ambient = ambientStrength * lightColor * diffuseColor;
    
    // Luz difusa con rango reducido
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    float distanceFactor = 1.0 / (1.0 + 0.1 * length(lightPos - FragPos)); // Atenuación
    vec3 diffuse = diff * lightColor * diffuseColor * distanceFactor;
    
    // Sin especular para ambiente oscuro
    vec3 specular = vec3(0.0);
    
    // Luminiscencia tenue con color frío
    vec3 emissionColor = vec3(0.1, 0.1, 0.3); // Azul oscuro
    vec3 emission = emissionColor * emissionStrength * diffuseColor;
    
    // Combinación final
    vec3 result = ambient + diffuse + specular + emission;
    FragColor = vec4(result, 1.0);
}