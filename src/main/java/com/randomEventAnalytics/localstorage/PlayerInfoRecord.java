package com.randomEventAnalytics.localstorage;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Data
@AllArgsConstructor
public class PlayerInfoRecord
{
	public final int combatLevel;
	public final int localX;
	public final int localY;
	public final int worldX;
	public final int worldY;
	public final int worldPlane;

	public static PlayerInfoRecord create(Player player)
	{
		LocalPoint playerLocalLocation = player.getLocalLocation();
		WorldPoint playerWorldLocation = player.getWorldLocation();
		return new PlayerInfoRecord(player.getCombatLevel(), playerLocalLocation.getX(), playerLocalLocation.getY(),
			playerWorldLocation.getX(), playerWorldLocation.getY(), playerWorldLocation.getPlane());
	}
}
