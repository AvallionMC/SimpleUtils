package io.github.mooy1.gridfoundation.implementation.generators;

import io.github.mooy1.gridfoundation.implementation.AbstractGridContainer;
import io.github.mooy1.gridfoundation.implementation.powergrid.GridGenerator;
import io.github.mooy1.gridfoundation.implementation.powergrid.PowerGrid;
import io.github.mooy1.gridfoundation.implementation.upgrades.UpgradeableBlock;
import io.github.mooy1.gridfoundation.setup.Categories;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.List;

public abstract class AbstractGridGenerator extends AbstractGridContainer implements UpgradeableBlock {

    private final int statusSlot;

    public AbstractGridGenerator(SlimefunItemStack item, ItemStack[] recipe, int statusSlot) {
        super(Categories.GENERATORS, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
        this.statusSlot = statusSlot;
        addMeta(item);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void onBreak(@Nonnull BlockBreakEvent e, @Nonnull Location l, @Nonnull BlockMenu menu, @Nonnull PowerGrid grid) {
        breakUpgrade(e, e.getBlock().getLocation(), getItem().clone());
        grid.removeGenerator(e.getBlock().getLocation().hashCode());
    }

    @Override
    public void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b, @Nonnull PowerGrid grid) {
        grid.addGenerator(b.getLocation().hashCode(), getItem(), b.getLocation()).updateStatus(menu, this.statusSlot);
        updateMenuUpgrade(menu, b.getLocation());
    }

    @Override
    public void onPlace(@Nonnull BlockPlaceEvent e) {
        placeUpgrade(e.getBlockPlaced().getLocation(), e.getItemInHand());
    }

    @Override
    public final void tick(@Nonnull Block block, @Nonnull BlockMenu blockMenu, @Nonnull PowerGrid grid) {
        GridGenerator generator = grid.getGenerator(block.getLocation().hashCode());
        if (generator != null) {
            int tier = getTier(block);
            int generation = getGeneration(blockMenu, block, tier) << tier;
            generator.setGeneration(generation);
            if (blockMenu.hasViewer()) {
                generator.updateStatus(blockMenu, this.statusSlot);
            }
        }
    }

    public abstract int getGeneration(@Nonnull BlockMenu menu, @Nonnull Block b, int tier);

    @Override
    @OverridingMethodsMustInvokeSuper
    public void getStats(@Nonnull List<String> stats, int tier) {
        stats.add("&6Generation: &e" + (1 << tier) + "x");
    }
    
}
