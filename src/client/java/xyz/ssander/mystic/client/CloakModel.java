package xyz.ssander.mystic.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import xyz.ssander.mystic.Mystic;

/**
 * The Cloak of Levitation: a long, wide cloak hanging from the shoulders, a mantle over each shoulder, and a high
 * collar standing up behind the head. Built on the player model so it moves with the body and arms; the cloak swings
 * like a cape (and billows while levitating).
 */
public class CloakModel extends PlayerModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Mystic.id("cloak_of_levitation"), "main");
	public static final Identifier TEXTURE = Mystic.id("textures/entity/cloak_of_levitation.png");

	private final ModelPart cloak;

	public CloakModel(ModelPart root) {
		super(root, false);
		this.cloak = this.body.getChild("cloak");
	}

	/** Texture layout: see cloak() in tools/gen_textures.py. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition root = mesh.getRoot().clearRecursively();
		PartDefinition body = root.getChild("body");
		// Hangs just behind the tunic (worn as chest armor, which stands a pixel out from the body)
		body.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, 0.0F, -1.0F, 12.0F, 20.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -0.5F, 3.2F, 0.0F, Mth.PI, 0.0F));
		body.addOrReplaceChild("collar_back", CubeListBuilder.create().texOffs(0, 40).addBox(-5.5F, -7.0F, 4.5F, 11.0F, 7.0F, 1.0F),
				PartPose.rotation(-0.12F, 0.0F, 0.0F));
		body.addOrReplaceChild("collar_right", CubeListBuilder.create().texOffs(24, 40).addBox(-5.5F, -7.0F, -1.0F, 1.0F, 7.0F, 6.0F),
				PartPose.rotation(0.0F, 0.0F, 0.08F));
		body.addOrReplaceChild("collar_left", CubeListBuilder.create().texOffs(38, 40).addBox(4.5F, -7.0F, -1.0F, 1.0F, 7.0F, 6.0F),
				PartPose.rotation(0.0F, 0.0F, -0.08F));
		root.getChild("right_arm").addOrReplaceChild("right_mantle",
				CubeListBuilder.create().texOffs(0, 24).addBox(-4.5F, -3.5F, -3.5F, 7.0F, 5.0F, 7.0F), PartPose.ZERO);
		root.getChild("left_arm").addOrReplaceChild("left_mantle",
				CubeListBuilder.create().texOffs(28, 24).addBox(-2.5F, -3.5F, -3.5F, 7.0F, 5.0F, 7.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		// Swings like a cape. Levitating, it billows out behind, rippling.
		float levitation = ((MysticRenderState) state).mystic$levitation();
		float billow = levitation * (14.0F + 6.0F * Mth.sin(state.ageInTicks * 0.18F));
		this.cloak.rotateBy(new Quaternionf()
				.rotateY(-Mth.PI)
				.rotateX((6.0F + state.capeLean / 2.0F + state.capeFlap + billow) * Mth.DEG_TO_RAD)
				.rotateZ(state.capeLean2 / 2.0F * Mth.DEG_TO_RAD)
				.rotateY((180.0F - state.capeLean2 / 2.0F) * Mth.DEG_TO_RAD));
	}
}
