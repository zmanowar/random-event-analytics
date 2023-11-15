package com.randomEventAnalytics;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.randomEventAnalytics.module.ComponentManager;
import com.randomEventAnalytics.module.RandomEventHelperModule;
import com.randomEventAnalytics.localstorage.NpcInfoRecord;
import com.randomEventAnalytics.localstorage.PlayerInfoRecord;
import com.randomEventAnalytics.localstorage.RandomEventAnalyticsLocalStorage;
import com.randomEventAnalytics.localstorage.RandomEventRecord;
import com.randomEventAnalytics.localstorage.XpInfoRecord;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
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
	private static final String PLANT_SPAWNED_NOTIFICATION_MESSAGE =
		"A Strange Plant has spawned, please visit the Random Event Analytics panel to confirm the random.";
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
	private TimeTracking timeTracking;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private Notifier notifier;
	@Inject
	private XpTrackerService xpTrackerService;
	@Inject
	private Injector injector;
	private ComponentManager componentManager = null;

	@Setter
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
	public void configure(Binder binder)
	{
		binder.install(new RandomEventHelperModule());
	}

	@Override
	protected void startUp() throws Exception
	{
		client.setIdleTimeout((int) Duration.ofMinutes(30).toMillis() / Constants.CLIENT_TICK_LENGTH);
		overlayManager.add(overlay);
		panel = injector.getInstance(RandomEventAnalyticsPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "random_event_analytics.png");

		navButton =
			NavigationButton.builder().tooltip("Random Event Analytics").icon(icon).panel(panel).priority(7).build();

		clientToolbar.addNavigation(navButton);

		if (componentManager == null)
		{
			componentManager = injector.getInstance(ComponentManager.class);
		}
		componentManager.onPluginStart();
		if (!this.isLoggedIn())
		{
			return;
		}
		this.loadConfig();
		loadPreviousRandomEvents();
	}

	@Override
	protected void shutDown()
	{
		persistTimeTrackingConfig();
		lastNotificationTick = 0;
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
		componentManager.onPluginStop();
	}

	private boolean isLoggedIn()
	{
		return client.getAccountHash() != -1;
	}

	private void loadConfig()
	{
		profile = configManager.getRSProfileKey();
		timeTracking.init(
			Instant.now(),
			getLastRandomSpawnInstant()
		);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(RandomEventAnalyticsConfig.CONFIG_GROUP))
		{
			return;
		}

		panel.updateConfig();
		if (configChanged.getKey().equals("logTimeFormat")) {
			SwingUtilities.invokeLater(panel::updateAllRandomEventBoxes);
		}
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		persistTimeTrackingConfig();
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{

		GameState state = gameStateChanged.getGameState();
		// TODO: Update timeTracking loginTime when relogging.
		if (state == GameState.LOGGED_IN)
		{
			if (timeTracking.getLoginTime() == null)
			{
				timeTracking.setLoginTime(Instant.now());
			}

			final long hash = client.getAccountHash();
			if (String.valueOf(hash).equalsIgnoreCase(localStorage.getUsername()))
			{
				return;
			}

			String username = client.getUsername();
			if (username != null && username.length() > 0)
			{
				localStorage.renameUsernameFolderToAccountHash(username, hash);
			}

			if (localStorage.setPlayerUsername(String.valueOf(hash)))
			{
				this.loadConfig();
				loadPreviousRandomEvents();
			}
		}
		else if (state == GameState.CONNECTION_LOST || state == GameState.UNKNOWN)
		{
			persistTimeTrackingConfig();
		}
		else if (state == GameState.HOPPING)
		{
			persistTimeTrackingConfig();
			timeTracking.setLoginTime(null);
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			persistTimeTrackingConfig();
			timeTracking.setLoginTime(null);
			panel.updateEstimation();
		}
	}

	private Instant getLastRandomSpawnInstant()
	{
		Instant spawned = getInstantFromProfileConfig(RandomEventAnalyticsConfig.LAST_RANDOM_SPAWN_INSTANT);
		if (spawned != null)
		{
			return spawned;
		}

		// One-time Update: This handles outdated profile config, should only ever need to be called once per profile.
		RandomEventRecord record = localStorage.getMostRecentRandom();
		if (record.spawnedTime < 0)
		{
			return null;
		}

		return Instant.ofEpochMilli(record.spawnedTime);
	}

	private Instant getInstantFromProfileConfig(String key)
	{
		try
		{
			return configManager.getConfiguration(RandomEventAnalyticsConfig.CONFIG_GROUP, profile, key,
				Instant.class);
		}
		catch (NullPointerException e)
		{
			log.debug("No config loaded for: {}@{}", key, profile);
			return null;
		}
	}

	private synchronized void loadPreviousRandomEvents()
	{
		panel.clearAllRandomsView();
		ArrayList<RandomEventRecord> randomEvents = localStorage.loadRandomEventRecords();
		if (randomEvents.size() > 0)
		{
			randomEvents.forEach(panel::addRandom);
		} else {
			panel.clearAllRandomsView();
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

		/*
		  This is brought to you by the RandomEventPlugin. It seems sometimes you can
		  have multiple notifications for a single random, and in our case we can have
		  the same event added multiple times
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
		if (!isStrangePlant(npc.getId()))
		{
			return;
		}
		Player player = client.getLocalPlayer();
		if (player.getWorldLocation().distanceTo(npc.getWorldLocation()) != STRANGE_PLANT_SPAWN_RADIUS)
		{
			return;
		}
		if (!timeTracking.isPossibleTimeForRandomEvent())
		{
			// We only want to notify about strange plants when it's possibly the user's random
			return;
		}
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

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			panel.updateEstimation();
			timeTracking.onTick();
		}
	}

	private void persistTimeTrackingConfig()
	{
		timeTracking.persistConfig();
	}

	public void addRandomEvent(final NPC npc)
	{
		addRandomEvent(createRandomEventRecord(npc));
	}

	public void addRandomEvent(final RandomEventRecord record)
	{
		localStorage.addRandomEventRecord(record);
		panel.addRandom(record);
		/**
		 * The strange plant is added after the confirmation is clicked. This offsets
		 * our timers since the time the plant spawned.
		 */
		if (isStrangePlant(record.npcInfoRecord.npcId))
		{
			timeTracking.setStrangePlantSpawned(record);
		}
		else
		{
			timeTracking.setRandomEventSpawned();
		}
	}

	public int getNumberOfEventsLogged()
	{
		return localStorage.getNumberOfLoggedEvents();
	}

	private RandomEventRecord createRandomEventRecord(final NPC npc)
	{
		Player player = client.getLocalPlayer();
		PlayerInfoRecord playerInfoRecord = PlayerInfoRecord.create(player);
		NpcInfoRecord npcInfoRecord = NpcInfoRecord.create(npc);
		XpInfoRecord xpInfoRecord = XpInfoRecord.create(client, xpTrackerService);
		RandomEventRecord record = new RandomEventRecord(Instant.now().toEpochMilli(), timeTracking, npcInfoRecord,
			playerInfoRecord, xpInfoRecord);
		return record;
	}

	private boolean isStrangePlant(int npcId)
	{
		return npcId == NpcID.STRANGE_PLANT;
	}

	@Schedule(
		period = 500,
		unit = ChronoUnit.MILLIS
	)
	public void updateSchedule()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			panel.updateEstimation();
		}
	}
}
