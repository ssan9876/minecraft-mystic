package xyz.ssander.mystic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/** The Eye of Agamotto. Hold right-click to turn back time around you (see TimeRewind); let go to stop. */
public class EyeOfAgamottoItem extends Item {
	private static final int USE_TICKS = 72000;

	public EyeOfAgamottoItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return USE_TICKS;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		// The arms are posed by the client (PlayerModelMixin)
		return ItemUseAnimation.NONE;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
		if (entity instanceof ServerPlayer player) {
			TimeRewind.rewind(player, USE_TICKS - ticksRemaining);
			if ((USE_TICKS - ticksRemaining) % 5 == 0) {
				player.sendOverlayMessage(Component.translatable("message.mystic.rewinding", String.format("%.1f", TimeRewind.secondsBack(player))));
			}
		}
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
		if (entity instanceof ServerPlayer player) {
			TimeRewind.stop(player);
		}
		return true;
	}
}
