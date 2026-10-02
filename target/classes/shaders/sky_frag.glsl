#version 330 core
out vec4 FragColor;

in vec3 v_worldDir;

uniform vec3 u_skyHorizonColor;
uniform vec3 u_skyZenithColor;

uniform float u_time;
uniform float u_speed;

// ------------------------------------------------------------------
// Ruido de valor, solo para dar estructura al cielo. Sin texturas ni
// uniforms extra: el cele ruido sale de la propia direccion de mirada, asi
// que el cielo no se "pega" a la camara al caminar.
// ------------------------------------------------------------------

// hash 2D -> [0,1)
float hash21(vec2 p)
{
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// Ruido de valor bilineal con las cuatro esquinas interpoladas.
float valueNoise(vec2 p)
{
    vec2 i = floor(p);
    vec2 f = fract(p);
    // Suavizado de Hermite: sin esto las celdas del ruido se ven como
    // cuadricula sobre el cielo.
    f = f * f * (3.0 - 2.0 * f);

    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Suma de octavas. Cuatro bastan: mas octavas solo anaden coste por debajo
// de lo que se distingue a esta escala.
float fbm(vec2 p)
{
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amp * valueNoise(p);
        p *= 2.03;      // casi 2, no 2 exacto, para que las octavas no se
                        // alineen y produzcan artefactos periodicos
        amp *= 0.5;
    }
    return sum;
}

void main()
{
    // Direccion de la textura de cubo: la normalizada es la direccion de
    // mirada dentro del cielo.
    vec3 dir = normalize(v_worldDir);

    // h va de 0 en el horizonte a 1 hacia arriba. El +0.2 sube un poco el
    // horizonte para que el degradado empiece justo a la altura de los ojos y
    // no en el suelo.
    float h = clamp(dir.y * 2.0 + 0.2, 0.0, 1.0);

    // El degradado de los dos colores del preset es la base y se mantiene
    // intacto: todo lo demas se modula sobre el, nunca lo sustituye.
    vec3 color = mix(u_skyHorizonColor, u_skyZenithColor, h);

    // u_speed es tiny (0.02-0.06) y esta vez controla el MOVIMIENTO de las
    // capas, no solo la deriva de la direccion. Con speed 0 el cielo queda
    // completamente quieto, igual que antes.
    float t = u_time * u_speed;

    // Proyeccion del cielo sobre un plano para el ruido. Se usa atan para
    // el angulo: hace que las nubes rodeen el horizonte sin costura visible
    // al girar la camara.
    vec2 skyUv = vec2(atan(dir.z, dir.x) * 0.15915494, dir.y);

    // ---- Nubes: banda suave que deriva en horizontal y respira despacio.
    float band = fbm(vec2(skyUv.x * 2.0 + t * 1.6, skyUv.y * 3.5 - t * 0.3));
    // Se recortan para que solo se vean en el tercio alto y no manchen el
    // horizonte, que es la parte que el jugador ve de cerca al andar.
    float cloud = smoothstep(0.45, 0.85, band) * smoothstep(0.05, 0.5, h);
    // Las nubes aclaran el cielo: se mezclan hacia el blanco del cenit
    // aclarado, no hacia un gris plano que lo dejaria mas sucio.
    color = mix(color, color + vec3(0.06, 0.06, 0.05), cloud * 0.7);

    // Deliberadamente NO se anade ningun resplandor ni realce sobre el
    // horizonte: el brillo de la escena ya esta aprobado y subirlo aqui
    // cambiaba el tono de la linea de horizonte, que es justo lo que el
    // jugador ve al andar. El movimiento viene de las capas, no del brillo.

    // ---- Bajo el horizonte: se hunde en la oscuridad.
    // El color del preset se mantiene intacto justo en la linea del horizonte,
    // que es donde termina la niebla del escenario, y a partir de ahi baja
    // hacia una version casi negra de si mismo, conservando el tono. Asi el
    // vacio se lee como un pozo profundo y no como un suelo pintado del color
    // del cielo, que es lo que daba el look plano de una sola superficie. Con
    // el mismo color y solo cambiando la luz hacia abajo, el hueco de los
    // railings gana profundidad sin tocar la linea aprobada.
    float below = clamp(-dir.y * 1.7, 0.0, 1.0);
    float sink = smoothstep(0.0, 0.6, below);
    color = mix(color, color * 0.08, sink);

    // ---- Estrellas: solo arriba del todo, y con un parpadeo

    // lento. SeComputan con el hash ya existente para no anadir otro.
    float starCell = hash21(floor(skyUv * 90.0));
    float star = step(0.985, starCell) * smoothstep(0.55, 1.0, h);
    // Parpadeo: cada estrella tiene su fase, por eso multiplica por su hash
    // en vez de por el tiempo global.
    float twinkle = 0.6 + 0.4 * sin(t * 3.0 + starCell * 62.83);
    color += vec3(star * twinkle * 0.5);

    FragColor = vec4(color, 1.0);
}
