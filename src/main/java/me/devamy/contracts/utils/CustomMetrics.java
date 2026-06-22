package me.devamy.contracts.utils;

import dev.faststats.data.Metric;
import me.devamy.contracts.api.events.*;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.data.DataCache;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomMetrics {
    public static final Metric<Boolean> API_USAGE = Metric.bool("api_usage", CustomMetrics::apiUsage);
    public static final Metric<? extends Number> ORDER_AMOUNT = Metric.number("order_amount", CustomMetrics::orderAmount);
    public static final Metric<? extends Number> ITEMS_COLLECTED = Metric.number("items_collected", CustomMetrics::itemsCollected);
    public static final Metric<String[]> EXPERIMENTAL_FEATURES = Metric.stringArray("experimental_features", CustomMetrics::experimentalFeatures);

    private static boolean apiUsage() {
        final HandlerList[] handlerLists = new HandlerList[]{
                OrderRemoveEvent.Pre.getHandlerList(),
                OrderRemoveEvent.Post.getHandlerList(),
                PlayerCancelOrderEvent.Pre.getHandlerList(),
                PlayerCancelOrderEvent.Post.getHandlerList(),
                PlayerCollectItemsEvent.Pre.getHandlerList(),
                PlayerCollectItemsEvent.Post.getHandlerList(),
                PlayerCreateOrderEvent.Pre.getHandlerList(),
                PlayerCreateOrderEvent.Post.getHandlerList(),
                PlayerDeliverOrderEvent.Pre.getHandlerList(),
                PlayerDeliverOrderEvent.Post.getHandlerList(),
        };

        for (final HandlerList handlerList : handlerLists) {
            if (handlerList.getRegisteredListeners().length > 0) {
                return true;
            }
        }
        return false;
    }

    public static final AtomicInteger ORDER_AMOUNT_CACHE = new AtomicInteger(0);

    public static Integer orderAmount() {
        return ORDER_AMOUNT_CACHE.get();
    }

    public static final AtomicInteger ITEMS_COLLECTED_CACHE = new AtomicInteger(0);

    public static Integer itemsCollected() {
        return ITEMS_COLLECTED_CACHE.get();
    }

    private static String[] experimentalFeatures() {
        final Config config = Config.config;
        final List<String> experimentalFeatures = new ArrayList<>(3);
        if (config.enchantGUIConfig.enabled) experimentalFeatures.add("enchant_gui");
        if (!DataCache.getInstance().getCustomItems().isEmpty()) experimentalFeatures.add("custom_items");
        return experimentalFeatures.toArray(new String[0]);
    }

    public static void FLUSH() {
        ORDER_AMOUNT_CACHE.set(0);
        ITEMS_COLLECTED_CACHE.set(0);
    }
}
