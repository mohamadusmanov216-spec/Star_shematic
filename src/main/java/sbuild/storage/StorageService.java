package sbuild.storage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import sbuild.materials.MaterialAvailability;
import sbuild.materials.MaterialReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StorageService {
    private final StorageRegistry registry;
    private final StorageController controller;
    private final RestockPlanner restockPlanner;

    public StorageService() {
        this.registry = new StorageRegistry();
        this.controller = new StorageController(registry, new StorageScanner());
        this.restockPlanner = new RestockPlanner();
    }

    public StoragePoint registerLookedAtChest(ServerWorld world, PlayerEntity player, String name) {
        return controller.registerLookedAtChest(world, player, name);
    }

    public List<StoragePoint> listStoragePoints() {
        return controller.listStoragePoints();
    }

    public Map<String, Long> scanStoragePoint(StoragePoint point, ServerWorld world) {
        return controller.scan(point, world);
    }

    public MaterialAvailability aggregateAvailability(ServerWorld world) {
        Map<String, Long> total = new HashMap<>();
        for (StoragePoint point : listStoragePoints()) {
            Map<String, Long> scanned = scanStoragePoint(point, world);
            for (Map.Entry<String, Long> entry : scanned.entrySet()) {
                total.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }
        return new MaterialAvailability(total);
    }

    public RestockPlanner.RestockPlan planRestock(MaterialReport report) {
        return restockPlanner.buildPlan(report);
    }
}
