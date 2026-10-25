package xyz.ssander.mystic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;

import xyz.ssander.mystic.Mystic;

/**
 * Glowing sigils drawn in the world: a square texture of white lines on transparent, tinted to the spell's colour,
 * full bright and seen from both sides. They're blended over what's behind rather than added to it, so they keep
 * their colour against a bright sky.
 */
public final class Glyphs {
	/** The mandala shield's sigil: circles, runes and two squares turned to an eight-pointed star. */
	public static final Identifier MANDALA = Mystic.id("textures/effect/mandala.png");
	/** The Eye of Agamotto's sigil: a clock face of runes. */
	public static final Identifier TIME = Mystic.id("textures/effect/time_mandala.png");
	/** A sling ring portal's rim of sparks, and the faint glow filling it. */
	public static final Identifier PORTAL_RING = Mystic.id("textures/effect/portal_ring.png");
	public static final Identifier PORTAL_GLOW = Mystic.id("textures/effect/portal_glow.png");

	private Glyphs() {
	}

	/**
	 * Submits the sigil as a square in the pose's XY plane, centred on its origin and {@code radius} from the middle to
	 * each edge.
	 */
	public static void submit(SubmitNodeCollector collector, PoseStack poseStack, Identifier texture, float radius, int rgb, float alpha) {
		float r = (rgb >> 16 & 0xFF) / 255.0F;
		float g = (rgb >> 8 & 0xFF) / 255.0F;
		float b = (rgb & 0xFF) / 255.0F;
		collector.submitCustomGeometry(poseStack, RenderTypes.eyes(texture), (pose, consumer) -> {
			Matrix4f m = pose.pose();
			// Front, then back: the pipeline culls faces turned away
			vertex(m, pose, consumer, -radius, -radius, 0.0F, 1.0F, r, g, b, alpha, 1.0F);
			vertex(m, pose, consumer, radius, -radius, 1.0F, 1.0F, r, g, b, alpha, 1.0F);
			vertex(m, pose, consumer, radius, radius, 1.0F, 0.0F, r, g, b, alpha, 1.0F);
			vertex(m, pose, consumer, -radius, radius, 0.0F, 0.0F, r, g, b, alpha, 1.0F);
			vertex(m, pose, consumer, -radius, radius, 0.0F, 0.0F, r, g, b, alpha, -1.0F);
			vertex(m, pose, consumer, radius, radius, 1.0F, 0.0F, r, g, b, alpha, -1.0F);
			vertex(m, pose, consumer, radius, -radius, 1.0F, 1.0F, r, g, b, alpha, -1.0F);
			vertex(m, pose, consumer, -radius, -radius, 0.0F, 1.0F, r, g, b, alpha, -1.0F);
		});
	}

	private static void vertex(Matrix4f m, PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float u, float v,
			float r, float g, float b, float alpha, float facing) {
		consumer.addVertex(m, x, y, 0.0F)
				.setColor(r, g, b, alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, 0.0F, 0.0F, facing);
	}
}
