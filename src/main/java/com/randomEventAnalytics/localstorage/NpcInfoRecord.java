package com.randomEventAnalytics.localstorage;

import lombok.AllArgsConstructor;
import lombok.Data;

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
}