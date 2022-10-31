package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.Arrays;
import java.util.HashSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PinballSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;
	public static Integer PINBALL_SWIRL_ANIMATION_ID = 4005;
	public static Integer PINBALL_MAP_REGION = 7758;
	public static HashSet<Integer> PINBALL_GAME_OBJECT_IDS = new HashSet<>(Arrays.asList(8982, 8984, 9079, 9081, 9258));

	public HashSet<GameObject> pinballObjects = new HashSet<>();

	public GameObject getActivePinballGameObject() {
		return pinballObjects.stream().filter(gameObject -> isPlayingPinballAnimation(gameObject)).findFirst().orElse(null);
	}

	@Override
	public boolean isEnabled() {
		return client.isInInstancedRegion() && client.getMapRegions() != null
			&&  client.getMapRegions()[0] == PINBALL_MAP_REGION;
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

	private void addGameObject(GameObject gameObject) {
		if (isEnabled() && isPinballObject(gameObject)) {
			pinballObjects.add(gameObject);
		}
	}

	private void removeGameObject(GameObject gameObject) {
		if (isPinballObject(gameObject)) {
			pinballObjects.remove(gameObject);
		}
	}

	private boolean isPinballObject(GameObject gameObject) {
		return PINBALL_GAME_OBJECT_IDS.contains(gameObject.getId());
	}

	private boolean isPlayingPinballAnimation(GameObject gameObject) {
		return ((DynamicObject) gameObject.getRenderable()).getAnimation().getId() == PINBALL_SWIRL_ANIMATION_ID;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		if(!isEnabled()) return;
		addGameObject(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		if(!isEnabled()) return;
		removeGameObject(event.getGameObject());
	}
}
