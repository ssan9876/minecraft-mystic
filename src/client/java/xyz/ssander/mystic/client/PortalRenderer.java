package xyz.ssander.mystic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

import xyz.ssander.mystic.SlingPortal;

/**
 * Draws a sling ring portal: two rims of sparks spinning against each other around a faint warm glow, opening out
 * from a point and shrinking back to one as it closes.
 */
public class PortalRenderer extends EntityRenderer<SlingPortal, PortalRenderer.State> {
	private static final int SPARKS = 0xFF7A14;
	private static final int HOT = 0xFFC458;
	private static final int GLOW = 0xFF8A2A;

	public static class State extends EntityRenderState {
		float openness;
		float facing;
	}

	public PortalRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SlingPortal portal, State state, float partialTick) {
		super.extractRenderState(portal, state, partialTick);
		state.openness = portal.openness(partialTick);
		state.facing = portal.facing();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.openness > 0.0F) {
			// Eases out as it opens, so the rim flares out fast and settles
			float open = 1.0F - (1.0F - state.openness) * (1.0F - state.openness);
			float radius = SlingPortal.RADIUS * open;
			float flicker = 0.85F + 0.15F * Mth.sin(state.ageInTicks * 1.7F);

			poseStack.pushPose();
			poseStack.translate(0.0F, SlingPortal.CENTER_HEIGHT, 0.0F);
			poseStack.rotateDegrees(Axis.YP, -state.facing);

			Glyphs.submit(collector, poseStack, Glyphs.PORTAL_GLOW, radius * 1.02F, GLOW, 0.8F);

			poseStack.pushPose();
			poseStack.translate(0.0F, 0.0F, 0.01F);
			poseStack.rotateDegrees(Axis.ZP, -state.ageInTicks * 9.0F);
			Glyphs.submit(collector, poseStack, Glyphs.PORTAL_RING, radius * 1.22F, SPARKS, flicker);
			poseStack.popPose();

			poseStack.pushPose();
			poseStack.translate(0.0F, 0.0F, -0.01F);
			poseStack.rotateDegrees(Axis.ZP, state.ageInTicks * 5.0F + 40.0F);
			Glyphs.submit(collector, poseStack, Glyphs.PORTAL_RING, radius * 1.14F, HOT, 0.7F * flicker);
			poseStack.popPose();

			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
