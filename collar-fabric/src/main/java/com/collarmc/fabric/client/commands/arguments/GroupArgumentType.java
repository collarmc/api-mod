package com.collarmc.fabric.client.commands.arguments;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.GroupType;
import com.collarmc.client.Collar;
import com.collarmc.fabric.client.commands.CommandTargetNotFoundException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GroupArgumentType implements ArgumentType<Group> {

    private final Collar collar;
    private final GroupType type;

    public GroupArgumentType(Collar collar, GroupType type) {
        this.collar = collar;
        this.type = type;
    }

    public static Group getGroup(CommandContext<?> context, String name) {
        return context.getArgument(name, Group.class);
    }

    @Override
    public Group parse(StringReader reader) throws CommandSyntaxException {
        if (collar.getState() != Collar.State.CONNECTED) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("Collar is " + collar.getState());
        }
        String input = reader.readUnquotedString();
        return groupList().stream()
                .filter(group -> group.name.equals(input))
                .findFirst().orElseThrow(() -> new CommandTargetNotFoundException("group '" + input +  "' not found"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        groupList().stream()
                .filter(group -> type == null || group.type.equals(type))
                .filter(group -> group.name.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                .forEach(group -> builder.suggest(group.name));
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return groupList().stream()
                .limit(3).map(group -> group.name).collect(Collectors.toList());
    }

    List<Group> groupList() {
        if (collar.getState() != Collar.State.CONNECTED) {
            return new ArrayList<>();
        }
        if (type == null) {
            return collar.groups().matching(GroupType.GROUP, GroupType.PARTY);
        } else {
            return collar.groups().matching(type);
        }
    }
}
