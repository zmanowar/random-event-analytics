package com.randomEventAnalytics.helpers.mazeHelper;

import java.util.Set;
import lombok.Getter;
import net.runelite.api.Tile;

public class Node
{
	private static final Integer MAZE_DOOR_WALL_OBJECT_ID = 14979;
	private static final Integer MAZE_WALL_WALL_OBJECT_ID = 14977;

	@Getter
	private Tile tile;
	private Set<MovementFlag> flags;

	public boolean isWall() {
		return tile.getWallObject() != null && tile.getWallObject().getId() == MAZE_WALL_WALL_OBJECT_ID;
	}

	public boolean isDoor() {
		return tile.getWallObject() != null && tile.getWallObject().getId() == MAZE_DOOR_WALL_OBJECT_ID;
	}

	public Node(Tile tile, Set<MovementFlag> flags) {
		this.tile = tile;
		this.flags = flags;
	}

}