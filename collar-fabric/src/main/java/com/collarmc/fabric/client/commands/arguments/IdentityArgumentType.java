package com.collarmc.fabric.client.commands.arguments;

import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.client.Collar;
import com.collarmc.fabric.client.commands.CommandTargetNotFoundException;
import com.collarmc.fabric.client.mixins.WorldMixin;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class IdentityArgumentType implements ArgumentType<IdentityArgumentType.IdentityArgument>, WorldMixin {
    private final Collar collar;
    private final MinecraftClient mc;

    public IdentityArgumentType(Collar collar, MinecraftClient mc) {
        this.collar = collar;
        this.mc = mc;
    }

    @Override
    public IdentityArgument parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();
        return identities().stream()
                .filter(identityArgument -> identityArgument.name.equals(input))
                .findFirst().orElseThrow(() -> new CommandTargetNotFoundException("player '" + input +  "' not found"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        identities().forEach(player -> {
            if (player.name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(player.name);
            }
        });
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return identities().stream().limit(5).map(identityArgument -> identityArgument.name).collect(Collectors.toList());
    }

    private Set<IdentityArgument> identities() {
        if (collar.getState() != Collar.State.CONNECTED) {
            return new HashSet<>();
        }
        return ImmutableSet.<IdentityArgument>builder()
                .addAll(allPlayers().map(player -> new IdentityArgument(player, null)).collect(Collectors.toList()))
                .addAll(collar.friends().list().stream().map(friend -> new IdentityArgument(null, friend.profile)).collect(Collectors.toList()))
                .addAll(collar.groups().all().stream().flatMap(group -> group.members.stream()).map(member -> new IdentityArgument(null, member.profile)).collect(Collectors.toList()))
                .build();
    }

    public static final class IdentityArgument {
        public final String name;
        public final PlayerEntity player;
        public final PublicProfile profile;

        public IdentityArgument(PlayerEntity player, PublicProfile profile) {
            if (player != null) {
                this.name = player.getEntityName();
            } else if (profile != null) {
                this.name = profile.name;
            } else {
                throw new IllegalStateException("player and profile was null");
            }
            this.player = player;
            this.profile = profile;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IdentityArgument that = (IdentityArgument) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
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
