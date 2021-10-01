/*
 * MIT License
 *
 * Copyright (c) 2020 Headpat Services
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.collarmc.fabric.client.commands.arguments;

import com.collarmc.fabric.client.commands.CommandTargetNotFoundException;
import com.collarmc.fabric.client.mixins.WorldMixin;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlayerArgumentType implements ArgumentType<PlayerEntity>, WorldMixin {
	private final MinecraftClient mc;

	public PlayerArgumentType(MinecraftClient mc) {
		this.mc = mc;
	}

	/**
	 * Quick shortcut of {@link CommandContext#getArgument(String, Class)} for a player argument.
	 *
	 * @param context Command context.
	 * @param name    Name of the argument.
	 * @return The player specified by the argument name in the command context.
	 */
	public static PlayerEntity getPlayer(CommandContext<?> context, String name) {
		return context.getArgument(name, PlayerEntity.class);
	}

	@Override
	public PlayerEntity parse(StringReader reader) throws CommandSyntaxException {
		String input = reader.readUnquotedString();
		return allPlayers()
				.filter(thePlayer -> thePlayer.getEntityName().equalsIgnoreCase(input))
				.findFirst().orElseThrow(() -> new CommandTargetNotFoundException("player '" + input +  "' not found"));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		allPlayers().forEach(player -> {
			if (player.getEntityName().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
				builder.suggest(player.getEntityName());
			}
		});
		return builder.buildFuture();
	}

	@Override
	public Collection<String> getExamples() {
		return allPlayers().map(PlayerEntity::getEntityName).collect(Collectors.toList());
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
