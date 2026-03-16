package sbuild.state;

import sbuild.ai.AiService;
import sbuild.block.BlockPlacementService;
import sbuild.bot.BuildBotService;
import sbuild.config.ConfigService;
import sbuild.command.CommandService;
import sbuild.gui.GuiService;
import sbuild.materials.MaterialAnalysisService;
import sbuild.planner.BuildPlannerService;
import sbuild.redstone.RedstoneService;
import sbuild.render.RenderService;
import sbuild.schematic.SchematicService;
import sbuild.storage.StorageService;
import sbuild.world.WorldService;
import sbuild.util.UtilityService;

/**
 * Lightweight dependency container for module services.
 *
 * <p>The context is passed to command handlers and future subsystems so dependencies are
 * explicitly provided instead of resolved from global static state.</p>
 */
public record AppContext(
    ConfigService configService,
    CommandService commandService,
    BuildStateService buildStateService,
    SchematicService schematicService,
    GuiService guiService,
    RenderService renderService,
    WorldService worldService,
    MaterialAnalysisService materialAnalysisService,
    StorageService storageService,
    BuildPlannerService plannerService,
    AiService aiService,
    RedstoneService redstoneService,
    BlockPlacementService blockPlacementService,
    BuildBotService buildBotService,
    UtilityService utilityService
) {
}
