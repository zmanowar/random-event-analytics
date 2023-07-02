package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.helpers.examHelper.ExamItemModel;
import com.randomEventAnalytics.helpers.examHelper.ExamItemType;
import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.apache.commons.lang3.ArrayUtils;


/*
 	Note to self, you can still fail the random. To prolong the random for more item identifications
 	answer correctly then incorrectly.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SurpriseExamSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;

	private static final int EXAM_MAP_REGION = 7502;
	// This seems like a low group id, is this used elsewhere?
	private static final int WHAT_NEXT_WIDGET_GROUP_ID = 103;
	private static final int WHAT_NEXT_HINT_WIDGET_START_CHILD_ID = 10;
	private static final int NUM_WHAT_NEXT_HINT_ITEMS = 3;
	private static final int WHAT_NEXT_ANSWER_WIDGET_START_CHILD_ID = 15;
	private static final int NUM_WHAT_NEXT_ANSWER_ITEMS = 3;
	private static final int MATCH_THREE_WIDGET_GROUP_ID = 559;
	private static final int MATCH_THREE_HINT_WIDGET_CHILD_ID = 67;
	private static final int MATCH_THREE_ITEM_WIDGET_START_CHILD_ID = 21;
	private static final int NUM_MATCH_THREE_ITEMS = 5 * 3;
	private static final HashMap<Pattern, ExamItemType> MATCH_THREE_HINT_TO_ITEM_TYPE = new HashMap<Pattern,
		ExamItemType>()
	{{
		put(Pattern.compile("drink|thirsty|dehydrated"), ExamItemType.DRINK);
		put(Pattern.compile("burn|igniting|light my fire"), ExamItemType.FIREMAKING);
		put(Pattern.compile("shiny|accessories|bangle"), ExamItemType.JEWELRY);
		put(Pattern.compile("ranged|arrow|hate melee"), ExamItemType.RANGED);
		put(Pattern.compile("headgear|tip my hat|mess up your hairstyle"), ExamItemType.HAT);
		put(Pattern.compile("warriors|hand-to-hand"), ExamItemType.MELEE); // TODO: Fill this out, missing one
		put(Pattern.compile("fish|sea food"), ExamItemType.FISHING); // TODO: Check "fish" doesn't appear in other patterns
	}};

	@Getter
	private HashSet<Widget> matchThreeAnswerWidgets = new HashSet<>();
	@Getter
	private Widget whatNextAnswerWidget;

	private boolean matchThreeWidgetLoaded = false;
	private boolean whatNextWidgetLoaded = false;

	private ExamItemType getMatchThreeRequiredItemType()
	{
		client.loadModel(1);
		Widget widget = client.getWidget(MATCH_THREE_WIDGET_GROUP_ID, MATCH_THREE_HINT_WIDGET_CHILD_ID);
		if (widget == null)
		{
			return null;
		}

		String text = widget.getText().toLowerCase();
		return MATCH_THREE_HINT_TO_ITEM_TYPE.entrySet().stream().filter(entry -> entry.getKey().matcher(text).find()).map(Map.Entry::getValue).findFirst().orElse(null);
	}

	private void setMatchThreeAnswerWidgets(ExamItemType examItemType)
	{
		for (int i = 0; i <= NUM_MATCH_THREE_ITEMS; i++)
		{
			Widget widget = client.getWidget(MATCH_THREE_WIDGET_GROUP_ID, MATCH_THREE_ITEM_WIDGET_START_CHILD_ID + i);
			if (widget == null)
			{
				continue;
			}
			ExamItemModel itemModel = getExamItemModelFromWidget(widget);
			if (itemModel == null) continue;

			if (itemModel.isType(examItemType))
			{
				matchThreeAnswerWidgets.add(widget);
			}
		}
	}

	private ExamItemType getWhatNextRequiredItemType()
	{
		/*
			In case there are unexpected items, and we need to filter multiple item types,
			we'll add the item types to list and find the mode (most occurring item type)
		 */
		List<ExamItemType> itemTypes = new ArrayList<>();
		for (int i = 0; i < NUM_WHAT_NEXT_HINT_ITEMS; i++)
		{
			Widget widget = client.getWidget(WHAT_NEXT_WIDGET_GROUP_ID, WHAT_NEXT_HINT_WIDGET_START_CHILD_ID + i);
			if (widget == null)
			{
				continue;
			}
			ExamItemModel itemModel = getExamItemModelFromWidget(widget);
			if (itemModel == null) continue;
			itemTypes.addAll(itemModel.getExamItemTypes());
		}
		return itemTypes.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
	}

	private void setWhatNextAnswerWidget(ExamItemType examItemType) {
		/*
			This is a ridiculous hack, it seems like sometimes the widgets
			skip ids -- so we'll just loop over two times the amount and hope.
		 */
		for (int i = 0; i <= NUM_WHAT_NEXT_ANSWER_ITEMS * 2; i++) {
			Widget widget = client.getWidget(WHAT_NEXT_WIDGET_GROUP_ID, WHAT_NEXT_ANSWER_WIDGET_START_CHILD_ID + i);
			if (widget == null) continue;
			ExamItemModel itemModel = getExamItemModelFromWidget(widget);
			if (itemModel == null) continue;
			if (itemModel.isType(examItemType))
			{
				whatNextAnswerWidget = widget;
				return;
			}
		}
	}

	private ExamItemModel getExamItemModelFromWidget(Widget widget) {
		int modelId = widget.getModelId();
		if (modelId == -1) return null;

		ExamItemModel itemModel = ExamItemModel.getExamItemModel(modelId);
		if (itemModel == null) log.debug("{} not set in item model", modelId);

		return itemModel;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!isEnabled())
		{
			return;
		}

		if (matchThreeWidgetLoaded)
		{
			ExamItemType examItemType = getMatchThreeRequiredItemType();
			if (examItemType != null)
			{
				setMatchThreeAnswerWidgets(examItemType);
			}
		}
		else if (whatNextWidgetLoaded)
		{
			ExamItemType examItemType = getWhatNextRequiredItemType();
			if (examItemType != null) {
				setWhatNextAnswerWidget(examItemType);
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (!isEnabled())
		{
			return;
		}
		if (event.getGroupId() == MATCH_THREE_WIDGET_GROUP_ID)
		{
			matchThreeWidgetLoaded = true;
		}
		else if (event.getGroupId() == WHAT_NEXT_WIDGET_GROUP_ID)
		{
			whatNextWidgetLoaded = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (!isEnabled())
		{
			return;
		}
		if (event.getGroupId() == MATCH_THREE_WIDGET_GROUP_ID)
		{
			matchThreeWidgetLoaded = false;
			matchThreeAnswerWidgets.clear();
		}
		else if (event.getGroupId() == WHAT_NEXT_WIDGET_GROUP_ID)
		{
			whatNextWidgetLoaded = false;
			whatNextAnswerWidget = null;
		}
	}

	@Override
	public boolean isEnabled()
	{
		return client.getMapRegions() != null && ArrayUtils.contains(client.getMapRegions(), EXAM_MAP_REGION);
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
	 * 8836: Sword (longsword?)
	 * 8837: Spade
	 */
}
