package xyz.ssander.mystic;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import xyz.ssander.mystic.MysticComponents.Destination;

/**
 * The sling ring. Sneak and use it to attune it to where you stand; use it anywhere else to trace a portal there,
 * and again to close it.
 */
public class SlingRingItem extends Item {
	private static final int COOLDOWN_TICKS = 20;

	public SlingRingItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			attune(serverLevel, serverPlayer, stack);
			return InteractionResult.SUCCESS;
		}

		SlingPortal open = SlingPortal.openBy(serverPlayer);
		if (open != null) {
			open.closePair();
			return InteractionResult.SUCCESS;
		}

		Destination destination = stack.get(MysticComponents.DESTINATION);
		if (destination == null) {
			player.sendOverlayMessage(Component.translatable("message.mystic.not_attuned"));
			return InteractionResult.FAIL;
		}
		ServerLevel destinationLevel = serverLevel.getServer().getLevel(destination.pos().dimension());
		if (destinationLevel == null) {
			player.sendOverlayMessage(Component.translatable("message.mystic.destination_gone"));
			return InteractionResult.FAIL;
		}

		SlingPortal.open(serverPlayer, destinationLevel, destination);
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}

	private static void attune(ServerLevel level, ServerPlayer player, ItemStack stack) {
		BlockPos pos = player.blockPosition();
		stack.set(MysticComponents.DESTINATION, new Destination(GlobalPos.of(level.dimension(), pos), Mth.wrapDegrees(player.getYRot())));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
		// A ring of sparks marks the spot
		for (int i = 0; i < 24; i++) {
			float angle = Mth.TWO_PI * i / 24;
			level.sendParticles(SlingPortal.SPARK, player.getX() + Mth.cos(angle) * 0.8, player.getY() + 0.1, player.getZ() + Mth.sin(angle) * 0.8,
					1, 0.0, 0.0, 0.0, 0.0);
		}
		player.sendOverlayMessage(Component.translatable("message.mystic.attuned", pos.getX(), pos.getY(), pos.getZ()));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		Destination destination = stack.get(MysticComponents.DESTINATION);
		if (destination == null) {
			builder.accept(Component.translatable("item.mystic.sling_ring.unattuned").withStyle(ChatFormatting.GRAY));
			return;
		}
		BlockPos pos = destination.pos().pos();
		builder.accept(Component.translatable("item.mystic.sling_ring.attuned", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD));
		builder.accept(Component.literal(destination.pos().dimension().identifier().toString()).withStyle(ChatFormatting.DARK_GRAY));
	}
}
