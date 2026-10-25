package xyz.ssander.mystic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import xyz.ssander.mystic.TimeRewind;

@Mixin(Level.class)
public abstract class LevelMixin {
	// Remembers every block change on the server, with what was there before, so the Eye of Agamotto can undo it
	@WrapOperation(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState mystic$recordChange(LevelChunk chunk, BlockPos pos, BlockState state, int flags, Operation<BlockState> original) {
		if (!((Object) this instanceof ServerLevel level) || TimeRewind.isRestoring()) {
			return original.call(chunk, pos, state, flags);
		}
		// Saved before the change, while a container still holds what it would spill
		BlockState before = chunk.getBlockState(pos);
		BlockEntity blockEntity = before != state && before.hasBlockEntity() ? chunk.getBlockEntity(pos) : null;
		CompoundTag saved = blockEntity != null ? blockEntity.saveWithFullMetadata(level.registryAccess()) : null;
		BlockState old = original.call(chunk, pos, state, flags);
		if (old != null && old != state) {
			TimeRewind.record(level, pos, old, saved);
		}
		return old;
	}
}
