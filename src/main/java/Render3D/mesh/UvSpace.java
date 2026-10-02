package Render3D.mesh;

/**
 * Referencia de coordenadas usada al calcular las UV de cada cara.
 *
 * <p>En ambos casos la UV es proporcional al tamaño real de la cara
 * (world-space tiling): {@code U_max - U_min = anchoDeCara * u_scale} y
 * {@code V_max - V_min = altoDeCara * v_scale}. Lo único que cambia es el
 * origen desde el que se cuentan esos tamaños, y eso decide si la textura
 * queda alineada entre cajas contiguas o si "nada" con el objeto al moverse.
 */
public enum UvSpace {

    /**
     * La geometría ya está en coordenadas de mundo. La UV sale directamente de
     * la posición del vértice, así que dos cajas que comparten cara quedan
     * perfectamente alineadas: la textura no tiene costuras ni reinicios en las
     * uniones. Es lo que usan las mallas estáticas de la mazmorra (muros,
     * pisos, techos), que ya viven en el mundo y no se mueven.
     *
     * <p>Requiere que las texturas se carguen con {@code GL_REPEAT}: las UV
     * pueden ser mayores que 1.
     */
    WORLD,

    /**
     * La geometría está en el espacio local del objeto (centrada en el origen) y
     * se lleva al mundo con la matriz {@code model}. La UV se mide desde el
     * centro de la caja, no desde el mundo, para que la textura quede pegada al
     * objeto cuando rota o se desplaza. Es lo que usan los objetos móviles
     * (los items que giran).
     */
    LOCAL
}
