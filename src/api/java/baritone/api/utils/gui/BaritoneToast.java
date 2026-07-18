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

package baritone.api.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class BaritoneToast implements Toast {

    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");

    private String title;
    private String subtitle;
    private long firstDrawTime;
    private boolean newDisplay;
    private final long totalShowTime;
    private Visibility visibility = Visibility.SHOW;

    public BaritoneToast(Component titleComponent, Component subtitleComponent, long totalShowTime) {
        this.title = titleComponent.getString();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getString();
        this.totalShowTime = totalShowTime;
    }

    @Override
    public Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(ToastManager toastManager, long visibilityTime) {
        if (this.newDisplay) {
            this.firstDrawTime = visibilityTime;
            this.newDisplay = false;
        }

        this.visibility = visibilityTime - this.firstDrawTime < this.totalShowTime
                ? Visibility.SHOW
                : Visibility.HIDE;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Font font, long delta) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());

        if (this.subtitle == null) {
            guiGraphics.drawString(font, this.title, 18, 12, -11534256, false);
        } else {
            guiGraphics.drawString(font, this.title, 18, 7, -11534256, false);
            guiGraphics.drawString(font, this.subtitle, 18, 18, -16777216, false);
        }
    }

    public void setDisplayedText(Component titleComponent, Component subtitleComponent) {
        this.title = titleComponent.getString();
        this.subtitle = subtitleComponent == null ? null : subtitleComponent.getString();
        this.newDisplay = true;
    }

    public static void addOrUpdate(ToastManager toast, Component title, Component subtitle, long totalShowTime) {
        BaritoneToast baritonetoast = toast.getToast(BaritoneToast.class, Toast.NO_TOKEN);

        if (baritonetoast == null) {
            toast.addToast(new BaritoneToast(title, subtitle, totalShowTime));
        } else {
            baritonetoast.setDisplayedText(title, subtitle);
        }
    }

    public static void addOrUpdate(Component title, Component subtitle) {
        addOrUpdate(Minecraft.getInstance().getToastManager(), title, subtitle, baritone.api.BaritoneAPI.getSettings().toastTimer.value);
    }
}
