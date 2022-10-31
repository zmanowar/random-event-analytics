package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PrisonPeteSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;

	private static final Integer PRISON_PETE_MAP_REGION = 8261;
	private static final Integer BALLOON_ANIMAL_WIDGET_GROUP_ID = 273;
	private static final Integer BALLOON_ANIMAL_WIDGET_CHILD_ID = 4;
	private static final HashMap<Integer, Integer> BALLOON_ANIMAL_MODEL_TO_NPC_ID = new HashMap<Integer, Integer>() {{
		put(10751, 369); // Big no horns
		put(11028, 370); //  370: thin dog with ball on tail
		put(11034, 5492); // 5492: Ram
		put(10750, 5489);
	}};

	private Integer balloonAnimalModelId;
	private final HashMap<NPC, Boolean> balloonAnimals = new HashMap<>();

	public Set<NPC> getValidBalloonAnimals() {
		return balloonAnimals.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
	}

	@Override
	public boolean isEnabled()
	{
		return client.getMapRegions() != null &&
			ArrayUtils.contains(client.getMapRegions(), PRISON_PETE_MAP_REGION);
	}


	/**
	 * TODO: Change this to use onWidgetLoaded
	 */
	private void setBalloonAnimalModelId() {
		Widget balloonAnimalWidget = client.getWidget(BALLOON_ANIMAL_WIDGET_GROUP_ID, BALLOON_ANIMAL_WIDGET_CHILD_ID);

		if (balloonAnimalWidget == null || balloonAnimalWidget.getModelId() == -1) {
			return;
		}

		this.balloonAnimalModelId = balloonAnimalWidget.getModelId();
	}

	private void setValidBalloonAnimals() {
		if (this.balloonAnimalModelId == null) return;

		Integer balloonAnimalNpcId = BALLOON_ANIMAL_MODEL_TO_NPC_ID.get(balloonAnimalModelId);
		if (balloonAnimalNpcId == null) return;

		balloonAnimals.replaceAll((npc, isCorrectBalloonAnimal) -> npc.getId() == balloonAnimalNpcId);
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled()) return;
		setBalloonAnimalModelId();
		setValidBalloonAnimals();
	}


	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!isEnabled())
		{
			return;
		}
		NPC npc = event.getNpc();
		if (BALLOON_ANIMAL_MODEL_TO_NPC_ID.containsValue(npc.getId()))
		{
			balloonAnimals.put(npc, false);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!isEnabled())
		{
			return;
		}
		NPC npc = event.getNpc();
		if (BALLOON_ANIMAL_MODEL_TO_NPC_ID.containsValue(npc.getId()))
		{
			balloonAnimals.remove(npc);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.CONNECTION_LOST || state == GameState.HOPPING
			|| state == GameState.LOGIN_SCREEN || state == GameState.UNKNOWN
			|| state == GameState.LOADING
		) {
			balloonAnimals.clear();
		}
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
}
