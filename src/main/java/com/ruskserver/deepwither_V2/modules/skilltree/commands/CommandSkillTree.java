package com.ruskserver.deepwither_V2.modules.skilltree.commands;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterClass;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterClassProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.gui.ClassSelectionGui;
import com.ruskserver.deepwither_V2.modules.skilltree.gui.SkillTreeGui;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Component
@Command(name = "skilltree", description = "スキルツリーGUIを開きます", aliases = {"stree", "st"})
public class CommandSkillTree implements BasicCommand {

    private final SkillTreeGui skillTreeGui;
    private final ClassSelectionGui classSelectionGui;
    private final CharacterService characterService;
    private final CharacterDataRepository characterDataRepository;

    @Inject
    public CommandSkillTree(SkillTreeGui skillTreeGui, ClassSelectionGui classSelectionGui, CharacterService characterService, CharacterDataRepository characterDataRepository) {
        this.skillTreeGui = skillTreeGui;
        this.classSelectionGui = classSelectionGui;
        this.characterService = characterService;
        this.characterDataRepository = characterDataRepository;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("プレイヤーのみ実行可能です。");
            return;
        }

        UUID uuid = player.getUniqueId();
        characterService.getActiveCharacter(uuid).ifPresentOrElse(c -> {
            characterDataRepository.get(c.characterId()).ifPresentOrElse(data -> {
                CharacterClass charClass = data.get(CharacterClassProvider.KEY);
                if (charClass == null || charClass == CharacterClass.UNKNOWN) {
                    classSelectionGui.open(player);
                } else {
                    // "global" ツリーを直接開く
                    skillTreeGui.openTree(player, "global");
                }
            }, () -> {
                player.sendMessage("キャラクターデータを読み込めませんでした。");
            });
        }, () -> {
            player.sendMessage("アクティブなキャラクターがいません。");
        });
    }
}
