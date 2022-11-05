package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class QuizMasterSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;
	private static final Integer QUIZ_MASTER_MAP_REGION = 7754;
	private static final Integer QUIZ_ITEM_WIDGET_GROUP_ID = 191;
	private static final Integer QUIZ_ITEM_WIDGET_CHILD_ID = 1;

	/**
	 * 8830: Tuna Fish
	 * 8831: Necklace
	 * 8834: Gem Ring
	 * 8837: Spade
	 * 8835: Shears
	 * 8832: Shield
	 */

	private boolean widgetLoaded = false;
	private static final HashMap<Integer, String> MODEL_ID_CLASSIFICATION = new HashMap<Integer, String>() {{
		put(8830, "fish"); // tuna
		put(8829, "fish"); // salmon/pike
		put(8831, "jewelry"); // necklace
		put(8834, "jewelry"); // gem ring
		put(8837, "farming"); // spade
		put(8835, "farming"); // shears
		put(8832, "armor"); // shield
		put(8833, "armor"); // helmet
		put(8836, "combat"); // sword
		put(8828, "combat"); // battleaxe
	}};

	@Getter
	private Widget oddWidget;

	private Widget[] getQuizItemWidgets() {
		if (!widgetLoaded) return new Widget[0];
		Widget parentWidget = client.getWidget(QUIZ_ITEM_WIDGET_GROUP_ID, QUIZ_ITEM_WIDGET_CHILD_ID);

		if (parentWidget == null || parentWidget.getChildren() == null) return new Widget[0];

		return parentWidget.getChildren();
	}

	private void setOddWidgetFromWidgets(Widget[] widgets) {
		String oddType = Arrays.stream(widgets)
			.collect(
				Collectors.groupingBy(widget -> {
						String classification = MODEL_ID_CLASSIFICATION.get(widget.getModelId());
						return classification != null ? classification : "";
					},
					Collectors.counting()
				)
			).entrySet().stream().min(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey).orElse(null);

		if (oddType == null) return;

		Widget oddWidget = Arrays.stream(widgets).filter(widget -> MODEL_ID_CLASSIFICATION.get(widget.getModelId()).equals(oddType)).findFirst().orElse(null);
		if (oddWidget != null)
		{
			this.oddWidget = oddWidget;
		}

	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if(!isEnabled()) return;
		if (event.getGroupId() != QUIZ_ITEM_WIDGET_GROUP_ID)
		{
			return;
		}

		widgetLoaded = true;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled()) return;

		Widget[] widgets = getQuizItemWidgets();
		if (widgets.length == 0) {
			this.oddWidget = null;
			return;
		}

		setOddWidgetFromWidgets(widgets);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.CONNECTION_LOST || state == GameState.HOPPING
			|| state == GameState.LOGIN_SCREEN || state == GameState.UNKNOWN
			|| state == GameState.LOADING
		) {
			oddWidget = null;
		}
	}

	@Override
	public boolean isEnabled()
	{
		return client.getMapRegions() != null &&
			ArrayUtils.contains(client.getMapRegions(), QUIZ_MASTER_MAP_REGION);
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
