package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.NpcID;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetModelType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DrillDemonSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;
	private static final Integer DRILL_DEMON_MAP_REGION = 12619;
	private static final HashMap<Integer, String> VARBIT_VALUE_TO_EXERCISE_NAME = new HashMap<Integer, String>() {{
		put(1, "jog");
		put(2, "sit up");
		put(3, "push up");
		put(4, "star jump");
	}};

	private final HashSet<Exercise> exercises = new HashSet<Exercise>() {{
		// Order of exercises (varbitIds, mats) from left to right.
		add(new Exercise(1335, 20810));
		add(new Exercise(1336, 16508));
		add(new Exercise(1337, 9313));
		add(new Exercise(1338, 20801));
	}};

	private Exercise currentExercise;
	@Getter
	private TileObject currentTileObject;

	@Override
	public boolean isEnabled() {
		return ArrayUtils.contains(client.getMapRegions(), DRILL_DEMON_MAP_REGION);
	}

	private Exercise getExerciseByVarbit(int varbitId) {
		return exercises.stream().filter(exercise -> exercise.getVarbitId() == varbitId).findFirst().orElse(null);
	}

	private Exercise getExerciseByName(String name) {
		return exercises.stream().filter(exercise -> Objects.equals(exercise.getExerciseName(), name)).findFirst().orElse(null);
	}

	private String getSergeantText() {
		Widget npcDialog = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
		if (npcDialog == null)
		{
			return null;
		}

		Widget name = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
		Widget head = client.getWidget(WidgetInfo.DIALOG_NPC_HEAD_MODEL);
		if (name == null || head == null || head.getModelType() != WidgetModelType.NPC_CHATHEAD)
		{
			return null;
		}

		final int npcId = head.getModelId();
		if (npcId != NpcID.SERGEANT_DAMIEN && npcId != NpcID.SERGEANT_DAMIEN_6743)
		{
			return null;
		}

		return Text.sanitizeMultilineText(npcDialog.getText());
	}

	private TileObject getExerciseTileObject(Exercise exercise) {
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

				if (tile.getGroundObject() == null) {
					continue;
				}

				if (tile.getGroundObject().getId() == exercise.getMatGroundObjectId()) {
					return tile.getGroundObject();
				}
			}
		}
		return null;
	}

	private void setCurrentExercise(String exerciseName) {
		if (exerciseName == null) return;
		currentExercise = getExerciseByName(exerciseName);
		currentTileObject = getExerciseTileObject(currentExercise);
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled()) return;

		String sergeantText = getSergeantText();
		if (sergeantText == null) return;

		for(String val : VARBIT_VALUE_TO_EXERCISE_NAME.values()) {
			if (sergeantText.contains(val)) {
				setCurrentExercise(val);
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		if (!isEnabled()) return;

		String exerciseName = VARBIT_VALUE_TO_EXERCISE_NAME.get(event.getValue());
		if (exerciseName == null) return;

		Exercise exercise = getExerciseByVarbit(event.getVarbitId());
		if (exercise == null) return;


		exercise.setExerciseName(exerciseName);
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

	public class Exercise {
		@Getter
		private Integer varbitId;

		@Getter
		@Setter
		private String exerciseName;

		@Getter
		@Setter
		private Integer matGroundObjectId;

		Exercise(Integer varbitId, Integer matGroundObjectId) {
			this.varbitId = varbitId;
			this.matGroundObjectId = matGroundObjectId;
		}
	}
}
