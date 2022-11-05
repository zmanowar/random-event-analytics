package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EvilBobSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;

	private static Integer EVIL_BOB_MAP_REGION = 10058;

	/*
	 * TODO: The correct fish Id is 6202. When fishing from an incorrect
	 *  fishing spot you receive a 6206 fish. May want to highlight the
	 *  servant if the wrong fish is in the inventory? I don't feel like
	 *  adding more state at the moment.
	 */
	public static HashSet<Integer> CAT_STATUE_GAME_OBJECT_IDS = new HashSet<>(Arrays.asList(23118, 23121, 23119, 23120));

	private HashSet<GameObject> catStatues = new HashSet<>();

	@Getter
	private GameObject correctCatStatue;


	public boolean isEnabled() {
		return client.getMapRegions() != null
			&& ArrayUtils.contains(client.getMapRegions(), EVIL_BOB_MAP_REGION);
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (isPlayerInCanvas()) return;
		for(GameObject catStatue : catStatues) {
			if (isGameObjectInCanvas(catStatue)) {
				correctCatStatue = catStatue;
			}
		}
	}

	private boolean isPlayerInCanvas() {
		Polygon playerCanvasLocation = client.getLocalPlayer().getCanvasTilePoly();
		if (playerCanvasLocation == null || playerCanvasLocation.getBounds() == null) return false;

		Rectangle bounds = playerCanvasLocation.getBounds();
		return bounds.getX() >= 0 && bounds.getY() >= 0 && bounds.getX() <= client.getCanvasWidth() && bounds.getY() <= client.getCanvasHeight();
	}

	private boolean isGameObjectInCanvas(GameObject gameObject) {
		if (gameObject == null || gameObject.getCanvasLocation() == null) return false;

		Point bounds = gameObject.getCanvasLocation();
		return bounds.getX() >= 0 && bounds.getY() >= 0 && bounds.getX() <= client.getCanvasWidth() && bounds.getY() <= client.getCanvasHeight();
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

	private boolean isCatStatueObject(GameObject gameObject) {
		return CAT_STATUE_GAME_OBJECT_IDS.contains(gameObject.getId());
	}

	private void addGameObject(GameObject gameObject) {
		if (isCatStatueObject(gameObject)) {
			catStatues.add(gameObject);
		}
	}

	private void removeGameObject(GameObject gameObject) {
		if (isCatStatueObject(gameObject)) {
			catStatues.remove(gameObject);
		}
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
