package me.devamy.contracts.data;

import me.devamy.contracts.api.events.OrderRemoveEvent;
import me.devamy.contracts.obj.Order;
import me.devamy.contracts.obj.SortType;
import me.devamy.contracts.obj.orderitem.BlacklistedItem;
import me.devamy.contracts.obj.orderitem.CustomItem;
import me.devamy.contracts.obj.orderitem.OrderItem;
import me.devamy.contracts.obj.orderitem.VanillaItem;
import me.devamy.contracts.utils.AlgoUtils;
import me.devamy.contracts.utils.Log;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static me.devamy.contracts.config.Config.config;

public final class DataCache {
    private static final DataCache INSTANCE = new DataCache();

    public static @NotNull DataCache getInstance() {
        return INSTANCE;
    }

    private static final Registry<BlockType> BLOCK_REGISTRY = Registry.BLOCK;

    private static Comparator<OrderItem> itemComparator(SortType sortType) {
        return AlgoUtils.getComparator(sortType);
    }

    private static NavigableSet<Order> newOrdersSet(Comparator<Order> comparator) {
        return new ConcurrentSkipListSet<>(comparator);
    }

    private static final Comparator<Order> MOST_MONEY_PER_ITEM_COMPARATOR = Comparator.comparingDouble(Order::getMoneyPer).reversed().thenComparing(Order::getId);
    private static final Comparator<Order> RECENTLY_LISTED_COMPARATOR = Comparator.comparingLong(Order::getExpiresAt).reversed().thenComparing(Order::getId);
    private static final Comparator<Order> MOST_DELIVERED_COMPARATOR = Comparator.comparingInt(Order::getDelivered).reversed().thenComparing(Order::getId);
    private static final Comparator<Order> MOST_PAID_COMPARATOR = Comparator.comparingDouble(Order::getPaid).reversed().thenComparing(Order::getId);
    private static final Comparator<Order> OLDEST_COMPARATOR = Comparator.comparingLong(Order::getExpiresAt).thenComparing(Order::getId);
    private static final Comparator<Order> PRICIEST_COMPARATOR = Comparator.<Order>comparingDouble(o -> o.getMoneyPer() * o.getAmount()).reversed().thenComparing(Order::getId);
    private static final Comparator<Order> CHEAPEST_COMPARATOR = Comparator.comparingDouble(Order::getMoneyPer).thenComparing(Order::getId);

    private volatile NavigableSet<OrderItem> itemsAZ = new ConcurrentSkipListSet<>(itemComparator(SortType.A_Z));
    private volatile NavigableSet<OrderItem> itemsZA = new ConcurrentSkipListSet<>(itemComparator(SortType.Z_A));

    private final Set<CustomItem> customItems = ConcurrentHashMap.newKeySet();
    private final Set<BlacklistedItem> blacklist = ConcurrentHashMap.newKeySet();

    private volatile NavigableSet<Order> mostMoneyPerItem = newOrdersSet(MOST_MONEY_PER_ITEM_COMPARATOR);
    private volatile NavigableSet<Order> recentlyListed = newOrdersSet(RECENTLY_LISTED_COMPARATOR);
    private volatile NavigableSet<Order> mostDelivered = newOrdersSet(MOST_DELIVERED_COMPARATOR);
    private volatile NavigableSet<Order> mostPaid = newOrdersSet(MOST_PAID_COMPARATOR);
    private volatile NavigableSet<Order> oldest = newOrdersSet(OLDEST_COMPARATOR);
    private volatile NavigableSet<Order> priciest = newOrdersSet(PRICIEST_COMPARATOR);
    private volatile NavigableSet<Order> cheapest = newOrdersSet(CHEAPEST_COMPARATOR);

    private void setBlacklistAndCustomItems(Collection<BlacklistedItem> blacklist, Collection<CustomItem> customItems) {
        this.blacklist.clear();
        this.customItems.clear();
        this.blacklist.addAll(blacklist);
        this.customItems.addAll(customItems);
    }

    public void setItems(Collection<VanillaItem> vanillaItems, Collection<BlacklistedItem> blacklistedItems, Collection<CustomItem> customItems) {
        final NavigableSet<OrderItem> newItemsAZ = new ConcurrentSkipListSet<>(itemComparator(SortType.A_Z));
        final NavigableSet<OrderItem> newItemsZA = new ConcurrentSkipListSet<>(itemComparator(SortType.Z_A));
        newItemsAZ.addAll(vanillaItems);
        newItemsZA.addAll(vanillaItems);

        newItemsAZ.addAll(customItems);
        newItemsZA.addAll(customItems);

        for (final BlacklistedItem e : blacklistedItems) {
            newItemsAZ.removeIf(orderItem -> orderItem.getItemStack().equals(e.getItemStack()));
            newItemsZA.removeIf(orderItem -> orderItem.getItemStack().equals(e.getItemStack()));
        }

        this.itemsAZ = newItemsAZ;
        this.itemsZA = newItemsZA;
        setBlacklistAndCustomItems(blacklistedItems, customItems);

        Log.info("Loaded " + newItemsAZ.size() + " items.");
    }

    public void setOrders(Collection<Order> orders) {
        final boolean parallel = config != null && config.parallelProcessing;

        final NavigableSet<Order> newMostMoneyPerItem = newOrdersSet(MOST_MONEY_PER_ITEM_COMPARATOR);
        final NavigableSet<Order> newRecentlyListed = newOrdersSet(RECENTLY_LISTED_COMPARATOR);
        final NavigableSet<Order> newMostDelivered = newOrdersSet(MOST_DELIVERED_COMPARATOR);
        final NavigableSet<Order> newMostPaid = newOrdersSet(MOST_PAID_COMPARATOR);
        final NavigableSet<Order> newOldest = newOrdersSet(OLDEST_COMPARATOR);
        final NavigableSet<Order> newPriciest = newOrdersSet(PRICIEST_COMPARATOR);
        final NavigableSet<Order> newCheapest = newOrdersSet(CHEAPEST_COMPARATOR);

        if (parallel) {
            final List<NavigableSet<Order>> sets = List.of(newMostMoneyPerItem, newRecentlyListed, newMostDelivered,
                    newMostPaid, newOldest, newPriciest, newCheapest);
            sets.parallelStream().forEach(set -> set.addAll(orders));
        } else {
            newMostMoneyPerItem.addAll(orders);
            newRecentlyListed.addAll(orders);
            newMostDelivered.addAll(orders);
            newMostPaid.addAll(orders);
            newOldest.addAll(orders);
            newPriciest.addAll(orders);
            newCheapest.addAll(orders);
        }

        this.mostMoneyPerItem = newMostMoneyPerItem;
        this.recentlyListed = newRecentlyListed;
        this.mostDelivered = newMostDelivered;
        this.mostPaid = newMostPaid;
        this.oldest = newOldest;
        this.priciest = newPriciest;
        this.cheapest = newCheapest;
    }
    public void updateOrder(Order order, double moneyPer, int amount, int delivered, int inStorage) {

        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);
        oldest.remove(order);
        priciest.remove(order);
        cheapest.remove(order);

        order.moneyPer = moneyPer;
        order.amount = amount;
        order.delivered = delivered;
        order.inStorage = inStorage;

        // Re-add the order to not mess up the sorted collections after updating
        mostMoneyPerItem.add(order);
        recentlyListed.add(order);
        mostDelivered.add(order);
        mostPaid.add(order);
        oldest.add(order);
        priciest.add(order);
        cheapest.add(order);
    }

    public void deleteOrder(Order order, boolean isAsync) {
        OrderRemoveEvent.Pre preEvent = new OrderRemoveEvent.Pre(order, isAsync);
        if (!preEvent.callEvent()) return;

        mostMoneyPerItem.remove(order);
        recentlyListed.remove(order);
        mostDelivered.remove(order);
        mostPaid.remove(order);
        oldest.remove(order);
        priciest.remove(order);
        cheapest.remove(order);

        OrderRemoveEvent.Post postEvent = new  OrderRemoveEvent.Post(order, isAsync);
        postEvent.callEvent();
    }

    public void addOrder(Order order) {
        mostMoneyPerItem.add(order);
        recentlyListed.add(order);
        mostDelivered.add(order);
        mostPaid.add(order);
        oldest.add(order);
        priciest.add(order);
        cheapest.add(order);
    }

    public List<Order> getOrders(UUID ownerId, boolean isAsync) {
        List<Order> toDel = new ArrayList<>();
        List<Order> ownerOrders = mostMoneyPerItem.stream().filter(order -> {
            if (!order.getOwnerUniqueId().equals(ownerId)) return false;
            if (order.shouldBeDeleted()) {
                toDel.add(order);
                return false;
            }
            return true;
        }).toList();
        toDel.forEach(order -> deleteOrder(order, isAsync));
        return ownerOrders;
    }

    public NavigableSet<Order> getSortedOrders(SortType sortType) {
        switch (sortType) {
            case MOST_MONEY_PER_ITEM -> { return mostMoneyPerItem; }
            case RECENTLY_LISTED -> { return recentlyListed; }
            case MOST_DELIVERED -> { return mostDelivered; }
            case MOST_PAID -> { return mostPaid; }
            case OLDEST -> { return oldest; }
            case PRICIEST -> { return priciest; }
            case CHEAPEST -> { return cheapest; }
        }
        return mostMoneyPerItem;
    }

    public NavigableSet<OrderItem> getItems(SortType sortType) {
        switch (sortType) {
            case A_Z -> { return itemsAZ; }
            case Z_A -> { return itemsZA; }
        }
        return itemsAZ;
    }

    public Set<CustomItem> getCustomItems() { return customItems; }
    public Set<BlacklistedItem> getBlacklist() { return blacklist; }

    public BlockType getBlockType(@KeyPattern String identifier) {
        return BLOCK_REGISTRY.get(Key.key(identifier));
    }
}
