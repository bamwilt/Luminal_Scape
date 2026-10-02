#version 330 core
layout (location = 0) in vec3 aPos;

out vec3 v_worldDir;

uniform mat4 u_projection;
uniform mat4 u_view;

// Lado del cubo de cielo. El cubo es unitario (1x1x1) en el VAO y se escala
// aqui para que siempre enclose la camara y cubra el far plane entero.
uniform float u_skyScale;

// Centro del cubo: la camara. Es lo que compensa haber quitado la traslacion
// de la vista; sin esto el cubo se quedaria atras al caminar.
uniform vec3 u_cameraPos;

uniform float u_time;
uniform float u_speed;

void main()
{
    // staticView conserva la rotacion de la camara pero descarta su
    // traslacion: el cubo deja de moverse con el jugador y el cielo queda
    // anclado al mundo en vez de flotar pegado a la pantalla.
    mat4 staticView = mat4(mat3(u_view));

    // Deriva lenta: gira la direccion de muestreo alrededor del eje Y. Con
    // sky_speed 0 el cielo queda completamente quieto.
    float angle = u_time * u_speed;
    float c = cos(angle);
    float s = sin(angle);
    v_worldDir = vec3(aPos.x * c + aPos.z * s, aPos.y, -aPos.x * s + aPos.z * c);

    vec3 world = aPos * u_skyScale + u_cameraPos;
    gl_Position = u_projection * staticView * vec4(world, 1.0);
}
