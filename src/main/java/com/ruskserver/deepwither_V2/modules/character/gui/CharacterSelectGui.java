package com.ruskserver.deepwither_V2.modules.character.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterNameTagService;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.CharacterStatus;
import com.ruskserver.deepwither_V2.modules.character.GameCharacter;
import com.ruskserver.deepwither_V2.modules.gui.GuiClickContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiItemBuilder;
import com.ruskserver.deepwither_V2.modules.gui.GuiRenderContext;
import com.ruskserver.deepwither_V2.modules.gui.GuiView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class CharacterSelectGui implements GuiView {
    public static final String ID = "character_select";
    private static final int SIZE = 54;
    private static final int CREATE_SLOT = 49;
    private static final int[] CHARACTER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final CharacterService characterService;
    private final CharacterNameTagService nameTagService;
    private final Deepwither_V2 plugin;

    @Inject
    public CharacterSelectGui(CharacterService characterService, CharacterNameTagService nameTagService, Deepwither_V2 plugin) {
        this.characterService = characterService;
        this.nameTagService = nameTagService;
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getTitle(Player player, GuiContext context) {
        return Component.text("キャラクター選択", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public int getSize(Player player, GuiContext context) {
        return SIZE;
    }

    @Override
    public void render(GuiRenderContext context) {
        for (int i = 0; i < SIZE; i++) {
            context.setItem(i, GuiItemBuilder.background(Material.GRAY_STAINED_GLASS_PANE));
        }

        Player player = context.player();
        List<GameCharacter> characters = characterService.getCachedCharacters(player.getUniqueId());
        Optional<GameCharacter> active = characterService.getCachedActiveCharacter(player.getUniqueId());

        for (int i = 0; i < Math.min(characters.size(), CHARACTER_SLOTS.length); i++) {
            GameCharacter character = characters.get(i);
            boolean activeCharacter = active.map(GameCharacter::characterId).filter(character.characterId()::equals).isPresent();
            context.setItem(CHARACTER_SLOTS[i], createCharacterItem(character, activeCharacter));
        }

        context.setItem(CREATE_SLOT, GuiItemBuilder.of(Material.EMERALD_BLOCK)
                .name(Component.text("新規キャラクター作成", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(
                        Component.text("モードを選んで新しいキャラクターを作成します。", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("クリックして作成画面へ", NamedTextColor.YELLOW)
                )
                .build());
    }

    @Override
    public void onClick(GuiClickContext context) {
        Player player = context.player();
        int slot = context.slot();
        if (slot == CREATE_SLOT) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            context.open(CharacterCreateGui.ID);
            return;
        }

        int index = indexOfSlot(slot);
        if (index < 0) {
            return;
        }

        List<GameCharacter> characters = characterService.getCachedCharacters(player.getUniqueId());
        if (index >= characters.size()) {
            return;
        }

        GameCharacter character = characters.get(index);
        if (!character.isSelectable()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            player.sendMessage(Component.text("死亡済みキャラクターは選択できません。", NamedTextColor.RED));
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID characterId = character.characterId();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        player.sendMessage(Component.text("キャラクターを選択しています...", NamedTextColor.GRAY));

        characterService.switchCharacterAsync(player, characterId,
                // 成功
                () -> {
                    Player online = plugin.getServer().getPlayer(playerId);
                    if (online == null || !online.isOnline()) return;
                    nameTagService.refresh(online, character.mode());
                    online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                    online.sendMessage(Component.text("キャラクターを選択しました: ", NamedTextColor.GREEN)
                            .append(Component.text(character.name(), NamedTextColor.YELLOW)));
                    online.closeInventory();
                },
                // 失敗
                () -> {
                    Player online = plugin.getServer().getPlayer(playerId);
                    if (online == null || !online.isOnline()) return;
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                    online.sendMessage(Component.text("キャラクターの選択に失敗しました。", NamedTextColor.RED));
                }
        );
    }

    private ItemStack createCharacterItem(GameCharacter character, boolean activeCharacter) {
        Material material = switch (character.mode()) {
            case STANDARD -> Material.PLAYER_HEAD;
            case SOFT_HARDCORE -> Material.TOTEM_OF_UNDYING;
            case TRUE_HARDCORE -> Material.WITHER_SKELETON_SKULL;
        };
        NamedTextColor nameColor = character.isSelectable() ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY;
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("モード: ", NamedTextColor.GRAY).append(Component.text(character.mode().getDisplayName(), NamedTextColor.AQUA)));
        lore.add(Component.text("状態: ", NamedTextColor.GRAY).append(Component.text(character.status().name(), statusColor(character.status()))));
        if (activeCharacter) {
            lore.add(Component.text("現在選択中", NamedTextColor.GOLD));
        }
        lore.add(Component.empty());
        lore.add(character.isSelectable()
                ? Component.text("クリックして選択", NamedTextColor.YELLOW)
                : Component.text("死亡済み - 管理者復活のみ", NamedTextColor.RED));

        return GuiItemBuilder.of(material)
                .name(Component.text(character.name(), nameColor, TextDecoration.BOLD))
                .lore(lore)
                .build();
    }

    private NamedTextColor statusColor(CharacterStatus status) {
        return status == CharacterStatus.ALIVE ? NamedTextColor.GREEN : NamedTextColor.RED;
    }

    private int indexOfSlot(int slot) {
        for (int i = 0; i < CHARACTER_SLOTS.length; i++) {
            if (CHARACTER_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}
