package sbuild.storage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import sbuild.materials.MaterialReport;

import java.util.List;
import java.util.Map;

/**
 * Управляет регистрацией и анализом storage chest для бота.
 */
public final class StorageService {
    private final StorageRegistry registry;
    private final StorageScanner scanner;
    private final StorageController controller;
    private final RestockPlanner restockPlanner;

    public StorageService() {
        this.registry = new StorageRegistry();
        this.scanner = new StorageScanner();
        this.controller = new StorageController(registry, scanner);
        this.restockPlanner = new RestockPlanner();
    }

    public void initialize() {
        // Reserved for future persistence bootstrap.
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

    public RestockPlanner.RestockPlan planRestock(MaterialReport report) {
        return restockPlanner.buildPlan(report);
    }
}
