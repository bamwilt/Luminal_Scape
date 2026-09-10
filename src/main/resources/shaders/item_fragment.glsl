#version 330 core
out vec4 FragColor;

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

void main()
{
    vec3 color;

    if (useTexture) {
        color = texture(textureSampler, TexCoords).rgb;
    } else {
        color = objectColor;
    }

    if (useLighting) {
        float ambientStrength = 0.35;
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

        // Luz propia suave: pulso lento para que el item brille ligeramente.
        float pulse = 0.5 + 0.5 * sin(time * 2.0);
        vec3 emission = color * (0.2 + 0.3 * pulse);

        vec3 result = (ambient + diffuse + specular) * color + emission;
        FragColor = vec4(result, 1.0);
    } else {
        FragColor = vec4(color, 1.0);
    }
}