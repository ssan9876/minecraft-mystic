package xyz.ssander.mystic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import xyz.ssander.mystic.TimeRewind;

@Mixin(Containers.class)
public abstract class ContainersMixin {
	// Same for what spills out of broken chests, furnaces and other containers
	@WrapOperation(method = "dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private static boolean mystic$recordDrop(Level level, Entity entity, Operation<Boolean> original) {
		boolean added = original.call(level, entity);
		if (added && level instanceof ServerLevel serverLevel) {
			TimeRewind.recordDrop(serverLevel, entity);
		}
		return added;
	}
}
