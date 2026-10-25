package xyz.ssander.mystic.gametest;

import java.util.List;
import java.util.function.Function;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import xyz.ssander.mystic.MandalaShield;
import xyz.ssander.mystic.Mystic;
import xyz.ssander.mystic.MysticAttachments;
import xyz.ssander.mystic.MysticComponents;
import xyz.ssander.mystic.SlingPortal;
import xyz.ssander.mystic.client.MysticClient;
import xyz.ssander.mystic.client.MysticPoses;

/** Plays through the mystic arts: sling ring portals both ways, the cloak, the mandala shield and the Eye of Agamotto. */
public class MysticClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerConnection connection = world.getConnection();
			TestServerContext server = world.getServer();
			connection.waitForChunksRender();
			server.runCommand("gamemode survival @a");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("tp @a 0.5 -60 0.5 0 0");
			connection.waitForClientboundPackets();

			slingRing(context, connection, server);
			cloak(context, connection, server);
			shield(context, connection, server);
			eye(context, connection, server);
		}
	}

	private static void slingRing(ClientGameTestContext context, TestServerConnection connection, TestServerContext server) {
		server.runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Mystic.SLING_RING)));
		connection.waitForClientboundPackets();

		// Attune it here, facing south
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse);
		connection.waitForServerboundPackets();
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(25);
		check(server, p -> {
			MysticComponents.Destination destination = p.getMainHandItem().get(MysticComponents.DESTINATION);
			return destination != null && destination.pos().pos().equals(new BlockPos(0, -60, 0));
		}, "sneak-using the sling ring attunes it to where the player stands");

		// Somewhere else, open a portal back
		server.runCommand("tp @a 20.5 -60 0.5 0 0");
		connection.waitForClientboundPackets();
		context.waitTicks(5);
		context.getInput().pressKey(options -> options.keyUse);
		connection.waitForServerboundPackets();
		context.waitTicks(8);
		check(server, p -> portals(p.level()).size() == 2, "using the sling ring opens a pair of portals");
		checkClient(context, mc -> MysticPoses.tracing(mc.player, 1.0F) >= 0.0F, "opening a portal traces it with the ring hand");
		camera(context, CameraType.THIRD_PERSON_BACK);
		TestCamera.yawOffset = 120.0F;
		context.waitTick();
		context.takeScreenshot("mystic-1-tracing");
		TestCamera.yawOffset = 0.0F;
		context.waitTicks(25);
		check(server, p -> portals(p.level()).stream().anyMatch(portal -> portal.position().distanceTo(new Vec3(20.5, -60, 2.7)) < 0.2),
				"the near portal opens in front of the player");
		check(server, p -> portals(p.level()).stream().anyMatch(portal -> portal.position().distanceTo(new Vec3(0.5, -60, 0.5)) < 0.2),
				"the far portal opens on the attuned spot");
		context.takeScreenshot("mystic-2-portal-back");
		TestCamera.yawOffset = 150.0F;
		context.waitTick();
		context.takeScreenshot("mystic-2-portal-front-quarter");
		TestCamera.yawOffset = 0.0F;
		camera(context, CameraType.FIRST_PERSON);
		context.takeScreenshot("mystic-2-portal-first-person");

		// Walk through: out at the attuned spot, still heading south
		context.getInput().holdKey(options -> options.keyUp);
		for (int i = 0; i < 60 && context.computeOnClient(mc -> mc.player.getX() > 10.0); i++) {
			context.waitTick();
		}
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(5);
		check(server, p -> p.getX() < 3.0 && p.getZ() > 0.5 && p.getZ() < 6.0, "walking through the portal comes out at the attuned spot");

		// Turn round and look back through the far end
		context.runOnClient(mc -> mc.player.setYRot(180.0F));
		context.waitTicks(3);
		context.takeScreenshot("mystic-3-far-end");

		// And back through it, the other way
		context.getInput().holdKey(options -> options.keyUp);
		for (int i = 0; i < 60 && context.computeOnClient(mc -> mc.player.getX() < 10.0); i++) {
			context.waitTick();
		}
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(5);
		check(server, p -> p.getX() > 18.0 && p.getZ() < 2.7, "walking back through the far end comes out behind the near one");

		// Thrown items go through too
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			ItemEntity item = new ItemEntity(p.level(), 20.5, -59.0, 1.6, new ItemStack(Items.DIAMOND));
			item.setDeltaMovement(0.0, 0.1, 0.4);
			item.setPickUpDelay(200);
			p.level().addFreshEntity(item);
		});
		context.waitTicks(15);
		check(server, p -> !p.level().getEntitiesOfClass(ItemEntity.class, new AABB(new BlockPos(0, -60, 0)).inflate(5.0)).isEmpty(),
				"an item thrown through the portal comes out the far end");

		// Using the ring again closes both ends
		context.getInput().pressKey(options -> options.keyUse);
		connection.waitForServerboundPackets();
		context.waitTicks(30);
		check(server, p -> portals(p.level()).isEmpty(), "using the ring again closes the portals");
		server.runCommand("kill @e[type=item]");
	}

	private static void cloak(ClientGameTestContext context, TestServerConnection connection, TestServerContext server) {
		server.runCommand("tp @a 0.5 -60 0.5 0 0");
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Mystic.CLOAK_OF_LEVITATION));
		});
		connection.waitForClientboundPackets();
		context.waitTicks(5);
		check(server, p -> p.getAbilities().mayfly, "the cloak lets its wearer fly");

		context.runOnClient(mc -> mc.gui.hud.toggle());
		camera(context, CameraType.THIRD_PERSON_FRONT);
		context.takeScreenshot("mystic-4-cloak-front");
		camera(context, CameraType.THIRD_PERSON_BACK);
		context.takeScreenshot("mystic-4-cloak-back");

		// Levitating, filmed from the side
		server.runCommand("execute as @a at @s run tp @s ~ ~3 ~");
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.getAbilities().flying = true;
			p.onUpdateAbilities();
		});
		connection.waitForClientboundPackets();
		context.waitTicks(20);
		check(server, p -> p.getAttachedOrElse(MysticAttachments.LEVITATING, false), "flying on the cloak is flagged");
		checkClient(context, mc -> MysticPoses.levitation(mc.player, 1.0F) > 0.9F, "cloak flight eases into the levitating pose");
		TestCamera.yawOffset = 90.0F;
		context.waitTick();
		context.takeScreenshot("mystic-5-levitating-side");
		TestCamera.yawOffset = 160.0F;
		context.waitTick();
		context.takeScreenshot("mystic-5-levitating-front-quarter");
		TestCamera.yawOffset = 0.0F;
		context.runOnClient(mc -> mc.gui.hud.toggle());
		camera(context, CameraType.FIRST_PERSON);

		// A long fall: the cloak catches its wearer
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			p.getAbilities().flying = false;
			p.onUpdateAbilities();
		});
		server.runCommand("execute as @a at @s run tp @s ~ ~30 ~");
		connection.waitForClientboundPackets();
		for (int i = 0; i < 400 && !context.computeOnClient(mc -> mc.player.onGround()); i++) {
			context.waitTick();
		}
		context.waitTicks(5);
		check(server, p -> p.getHealth() >= p.getMaxHealth(), "falling 30 blocks in the cloak does no damage");
	}

	private static void shield(ClientGameTestContext context, TestServerConnection connection, TestServerContext server) {
		server.runCommand("tp @a 0.5 -60 0.5 0 0");
		connection.waitForClientboundPackets();
		context.waitTicks(5);

		context.getInput().holdKey(options -> MysticClient.shieldKey);
		context.waitTicks(5);
		connection.waitForServerboundPackets();
		check(server, MandalaShield::isShielding, "holding the shield key in the cloak conjures a shield");
		connection.waitForClientboundPackets();
		checkClient(context, mc -> MandalaShield.isShielding(mc.player), "the shield syncs to the client");
		context.takeScreenshot("mystic-6-shield-first-person");
		context.runOnClient(mc -> mc.gui.hud.toggle());
		camera(context, CameraType.THIRD_PERSON_FRONT);
		context.takeScreenshot("mystic-6-shield-front");
		camera(context, CameraType.THIRD_PERSON_BACK);
		TestCamera.yawOffset = 110.0F;
		context.waitTick();
		context.takeScreenshot("mystic-6-shield-side");
		TestCamera.yawOffset = 0.0F;
		context.runOnClient(mc -> mc.gui.hud.toggle());
		camera(context, CameraType.FIRST_PERSON);

		// An arrow from the front bounces back off it
		server.runOnServer(s -> {
			ServerPlayer p = player(s);
			Arrow arrow = new Arrow(p.level(), p.getX(), p.getEyeY() - 0.2, p.getZ() + 7.0, new ItemStack(Items.ARROW), null);
			arrow.setDeltaMovement(0.0, 0.0, -1.5);
			p.level().addFreshEntity(arrow);
		});
		context.waitTicks(8);
		check(server, p -> p.getHealth() >= p.getMaxHealth(), "the shield stops an arrow from the front");
		check(server, p -> p.level().getEntitiesOfClass(Arrow.class, p.getBoundingBox().inflate(20.0)).stream()
				.anyMatch(arrow -> arrow.getDeltaMovement().z > 0.0 && arrow.getOwner() == p), "the arrow flies back out, now the caster's");

		// A blow from in front is stopped; one from behind isn't
		check(server, p -> !p.hurtServer(p.level(), p.damageSources().mobAttack(zombie(p, 1.5)), 4.0F), "the shield stops a blow from the front");
		check(server, p -> p.hurtServer(p.level(), p.damageSources().mobAttack(zombie(p, -1.5)), 4.0F), "a blow from behind gets past the shield");
		server.runCommand("kill @e[type=zombie]");
		server.runCommand("kill @e[type=arrow]");
		server.runCommand("kill @e[type=item]");

		context.getInput().releaseKey(options -> MysticClient.shieldKey);
		context.waitTicks(3);
		connection.waitForServerboundPackets();
		check(server, p -> !MandalaShield.isShielding(p), "letting go of the shield key drops the shield");
		server.runOnServer(s -> player(s).setHealth(player(s).getMaxHealth()));
	}

	private static void eye(ClientGameTestContext context, TestServerConnection connection, TestServerContext server) {
		server.runCommand("tp @a 0.5 -60 0.5 0 0");
		// A little stone house with a chest of diamonds in it, to wreck and then put back
		server.runCommand("fill -3 -60 6 3 -58 10 minecraft:stone_bricks");
		server.runCommand("fill -2 -60 7 2 -58 9 minecraft:air");
		server.runCommand("setblock 0 -60 8 minecraft:chest");
		server.runOnServer(s -> {
			if (player(s).level().getBlockEntity(new BlockPos(0, -60, 8)) instanceof ChestBlockEntity chest) {
				chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
			}
		});
		int walls = server.computeOnServer(s -> stoneBricks(player(s).level()));
		// Long enough ago that the rewind below won't reach back to the building
		context.waitTicks(200);

		// Break two blocks of the wall by hand (they drop), and blow up the inside
		server.runCommand("setblock 0 -59 6 minecraft:air destroy");
		server.runCommand("setblock -1 -59 6 minecraft:air destroy");
		server.runOnServer(s -> {
			ServerLevel level = player(s).level();
			PrimedTnt tnt = new PrimedTnt(level, 0.5, -60.0, 9.5, null);
			tnt.setFuse(1);
			level.addFreshEntity(tnt);
		});
		context.waitTicks(20);
		check(server, p -> stoneBricks(p.level()) < walls - 6, "the explosion blows holes in the house");
		check(server, p -> !p.level().getBlockState(new BlockPos(0, -60, 8)).is(Blocks.CHEST), "the explosion destroys the chest");
		check(server, p -> !p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(30.0)).isEmpty(), "the wreck spills items");
		context.runOnClient(mc -> mc.gui.hud.toggle());
		camera(context, CameraType.THIRD_PERSON_BACK);
		context.takeScreenshot("mystic-7-wrecked");

		// Turn back time
		server.runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Mystic.EYE_OF_AGAMOTTO)));
		connection.waitForClientboundPackets();
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(14);
		checkClient(context, mc -> MysticPoses.isRewinding(mc.player), "holding the eye turns back time");
		camera(context, CameraType.THIRD_PERSON_FRONT);
		context.takeScreenshot("mystic-8-rewinding-front");
		camera(context, CameraType.THIRD_PERSON_BACK);
		TestCamera.yawOffset = 30.0F;
		context.waitTick();
		context.takeScreenshot("mystic-8-rewinding-back");
		TestCamera.yawOffset = 0.0F;
		camera(context, CameraType.FIRST_PERSON);
		context.takeScreenshot("mystic-8-rewinding-first-person");
		context.waitTicks(10);
		context.getInput().releaseKey(options -> options.keyUse);
		context.waitTicks(5);

		check(server, p -> stoneBricks(p.level()) == walls, "turning back time rebuilds the house");
		check(server, p -> p.level().getBlockEntity(new BlockPos(0, -60, 8)) instanceof ChestBlockEntity chest
				&& chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 5, "the chest comes back with its diamonds");
		check(server, p -> p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(30.0)).isEmpty(),
				"what spilled out goes back, so nothing is doubled");
		camera(context, CameraType.THIRD_PERSON_BACK);
		context.takeScreenshot("mystic-9-rebuilt");
		context.runOnClient(mc -> mc.gui.hud.toggle());
		camera(context, CameraType.FIRST_PERSON);
	}

	private static int stoneBricks(ServerLevel level) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(-3, -60, 6, 3, -58, 10)) {
			if (level.getBlockState(pos).is(Blocks.STONE_BRICKS)) {
				count++;
			}
		}
		return count;
	}

	private static LivingEntity zombie(ServerPlayer player, double south) {
		LivingEntity zombie = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.COMMAND);
		zombie.setPos(player.getX(), player.getY(), player.getZ() + south);
		player.level().addFreshEntity(zombie);
		return zombie;
	}

	private static List<SlingPortal> portals(ServerLevel level) {
		return level.getEntitiesOfClass(SlingPortal.class, new AABB(new BlockPos(0, -60, 0)).inflate(64.0));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void camera(ClientGameTestContext context, CameraType type) {
		context.runOnClient(mc -> mc.options.setCameraType(type));
		context.waitTicks(3);
	}

	private static void check(TestServerContext server, Function<ServerPlayer, Boolean> condition, String what) {
		if (!server.computeOnServer(s -> condition.apply(player(s)))) {
			throw new AssertionError("Expected: " + what);
		}
	}

	private static void checkClient(ClientGameTestContext context, Function<Minecraft, Boolean> condition, String what) {
		if (!context.computeOnClient(condition::apply)) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
