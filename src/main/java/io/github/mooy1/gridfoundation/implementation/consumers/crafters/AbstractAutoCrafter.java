package io.github.mooy1.gridfoundation.implementation.consumers.crafters;

import io.github.mooy1.infinitylib.filter.FilterType;
import io.github.mooy1.infinitylib.filter.MultiFilter;
import io.github.mooy1.infinitylib.items.StackUtils;
import io.github.mooy1.infinitylib.presets.MenuPreset;
import io.github.mooy1.gridfoundation.implementation.consumers.AbstractGridConsumer;
import io.github.mooy1.gridfoundation.implementation.grid.PowerGrid;
import io.github.mooy1.gridfoundation.implementation.upgrades.UpgradeType;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.collections.Pair;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAutoCrafter extends AbstractGridConsumer {
    
    private static final int[] inputSlots = MenuPreset.craftingInput;
    private static final int keySlot = 23;
    private static final int outputSlots = MenuPreset.craftingOutput;
    private static final int EMPTY = new MultiFilter(FilterType.MIN_AMOUNT, new ItemStack[9]).hashCode();

    private final Map<Location, Pair<MultiFilter, ItemStack>> cache = new HashMap<>();
    
    public AbstractAutoCrafter(SlimefunItemStack item, int gp, ItemStack[] recipe) {
        super(item, recipe, 7, gp);
    }

    @Override
    public final void onBreak(Player p, Block b, BlockMenu menu, PowerGrid grid) {
        menu.dropItems(b.getLocation(), inputSlots);
        menu.dropItems(b.getLocation(), outputSlots);
        this.cache.remove(b.getLocation());
    }

    @Override
    public final void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b, @Nonnull PowerGrid grid) {
        super.onNewInstance(menu, b, grid);

        menu.addMenuClickHandler(keySlot, new ChestMenu.AdvancedMenuClickHandler() {
            @Override
            public boolean onClick(InventoryClickEvent inventoryClickEvent, Player player, int i, ItemStack itemStack, ClickAction clickAction) {
                if (inventoryClickEvent.getAction() != InventoryAction.HOTBAR_SWAP) {
                    ItemStack item = player.getItemOnCursor();
                    if (updateCache(b, item)) {
                        menu.replaceExistingItem(keySlot, StackUtils.getUnique(new CustomItem(item, 1)));
                    } else {
                        menu.replaceExistingItem(keySlot, MenuPreset.emptyKey);
                    }
                }
                return false;
            }

            @Override
            public boolean onClick(Player player, int i, ItemStack itemStack, ClickAction clickAction) {
                return false;
            }
        });

        // add to cache
        ItemStack current = menu.getItemInSlot(keySlot);
        if (current == null || !updateCache(b, current)) {
            menu.replaceExistingItem(keySlot, MenuPreset.emptyKey, false);
        }
    }

    private boolean updateCache(@Nonnull Block b, @Nonnull ItemStack item) {
        if (item.getType() == Material.AIR) {
            this.cache.put(b.getLocation(), null);
        } else {
            Pair<MultiFilter, ItemStack> pair = getRecipe(item);
            if (pair != null) {
                this.cache.put(b.getLocation(), pair);
                return true;
            }
        }
        return false;
    }

    @Override
    public final void setupInv(@Nonnull BlockMenuPreset blockMenuPreset) {
        for (int slot : MenuPreset.craftingInputBorder) {
            blockMenuPreset.addItem(slot, MenuPreset.borderItemInput, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : MenuPreset.craftingOutputBorder) {
            blockMenuPreset.addItem(slot, MenuPreset.borderItemOutput, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : MenuPreset.background) {
            blockMenuPreset.addItem(slot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
    }

    @Nonnull
    @Override
    public final int[] getTransportSlots(@Nonnull DirtyChestMenu menu, @Nonnull ItemTransportFlow flow, @Nonnull ItemStack item) {
        if (flow == ItemTransportFlow.WITHDRAW) {
            return outputSlots;
        }
        @Nullable Pair<MultiFilter, ItemStack> pair = this.cache.get(((BlockMenu) menu).getLocation());
        if (pair != null) {
            return pair.getFirstValue().getTransportSlot(item, inputSlots);
        }
        return new int[0];
    }

    @Override
    public final boolean process(@Nonnull BlockMenu menu, @Nonnull Block b) {

        Pair<MultiFilter, ItemStack> pair = this.cache.get(b.getLocation());

        if (pair == null || !menu.fits(pair.getSecondValue(), outputSlots)) {
            return false;
        }

        MultiFilter input = MultiFilter.fromMenu(FilterType.MIN_AMOUNT, menu, inputSlots);

        if (input.hashCode() == EMPTY || !pair.getFirstValue().fits(input, FilterType.MIN_AMOUNT)) {
            return false;
        }

        menu.pushItem(pair.getSecondValue().clone(), outputSlots);

        for (int i = 0 ; i < pair.getFirstValue().getAmounts().length ; i++) {
            int amount = pair.getFirstValue().getAmounts()[i];
            if (amount > 0) {
                menu.consumeItem(inputSlots[i], amount); // use to update cache
            }
        }

        return true;

    }
    
    @Nullable
    abstract Pair<MultiFilter, ItemStack> getRecipe(@Nonnull ItemStack item);

    @Nonnull
    @Override
    public UpgradeType getMaxLevel() {
        return UpgradeType.NONE;
    }

    @Override
    public int getUpgradeSlot() {
        return 0;
    }

}
