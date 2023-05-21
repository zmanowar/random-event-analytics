package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EvilTwinSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;

	private static String MOLLY_NAME = "Molly";
	private static Integer EVIL_TWIN_MAP_REGION = 7504;

	private Set<Integer> mollyModels;

	@Getter
	private NPC evilTwin;

	@Override
	public boolean isEnabled()
	{
		return client.isInInstancedRegion() && client.getMapRegions() != null &&
			client.getMapRegions()[0] == EVIL_TWIN_MAP_REGION;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	private boolean isEvilTwin(NPC npc)
	{
		return getSetFromIntArray(npc.getComposition().getModels()).equals(mollyModels);
	}

	private boolean isMolly(NPC npc) {
		return npc.getName() != null && npc.getName().equals(MOLLY_NAME);
	}

	private Set<Integer> getSetFromIntArray(int[] array)
	{
		return Arrays.stream(array).boxed().collect(Collectors.toSet());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event) {
		if (!isEnabled()) return;

		if (event.getNpc() == evilTwin) {
			mollyModels = null;
			evilTwin = null;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!isEnabled())
		{
			return;
		}

		NPC npc = event.getNpc();

		/**
		 * We could potentially build a map of Molly -> Evil twin NPCIds,
		 * but that would require getting the random (or a npc viewer) to
		 * check for each NPC. Note NPCId seems to have an exhaustive list.
		 */
		if (isMolly(npc))
		{
			mollyModels = getSetFromIntArray(npc.getComposition().getModels());
		}
		else if (isEvilTwin(npc))
		{
			evilTwin = npc;
		}
	}
}
