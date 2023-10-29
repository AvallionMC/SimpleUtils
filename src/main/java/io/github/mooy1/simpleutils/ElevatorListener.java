package io.github.mooy1.simpleutils;

import io.github.mooy1.simpleutils.implementation.blocks.Elevator;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ElevatorListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            final Block block = event.getClickedBlock();
            final Location location = block.getLocation();
            if (BlockStorage.hasBlockInfo(location) && BlockStorage.check(location) instanceof Elevator) {
                event.setCancelled(true);
            }
        }
    }
}
