#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;

out vec3 FragPos;
out vec3 Normal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    // Los modelos se colocan con una matriz propia: uno por planta, girado
    // según su celda. El escenario usa identidad, pero aqui hace falta la
    // transformación completa.
    vec4 world = model * vec4(aPos, 1.0);
    FragPos = world.xyz;
    Normal = mat3(transpose(inverse(model))) * aNormal;

    gl_Position = projection * view * world;
}
