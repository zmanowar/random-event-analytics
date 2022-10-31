package com.randomEventAnalytics;

import com.google.inject.Provides;
import com.randomEventAnalytics.localstorage.NpcInfoRecord;
import com.randomEventAnalytics.localstorage.PlayerInfoRecord;
import com.randomEventAnalytics.localstorage.RandomEventAnalyticsLocalStorage;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import com.randomEventAnalytics.localstorage.XpInfoRecord;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.xptracker.XpTrackerPlugin;
import net.runelite.client.plugins.xptracker.XpTrackerService;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(name = "Random Event Analytics")
@PluginDependency(XpTrackerPlugin.class)
public class RandomEventAnalyticsPlugin extends Plugin
{
	private static final int RANDOM_EVENT_TIMEOUT = 150;
	private static final int STRANGE_PLANT_SPAWN_RADIUS = 1;
	private final String PLANT_SPAWNED_NOTIFICATION_MESSAGE =
		"A Strange Plant has spawned, please visit the Random " + "Event Analytics panel to confirm the random.";
	@Inject
	private ConfigManager configManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private RandomEventAnalyticsOverlay overlay;
	@Inject
	private Client client;
	@Inject
	private RandomEventAnalyticsConfig config;
	@Inject
	private RandomEventAnalyticsLocalStorage localStorage;
	@Inject
	private RandomEventAnalyticsTimeTracking timeTracking;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private Notifier notifier;
	@Inject
	private XpTrackerService xpTrackerService;

	private RandomEventAnalyticsPanel panel;
	private String profile;
	private int lastNotificationTick = -RANDOM_EVENT_TIMEOUT;
	private NavigationButton navButton;

	@Provides
	RandomEventAnalyticsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RandomEventAnalyticsConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		panel = injector.getInstance(RandomEventAnalyticsPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "random_event_analytics.png");

		navButton =
			NavigationButton.builder().tooltip("Random Event Analytics").icon(icon).panel(panel).priority(7).build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		updateConfig();
		lastNotificationTick = 0;
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(RandomEventAnalyticsConfig.CONFIG_GROUP))
		{
			return;
		}

		panel.updateConfig();
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		updateConfig();
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			final long hash = client.getAccountHash();
			if (String.valueOf(hash).equalsIgnoreCase(localStorage.getUsername()))
			{
				return;
			}

			String username = client.getUsername();
			if (username != null && username.length() > 0 && hash != -1)
			{
				localStorage.renameUsernameFolderToAccountHash(username, hash);
			}

			if (localStorage.setPlayerUsername(String.valueOf(hash)))
			{
				profile = configManager.getRSProfileKey();
				timeTracking.init(getIntFromProfileConfig(RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM),
					getIntFromProfileConfig(RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE),
					getIntFromProfileConfig(RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM));
				loadPreviousRandomEvents();
			}
		}
		else if (state == GameState.CONNECTION_LOST || state == GameState.HOPPING || state == GameState.LOGIN_SCREEN || state == GameState.UNKNOWN || state == GameState.LOADING)
		{
			updateConfig();
		}
	}

	private int getIntFromProfileConfig(String key)
	{
		try
		{
			return configManager.getConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, profile, key, int.class);
		}
		catch (NullPointerException e)
		{
			log.debug("No config loaded for: {}", profile);
			return 0;
		}
	}

	private synchronized void loadPreviousRandomEvents()
	{
		panel.clearAllRandomsView();
		ArrayList<RandomEventRecord> randomEvents = localStorage.loadRandomEventRecords();
		if (randomEvents.size() > 0)
		{
			randomEvents.forEach(panel::addRandom);
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		Actor source = event.getSource();
		Actor target = event.getTarget();
		Player player = client.getLocalPlayer();
		// Check that the npc is interacting with the player and the player isn't interacting with the npc, so
		// that the notification doesn't fire from talking to other user's randoms
		if (player == null || target != player || player.getInteracting() == source || !(source instanceof NPC) || !RandomEventAnalyticsUtil.getEventNpcIds().contains(((NPC) source).getId()))
		{
			return;
		}

		/**
		 * This is brought to you by the RandomEventPlugin. It seems sometimes you can
		 * have multiple notifications for a single random, and in our case we can have
		 * the same event added multiple times
		 */
		if (client.getTickCount() - lastNotificationTick > RANDOM_EVENT_TIMEOUT)
		{
			lastNotificationTick = client.getTickCount();
			handleRandomEvent((NPC) source);
		}
	}

	private void handleRandomEvent(NPC npc)
	{
		addRandomEvent(npc);
	}

	@Subscribe
	public void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();
		if (isStrangePlant(npc.getId()))
		{
			Player player = client.getLocalPlayer();
			if (player.getWorldLocation().distanceTo(npc.getWorldLocation()) == STRANGE_PLANT_SPAWN_RADIUS)
			{
				/**
				 * Unfortunately we cannot determine if the Strange Plant belongs to the player
				 * (See onInteractingChange)
				 * So we need to add an unconfirmed record (UI only) that allows the player to
				 * confirm if the plant belongs to them. Only then will it update the records.
				 */
				RandomEventRecord record = createRandomEventRecord(npc);
				panel.addUnconfirmedRandom(record);
				chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(PLANT_SPAWNED_NOTIFICATION_MESSAGE).build());
				notifier.notify(PLANT_SPAWNED_NOTIFICATION_MESSAGE);
			}
		}
	}

	@Schedule(period = 1, unit = ChronoUnit.SECONDS)
	public void timeSchedule()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			timeTracking.incrementSeconds();
			panel.updateEstimation();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			timeTracking.incrementTicks();
		}
	}

	private void updateConfig()
	{
		configManager.setConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, profile,
			RandomEventAnalyticsConfig.SECONDS_SINCE_LAST_RANDOM, timeTracking.getSecondsSinceLastRandomEvent());
		configManager.setConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, profile,
			RandomEventAnalyticsConfig.TICKS_SINCE_LAST_RANDOM, timeTracking.getTicksSinceLastRandomEvent());
		configManager.setConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, profile,
			RandomEventAnalyticsConfig.SECONDS_IN_INSTANCE, timeTracking.getSecondsInInstance());
	}

	public void addRandomEvent(final NPC npc)
	{
		addRandomEvent(createRandomEventRecord(npc));
	}

	public void addRandomEvent(final RandomEventRecord record)
	{
		localStorage.addRandomEventRecord(record);
		panel.addRandom(record);
		timeTracking.reset();

		/**
		 * The strange plant is added after the confirmation is clicked. This offsets
		 * our timers since the time the plant spawned.
		 * TODO: Determine if there's a way to correctly set ticksSinceLastRandom
		 */
		if (isStrangePlant(record.npcInfoRecord.npcId))
		{
			timeTracking.setSecondsSinceLastRandomEvent((int) (new Date().getTime() - record.spawnedTime) / 1000);
		}
		updateConfig();
	}

	private RandomEventRecord createRandomEventRecord(final NPC npc)
	{
		Player player = client.getLocalPlayer();
		PlayerInfoRecord playerInfoRecord = PlayerInfoRecord.create(player);
		NpcInfoRecord npcInfoRecord = NpcInfoRecord.create(npc);
		XpInfoRecord xpInfoRecord = XpInfoRecord.create(client, xpTrackerService);
		RandomEventRecord record = new RandomEventRecord(new Date().getTime(), timeTracking, npcInfoRecord,
			playerInfoRecord, xpInfoRecord);
		return record;
	}

	private boolean isStrangePlant(int npcId)
	{
		return npcId == NpcID.STRANGE_PLANT;
	}
}
