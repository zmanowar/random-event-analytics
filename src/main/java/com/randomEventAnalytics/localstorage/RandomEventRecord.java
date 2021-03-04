package com.randomEventAnalytics.localstorage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RandomEventRecord
{
	public final long spawnedTime;
	public final int secondsSinceLastRandomEvent;
	public final int ticksSinceLastRandomEvent;
	public final NpcInfoRecord npcInfoRecord;
	public final PlayerInfoRecord playerInfoRecord;
	public final XpInfoRecord xpInfoRecord;
}