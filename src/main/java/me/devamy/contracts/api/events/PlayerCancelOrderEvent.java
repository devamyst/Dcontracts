package me.devamy.contracts.api.events;

import me.devamy.contracts.api.Order;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerCancelOrderEvent {
    private PlayerCancelOrderEvent() {}

    public static class Pre extends ContractsPlayerEvent implements Cancellable {
        private boolean cancelled = false;
        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Pre(@NotNull Player player, @NotNull Order order, boolean isAsync) {
            super(player, order, isAsync);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            cancelled = cancel;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }
    }

    public static class Post extends ContractsPlayerEvent {
        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Post(@NotNull Player player, @NotNull Order order, boolean isAsync) {
            super(player, order, isAsync);
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        @SuppressWarnings("unused")
        public static HandlerList getHandlerList() { return HANDLER_LIST; }
    }
}
