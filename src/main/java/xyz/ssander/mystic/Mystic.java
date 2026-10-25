package xyz.ssander.mystic;

import java.util.function.Function;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import xyz.ssander.mystic.network.ShieldPayload;

public class Mystic implements ModInitializer {
	public static final String MOD_ID = "mystic";

	/** The sorcerer's tunic drawn over the body while the cloak is worn (assets/mystic/equipment/sorcerer.json). */
	public static final ResourceKey<EquipmentAsset> SORCERER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, id("sorcerer"));

	/** Opens a portal to wherever it's attuned. Sneak and use it to attune it to where you stand. */
	public static final SlingRingItem SLING_RING = register("sling_ring", SlingRingItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	/** Worn like a chestplate: flight, no fall damage, and the sorcerer's tunic and cape. Its wearer can conjure shields. */
	public static final Item CLOAK_OF_LEVITATION = register("cloak_of_levitation", Item::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
					.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
							.setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
							.setAsset(SORCERER_ASSET)
							.build())
					.attributes(ItemAttributeModifiers.builder()
							.add(Attributes.ARMOR, new AttributeModifier(id("cloak_armor"), 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST)
							.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id("cloak_toughness"), 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST)
							.build()));

	/** Held down, it turns back time around its holder: blocks broken, placed or blown up run backwards. */
	public static final EyeOfAgamottoItem EYE_OF_AGAMOTTO = register("eye_of_agamotto", EyeOfAgamottoItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	public static final EntityType<SlingPortal> SLING_PORTAL = registerEntity("sling_portal",
			EntityType.Builder.<SlingPortal>of(SlingPortal::new, MobCategory.MISC)
					.sized(SlingPortal.SIZE, SlingPortal.SIZE)
					.clientTrackingRange(10)
					.updateInterval(1)
					.noSummon()
					.fireImmune());

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}

	private static <T extends Entity> EntityType<T> registerEntity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	@Override
	public void onInitialize() {
		MysticAttachments.init();
		MysticComponents.init();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(SLING_RING);
			output.accept(CLOAK_OF_LEVITATION);
			output.accept(EYE_OF_AGAMOTTO);
		});

		PayloadTypeRegistry.serverboundPlay().register(ShieldPayload.TYPE, ShieldPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ShieldPayload.TYPE, (payload, context) -> MandalaShield.setShielding(context.player(), payload.shielding()));

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof Player player)) {
				return true;
			}
			// The cloak catches its wearer before they hit the ground
			if (source.is(DamageTypeTags.IS_FALL) && Cloak.isWorn(player)) {
				return false;
			}
			return !(player instanceof ServerPlayer serverPlayer && MandalaShield.blocks(serverPlayer, source));
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Cloak.tick(player);
				MandalaShield.tick(player);
				TimeRewind.tickPlayer(player);
			}
			TimeRewind.prune(server);
		});
	}
}
