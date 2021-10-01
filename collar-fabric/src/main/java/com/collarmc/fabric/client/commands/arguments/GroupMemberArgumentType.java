package com.collarmc.fabric.client.commands.arguments;

import com.collarmc.api.groups.Member;
import com.collarmc.client.Collar;
import com.collarmc.fabric.client.commands.CommandTargetNotFoundException;
import com.collarmc.fabric.client.mixins.WorldMixin;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class GroupMemberArgumentType implements ArgumentType<Member>, WorldMixin {

    private final Collar collar;
    private final MinecraftClient mc;

    public GroupMemberArgumentType(Collar collar, MinecraftClient mc) {
        this.mc = mc;
        this.collar = collar;
    }

    @Override
    public Member parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();
        return members().stream()
                .filter(thePlayer -> thePlayer.profile.name.equals(input))
                .findFirst().orElseThrow(() -> new CommandTargetNotFoundException("group member '" + reader.readUnquotedString() +  "' not found"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        members().forEach(member -> {
            // Allow searching by the players minecraft name
            if (member.player.minecraftPlayer != null) {
                findPlayerById(member.player.minecraftPlayer.id).ifPresent(player -> {
                    if (player.getEntityName().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                        builder.suggest(player.getEntityName());
                    }
                });
            }
            if (member.profile.name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(member.profile.name);
            }
        });
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return members().stream().limit(5).map(GroupMemberArgumentType::name).collect(Collectors.toList());
    }

    private static String name(Member member) {
        return member.profile.name;
    }

    private List<Member> members() {
        if (collar.getState() != Collar.State.CONNECTED) {
            return new ArrayList<>();
        }
        Collection<Member> values = collar.groups().all().stream()
                .flatMap(group -> group.members.stream())
                .collect(Collectors.toMap(member -> member.profile.id, o -> o, (member, member2) -> member)).values();
        return ImmutableList.copyOf(values);
    }

    @Override
    public ClientWorld world() {
        return mc.world;
    }

    @Override
    public PlayerEntity player() {
        return mc.player;
    }
}
