package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModelType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SandwichLadySolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;

	private static final Pattern FOOD_ITEM_PATTERN = Pattern.compile("Have a (?<foodItem>[\\w\\s]+) for free!");
	private static final int SANDWICH_LADY_WIDGET_GROUP_ID = 297;
	private static final int SANDWICH_LADY_REQUEST_WIDGET_CHILD_ID = 2;
	private static final int FOOD_ITEM_WIDGET_START_CHILD_ID = 5;
	private static final int NUMBER_OF_FOOD_ITEMS = 7;
	/**
	 * Widget model ids?
	 * 10731: Square Sandwich
	 * 10732: Triangle sandwich
	 * 10729: Kebab
	 * 10726: Baguette
	 * 10727: Bread roll
	 * 10728: Chocolate bar
	 * 10730: Meat Pie
	 *
	 */
	private static final HashMap<String, Integer> FOOD_ITEM_TO_WIDGET_MODEL = new HashMap<String, Integer>(){{
		put("baguette", 10726);
		put("bread roll", 10727);
		put("chocolate bar", 10728);
		put("kebab", 10729);
		put("pie", 10730);
		put("square sandwich", 10731);
		put("triangle sandwich", 10732);
	}};
	private boolean sandwichLadySpawned = false;

	@Getter
	private Widget foodItemWidget;

	private boolean isSandwichLady(NPC npc) {
		return npc.getId() == NpcID.SANDWICH_LADY;
	}
	
	private Integer getCorrectFoodItemModelId() {
		Widget foodItemWidget = client.getWidget(SANDWICH_LADY_WIDGET_GROUP_ID, SANDWICH_LADY_REQUEST_WIDGET_CHILD_ID);
		if (foodItemWidget == null || foodItemWidget.getText().equals("")) {
			return null;
		}

		Matcher matcher = FOOD_ITEM_PATTERN.matcher(foodItemWidget.getText());
		if (!matcher.find()) {
			return null;
		}

		String foodItem = matcher.group("foodItem");
		if (foodItem == null || foodItem.equals("")) return null;
		
		return FOOD_ITEM_TO_WIDGET_MODEL.get(foodItem.toLowerCase());
	}

	private Widget getCorrectWidget(Integer foodItemModelId) {
		Widget baseWidget = client.getWidget(SANDWICH_LADY_WIDGET_GROUP_ID, FOOD_ITEM_WIDGET_START_CHILD_ID);
		if (baseWidget == null) return null;

		for (int i = 0; i < NUMBER_OF_FOOD_ITEMS; i++) {
			Widget widget = client.getWidget(SANDWICH_LADY_WIDGET_GROUP_ID, FOOD_ITEM_WIDGET_START_CHILD_ID + i);
			if (widget == null) continue;
			if (widget.getModelType() == WidgetModelType.MODEL && widget.getModelId() == foodItemModelId) {
				return widget;
			}
		}
		return null;
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

	/**
	 * Iterate over 297.5 children, check:
	 * 		childWidget.getModelType() == 1 &&
	 * 			childWidget.getModelId() == correctFoodItemModel;
	 * Widget S297.2 contains the text:
	 * Have a (?<foodItem>[\w\s]+) for free!
	 * Widget S297.5 contains the models the player
	 * is intended to click
	 */
	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled() || !sandwichLadySpawned) return;

		Integer foodItemModelId = getCorrectFoodItemModelId();
		if (foodItemModelId == null) return;

		foodItemWidget = getCorrectWidget(foodItemModelId);


	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event) {
		if (!isEnabled()) return;
		if (isSandwichLady(event.getNpc())) {
			sandwichLadySpawned = true;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event) {
		if (!isEnabled()) return;
		if (isSandwichLady(event.getNpc())) {
			sandwichLadySpawned = false;
			foodItemWidget = null;
		}
	}
}
