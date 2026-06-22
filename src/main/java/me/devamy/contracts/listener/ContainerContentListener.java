package me.devamy.contracts.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import me.devamy.contracts.utils.Log;
import me.devamy.contracts.utils.PDCUtils;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

import static me.devamy.contracts.utils.Values.ERROR_TRACKER;

/// This class is used for listening to container content packets to remove unnecessary nbt data for compatibility with custom items and to make it as vanilla as possible
public class ContainerContentListener implements PacketListener {

    @Override
    public void onPacketSend(@NonNull PacketSendEvent event) {
        try {
            switch (event.getPacketType()) {
                case PacketType.Play.Server.SET_SLOT -> {
                    WrapperPlayServerSetSlot setSlotPacket = new WrapperPlayServerSetSlot(event);
                    stripItemPD(setSlotPacket.getItem());
                }

                case PacketType.Play.Server.WINDOW_ITEMS -> {
                    WrapperPlayServerWindowItems containerContentPacket = new WrapperPlayServerWindowItems(event);
                    for (ItemStack item : containerContentPacket.getItems()) {
                        stripItemPD(item);
                    }
                }

                default -> {
                    return;
                }
            }
            event.markForReEncode(true);
        } catch (Exception e) {
            Log.error("Failed while handling packet", e);
            ERROR_TRACKER.trackError(e);
        }
    }
    
    /// Strip persistent data of Contracts from this item
    private void stripItemPD(ItemStack item) {
        Optional<NBTCompound> nbtOptional = item.getComponent(ComponentTypes.CUSTOM_DATA);
        if (nbtOptional.isEmpty()) return;
        NBTCompound nbt = nbtOptional.get();
        NBTCompound persistentData = nbt.getCompoundTagOrNull("PublicBukkitValues");
        if (persistentData == null) return;
        for (NamespacedKey key : PDCUtils.KEYS) {
            persistentData.removeTag(key.toString());
        }
        if (persistentData.getTags().isEmpty()) nbt.removeTag("PublicBukkitValues");
        if (nbt.getTags().isEmpty()) item.unsetComponent(ComponentTypes.CUSTOM_DATA);
    }
}
