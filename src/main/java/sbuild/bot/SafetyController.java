package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Performs safety checks before bot actions are executed.
 */
public final class SafetyController {
    public boolean isSafeTarget(ServerWorld world, ServerPlayerEntity player, BlockPos target) {
        if (!world.getWorldBorder().contains(target)) {
            return false;
        }
        if (!world.isChunkLoaded(target)) {
            return false;
        }
        return target.getY() > world.getBottomY() + 1 && target.getY() < world.getTopY();
    }

    public boolean shouldRecover(ServerPlayerEntity player) {
        return player.isFallFlying() || player.fallDistance > 6.0F;
    }
}
