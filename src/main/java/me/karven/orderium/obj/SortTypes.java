package me.karven.orderium.obj;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum SortTypes {
    MOST_MONEY_PER_ITEM("most-money-per-item"),
    RECENTLY_LISTED("recently-listed"),
    MOST_DELIVERED("most-delivered"),
    MOST_PAID("most-paid"),
    A_Z("a-z"),
    Z_A("z-a");

    private final String identifier;
    private String display;

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(final @NotNull String display) {
        this.display = display;
    }

    SortTypes(String identifier) {
        this.identifier = identifier;
    }

    public static SortTypes fromIdentifier(String identifier) {
        List<SortTypes> sortType = Arrays.stream(SortTypes.values()).filter(s -> s.is(identifier)).toList();
        if (sortType.isEmpty()) return null;
        return sortType.getFirst();
    }

    private boolean is(String identifier) {
        return this.identifier.equals(identifier);
    }
}
