package com.randomEventAnalytics.helpers.mazeHelper;

import java.util.ArrayList;
import java.util.Set;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;

public class Maze
{
	private ArrayList<ArrayList<Node>> maze = new ArrayList<>();
	private WorldPoint start;
	private WorldPoint end;

	public void addNode(Tile tile, Set<MovementFlag> flags) {
		Point point = tile.getSceneLocation();
		int x = point.getX();
		if (maze.get(x) == null) maze.add(x, new ArrayList<>());

		int y = point.getY();
		maze.get(x).add(y, new Node(tile, flags));
	}

	private ArrayList<Node> calculatePath(Point start) {
		ArrayList<Node> path = new ArrayList<>();
		explore(start.getX(), start.getY(), path);
		return path;
	}

	private boolean explore(int x, int y, ArrayList<Node> path) {
		if (maze.get(x) == null || maze.get(x).get(y) == null) return false;
		Node node = maze.get(x).get(y);
//
//		if (node.isWall() || node.isVisited()) {
//			return false;
//		}

		if (node.isDoor()) {
			return true;
		}

		path.add(node);
//		node.setVisited(true);
		for (int dx = -1; dx < 1; dx++) {
			for (int dy = -1; dy < 1; dy++) {
				if (explore(x + dx, y + dy, path)) {
					return true;
				}
			}
		}
		path.remove(path.size() - 1);
		return false;
	}
}
