/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.utils.accessor.IEntityRenderManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

/**
 * Line rendering for 1.21.11.
 * <p>
 * The old immediate-mode path (Tesselator/BufferBuilder plus manual shader and GL state juggling)
 * was removed upstream, so drawing now goes through the shared {@link MultiBufferSource} using the
 * vanilla {@code lines} render type. Vertices are appended between {@link #startLines} and
 * {@link #endLines}, which flushes the batch.
 */
public interface IRenderer {

    IEntityRenderManager renderManager = (IEntityRenderManager) Minecraft.getInstance().getEntityRenderDispatcher();
    TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    Settings settings = BaritoneAPI.getSettings();

    float[] color = new float[]{1.0F, 1.0F, 1.0F, 255.0F};

    /**
     * Holds the buffer that the {@code emit*} calls are currently writing into. Interface fields are
     * implicitly final, so the mutable state lives in this holder.
     */
    Batch batch = new Batch();

    final class Batch {
        MultiBufferSource.BufferSource source;
        VertexConsumer consumer;
    }

    static void glColor(Color color, float alpha) {
        float[] colorComponents = color.getColorComponents(null);
        IRenderer.color[0] = colorComponents[0];
        IRenderer.color[1] = colorComponents[1];
        IRenderer.color[2] = colorComponents[2];
        IRenderer.color[3] = alpha;
    }

    static void startLines(Color color, float alpha, float lineWidth, boolean ignoreDepth) {
        glColor(color, alpha);

        // TODO(1.21.11): ignoreDepth (drawing paths through walls) is not honoured yet. Depth
        // testing is now baked into the RenderPipeline behind a RenderType, so restoring it needs a
        // custom no-depth-test pipeline rather than a RenderSystem call.
        batch.source = Minecraft.getInstance().renderBuffers().bufferSource();
        batch.consumer = batch.source.getBuffer(RenderTypes.lines());
    }

    static void startLines(Color color, float lineWidth, boolean ignoreDepth) {
        startLines(color, .4f, lineWidth, ignoreDepth);
    }

    static void endLines(boolean ignoredDepth) {
        if (batch.source != null) {
            batch.source.endBatch(RenderTypes.lines());
        }
        batch.consumer = null;
        batch.source = null;
    }

    static void emitLine(PoseStack stack, double x1, double y1, double z1, double x2, double y2, double z2) {
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double dz = z2 - z1;

        final double invMag = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
        final float nx = (float) (dx * invMag);
        final float ny = (float) (dy * invMag);
        final float nz = (float) (dz * invMag);

        emitLine(stack, x1, y1, z1, x2, y2, z2, nx, ny, nz);
    }

    static void emitLine(PoseStack stack,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double nx, double ny, double nz) {
        emitLine(stack,
                (float) x1, (float) y1, (float) z1,
                (float) x2, (float) y2, (float) z2,
                (float) nx, (float) ny, (float) nz
        );
    }

    static void emitLine(PoseStack stack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float nx, float ny, float nz) {
        if (batch.consumer == null) {
            return;
        }
        final PoseStack.Pose pose = stack.last();

        batch.consumer.addVertex(pose, x1, y1, z1)
                .setColor(color[0], color[1], color[2], color[3])
                .setNormal(pose, nx, ny, nz);
        batch.consumer.addVertex(pose, x2, y2, z2)
                .setColor(color[0], color[1], color[2], color[3])
                .setNormal(pose, nx, ny, nz);
    }

    static void emitAABB(PoseStack stack, AABB aabb) {
        AABB toDraw = aabb.move(-renderManager.renderPosX(), -renderManager.renderPosY(), -renderManager.renderPosZ());

        // bottom
        emitLine(stack, toDraw.minX, toDraw.minY, toDraw.minZ, toDraw.maxX, toDraw.minY, toDraw.minZ, 1.0, 0.0, 0.0);
        emitLine(stack, toDraw.maxX, toDraw.minY, toDraw.minZ, toDraw.maxX, toDraw.minY, toDraw.maxZ, 0.0, 0.0, 1.0);
        emitLine(stack, toDraw.maxX, toDraw.minY, toDraw.maxZ, toDraw.minX, toDraw.minY, toDraw.maxZ, -1.0, 0.0, 0.0);
        emitLine(stack, toDraw.minX, toDraw.minY, toDraw.maxZ, toDraw.minX, toDraw.minY, toDraw.minZ, 0.0, 0.0, -1.0);
        // top
        emitLine(stack, toDraw.minX, toDraw.maxY, toDraw.minZ, toDraw.maxX, toDraw.maxY, toDraw.minZ, 1.0, 0.0, 0.0);
        emitLine(stack, toDraw.maxX, toDraw.maxY, toDraw.minZ, toDraw.maxX, toDraw.maxY, toDraw.maxZ, 0.0, 0.0, 1.0);
        emitLine(stack, toDraw.maxX, toDraw.maxY, toDraw.maxZ, toDraw.minX, toDraw.maxY, toDraw.maxZ, -1.0, 0.0, 0.0);
        emitLine(stack, toDraw.minX, toDraw.maxY, toDraw.maxZ, toDraw.minX, toDraw.maxY, toDraw.minZ, 0.0, 0.0, -1.0);
        // corners
        emitLine(stack, toDraw.minX, toDraw.minY, toDraw.minZ, toDraw.minX, toDraw.maxY, toDraw.minZ, 0.0, 1.0, 0.0);
        emitLine(stack, toDraw.maxX, toDraw.minY, toDraw.minZ, toDraw.maxX, toDraw.maxY, toDraw.minZ, 0.0, 1.0, 0.0);
        emitLine(stack, toDraw.maxX, toDraw.minY, toDraw.maxZ, toDraw.maxX, toDraw.maxY, toDraw.maxZ, 0.0, 1.0, 0.0);
        emitLine(stack, toDraw.minX, toDraw.minY, toDraw.maxZ, toDraw.minX, toDraw.maxY, toDraw.maxZ, 0.0, 1.0, 0.0);
    }

    static void emitAABB(PoseStack stack, AABB aabb, double expand) {
        emitAABB(stack, aabb.inflate(expand, expand, expand));
    }

    static void emitLine(PoseStack stack, Vec3 start, Vec3 end) {
        double vpX = renderManager.renderPosX();
        double vpY = renderManager.renderPosY();
        double vpZ = renderManager.renderPosZ();
        emitLine(stack, start.x - vpX, start.y - vpY, start.z - vpZ, end.x - vpX, end.y - vpY, end.z - vpZ);
    }
}
