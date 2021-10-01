package com.collarmc.fabric.client.commands;

import com.collarmc.api.friends.Friend;
import com.collarmc.api.friends.Status;
import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.GroupType;
import com.collarmc.api.location.Dimension;
import com.collarmc.api.location.Location;
import com.collarmc.api.waypoints.Waypoint;
import com.collarmc.client.Collar;
import com.collarmc.client.api.groups.GroupInvitation;
import com.collarmc.fabric.client.commands.arguments.*;
import com.collarmc.fabric.client.commands.arguments.IdentityArgumentType.IdentityArgument;
import com.collarmc.fabric.client.commands.arguments.WaypointArgumentType.WaypointArgument;
import com.collarmc.fabric.client.messaging.Messages;
import com.collarmc.fabric.client.mixins.DisplayMixin;
import com.collarmc.fabric.client.mixins.LocationMixin;
import com.collarmc.fabric.client.mixins.WorldMixin;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.collarmc.fabric.client.commands.arguments.DimensionArgumentType.dimension;
import static com.collarmc.fabric.client.commands.arguments.GroupArgumentType.getGroup;
import static com.collarmc.fabric.client.commands.arguments.InvitationArgumentType.getInvitation;
import static com.collarmc.fabric.client.commands.arguments.PlayerArgumentType.getPlayer;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public final class Commands<S> implements DisplayMixin, WorldMixin, LocationMixin {

    private final Collar collar;
    private final Messages messages;
    private final MinecraftClient mc;
    private final boolean prefixed;

    public Commands(Collar collar, Messages messages, MinecraftClient mc, boolean prefixed) {
        this.collar = collar;
        this.messages = messages;
        this.mc = mc;
        this.prefixed = prefixed;
    }

    public void register(CommandDispatcher<S> dispatcher) {
        registerServiceCommands(dispatcher);
        registerFriendCommands(dispatcher);
        registerLocationCommands(dispatcher);
        registerWaypointCommands(dispatcher);
        registerGroupCommands(GroupType.PARTY, dispatcher);
        registerGroupCommands(GroupType.GROUP, dispatcher);
        registerChatCommands(dispatcher);
    }

    private LiteralArgumentBuilder<S> prefixed(String name, LiteralArgumentBuilder<S> argumentBuilder) {
        return this.prefixed ? literal("collar").then(literal(name).then(argumentBuilder)) : literal(name).then(argumentBuilder);
    }

    private LiteralArgumentBuilder<S> prefixed(String name, ArgumentBuilder<S, ?> argument) {
        return this.prefixed ? literal("collar").then(literal(name).then(argument)) : literal(name).then(argument);
    }

    private LiteralArgumentBuilder<S> prefixed(String name, Command<S> command) {
        return this.prefixed ? literal("collar").then(literal(name).executes(command)) : literal(name).executes(command);
    }

    private void registerServiceCommands(CommandDispatcher<S> dispatcher) {
        // collar connect
        dispatcher.register(prefixed("connect", context -> {
            collar.connect();
            return 1;
        }));

        // collar disconnect
        dispatcher.register(prefixed("disconnect", context -> {
            collar.disconnect();
            return 1;
        }));

        // collar status
        dispatcher.register(prefixed("status", context -> {
            displayInfoMessage("Collar is " + collar.getState().name().toLowerCase());
            return 1;
        }));

        // collar me
        dispatcher.register(prefixed("me", context -> {
            collar.identities().resolveProfile(collar.player()).thenAccept(publicProfile -> {
                PlayerEntity player = findPlayerById(collar.player().minecraftPlayer.id).orElseThrow(() -> new IllegalStateException("should have been able to find self"));
                publicProfile.ifPresent(profile -> displayInfoMessage("You are connected as " + profile.name + " on minecraft account " + player.getEntityName()));
            });
            return 1;
        }));
    }

    private void registerFriendCommands(CommandDispatcher<S> dispatcher) {
        // collar friend add [user]
        dispatcher.register(prefixed("friend", literal("add")
                .then(argument("name", identity())
                    .executes(context -> {
                        IdentityArgument player = context.getArgument("name", IdentityArgument.class);
                        if (player.player != null) {
                            collar.friends().addFriend(new MinecraftPlayer(player.player.getUuid(), collar.player().minecraftPlayer.server, collar.player().minecraftPlayer.networkId));
                        } else if (player.profile != null) {
                            collar.friends().addFriend(player.profile.id);
                        }
                        return 1;
                    }))));

        // collar friend remove [user]
        dispatcher.register(prefixed("friend", literal("remove")
                .then(argument("name", identity())
                        .executes(context -> {
                            with(collar -> {
                                IdentityArgument player = context.getArgument("name", IdentityArgument.class);
                                if (player.player != null) {
                                    collar.friends().removeFriend(new MinecraftPlayer(player.player.getUuid(), collar.player().minecraftPlayer.server, collar.player().minecraftPlayer.networkId));
                                } else if (player.profile != null) {
                                    collar.friends().removeFriend(player.profile.id);
                                } else {
                                    throw new IllegalStateException("was not profile or player");
                                }
                            });
                            return 1;
                        }))));

        // collar friend list
        dispatcher.register(prefixed("friend", literal("list")
                .executes(context -> {
                    with(collar -> {
                        Set<Friend> friends = collar.friends().list();
                        if (friends.isEmpty()) {
                            displayInfoMessage("You don't have any friends");
                        } else {
                            friends.stream().sorted(Comparator.comparing(o -> o.status)).forEach(friend -> {
                                switch (friend.status) {
                                    case OFFLINE:
                                        displayErrorMessage(friend.profile.name);
                                    case ONLINE:
                                        displaySuccessMessage(friend.profile.name);
                                    case UNKNOWN:
                                        displayInfoMessage(friend.profile.name);
                                }
                            });
                        }
                    });
                    return 1;
                })));
    }

    private void registerGroupCommands(GroupType type, CommandDispatcher<S> dispatcher) {
        // collar party create [name]
        dispatcher.register(prefixed(type.name, literal("create")
                .then(argument("name", string())
                        .executes(context -> {
                            with(collar -> {
                                collar.groups().create(getString(context, "name"), type, ImmutableList.of());
                            });
                            return 1;
                        }))));

        // collar party delete [name]
        dispatcher.register(prefixed(type.name, literal("delete")
                .then(argument("name", group(type))
                        .executes(context -> {
                            with(collar -> {
                                collar.groups().delete(getGroup(context, "name"));
                            });
                            return 1;
                        }))));

        // collar party leave [name]
        dispatcher.register(prefixed(type.name, literal("leave")
                        .then(argument("name", group(type))
                                .executes(context -> {
                                    with(collar -> {
                                        collar.groups().leave(getGroup(context, "name"));
                                    });
                                    return 1;
                                }))));

        // collar par/**/ty invites
        dispatcher.register(prefixed(type.name, literal("invites")
                        .executes(context -> {
                            with(collar -> {
                                List<GroupInvitation> invitations = collar.groups().invitations().stream()
                                        .filter(invitation -> invitation.type == type)
                                        .collect(Collectors.toList());
                                if (invitations.isEmpty()) {
                                    displayInfoMessage("You have no invites to any " + type.plural);
                                } else {
                                    displayInfoMessage("You have invites to:");
                                    invitations.forEach(invitation -> displayInfoMessage(invitation.name));
                                    displayInfoMessage("To accept type '/collar " + type.name  + " accept [name]");
                                }
                            });
                            return 1;
                        })));

        // collar party accept [name]
        dispatcher.register(prefixed(type.name, literal("accept")
                .then(argument("groupName", invitation(type))
                        .executes(context -> {
                            with(collar -> {
                                collar.groups().accept(getInvitation(context, "groupName"));
                            });
                            return 1;
                        }))));

        // collar party list
        dispatcher.register(prefixed(type.name, literal("list")
                .executes(context -> {
                    with(collar -> {
                        List<Group> parties = collar.groups().all().stream()
                                .filter(group -> group.type.equals(type))
                                .collect(Collectors.toList());
                        if (parties.isEmpty()) {
                            displayInfoMessage("You are not a member of any " + type.plural);
                        } else {
                            displayInfoMessage("You belong to the following " + type.plural + ":");
                            parties.forEach(group -> displayInfoMessage(group.name));
                        }
                    });
                    return 1;
                })));

        // collar party add [name] [player]
        dispatcher.register(prefixed(type.name, literal("add")
                .then(argument("groupName", group(type))
                        .then(argument("playerName", playerArgument())
                                .executes(context -> {
                                    with(collar -> {
                                        Group group = getGroup(context, "groupName");
                                        PlayerEntity player = getPlayer(context, "playerName");
                                        collar.groups().invite(group, ImmutableList.of(player.getUuid()));
                                    });
                                    return 1;
                                })))));

        // collar party remove [name] [player]
        dispatcher.register(prefixed(type.name, literal("remove")
                .then(argument("groupName", group(type))
                        .then(argument("playerName", identity())
                                .executes(context -> {
                                    with(collar -> {
                                        Group group = getGroup(context, "groupName");
                                        IdentityArgument identity = context.getArgument("playerName", IdentityArgument.class);
                                        group.members.stream().filter(candidate -> candidate.profile.id.equals(identity.profile.id)).findFirst().ifPresent(theMember -> {
                                            collar.groups().removeMember(group, theMember);
                                        });
                                    });
                                    return 1;
                                })))));

        // collar party members [name]
        dispatcher.register(prefixed(type.name, literal("members")
                .then(argument("groupName", group(type)))
                .executes(context -> {
                    with(collar -> {
                        Group group = getGroup(context, "groupName");
                        displayMessage("Members:");
                        group.members.forEach(member -> {
                            Optional<PlayerEntity> thePlayer = findPlayerById(member.player.minecraftPlayer.id);
                            String message;
                            if (thePlayer.isPresent()) {
                                message = member.profile.name + " playing as " + member.player.minecraftPlayer.id + " (" + member.membershipRole.name() + ")";
                            } else {
                                message = member.profile.name;
                            }
                            displayMessage(message + "(" + member.membershipRole.name() + ")");
                        });
                    });
                    return 1;
                })));
    }

    private void registerLocationCommands(CommandDispatcher<S> dispatcher) {
        // collar location share start [any group name]
        dispatcher.register(prefixed("location", literal("share")
                .then(literal("start")
                        .then(argument("groupName", groups())
                                .executes(context -> {
                                    with(collar -> {
                                        Group group = getGroup(context, "groupName");
                                        collar.location().startSharingWith(group);
                                    });
                                    return 1;
                                })))));

        // collar location share stop [any group name]
        dispatcher.register(prefixed("location", literal("share")
                .then(literal("stop")
                        .then(argument("groupName", groups())
                                .executes(context -> {
                                    with(collar -> {
                                        Group group = getGroup(context, "groupName");
                                        collar.location().stopSharingWith(group);
                                    });
                                    return 1;
                                })))));

        // collar location share stop [any group name]
        dispatcher.register(prefixed("location", literal("share")
                .then(literal("list")
                        .executes(context -> {
                            with(collar -> {
                                List<Group> active = collar.groups().all().stream()
                                        .filter(group -> collar.location().isSharingWith(group))
                                        .collect(Collectors.toList());
                                if (active.isEmpty()) {
                                    displayInfoMessage("You are not sharing your location with any groups");
                                } else {
                                    displayInfoMessage("You are sharing your location with groups:");
                                    active.forEach(group -> displayInfoMessage(group.name + " (" + group.type.name + ")"));
                                }
                            });
                            return 1;
                        }))));
    }

    private void registerWaypointCommands(CommandDispatcher<S> dispatcher) {

        // collar location waypoint add [name]
        dispatcher.register(prefixed("waypoint", literal("add")
                .then(argument("name", string())
                        .executes(context -> {
                            with(collar -> {
                                collar.location().addWaypoint(getString(context, "name"), playerLocation());
                            });
                            return 1;
                        }))));

        // collar waypoint remove [name]
        dispatcher.register(prefixed("waypoint", literal("remove")
                .then(argument("name", privateWaypoint())
                        .executes(context -> {
                            with(collar -> {
                                WaypointArgument argument = context.getArgument("name", WaypointArgument.class);
                                collar.location().removeWaypoint(argument.waypoint);
                            });
                            return 1;
                        }))));

        // collar location waypoint list
        dispatcher.register(prefixed("waypoint", literal("list")
                .executes(context -> {
                    with(collar -> {
                        Set<Waypoint> waypoints = collar.location().privateWaypoints();
                        if (waypoints.isEmpty()) {
                            displayInfoMessage("You have no private waypoints");
                        } else {
                            waypoints.forEach(waypoint -> displayInfoMessage(waypoint.displayName()));
                        }
                    });
                    return 1;
                })));

        // collar location waypoint list [any group name]
        dispatcher.register(prefixed("waypoint", literal("list")
                .then(argument("group", groups())
                        .executes(context -> {
                            with(collar -> {
                                Group group = getGroup(context, "group");
                                Set<Waypoint> waypoints = collar.location().groupWaypoints(group);
                                if (waypoints.isEmpty()) {
                                    displayInfoMessage("You have no group waypoints");
                                } else {
                                    waypoints.forEach(waypoint -> displayInfoMessage(waypoint.displayName()));
                                }
                            });
                            return 1;
                        }))));

        // collar location waypoint add [name] [x] [y] [z] to [group]
        dispatcher.register(prefixed("waypoint", literal("add")
                .then(argument("name", string())
                        .then(argument("x", doubleArg())
                                .then(argument("y", doubleArg())
                                        .then(argument("z", doubleArg())
                                                .then(argument("dimension", dimension())
                                                        .then(literal("to")
                                                                .then(argument("group", groups())
                                                                        .executes(context -> {
                                                                            with(collar -> {
                                                                                Group group = getGroup(context, "group");
                                                                                Dimension dimension = context.getArgument("dimension", Dimension.class);
                                                                                Location location = new Location(
                                                                                        getDouble(context, "x"),
                                                                                        getDouble(context, "y"),
                                                                                        getDouble(context, "z"),
                                                                                        dimension
                                                                                );
                                                                                collar.location().addWaypoint(group, getString(context, "name"), location);
                                                                            });
                                                                            return 1;
                                                                        }))))))))));

        // collar location waypoint remove [name] from [group]
        dispatcher.register(prefixed("waypoint", literal("remove")
                .then(argument("name", groupWaypoint())
                        .then(literal("from")
                                .then(argument("group", groups())
                                        .executes(context -> {
                                            with(collar -> {
                                                Group group = getGroup(context, "group");
                                                WaypointArgument argument = context.getArgument("waypoint", WaypointArgument.class);
                                                if (!group.id.equals(argument.group.id)) {
                                                    collar.location().removeWaypoint(argument.group, argument.waypoint);
                                                } else {
                                                    displayInfoMessage("Waypoint " + argument.waypoint + " does not belong to group " + group.name);
                                                }
                                            });
                                            return 1;
                                        }))))));

        // collar waypoint add [name] [x] [y] [z]
        dispatcher.register(prefixed("waypoint", literal("add")
                .then(argument("name", string())
                        .then(argument("x", doubleArg())
                                .then(argument("y", doubleArg())
                                        .then(argument("z", doubleArg())
                                                .then(argument("dimension", dimension())
                                                        .executes(context -> {
                                                            with(collar -> {
                                                                Dimension dimension = context.getArgument("dimension", Dimension.class);
                                                                Location location = new Location(
                                                                        getDouble(context, "x"),
                                                                        getDouble(context, "y"),
                                                                        getDouble(context, "z"),
                                                                        dimension
                                                                );
                                                                collar.location().addWaypoint(getString(context, "name"), location);
                                                            });
                                                            return 1;
                                                        }))))))));
    }

    private void registerChatCommands(CommandDispatcher<S> dispatcher) {
        // /msg player2 OwO
        dispatcher.register(literal("msg")
                .then(argument("recipient", playerArgument())
                .then(argument("rawMessage", string())
                .executes(context -> {
                    PlayerEntity recipient = getPlayer(context, "recipient");
                    String message = getString(context, "rawMessage");
                    messages.sendMessage(recipient, message);
                    return 1;
        }))));

        // collar chat with coolkids
        dispatcher.register(prefixed("chat", literal("with")
                .then(argument("group", groups())
                .executes(context -> {
                    Group group = getGroup(context, "group");
                    messages.switchToGroup(group);
            return 1;
        }))));

        // collar chat off
        dispatcher.register(prefixed("chat", literal("off").executes(context -> {
            messages.switchToGeneralChat();
            return 1;
        })));
    }

    public <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private LiteralArgumentBuilder<S> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private GroupArgumentType group(GroupType type) {
        return new GroupArgumentType(collar, type);
    }

    private GroupArgumentType groups() {
        return new GroupArgumentType(collar, null);
    }

    private InvitationArgumentType invitation(GroupType type) {
        return new InvitationArgumentType(collar, type);
    }

    private WaypointArgumentType privateWaypoint() {
        return new WaypointArgumentType(collar, true);
    }

    private WaypointArgumentType groupWaypoint() {
        return new WaypointArgumentType(collar, false);
    }

    private PlayerArgumentType playerArgument() {
        return new PlayerArgumentType(mc);
    }

    private IdentityArgumentType identity() {
        return new IdentityArgumentType(collar, mc);
    }

    private GroupMemberArgumentType groupMember() {
        return new GroupMemberArgumentType(collar, mc);
    }

    @Override
    public ClientWorld world() {
        return mc.world;
    }

    @Override
    public ClientPlayerEntity player() {
        return mc.player;
    }

    public void with(Consumer<Collar> collarConsumer) {
        Collar.State state = collar.getState();
        if (state == Collar.State.CONNECTED) {
            collarConsumer.accept(collar);
        } else {
            displayInfoMessage("Collar is " + state);
        }
    }
}
