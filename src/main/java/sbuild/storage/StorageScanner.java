package sbuild.storage;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;

/**
 * Сканирует инвентарь сундука и возвращает доступные материалы.
 */
public final class StorageScanner {
    public Map<String, Long> scanChestInventory(ServerWorld world, StoragePoint point) {
        BlockEntity blockEntity = world.getBlockEntity(point.blockPos());
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            return Map.of();
        }

        return scanInventory(chest);
    }

    public Map<String, Long> scanInventory(Inventory inventory) {
        Map<String, Long> counts = new HashMap<>();

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            counts.merge(itemId, (long) stack.getCount(), Long::sum);
        }

        return Map.copyOf(counts);
    }
}
