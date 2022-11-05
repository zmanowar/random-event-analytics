package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.Arrays;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;


// TODO: Make this also work when the beehive parts have been assigned incorrectly.
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BeekeeperSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;
	private static final int BEEKEEPER_MAP_REGION = 7758;
	private static final int BEEHIVE_WIDGET_GROUP_ID = 420;
	private static final int BEEHIVE_PARTS_WIDGET_CHILD_START_ID = 10;
	private static final int BEEHIVE_LABELS_WIDGET_CHILD_START_ID = 14;
	private static final int NUM_BEEHIVE_PARTS = 4;

	private static final HashMap<Integer, BeehivePosition> BEEHIVE_MODEL_ID_TO_POSITION = new HashMap<Integer, BeehivePosition>() {{
		put(28806, BeehivePosition.LID);
		put(28428, BeehivePosition.BODY);
		put(28803, BeehivePosition.ENTRANCE);
		put(28808, BeehivePosition.LEGS);
	}};

	private boolean beehiveWidgetLoaded = false;
	private HashMap<BeehivePosition, Widget> labelWidgets = new HashMap<>();
	private HashMap<BeehivePosition, Widget> partWidgets = new HashMap<>();
	@Getter
	private Widget[] answerWidgets = new Widget[2];

	public void setAnswerWidgets() {
		if (labelWidgets.size() == 0 || partWidgets.size() == 0) return;
		for (BeehivePosition position : BeehivePosition.values()) {
			Widget partWidget = partWidgets.get(position);
			Widget labelWidget = labelWidgets.get(position);
			if (partWidget == null || labelWidget == null) continue;
			answerWidgets[0] = partWidget;
			answerWidgets[1] = labelWidget;
		}
	}

	private void getBeehiveLabelWidgets() {
		for (int i = 0; i < NUM_BEEHIVE_PARTS * 2; i++) {
			Widget beehiveLabelWidget = client.getWidget(BEEHIVE_WIDGET_GROUP_ID, BEEHIVE_LABELS_WIDGET_CHILD_START_ID + i);
			if (beehiveLabelWidget == null || beehiveLabelWidget.getText() == null) continue;

			String label = beehiveLabelWidget.getText().toUpperCase();
			BeehivePosition labelPosition = Arrays.stream(BeehivePosition.values()).filter(position -> position.toString().equalsIgnoreCase(label)).findFirst().orElse(null);
			if (labelPosition == null) continue;
			labelWidgets.put(labelPosition, beehiveLabelWidget);
		}
	}

	private void getBeehivePartWidgets() {
		partWidgets.clear();
		boolean hasSetParts = false;
		for (int i = 0; i < NUM_BEEHIVE_PARTS; i++) {
			Widget beehivePart = client.getWidget(BEEHIVE_WIDGET_GROUP_ID, BEEHIVE_PARTS_WIDGET_CHILD_START_ID + i);
			if (beehivePart == null || !BEEHIVE_MODEL_ID_TO_POSITION.containsKey(beehivePart.getModelId())) continue;
			partWidgets.put(BEEHIVE_MODEL_ID_TO_POSITION.get(beehivePart.getModelId()), beehivePart);
			hasSetParts = true;
		}
		if (!hasSetParts) {
			Arrays.fill(answerWidgets, null);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled() || !beehiveWidgetLoaded) return;
		getBeehiveLabelWidgets();
		getBeehivePartWidgets();
		setAnswerWidgets();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (!isEnabled())
		{
			return;
		}
		if (event.getGroupId() == BEEHIVE_WIDGET_GROUP_ID)
		{
			beehiveWidgetLoaded = true;
		}
	}

	@Override
	public boolean isEnabled() {
		return client.isInInstancedRegion() && client.getMapRegions() != null &&
			client.getMapRegions()[0] == BEEKEEPER_MAP_REGION;
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

	enum BeehivePosition
	{
		LID, BODY, ENTRANCE, LEGS
	}
}
