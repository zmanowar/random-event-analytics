package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CerterSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;

	private static final int CERTER_WIDGET_GROUP_ID = 26;
	private static final int CERTER_MODEL_WIDGET_CHILD_ID = 7;
	private static final int[] CERTER_ANSWER_WIDGET_CHILD_IDS = new int[]{1, 2, 3};
	private boolean certerWidgetLoaded = false;

	private static final HashMap<Integer, String> WIDGET_ID_TO_ANSWER = new HashMap<Integer, String>(){{
		put(8828, "battle axe"); // untested (battle axe)
		put(8829, "fish"); // Pike/salmon
		put(8830, "fish"); // untested (pike/salmon)
		put(8831, "necklace"); // untested (gold necklace)
		put(8832, "shield"); // untested (kite shield)
		put(8833, "helmet"); // untested (med helm)
		put(8834, "ring"); // untested (gemmed ring)
		put(8835, "secateurs"); // untested (secateurs)
		put(8836, "sword"); // untested (sword)
		put(8837, "spade"); // untested (spade)
	}};

	@Getter
	private Widget correctAnswerWidget;

	private void setCorrectAnswer() {
		Widget modelWidget = client.getWidget(CERTER_WIDGET_GROUP_ID, CERTER_MODEL_WIDGET_CHILD_ID);

		int modelId = modelWidget.getModelId();
		if (modelId == -1) {
			log.debug("Something went wrong finding widget ({}, {})", CERTER_WIDGET_GROUP_ID, CERTER_MODEL_WIDGET_CHILD_ID);
			return;
		}

		String answerString = WIDGET_ID_TO_ANSWER.get(modelId);
		if (answerString == null) {
			log.debug("{} not set in item model", modelId);
			return;
		}

		Widget answerWidget = findWidgetByText(answerString);
		if (answerWidget == null) {
			log.debug("Failed to find answer widget for {}", answerString);
		}

		correctAnswerWidget = answerWidget;
	}

	private Widget findWidgetByText(String text) {
		for (int childId : CERTER_ANSWER_WIDGET_CHILD_IDS) {
			Widget answerWidget = client.getWidget(CERTER_WIDGET_GROUP_ID, childId);
			if (answerWidget == null) {
				log.debug("Child id ({}) not set.", childId);
				continue;
			}
			if (answerWidget.getText().toLowerCase().contains(text.toLowerCase())) {
				return answerWidget;
			}
		}
		return null;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled() || !certerWidgetLoaded) return;
		setCorrectAnswer();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == CERTER_WIDGET_GROUP_ID)
		{
			certerWidgetLoaded = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event) {
		if (!isEnabled()) {
			return;
		}

		if (event.getGroupId() == CERTER_WIDGET_GROUP_ID) {
			certerWidgetLoaded = false;
		}
	}

	@Override
	public boolean isEnabled()
	{
		return certerWidgetLoaded;
	}

	@Override
	public void startUp()
	{
		eventBus.register(this);
	}

	@Override
	public void shutDown()
	{
		eventBus.register(this);
	}
}
