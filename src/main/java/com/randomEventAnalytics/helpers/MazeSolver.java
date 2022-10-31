package com.randomEventAnalytics.helpers;


import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.HashSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.ArrayUtils;


/**
 * Not sure how far I want to implement this. DFS/BFS would work fine
 * just mapping the walls and doors might be a pain. There are existing
 * path finding runelite plugins, could possible leverage one?
 * TODO: Mapping client.getCollisionMaps to tiles and determining if the player
 *  is in the correct orientation.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MazeSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;
	private static final Integer MAZE_MAP_REGION = 11591;
	private static final HashSet<Integer> MAZE_DOOR_WALL_OBJECT_IDS = new HashSet<Integer>() {{
		add(14980);
		add(14981);
		add(14982);
		add(14983);
		// Presumably 14974-14978 are also doors or maze related wall objects?
		add(14979);
	}};

	//14985 is the goal

	@Getter
	private HashSet<WallObject> doors;

	@Override
	public boolean isEnabled() {
		return client.isInInstancedRegion() && ArrayUtils.contains(client.getMapRegions(), MAZE_MAP_REGION);
	}

	private void setDoors() {
		HashSet<WallObject> wallObjects = new HashSet<>();
		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();

		int z = client.getPlane();
		for (int x = 0; x < Constants.SCENE_SIZE; ++x)
		{
			for (int y = 0; y < Constants.SCENE_SIZE; ++y)
			{
				Tile tile = tiles[z][x][y];

				if (tile == null)
				{
					continue;
				}

				Player player = client.getLocalPlayer();
				if (player == null)
				{
					continue;
				}
				WallObject wallObject = tile.getWallObject();
				if (wallObject != null && MAZE_DOOR_WALL_OBJECT_IDS.contains(wallObject.getId())) {
					wallObjects.add(wallObject);
				}
			}
		}
		this.doors = wallObjects;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled()) return;
		setDoors();
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
