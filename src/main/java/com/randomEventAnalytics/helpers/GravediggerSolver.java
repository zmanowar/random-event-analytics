package com.randomEventAnalytics.helpers;


/**
 * Gravestones:
 * 9362: Farming
 * 9360: Woodcutting
 * 9359: Pottery
 * 9361: Mining
 * 9363: Cooking
 */

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;


/**
 * TODO: Varbits are most likely used for setting these, but they're set when you enter the
 *  instance for the first time. Check DrillDemonSolver as those are reloaded every time you
 *  login.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GravediggerSolver implements SolverModuleComponent
{
	private static Integer GRAVEDIGGER_MAP_REGION_ID = 7758;
	private static Integer GRAVEDIGGER_WIDGET_GROUP_ID = 175;
	private static Integer GRAVEDIGGER_WIDGET_CHILD_ID = 1;
	private static HashMap<Integer, String> HEADSTONE_WIDGET_ID_TO_NAME = new HashMap<Integer, String>()
	{{
		put(13402, "Farmer");
		put(13404, "Cook");
		put(16035, "Miner");
		put(13403, "Potter");
		put(13399, "Lumberjack");
	}};
	private static HashMap<Integer, String> COFFIN_ITEM_ID_TO_NAME = new HashMap<Integer, String>()
	{{
		put(7587, "Potter");
		put(7588, "Miner");
		put(7589, "Cook");
		put(7590, "Farmer");
		put(7591, "Lumberjack");
	}};
	private static HashSet<Integer> HEADSTONE_IDS = new HashSet<Integer>()
	{{
		add(9360);
		add(9362);
		add(9359);
		add(9361);
		add(9363);
	}};
	private final Client client;
	private final EventBus eventBus;
	private boolean widgetLoaded = false;

	private HashSet<Headstone> headstones = new HashSet<>();

	private HashSet<Coffin> coffins;
	private Headstone selectedHeadstone;

	public HashSet<Headstone> getHeadstones()
	{
		return headstones.stream().filter(headstone -> headstone.name != null).collect(Collectors.toCollection(HashSet::new));
	}

	public Coffin findCoffin(Integer itemId)
	{
		if (coffins == null)
		{
			return null;
		}
		return coffins.stream().filter(coffin -> coffin.getItem().getId() == itemId).findFirst().orElse(null);
	}

	@Override
	public boolean isEnabled()
	{
		return client.isInInstancedRegion() && client.getMapRegions() != null && client.getMapRegions()[0] == GRAVEDIGGER_MAP_REGION_ID;
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

	private boolean isCloseToHeadstone()
	{
		/*
			Due to the janky way we have to hook into the headstone interaction (ie. onMenuOptionClicked;
			there is no InteractingChanged event for the headstones) we'll filter out if the player is
			not next to the headstone to prevent incorrectly naming the gravestone.
			Better to not name a gravestone than to name it incorrectly.
		 */
		if (selectedHeadstone == null) return false;
		LocalPoint playerLoc = client.getLocalPlayer().getLocalLocation();
		LocalPoint headstoneLoc = selectedHeadstone.getGameObject().getLocalLocation();
		return headstoneLoc.getSceneX() + 1 >= playerLoc.getSceneX()
			&& headstoneLoc.getSceneX() - 1 <= playerLoc.getSceneX()
			&& headstoneLoc.getSceneY() + 1 >= playerLoc.getSceneY()
			&& headstoneLoc.getSceneY() - 1 <= playerLoc.getSceneY();
	}

	private void setInventoryItems(Item[] items)
	{
		coffins =
			Arrays.stream(items).filter(item -> COFFIN_ITEM_ID_TO_NAME.containsKey(item.getId())).map(item -> new Coffin(item, COFFIN_ITEM_ID_TO_NAME.get(item.getId()))).collect(Collectors.toCollection(HashSet::new));
	}

	private void setHeadstoneNameByWidget()
	{
		if (!widgetLoaded)
		{
			return;
		}
		if (!isCloseToHeadstone()) return;
		Widget gravestoneWidget = client.getWidget(GRAVEDIGGER_WIDGET_GROUP_ID, GRAVEDIGGER_WIDGET_CHILD_ID);
		if (gravestoneWidget == null || gravestoneWidget.getModelId() == -1)
		{
			return;
		}

		selectedHeadstone.setName(HEADSTONE_WIDGET_ID_TO_NAME.get(gravestoneWidget.getModelId()));
	}

	private boolean shouldRefreshCoffinItems()
	{
		return coffins == null || coffins.size() < COFFIN_ITEM_ID_TO_NAME.size() || coffins.stream().anyMatch(coffin -> coffin.getName() == null || coffin.getName().equals(""));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!isEnabled())
		{
			return;
		}

		if (shouldRefreshCoffinItems())
		{
			setInventoryItems(client.getItemContainer(InventoryID.INVENTORY).getItems());
		}

		if (!widgetLoaded)
		{
			return;
		}
		setHeadstoneNameByWidget();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != GRAVEDIGGER_WIDGET_GROUP_ID)
		{
			return;
		}

		widgetLoaded = true;
	}


	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!isEnabled())
		{
			return;
		}

		selectedHeadstone =
			headstones.stream().filter(headstone -> headstone.gameObject.getId() == event.getId()).findFirst().orElse(null);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (!isEnabled())
		{
			return;
		}

		ItemContainer container = event.getItemContainer();
		if (container != client.getItemContainer(InventoryID.INVENTORY))
		{
			return;
		}

		setInventoryItems(container.getItems());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject gameObject = event.getGameObject();
		if (!HEADSTONE_IDS.contains(gameObject.getId()))
		{
			return;
		}

		headstones.add(new Headstone(gameObject));
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject gameObject = event.getGameObject();
		if (!HEADSTONE_IDS.contains(gameObject.getId()))
		{
			return;
		}

		headstones.removeIf(headstone -> headstone.gameObject == gameObject);
	}

	public class Headstone
	{
		@Getter
		private GameObject gameObject;
		@Getter
		@Setter
		private String name;

		Headstone(GameObject gameObject)
		{
			this.gameObject = gameObject;
		}
	}

	public class Coffin
	{
		@Getter
		private Item item;

		@Getter
		private String name;

		Coffin(Item item, String name)
		{
			this.item = item;
			this.name = name;
		}
	}
}
