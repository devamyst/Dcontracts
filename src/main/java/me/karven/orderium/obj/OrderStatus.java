package me.karven.orderium.obj;

import org.jetbrains.annotations.NotNull;

public enum OrderStatus {
    EXPIRED("expired"),
    COMPLETED("completed"),
    AVAILABLE("available");

    private final String identifier;
    private String text;

    public @NotNull String getIdentifier() { return identifier; }
    public String getText() { return text; }
    public void setText(final @NotNull String text) { this.text = text; }

     OrderStatus(String identifier) {
        this.identifier = identifier;
    }
}
