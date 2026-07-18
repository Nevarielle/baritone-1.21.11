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

package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.RenderEvent;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 2/13/2020
 */
@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    /**
     * renderLevel no longer receives a PoseStack or the partial tick directly: as of 1.21 it takes a
     * frame graph allocator, a {@link DeltaTracker} and the matrices separately. The first Matrix4f
     * is the frustum/model-view matrix (it is the one multiplied into the model-view stack and
     * passed to prepareCullFrustum) and the third is the projection matrix, so the model-view
     * PoseStack that Baritone's renderers expect is rebuilt from the former.
     */
    @Inject(
            method = "renderLevel",
            at = @At("RETURN")
    )
    private void onStartHand(GraphicsResourceAllocator allocator,
                             DeltaTracker deltaTracker,
                             boolean renderBlockOutline,
                             Camera camera,
                             Matrix4f frustumMatrix,
                             Matrix4f worldSpaceMatrix,
                             Matrix4f projectionMatrix,
                             GpuBufferSlice fogBuffer,
                             Vector4f fogColor,
                             boolean shouldRenderSky,
                             CallbackInfo ci) {
        final float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

        final PoseStack modelViewStack = new PoseStack();
        modelViewStack.mulPose(frustumMatrix);

        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            ibaritone.getGameEventHandler().onRenderPass(new RenderEvent(partialTicks, modelViewStack, projectionMatrix));
        }
    }
}
