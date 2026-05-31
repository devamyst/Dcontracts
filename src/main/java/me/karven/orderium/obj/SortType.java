package me.karven.orderium.obj;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SortType {
    MOST_MONEY_PER_ITEM("most-money-per-item"),
    RECENTLY_LISTED("recently-listed"),
    MOST_DELIVERED("most-delivered"),
    MOST_PAID("most-paid"),
    A_Z("a-z"),
    Z_A("z-a");

    private final String identifier;
    private String displayActive;
    private String displayInactive;

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public String getDisplayActive() {
        return displayActive;
    }

    public String getDisplayInactive() {
        return displayInactive;
    }

    public void setDisplayActive(final @NotNull String displayActive) {
        this.displayActive = displayActive;
    }

    public void setDisplayInactive(final @NotNull String displayInactive) {
        this.displayInactive = displayInactive;
    }

    SortType(String identifier) {
        this.identifier = identifier;
    }

    public static @Nullable SortType fromIdentifier(String identifier) {
        for (SortType sortType : SortType.values()) {
            if (sortType.identifier.equals(identifier)) return sortType;
        }
        return null;
    }

    private boolean is(String identifier) {
        return this.identifier.equals(identifier);
    }
}
