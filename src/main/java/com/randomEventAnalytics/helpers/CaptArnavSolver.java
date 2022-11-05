package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
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

/*
	Could use varbits instead.
	9585 is left
	9593 is middle
	9594 is right

	3 is ring
	2 is bar
	1 is bowl
	0 is coins
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CaptArnavSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;
	private static final int ARNAV_WIDGET_GROUP_ID = 26;
	private static final int COLUMN_REQUEST_WIDGET_CHILD_START_ID = 22;
	private static final int TREASURE_ITEMS_WIDGET_CHILD_START_ID = 13;
	private static final int NUM_COLUMNS = 3;
	private static final HashMap<Integer, TreasureItemType> MODEL_ID_TO_TREASURE_TYPE = new HashMap<Integer, TreasureItemType>() {{
		put(7753, TreasureItemType.RING);
		put(7751, TreasureItemType.BAR);
		put(7752, TreasureItemType.COINS);
		put(20104, TreasureItemType.BOWL);
	}};

	private TreasureChestColumn[] columns = new TreasureChestColumn[]{
		new TreasureChestColumn(),
		new TreasureChestColumn(),
		new TreasureChestColumn()
	};
	private boolean arnavWidgetLoaded = false;

	@Getter
	private HashSet<Widget> correctTreasureWidgets = new HashSet<>();

	private TreasureItemType getTreasureItemTypeFromWidgetIds(int groupId, int childId) {
		Widget widget = client.getWidget(groupId, childId);
		return getTreasureItemTypeFromWidget(widget);
	}

	private TreasureItemType getTreasureItemTypeFromWidget(Widget widget) {
		if (widget == null) return null;

		int modelId = widget.getModelId();
		if (modelId == -1) {
			return TreasureItemType.valueOf(widget.getText());
		}
		return MODEL_ID_TO_TREASURE_TYPE.get(modelId);
	}

	private void setCorrectTreasureWidgets() {
		for (int columnIndex = 0; columnIndex < NUM_COLUMNS; columnIndex++) {
			columns[columnIndex].requiredType = getTreasureItemTypeFromWidgetIds(ARNAV_WIDGET_GROUP_ID, COLUMN_REQUEST_WIDGET_CHILD_START_ID + columnIndex);
			for (int i = 1; i < NUM_COLUMNS; i++) {
				Widget cellWidget = client.getWidget(ARNAV_WIDGET_GROUP_ID, TREASURE_ITEMS_WIDGET_CHILD_START_ID + columnIndex + (3 * i) );
				if (cellWidget == null || cellWidget.getModelId() == -1) continue;
				TreasureItemType cellItemType = getTreasureItemTypeFromWidget(cellWidget);
				if (columns[columnIndex].requiredType == cellItemType) {
					columns[columnIndex].correctWidget = cellWidget;
				}
			}
		}
		correctTreasureWidgets = Arrays.stream(columns).map(column -> column.correctWidget).collect(Collectors.toCollection(HashSet::new));
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled() || !arnavWidgetLoaded) return;
		setCorrectTreasureWidgets();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (!isEnabled())
		{
			return;
		}
		if (event.getGroupId() == ARNAV_WIDGET_GROUP_ID)
		{
			arnavWidgetLoaded = true;
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

	enum TreasureItemType
	{
		BOWL,
		COINS,
		RING,
		BAR
	}

	class TreasureChestColumn {
		int currentVarbit = -1;
		TreasureItemType requiredType;
		Widget correctWidget;
	}
}
