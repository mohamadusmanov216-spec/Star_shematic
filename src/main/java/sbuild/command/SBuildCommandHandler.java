package sbuild.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import sbuild.materials.MaterialAnalysisService;
import sbuild.materials.MaterialAvailability;
import sbuild.materials.MaterialReport;
import sbuild.planner.BuildPlannerService;
import sbuild.schematic.LoadedSchematic;
import sbuild.schematic.PlacementController;
import sbuild.schematic.SchematicService;
import sbuild.state.BuildStateService;
import sbuild.storage.StoragePoint;
import sbuild.storage.StorageService;
import sbuild.world.WorldService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class SBuildCommandHandler {
    private final BuildStateService buildState;
    private final SchematicService schematics;
    private final WorldService worldService;
    private final MaterialAnalysisService materials;
    private final StorageService storage;
    private final BuildPlannerService planner;

    public SBuildCommandHandler(
        BuildStateService buildState,
        SchematicService schematics,
        WorldService worldService,
        MaterialAnalysisService materials,
        StorageService storage,
        BuildPlannerService planner
    ) {
        this.buildState = buildState;
        this.schematics = schematics;
        this.worldService = worldService;
        this.materials = materials;
        this.storage = storage;
        this.planner = planner;
    }

    public int handleRoot(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("SBuild: /sbuild help"), false);
        return 1;
    }

    public int handleStatus(CommandContext<ServerCommandSource> ctx) {
        String schematic = buildState.loadedSchematic().map(LoadedSchematic::name).orElse("none");
        int blocks = buildState.loadedSchematic().map(LoadedSchematic::blockCount).orElse(0);
        ctx.getSource().sendFeedback(() -> Text.literal("status: schematic=" + schematic + ", blocks=" + blocks), false);
        return 1;
    }

    public int handleHelp(CommandContext<ServerCommandSource> ctx) {
        List<String> lines = List.of(
            "/sbuild status",
            "/sbuild schematic scan|list|load <name>|info",
            "/sbuild materials report",
            "/sbuild chest set <name>|list",
            "/sbuild planner preview"
        );
        for (String line : lines) {
            ctx.getSource().sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    public int handleSchematicScan(CommandContext<ServerCommandSource> ctx) {
        List<Path> paths = schematics.scanSchematics();
        ctx.getSource().sendFeedback(() -> Text.literal("found " + paths.size() + " schematics in " + schematics.rootDirectory()), false);
        return 1;
    }

    public int handleSchematicList(CommandContext<ServerCommandSource> ctx) {
        List<Path> paths = schematics.scanSchematics();
        if (paths.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("no .litematic files found"), false);
            return 1;
        }
        for (Path path : paths) {
            ctx.getSource().sendFeedback(() -> Text.literal("- " + path.getFileName()), false);
        }
        return 1;
    }

    public int handleSchematicLoad(CommandContext<ServerCommandSource> ctx, String name) {
        return schematics.loadByName(name)
            .map(loaded -> {
                buildState.setLoadedSchematic(loaded);
                ctx.getSource().sendFeedback(() -> Text.literal("loaded schematic: " + loaded.name() + " blocks=" + loaded.blockCount()), false);
                return 1;
            })
            .orElseGet(() -> {
                ctx.getSource().sendError(Text.literal("schematic not found: " + name));
                return 0;
            });
    }

    public int handleSchematicInfo(CommandContext<ServerCommandSource> ctx) {
        return buildState.loadedSchematic().map(schematic -> {
            ctx.getSource().sendFeedback(() -> Text.literal("name=" + schematic.name() + ", format=" + schematic.format() + ", blocks=" + schematic.blockCount()), false);
            ctx.getSource().sendFeedback(() -> Text.literal("bbox=" + schematic.boundingBox().min() + " -> " + schematic.boundingBox().max()), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendError(Text.literal("no loaded schematic"));
            return 0;
        });
    }

    public int handleMaterialsReport(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }

        PlacementController placement = buildState.placement().orElse(null);
        if (placement == null) {
            source.sendError(Text.literal("no loaded schematic"));
            return 0;
        }

        Map<LoadedSchematic.BlockPosition, String> worldStates = worldService.snapshotBlockStates(
            player.getServerWorld(),
            placement.transformedEntries().stream().map(Map.Entry::getKey).toList()
        );
        MaterialAvailability availability = storage.aggregateAvailability(player.getServerWorld());
        MaterialReport report = materials.analyze(placement, worldStates, availability);

        source.sendFeedback(() -> Text.literal("materials: required=" + report.totalRequired() + ", built=" + report.totalAlreadyBuilt()
            + ", remaining=" + report.totalRemaining() + ", available=" + report.totalAvailableInInventory()), false);
        report.rows().stream().limit(12).forEach(row ->
            source.sendFeedback(() -> Text.literal("- " + row.materialKey() + " need=" + row.remaining() + " inv=" + row.availableInInventory()), false)
        );
        return 1;
    }

    public int handlePlannerPreview(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = requirePlayer(ctx.getSource());
        if (player == null) {
            return 0;
        }
        PlacementController placement = buildState.placement().orElse(null);
        if (placement == null) {
            ctx.getSource().sendError(Text.literal("no loaded schematic"));
            return 0;
        }

        Map<LoadedSchematic.BlockPosition, String> worldStates = worldService.snapshotBlockStates(
            player.getServerWorld(),
            placement.transformedEntries().stream().map(Map.Entry::getKey).toList()
        );
        BuildPlannerService.BuildPlan plan = planner.createPlan(placement, worldStates);

        ctx.getSource().sendFeedback(() -> Text.literal("plan tasks=" + plan.tasks().size() + ", skipped=" + plan.skippedAlreadyCorrect()), false);
        plan.tasks().stream().limit(10).forEach(task ->
            ctx.getSource().sendFeedback(() -> Text.literal("- " + task.position() + " -> " + task.requiredState()), false)
        );
        return 1;
    }

    public int handleChestSet(CommandContext<ServerCommandSource> ctx, String name) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) {
            return 0;
        }
        try {
            ServerWorld world = player.getServerWorld();
            StoragePoint point = storage.registerLookedAtChest(world, player, name);
            source.sendFeedback(() -> Text.literal("storage registered: " + point.name() + " @ " + formatPos(point)), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("failed: " + e.getMessage()));
            return 0;
        }
    }

    public int handleChestList(CommandContext<ServerCommandSource> ctx) {
        List<StoragePoint> points = storage.listStoragePoints();
        if (points.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("no storage points"), false);
            return 1;
        }

        for (StoragePoint point : points) {
            ctx.getSource().sendFeedback(() -> Text.literal("- " + point.name() + " @ " + formatPos(point) + " p=" + point.priority()), false);
        }
        return 1;
    }

    private ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        try {
            return source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("player-only command"));
            return null;
        }
    }

    private String formatPos(StoragePoint point) {
        return point.blockPos().getX() + "," + point.blockPos().getY() + "," + point.blockPos().getZ();
    }
}
