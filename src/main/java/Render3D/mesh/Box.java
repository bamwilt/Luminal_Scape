package Render3D.mesh;

import Render3D.collision.Aabb;

/**
 * Caja alineada a los ejes en coordenadas de mundo, sin nada asociado de GPU.
 *
 * <p>Es la unidad con la que se describe la mazmorra ANTES de subirla: una caja
 * produce un trozo de geometría en el {@link MeshBuilder} y, si toca, también un
 * {@link Aabb} de colisión. Que las dos cosas salgan de la misma caja es lo que
 * garantiza que la huella visible y la de colisión no se separen nunca.
 */
public final class Box {

    public final float minX;
    public final float minY;
    public final float minZ;
    public final float maxX;
    public final float maxY;
    public final float maxZ;

    public Box(float minX, float minY, float minZ,
               float maxX, float maxY, float maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    /** Caja a partir de su centro en planta y su altura, apoyada en {@code baseY}. */
    public static Box centeredOnFloor(float centerX, float baseY, float centerZ,
                                      float width, float height, float depth) {
        return new Box(centerX - width * 0.5f, baseY, centerZ - depth * 0.5f,
                centerX + width * 0.5f, baseY + height, centerZ + depth * 0.5f);
    }

    public float width() {
        return maxX - minX;
    }

    public float height() {
        return maxY - minY;
    }

    public float depth() {
        return maxZ - minZ;
    }

    public Aabb toAabb() {
        return new Aabb(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public String toString() {
        return "Box[x " + minX + ".." + maxX + ", y " + minY + ".." + maxY
                + ", z " + minZ + ".." + maxZ + "]";
    }
}
