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

import baritone.api.utils.input.Input;
import net.minecraft.world.phys.Vec2;

/**
 * As of 1.21 the client input holder is {@link net.minecraft.client.player.ClientInput}: the pressed
 * keys live in an immutable {@code Input} record and the movement impulses are a {@link Vec2}, so
 * this mirrors what vanilla {@code KeyboardInput} does instead of writing the old mutable fields.
 * <p>
 * Note that the sneak slowdown is deliberately not applied here — vanilla moved it out of the input
 * and into the player movement code.
 */
public class PlayerMovementInput extends net.minecraft.client.player.ClientInput {

    private final InputOverrideHandler handler;

    PlayerMovementInput(InputOverrideHandler handler) {
        this.handler = handler;
    }

    @Override
    public void tick() {
        this.keyPresses = new net.minecraft.world.entity.player.Input(
                handler.isInputForcedDown(Input.MOVE_FORWARD),
                handler.isInputForcedDown(Input.MOVE_BACK),
                handler.isInputForcedDown(Input.MOVE_LEFT),
                handler.isInputForcedDown(Input.MOVE_RIGHT),
                handler.isInputForcedDown(Input.JUMP), // oppa gangnam style
                handler.isInputForcedDown(Input.SNEAK),
                handler.isInputForcedDown(Input.SPRINT)
        );

        this.moveVector = new Vec2(
                toAxis(this.keyPresses.left(), this.keyPresses.right()),
                toAxis(this.keyPresses.forward(), this.keyPresses.backward())
        );
    }

    private static float toAxis(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
