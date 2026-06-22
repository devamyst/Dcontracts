package me.devamy.contracts.api.events;

import me.devamy.contracts.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public abstract class OrderiumPlayerEvent extends PlayerEvent {
    private final Order order;

    public @NotNull Order getOrder() {
        return order;
    }

    protected OrderiumPlayerEvent(@NotNull Player player, @NotNull Order order, boolean isAsync) {
        super(player, isAsync);
        this.order = order;
    }

}
