package io.github.mooy1.gridfoundation.implementation.wireless;

import io.github.mooy1.gridfoundation.GridFoundation;
import io.github.mooy1.gridfoundation.setup.Categories;
import io.github.mooy1.infinitylib.PluginUtils;
import io.github.mooy1.infinitylib.menus.TransferUtils;
import io.github.mooy1.infinitylib.player.LeaveListener;
import io.github.mooy1.infinitylib.player.MessageUtils;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A tool for configuring wireless input/output nodes
 *
 * @author Mooy1
 * 
 */
public final class WirelessConfigurator extends SlimefunItem implements NotPlaceable, Listener {

    private final NamespacedKey key = PluginUtils.getKey("wireless");
    private final Map<UUID, Long> coolDowns = new HashMap<>();
    public static final SlimefunItemStack ITEM = new SlimefunItemStack(
            "WIRELESS_CONFIGURATOR",
            Material.BLAZE_ROD,
            "&9Wireless configurator",
            "&eRight-Click &7an input node then an output node to connect them",
            "&eCrouch + Right-Click &7an input node to remove connected output node",
            "&eCrouch + Right-Click &7the air to clear the input node currently being configured"
    );
    
    public WirelessConfigurator() {
        super(Categories.MAIN, ITEM, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {

        });
        Bukkit.getPluginManager().registerEvents(this, GridFoundation.getInstance());
        new LeaveListener(this.coolDowns);
    }
    
    @EventHandler
    public void onRightClick(@Nonnull PlayerRightClickEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        
        if (SlimefunItem.getByItem(e.getItem()) instanceof WirelessConfigurator) {

            if (e.getClickedBlock().isPresent()) {
                
                if (e.getPlayer().isSneaking()) {
                    clearHandler(e.getClickedBlock().get(), e.getPlayer());
                } else {
                    connectHandler(e.getClickedBlock().get(), e.getPlayer(), e.getItem());
                }
                
            } else if (e.getPlayer().isSneaking()) {
                resetHandler(e.getItem(), e.getPlayer());
            }
            
            e.setUseBlock(Event.Result.DENY);

        } else if (e.getPlayer().isSneaking() && e.getClickedBlock().isPresent()
                && Objects.equals(BlockStorage.checkID(e.getClickedBlock().get()), WirelessInputNode.ITEM.getItemId())
        ) {
            infoHandler(e.getClickedBlock().get(), e.getClickedBlock().get().getLocation(), e.getPlayer(), false);
            e.setUseBlock(Event.Result.DENY);
        }
    }

    private void resetHandler(@Nonnull ItemStack item, @Nonnull Player p) {
        setTemp(item, null);
        MessageUtils.messageWithCD(p, 500, "&eCleared selected input node!");
    }

    private void clearHandler(@Nonnull Block b, @Nonnull Player p) {
        if (Objects.equals(BlockStorage.checkID(b), WirelessInputNode.ITEM.getItemId())) {

            WirelessUtils.setConnected(b.getLocation(), null);
            MessageUtils.messageWithCD(p, 500, "&eConnected Location Cleared!");

        } else {
            clickInputNode(p);
        }
    }

    private void connectHandler(@Nonnull Block b, @Nonnull Player p, @Nonnull ItemStack item) {
        String id = BlockStorage.checkID(b);
        if (id == null) {
            clickInputNode(p);
            return;
        }
        Location temp = getTemp(item);
        if (temp == null) {
            if (id.equals(WirelessInputNode.ITEM.getItemId())) {
                setTemp(item, b.getLocation());
                MessageUtils.message(p, "&aNow click on a wireless output node to connect");
            } else {
                clickInputNode(p);
            }
        } else {
            if (id.equals(WirelessOutputNode.ITEM.getItemId())) {
                if (temp.getBlock().getType() != Material.AIR && Objects.equals(BlockStorage.checkID(temp), WirelessInputNode.ITEM.getItemId())) {
                    setTemp(item, null);
                    WirelessUtils.setConnected(temp, b.getLocation());
                    MessageUtils.message(p, "&aConnected Nodes");
                    infoHandler(temp.getBlock(), temp, p, true);
                } else {
                    MessageUtils.message(p, "&cInput node was broken or is unavailable!");
                }
            } else {
                MessageUtils.messageWithCD(p, 500, "&cClick on a wireless output node!");
            }
        }
    }

    private void clickInputNode(@Nonnull Player p) {
        MessageUtils.messageWithCD(p, 500, "&cClick on a wireless input node!");
    }

    private void infoHandler(@Nonnull Block b, @Nonnull Location l, @Nonnull Player p, boolean force) {

        if (!force && System.currentTimeMillis() - this.coolDowns.getOrDefault(p.getUniqueId(), 0L) < 2000) {
            return;
        }
        this.coolDowns.put(p.getUniqueId(), System.currentTimeMillis());
        
        Location source = WirelessUtils.getTarget(b.getLocation());
        
        if (source == null) {
            MessageUtils.message(p, "&eSource block was missing!");
            WirelessUtils.breakBlock(b, p);
            return;
        }
        
        Inventory sourceInventory = TransferUtils.getInventory(source.getBlock());
        BlockMenu sourceMenu = TransferUtils.getMenu(source);
        boolean sourceVanilla = WirelessUtils.isVanilla(l);

        if (displayInfo(l, sourceMenu, sourceInventory, p, "Source", sourceVanilla)) {
            return;
        }

        Location connected = WirelessUtils.getConnected(l);

        if (connected == null) {
            MessageUtils.message(p, "&aTarget Inventory: Not connected");
            return;
        }

        Location target = WirelessUtils.getTarget(connected);

        if (target == null) {
            MessageUtils.message(p, "&aTarget block was missing!");
            return;
        }

        Inventory targetInventory = TransferUtils.getInventory(target.getBlock());
        BlockMenu targetMenu = TransferUtils.getMenu(target);
        boolean targetVanilla = WirelessUtils.isVanilla(connected);

        if (displayInfo(connected, targetMenu, targetInventory, p, "Target", targetVanilla)) {
            return;
        }
        
        if (!WirelessUtils.centerAndTest(l, connected)) {
            MessageUtils.message(p, "&aCan't show visualization, Nodes are too far apart or in different worlds!");
            return;
        }
        
        for (long i = 5 ; i <= 40 ; i += 4) {
            PluginUtils.runSync(() -> WirelessUtils.sendParticle(p, l, connected), i);
        }
    }
    
    private boolean displayInfo(@Nonnull Location l, @Nullable BlockMenu menu, @Nullable Inventory inv, @Nonnull Player p, @Nonnull String s, boolean vanilla) {
        try {
            MessageUtils.message(p, "&a" + s + " Inventory: " + (vanilla
                    ? ChatColor.WHITE + Objects.requireNonNull(inv).getType().getDefaultTitle()
                    : Objects.requireNonNull(menu).getPreset().getTitle()
            ));
        } catch (NullPointerException e) {
            MessageUtils.message(p, "&e" + s + " inventory was missing!");
            WirelessUtils.breakBlock(l.getBlock(), p);
            return true;
        }
        return false;
    }
    
    @Nullable
    private Location getTemp(@Nonnull ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return null;

        return WirelessUtils.toLocation(meta.getPersistentDataContainer().get(this.key, PersistentDataType.STRING));
    }

    private void setTemp(@Nonnull ItemStack item, @Nullable Location l) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return;

        String set = WirelessUtils.toString(l);

        if (set == null) {
            meta.setDisplayName(getItemName());
            meta.getPersistentDataContainer().remove(this.key);
        } else {
            meta.setDisplayName(getItemName() + ChatColor.GREEN + " (Location Selected)");
            meta.getPersistentDataContainer().set(this.key, PersistentDataType.STRING, set);
        }

        item.setItemMeta(meta);
    }

}