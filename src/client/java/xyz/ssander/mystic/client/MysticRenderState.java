package xyz.ssander.mystic.client;

/** Mystic arts data added to the player render state (see AvatarRenderStateMixin). */
public interface MysticRenderState {
	/** How far into the cloak's levitating pose the player is (see MysticPoses). */
	float mystic$levitation();

	/** Whether the player holds up a mandala shield, both palms out. */
	boolean mystic$shielding();

	/** Whether the player is turning back time with the Eye of Agamotto. */
	boolean mystic$rewinding();

	/** How far through tracing a portal the player is, 0 to 1, or -1 if they aren't. */
	float mystic$tracing();

	void mystic$set(float levitation, boolean shielding, boolean rewinding, float tracing);
}
