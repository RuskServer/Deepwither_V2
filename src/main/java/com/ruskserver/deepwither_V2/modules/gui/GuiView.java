package com.ruskserver.deepwither_V2.modules.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface GuiView {

    String getId();

    Component getTitle(Player player, GuiContext context);

    int getSize(Player player, GuiContext context);

    void render(GuiRenderContext context);

    void onClick(GuiClickContext context);
}
