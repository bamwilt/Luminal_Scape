package Render3D.collision;

import org.joml.Vector3f;

/**
 * Caja alineada a los ejes en coordenadas de mundo, inmutable.
 *
 * <p>Sustituye al par posición+tamaño de los objetos dibujables como fuente de
 * colisión. Eso permite que la geometría se agrupe en pocas mallas (un solo
 * VAO para todos los muros del mapa) sin que el colisionador tenga que
 * depender de la existencia de un objeto por cada caja: el autotiling produce
 * directamente un {@code Aabb} por cada caja que dibuja.
 */
public final class Aabb {

    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;

    public Aabb(float minX, float minY, float minZ,
                float maxX, float maxY, float maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    /** Solapamiento estricto contra otro volumen definido por sus extremos. */
    public boolean intersects(float otherMinX, float otherMinY, float otherMinZ,
                              float otherMaxX, float otherMaxY, float otherMaxZ) {
        return maxX > otherMinX && minX < otherMaxX
                && maxY > otherMinY && minY < otherMaxY
                && maxZ > otherMinZ && minZ < otherMaxZ;
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMinZ() {
        return minZ;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMaxY() {
        return maxY;
    }

    public float getMaxZ() {
        return maxZ;
    }

    public float getCenterX() {
        return (minX + maxX) * 0.5f;
    }

    public float getCenterY() {
        return (minY + maxY) * 0.5f;
    }

    public float getCenterZ() {
        return (minZ + maxZ) * 0.5f;
    }

    public float getSizeX() {
        return maxX - minX;
    }

    public float getSizeY() {
        return maxY - minY;
    }

    public float getSizeZ() {
        return maxZ - minZ;
    }

    @Override
    public String toString() {
        return "Aabb[x " + minX + ".." + maxX
                + ", y " + minY + ".." + maxY
                + ", z " + minZ + ".." + maxZ + "]";
    }
}
