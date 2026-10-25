package xyz.ssander.mystic.client.mixin;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import xyz.ssander.mystic.client.MysticRenderState;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements MysticRenderState {
	@Unique
	private float mystic$levitation;
	@Unique
	private boolean mystic$shielding;
	@Unique
	private boolean mystic$rewinding;
	@Unique
	private float mystic$tracing = -1.0F;

	@Override
	public float mystic$levitation() {
		return this.mystic$levitation;
	}

	@Override
	public boolean mystic$shielding() {
		return this.mystic$shielding;
	}

	@Override
	public boolean mystic$rewinding() {
		return this.mystic$rewinding;
	}

	@Override
	public float mystic$tracing() {
		return this.mystic$tracing;
	}

	@Override
	public void mystic$set(float levitation, boolean shielding, boolean rewinding, float tracing) {
		this.mystic$levitation = levitation;
		this.mystic$shielding = shielding;
		this.mystic$rewinding = rewinding;
		this.mystic$tracing = tracing;
	}
}
