package com.collarmc.fabric.client.commands.arguments;

import com.collarmc.api.groups.GroupType;
import com.collarmc.client.Collar;
import com.collarmc.client.api.groups.GroupInvitation;
import com.collarmc.fabric.client.commands.CommandTargetNotFoundException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class InvitationArgumentType implements ArgumentType<GroupInvitation> {

    private final Collar collar;
    private final GroupType type;

    public InvitationArgumentType(Collar collar, GroupType type) {
        this.collar = collar;
        this.type = type;
    }

    public static GroupInvitation getInvitation(CommandContext<?> context, String name) {
        return context.getArgument(name, GroupInvitation.class);
    }

    @Override
    public GroupInvitation parse(StringReader reader) throws CommandSyntaxException {
        if (collar.getState() != Collar.State.CONNECTED) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("Collar is " + collar.getState());
        }
        String input = reader.readUnquotedString();
        return collar.groups().invitations().stream()
                .filter(invitation -> invitation.type.equals(type))
                .filter(invitation -> invitation.name.equals(input))
                .findFirst().orElseThrow(() -> new CommandTargetNotFoundException("invitation to group '" + input +  "' not found"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (collar.getState() != Collar.State.CONNECTED) {
            return builder.buildFuture();
        }
        collar.groups().invitations().stream().filter(invitation -> invitation.type.equals(type))
                .filter(invitation -> invitation.name.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                .forEach(group -> builder.suggest(group.name));
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        if (collar.getState() != Collar.State.CONNECTED) {
            return new HashSet<>();
        }
        return collar.groups().invitations().stream()
                .filter(invitation -> invitation.type.equals(type))
                .limit(3)
                .map(invitation -> invitation.name)
                .collect(Collectors.toList());
    }
}
