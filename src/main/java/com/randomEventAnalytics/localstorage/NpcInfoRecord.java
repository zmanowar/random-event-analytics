package com.randomEventAnalytics.localstorage;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Data
@AllArgsConstructor
public class NpcInfoRecord
{
	public final int npcId;
	public final String npcName;
	public final int combatLevel;
	public final int localX;
	public final int localY;
	public final int worldX;
	public final int worldY;
	public final int worldPlane;

	public static NpcInfoRecord create(NPC npc)
	{
		LocalPoint npcLocalLocation = npc.getLocalLocation();
		WorldPoint npcWorldLocation = npc.getWorldLocation();
		return new NpcInfoRecord(npc.getId(), npc.getName(), npc.getCombatLevel(), npcLocalLocation.getX(),
			npcLocalLocation.getY(), npcWorldLocation.getX(), npcWorldLocation.getY(), npcWorldLocation.getPlane());
	}
}