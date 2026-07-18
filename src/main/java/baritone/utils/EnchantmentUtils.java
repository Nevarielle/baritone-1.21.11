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

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * As of 1.21 enchantments are data-driven registry entries, so the {@link Enchantments} constants
 * are {@link ResourceKey}s that have to be resolved against the world's registries before they can
 * be queried. The convenience helpers that used to live on {@link EnchantmentHelper}
 * ({@code hasFrostWalker}, {@code getDepthStrider}, ...) were removed at the same time.
 *
 * @author Baritone 1.21.11 migration
 */
public final class EnchantmentUtils {

    private EnchantmentUtils() {}

    public static Holder<Enchantment> resolve(LivingEntity entity, ResourceKey<Enchantment> key) {
        return entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    public static int getLevel(LivingEntity entity, ResourceKey<Enchantment> key, ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(resolve(entity, key), stack);
    }

    /**
     * Variant for static call sites that have no entity at hand. Resolving the enchantment needs
     * access to the world's registries, so this falls back to the local player and reports level 0
     * when there is no world loaded.
     */
    public static int getLevel(ResourceKey<Enchantment> key, ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0 : getLevel(player, key, stack);
    }

    /**
     * Level of a boots-only enchantment (frost walker, depth strider) on the entity's footwear.
     */
    public static int getBootsLevel(LivingEntity entity, ResourceKey<Enchantment> key) {
        return getLevel(entity, key, entity.getItemBySlot(EquipmentSlot.FEET));
    }
}
