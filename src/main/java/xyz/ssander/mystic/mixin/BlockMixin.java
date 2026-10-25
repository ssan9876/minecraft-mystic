package xyz.ssander.mystic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import xyz.ssander.mystic.TimeRewind;

@Mixin(Block.class)
public abstract class BlockMixin {
	// Items dropped by broken blocks go back when time is turned back past their breaking
	@WrapOperation(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private static boolean mystic$recordDrop(Level level, Entity entity, Operation<Boolean> original) {
		boolean added = original.call(level, entity);
		if (added && level instanceof ServerLevel serverLevel) {
			TimeRewind.recordDrop(serverLevel, entity);
		}
		return added;
	}
}
