package com.collarmc.fabric.client.mixins;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public interface DisplayMixin {
    /**
     * Display a status message on screen
     * @param message to display
     */
    default void displayStatusMessage(String message) {
        displayStatusMessage(new LiteralText(message));
    }

    /**
     * Display a status message on screen
     * @param message to display
     */
    default void displayStatusMessage(Text message) {
        player().sendMessage(message, true);
    }

    /**
     * Send a message to the chat console
     * @param message to send
     */
    default void displayMessage(Text message) {
        player().sendMessage(message, false);
    }

    /**
     * Send a info message to the chat console
     * @param message to send
     */
    default void displayInfoMessage(String message) {
        displayMessage(new LiteralText(message).formatted(Formatting.GRAY));
    }

    /**
     * Send a success message to the chat console
     * @param message to send
     */
    default void displaySuccessMessage(String message) {
        displayMessage(new LiteralText(message).formatted(Formatting.GREEN));
    }

    /**
     * Send a warning message to the chat console
     * @param message to send
     */
    default void displayWarningMessage(String message) {
        displayMessage(new LiteralText(message).formatted(Formatting.YELLOW));
    }

    /**
     * Send a error message to the chat console
     * @param message to send
     */
    default void displayErrorMessage(String message) {
        displayMessage(new LiteralText(message).formatted(Formatting.RED));
    }

    /**
     * Send a success message to the chat console
     * @param message to send
     */
    default void displayMessage(String message) {
        displayMessage(new LiteralText(message).formatted(Formatting.GRAY));
    }

    /**
     * Create rainbow text
     * @param text of rainbow
     * @return text
     */
    default Text rainbowText(String text) {
        MutableText builder = new LiteralText("");
        Random random = new Random();
        List<Formatting> values = new ArrayList<>(Arrays.asList(Formatting.values()));
        // too dark to display in most contexts
        values.remove(Formatting.BLACK);
        values.remove(Formatting.GRAY);
        values.remove(Formatting.WHITE);
        values.remove(Formatting.DARK_GRAY);
        values.remove(Formatting.DARK_BLUE);
        values.remove(Formatting.DARK_GREEN);
        values.remove(Formatting.DARK_AQUA);
        values.remove(Formatting.DARK_RED);
        values.remove(Formatting.DARK_PURPLE);
        values.remove(Formatting.BOLD);
        values.remove(Formatting.ITALIC);
        values.remove(Formatting.OBFUSCATED);
        values.remove(Formatting.STRIKETHROUGH);
        values.remove(Formatting.UNDERLINE);
        values.remove(Formatting.RESET);
        Formatting lastColor = null;
        for (char c : text.toCharArray()) {
            Formatting color = values.get(random.nextInt(values.size()));
            while (color == lastColor) {
                color = values.get(random.nextInt(values.size()));
            }
            lastColor = color;
            builder = builder.append(new LiteralText(Character.toString(c)).formatted(color));
        }
        return builder;
    }

    /**
     * Current player
     * @return player
     */
    ClientPlayerEntity player();
}
