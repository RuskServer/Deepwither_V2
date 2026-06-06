package com.ruskserver.deepwither_V2.modules.skill.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterData;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.skill.provider.CharacterSkillSlotProvider;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillCooldownService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillRegistry;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class SkillAssignmentGui implements Listener {

    public static final Component GUI_TITLE = Component.text("スキル割り当て", NamedTextColor.DARK_GREEN);
    private static final int SKILLS_PER_PAGE = 36;
    private static final int BACK_SLOT = 40;
    private static final int MAX_EQUIPPED_SKILLS = 4;

    private final Deepwither_V2 plugin;
    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final SkillRegistry registry;
    private final ManaManager manaManager;
    private final SkillCooldownService cooldownService;
    private final SkillTreeService skillTreeService;
    private final Map<UUID, String> selectedSkillMap = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    @Inject
    public SkillAssignmentGui(
            Deepwither_V2 plugin,
            CharacterDataRepository characterDataRepository,
            CharacterService characterService,
            SkillRegistry registry,
            ManaManager manaManager,
            SkillCooldownService cooldownService,
            SkillTreeService skillTreeService
    ) {
        this.plugin = plugin;
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.registry = registry;
        this.manaManager = manaManager;
        this.cooldownService = cooldownService;
        this.skillTreeService = skillTreeService;
    }

    public void open(Player player) {
        getActiveSkillData(player).ifPresent(activeData -> {
            CharacterData data = activeData.data();
            CharacterSkillSlotProvider.SkillSlotData slotData = data.get(CharacterSkillSlotProvider.KEY);
            if (slotData == null) return;

            Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
            UUID uuid = player.getUniqueId();
            String selectedId = selectedSkillMap.get(uuid);

            List<Skill> skills = new ArrayList<>(registry.getAll());
            skills.removeIf(skill -> !skillTreeService.isSkillUnlocked(player, skill.getId()));
            skills.sort(Comparator.comparing(Skill::getId));

            int maxPage = Math.max(0, (skills.size() - 1) / SKILLS_PER_PAGE);
            int currentPage = Math.max(0, Math.min(playerPages.getOrDefault(uuid, 0), maxPage));
            playerPages.put(uuid, currentPage);

            int start = currentPage * SKILLS_PER_PAGE;
            int end = Math.min(start + SKILLS_PER_PAGE, skills.size());
            for (int i = start; i < end; i++) {
                Skill skill = skills.get(i);
                gui.setItem(i - start, buildSkillItem(player, skill, skill.getId().equals(selectedId), slotData));
            }

            ItemStack empty = namedItem(Material.GRAY_STAINED_GLASS_PANE, Component.text("空き", NamedTextColor.DARK_GRAY));
            for (int i = end - start; i < SKILLS_PER_PAGE; i++) {
                gui.setItem(i, empty);
            }

            ItemStack separator = namedItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
            for (int i = 36; i <= 44; i++) {
                gui.setItem(i, separator);
            }
            if (currentPage > 0) {
                gui.setItem(36, buildNavButton("前へ", currentPage - 1));
            }
            if (currentPage < maxPage) {
                gui.setItem(44, buildNavButton("次へ", currentPage + 1));
            }
            gui.setItem(BACK_SLOT, buildGuideItem());

            for (int i = 0; i < CharacterSkillSlotProvider.SLOT_COUNT; i++) {
                gui.setItem(45 + i, buildSlotItem(player, i, slotData));
            }

            player.openInventory(gui);
        });
    }

    private ItemStack buildSkillItem(Player player, Skill skill, boolean selected, CharacterSkillSlotProvider.SkillSlotData slotData) {
        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();

        Component name = Component.text(skill.getDisplayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);
        if (selected) {
            name = Component.text("> ", NamedTextColor.YELLOW).append(name);
        }
        meta.displayName(name);

        SkillContext context = new SkillContext(player, skill, skillTreeService.getSkillLevel(player, skill.getId()), manaManager, cooldownService);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: " + skill.getId(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("種別: " + skill.getCategory() + " / 対象: " + skill.getTargetType(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        for (String line : skill.getDescription()) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("マナ: " + formatDouble(skill.getManaCost(context)), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("CD: " + formatDuration(skill.getCooldown(context)), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("詠唱: " + formatDuration(skill.getCastTime(context)), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        String conflict = getConflictName(skill, slotData);
        if (conflict != null) {
            lore.add(Component.text("競合中: " + conflict, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else if (selected) {
            lore.add(Component.text("選択中: 下段スロットをクリック", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("左クリックで選択", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("skill:" + skill.getId(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        if (selected) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSlotItem(Player player, int slot, CharacterSkillSlotProvider.SkillSlotData slotData) {
        String skillId = slotData.getSkill(slot);
        if (skillId == null) {
            ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("スロット" + (slot + 1) + " - 未割り当て", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("スキルを選択してからクリック", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("slot:" + slot, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            return item;
        }

        Skill skill = registry.get(skillId);
        Material material = skill == null ? Material.BARRIER : skill.getIcon();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("スロット" + (slot + 1), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("割り当て: " + (skill == null ? skillId : skill.getDisplayName()), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("左クリック: 選択中スキルを割り当て", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("右クリック: スロットをクリア", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("slot:" + slot, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavButton(String label, int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("page:" + page, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildGuideItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("操作ガイド", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("上段: スキル選択", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("下段: ホットバースロット", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("最大 " + MAX_EQUIPPED_SKILLS + " 個まで装備可能", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().title().equals(GUI_TITLE)) return;
        UUID uuid = player.getUniqueId();
        // 1tick遅延でクリア: スキル選択やページ移動によるGUI再構築で発生するcloseを誤検知しない
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.getOpenInventory().title().equals(GUI_TITLE)) {
                selectedSkillMap.remove(uuid);
                playerPages.remove(uuid);
            }
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(GUI_TITLE)) return;

        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        List<Component> lore = clicked.getItemMeta().lore();
        if (lore == null) return;

        String skillId = null;
        int slot = -1;
        int page = -1;

        for (Component line : lore) {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            if (plain.startsWith("skill:")) {
                skillId = plain.substring("skill:".length()).trim();
            } else if (plain.startsWith("slot:")) {
                slot = parseInt(plain.substring("slot:".length()));
            } else if (plain.startsWith("page:")) {
                page = parseInt(plain.substring("page:".length()));
            }
        }

        UUID uuid = player.getUniqueId();
        if (page >= 0) {
            playerPages.put(uuid, page);
            open(player);
            return;
        }

        if (skillId != null) {
            selectSkill(player, skillId);
            return;
        }

        if (slot >= 0) {
            handleSlotClick(player, slot, event.getClick());
        }
    }

    private void selectSkill(Player player, String skillId) {
        getActiveSkillData(player).ifPresent(activeData -> {
            CharacterData data = activeData.data();
            CharacterSkillSlotProvider.SkillSlotData slotData = data.get(CharacterSkillSlotProvider.KEY);
            Skill skill = registry.get(skillId);
            if (skill == null || slotData == null) return;
            if (!skillTreeService.isSkillUnlocked(player, skillId)) {
                player.sendMessage(Component.text("このスキルはまだ解放されていません。", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            String conflict = getConflictName(skill, slotData);
            if (conflict != null) {
                player.sendMessage(Component.text("装備中のスキル「" + conflict + "」と競合しています。", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            selectedSkillMap.put(player.getUniqueId(), skillId);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            open(player);
        });
    }

    private void handleSlotClick(Player player, int slot, ClickType clickType) {
        getActiveSkillData(player).ifPresent(activeData -> {
            CharacterData data = activeData.data();
            CharacterSkillSlotProvider.SkillSlotData slotData = data.get(CharacterSkillSlotProvider.KEY);
            if (slotData == null) return;

            if (clickType == ClickType.RIGHT) {
                slotData.setSkill(slot, null);
                data.markDirty(CharacterSkillSlotProvider.KEY);
                characterDataRepository.save(activeData.characterId(), data);
                selectedSkillMap.remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                open(player);
                return;
            }

            String selected = selectedSkillMap.get(player.getUniqueId());
            if (selected == null) {
                player.sendMessage(Component.text("先にスキルを選択してください。", NamedTextColor.RED));
                return;
            }

            Skill selectedSkill = registry.get(selected);
            if (selectedSkill == null) return;
            if (!skillTreeService.isSkillUnlocked(player, selected)) {
                player.sendMessage(Component.text("このスキルはまだ解放されていません。", NamedTextColor.RED));
                selectedSkillMap.remove(player.getUniqueId());
                open(player);
                return;
            }

            String conflict = getConflictName(selectedSkill, slotData, slot);
            if (conflict != null) {
                player.sendMessage(Component.text("スキル「" + conflict + "」と競合しています。", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            for (int i = 0; i < CharacterSkillSlotProvider.SLOT_COUNT; i++) {
                if (i != slot && selected.equals(slotData.getSkill(i))) {
                    slotData.setSkill(i, null);
                }
            }

            if (slotData.getSkill(slot) == null && slotData.getEquippedCount() >= MAX_EQUIPPED_SKILLS) {
                player.sendMessage(Component.text("スキルは最大 " + MAX_EQUIPPED_SKILLS + " 個まで装備できます。", NamedTextColor.RED));
                return;
            }

            slotData.setSkill(slot, selected);
            data.markDirty(CharacterSkillSlotProvider.KEY);
            characterDataRepository.save(activeData.characterId(), data);
            selectedSkillMap.remove(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            open(player);
        });
    }

    private String getConflictName(Skill skill, CharacterSkillSlotProvider.SkillSlotData slotData) {
        return getConflictName(skill, slotData, -1);
    }

    private String getConflictName(Skill skill, CharacterSkillSlotProvider.SkillSlotData slotData, int ignoredSlot) {
        for (int i = 0; i < CharacterSkillSlotProvider.SLOT_COUNT; i++) {
            if (i == ignoredSlot) continue;
            String equippedId = slotData.getSkill(i);
            if (equippedId == null) continue;

            Skill equipped = registry.get(equippedId);
            if (equipped == null) continue;
            if (skill.getConflicts().contains(equippedId) || equipped.getConflicts().contains(skill.getId())) {
                return equipped.getDisplayName();
            }
        }
        return null;
    }

    private java.util.Optional<ActiveSkillData> getActiveSkillData(Player player) {
        return characterService.getCachedActiveCharacter(player.getUniqueId())
                .flatMap(character -> characterDataRepository.get(character.characterId())
                        .map(data -> {
                            CharacterSkillSlotProvider.SkillSlotData slotData = data.get(CharacterSkillSlotProvider.KEY);
                            if (slotData == null) {
                                data.set(CharacterSkillSlotProvider.KEY, new CharacterSkillSlotProvider.SkillSlotData());
                                characterDataRepository.save(character.characterId(), data);
                            }
                            return new ActiveSkillData(character.characterId(), data);
                        }));
    }

    private record ActiveSkillData(UUID characterId, CharacterData data) {
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return "なし";
        }
        return String.format("%.1fs", duration.toMillis() / 1000.0);
    }

    private String formatDouble(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format("%.1f", value);
    }
}
