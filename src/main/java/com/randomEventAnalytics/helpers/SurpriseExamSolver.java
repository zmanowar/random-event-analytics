package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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


enum ExamItemType
{
	DRINK, FIREMAKING, FISHING, RANGED, MINING, MELEE, FARMING, OTHER, FRUIT, HAT, MAGIC, ARMOR, JEWELRY, SHIELD, NONE
}

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
		put(Pattern.compile("melee|warriors|hand-to-hand"), ExamItemType.MELEE);
		put(Pattern.compile("fish|sea food"), ExamItemType.FISHING); // TODO: Check "fish" doesn't appear in other patterns
	}};
	private static final HashMap<Integer, ExamItemType> WIDGET_MODEL_ID_TO_ITEM_TYPE = new HashMap<Integer,
		ExamItemType>()
	{{
		put(41154, ExamItemType.FIREMAKING); // Tinderbox
		put(41232, ExamItemType.FIREMAKING); // Logs
		put(41229, ExamItemType.FIREMAKING); // Lantern
		put(41184, ExamItemType.FIREMAKING); // Axe
		put(27102, ExamItemType.FIREMAKING); // Candlestick
		put(41147, ExamItemType.FISHING); // Shrimp
		put(41173, ExamItemType.FISHING); // Sardine?
		put(41158, ExamItemType.FISHING); // Harpoon
		put(41163, ExamItemType.FISHING); // Pike?
		put(41217, ExamItemType.FISHING); // Trout
		put(41193, ExamItemType.FISHING); // Bass?
		put(41209, ExamItemType.FISHING); // Tuna
		put(41206, ExamItemType.FISHING); // Pike?
		put(41166, ExamItemType.FISHING); // Shark
		put(41171, ExamItemType.RANGED); // Bow
		put(27097, ExamItemType.DRINK); // Gnome cocktail of some sort
		put(41170, ExamItemType.MINING); // Copper ore?
		put(27094, ExamItemType.MINING); // Platebody
		put(41153, ExamItemType.MINING); // Ingot
		put(41194, ExamItemType.MINING); // Pickaxe
		put(41183, ExamItemType.MINING); // Hammer
		put(41152, ExamItemType.DRINK); // Beer
		put(41150, ExamItemType.MELEE); // Sword
		put(41176, ExamItemType.MELEE); // Battle-axe
		put(41192, ExamItemType.MELEE); // Scimitar
		put(41200, ExamItemType.SHIELD); // Kite shield
		put(41169, ExamItemType.SHIELD); // Square shield
		put(41188, ExamItemType.SHIELD); // Square shield with sword on it?
		put(41221, ExamItemType.SHIELD); // Round shield
		put(41162, ExamItemType.DRINK); // Tea
		put(41198, ExamItemType.RANGED); // Bow 2
		put(41222, ExamItemType.FRUIT); // Banana
		put(41230, ExamItemType.FRUIT); // Strawberry
		put(41207, ExamItemType.FRUIT); // Redberry
		put(41214, ExamItemType.FRUIT); // Pineapple
		put(41177, ExamItemType.RANGED); // Arrows
		put(41146, ExamItemType.RANGED); // Crossbow
		put(41208, ExamItemType.FARMING); // Plant pot?
		put(41197, ExamItemType.FARMING); // Secateurs
		put(41212, ExamItemType.FARMING); // Rake
		put(41213, ExamItemType.FARMING); // Watering can
		put(41195, ExamItemType.HAT); // Mask
		put(41196, ExamItemType.HAT); // Jester hat
		put(41187, ExamItemType.HAT); // Pirate hat
		put(41164, ExamItemType.HAT); // Lederhosen hat
		put(41168, ExamItemType.MAGIC); // Air rune
		put(41231, ExamItemType.MAGIC); // Water rune
		put(41215, ExamItemType.MAGIC); // Fire rune
		put(41157, ExamItemType.MAGIC); // Earth rune
		put(41174, ExamItemType.MAGIC); // Magic staff
		put(41189, ExamItemType.ARMOR); // Med helm
		put(27091, ExamItemType.JEWELRY); // Ring
		put(41216, ExamItemType.JEWELRY); // Necklace
		put(41151, ExamItemType.JEWELRY); // Gemstone?
		put(41159, ExamItemType.JEWELRY); // Holy symbol
		put(41199, ExamItemType.OTHER); // Small knife?
		put(41161, ExamItemType.OTHER); // Cheese
		put(2674, ExamItemType.OTHER); // Bones
		put(41223, ExamItemType.OTHER); // Pot
		put(41182, ExamItemType.OTHER); // Pure essence? TODO: Check this, may be a rune?
		put(41175, ExamItemType.OTHER); // Dye?
		put(29232, ExamItemType.OTHER); // Key
		put(41226, ExamItemType.OTHER); // Garlic/Onion
		put(41220, ExamItemType.OTHER); // Ranger boots
		put(27104, ExamItemType.OTHER); // Insulated boots?
		put(41160, ExamItemType.OTHER); // Fighting boots
		put(41172, ExamItemType.OTHER); // Bread
		put(41202, ExamItemType.OTHER); // Cake
		put(41167, ExamItemType.OTHER); // Legends cape
		put(979, ExamItemType.OTHER); // (Cocktail) Shaker
		put(41181, ExamItemType.OTHER);
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
			ExamItemType itemType = getExamItemTypeFromWidget(widget);
			if (itemType == null) continue;

			if (itemType == examItemType)
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
		ExamItemType[] itemTypes = new ExamItemType[]{ExamItemType.NONE, ExamItemType.NONE, ExamItemType.NONE};
		for (int i = 0; i < NUM_WHAT_NEXT_HINT_ITEMS; i++)
		{
			Widget widget = client.getWidget(WHAT_NEXT_WIDGET_GROUP_ID, WHAT_NEXT_HINT_WIDGET_START_CHILD_ID + i);
			if (widget == null)
			{
				continue;
			}
			ExamItemType itemType = getExamItemTypeFromWidget(widget);
			if (itemType == null) continue;
			itemTypes[i] = itemType;
		}
		return Arrays.stream(itemTypes).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
	}

	private void setWhatNextAnswerWidget(ExamItemType examItemType) {
		/*
			This is a ridiculous hack, it seems like sometimes the widgets
			skip ids -- so we'll just loop over two times the amount and hope.
		 */
		for (int i = 0; i <= NUM_WHAT_NEXT_ANSWER_ITEMS * 2; i++) {
			Widget widget = client.getWidget(WHAT_NEXT_WIDGET_GROUP_ID, WHAT_NEXT_ANSWER_WIDGET_START_CHILD_ID + i);
			if (widget == null) continue;
			ExamItemType itemType = getExamItemTypeFromWidget(widget);
			if (itemType == null) continue;
			if (itemType == examItemType) {
				whatNextAnswerWidget = widget;
				return;
			}
		}
	}

	private ExamItemType getExamItemTypeFromWidget(Widget widget) {
		int modelId = widget.getModelId();
		if (modelId == -1) return null;

		ExamItemType itemType = WIDGET_MODEL_ID_TO_ITEM_TYPE.get(modelId);
		if (itemType == null) log.debug("{} not set in item type", modelId);

		return itemType;
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
}
