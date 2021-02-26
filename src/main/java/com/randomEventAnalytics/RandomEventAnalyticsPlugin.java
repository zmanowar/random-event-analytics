package com.randomEventAnalytics;

import com.google.inject.Provides;

import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.inject.Inject;

import com.randomEventAnalytics.localstorage.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.xptracker.XpTrackerPlugin;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.xptracker.XpTrackerService;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Random Event Analytics"
)
@PluginDependency(XpTrackerPlugin.class)
public class RandomEventAnalyticsPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RandomEventAnalyticsOverlay overlay;

	@Inject
	private Client client;

	@Inject
	private RandomEventAnalyticsConfig config;

	@Inject
	private RandomEvenAnalyticsLocalStorage localStorage;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private XpTrackerService xpTrackerService;

	@Inject
	private ChatMessageManager chatMessageManager;

	RandomEventAnalyticsPanel panel;

	private NPC currentRandomEvent;
	private boolean isLoggedIn = false;
	private int secondsPerRandomEvent = 60 * 60;
	private int secondsSinceLastRandomEvent = 0;
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
		panel = new RandomEventAnalyticsPanel(this, config, client);
		final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "random_events_info_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Random Event Info")
				.icon(icon)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() {
		currentRandomEvent = null;

		overlayManager.removeIf(e -> e instanceof RandomEventAnalyticsOverlay);
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		isLoggedIn = false;
		localStorage.setSecondsSinceLastRandomEvent(secondsSinceLastRandomEvent);
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			isLoggedIn = true;
			localStorage.setPlayerUsername(client.getUsername());
			secondsSinceLastRandomEvent = localStorage.loadSecondsSinceLastRandomEvent();
			loadPreviousRandomEvents();
		}
		if (state == GameState.CONNECTION_LOST
				|| state == GameState.HOPPING
				|| state == GameState.LOGIN_SCREEN
				|| state == GameState.UNKNOWN
		) {
			isLoggedIn = false;
			localStorage.setSecondsSinceLastRandomEvent(secondsSinceLastRandomEvent);
		}
	}

	private synchronized void loadPreviousRandomEvents() {
		panel.clearAllRandomsView();
		ArrayList<RandomEventRecord> randomEvents = localStorage.loadRandomEventRecords();
		if (randomEvents.size() > 0) {
			panel.clearEventLog();
			//Collections.sort(randomEvents, Comparator.comparing(RandomEventRecord::getSpawnedTime).reversed());
			randomEvents.forEach(record -> {
				panel.addRandom(record);
			});
		} else {
			panel.setEmptyLog();
		}
	}

	@Subscribe
	public void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();
		if (npc.getId() == NpcID.STRANGE_PLANT) {
			/**
			 * Unfortunately we cannot determine if the Strange Plant belongs to the player
			 * (See onInteractingChange)
			 * So we need to add an unconfirmed record (UI only) that allows the player to
			 * confirm if the plant belongs to them. Only then will it update the records.
			 * Note: This bypasses setting currentRandomEvent
			 */
			RandomEventRecord record = createRandomEventRecord(npc);
			panel.addUnconfirmedRandom(record);
			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage("A Strange Plant has spawned, please visit the Random Event Analytics panel to confirm the random.")
					.build());
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		Actor source = event.getSource();
		Actor target = event.getTarget();
		Player player = client.getLocalPlayer();
		if (isInteractingWithPlant(source, target, player)) {
			/**
			 * TODO: Check for plant spawn within certain tiles of the player and there are
			 *  no other players. Also checking for "it's not here for you" in chat or that
			 *  the player was given an item (or on ground).
			 */
		}
		// Check that the npc is interacting with the player and the player isn't interacting with the npc, so
		// that the notification doesn't fire from talking to other user's randoms
		if (player == null
				|| target != player
				|| player.getInteracting() == source
				|| !(source instanceof NPC)
				|| !RandomEventAnalyticsUtil.EVENT_NPCS.contains(((NPC) source).getId()))
		{
			return;
		}
		currentRandomEvent = (NPC) source;
		handleRandomEvent(currentRandomEvent);
	}

	private boolean isInteractingWithPlant(Actor source, Actor target, Player player) {
		if (target == null
				|| !(target instanceof NPC)
				|| player.getInteracting() == null
		) {
			return false;
		}

		return ((NPC) target).getId() == NpcID.STRANGE_PLANT;
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();

		if (npc == currentRandomEvent)
		{
			currentRandomEvent = null;
			// TODO: Unsure if we need to reset the ticks after the NPC despawns or if the timer resets immediately
			secondsSinceLastRandomEvent = 0;
			localStorage.setSecondsSinceLastRandomEvent(0);
		}
	}

	@Schedule(
			period = 1,
			unit = ChronoUnit.SECONDS
	)
	public void tickTime() {
		if (isLoggedIn) {
			secondsSinceLastRandomEvent += 1;
			int estimatedSeconds = getNextRandomEventEstimation();
			overlay.updateTimeUntilRandomEvent(estimatedSeconds);
			panel.updateSeconds(estimatedSeconds);
			// TODO: panel.updateActionsPerHour, panel.updateXpPerHour
		}
	}

	public void handleRandomEvent(NPC npc) {
		currentRandomEvent = npc;
		addRandomEvent(npc);
	}

	public RandomEventRecord createRandomEventRecord(final NPC npc) {
		Player player = client.getLocalPlayer();
		PlayerInfoRecord playerInfoRecord = createPlayerInfoRecord(player);
		NpcInfoRecord npcInfoRecord = createNpcInfoRecord(npc);
		XpInfoRecord xpInfoRecord = createXpInfoRecord();
		RandomEventRecord record = new RandomEventRecord(
				new Date().getTime(),
				secondsSinceLastRandomEvent,
				npcInfoRecord,
				playerInfoRecord,
				xpInfoRecord
		);
		return record;
	}

	public void addRandomEvent(final NPC npc)
	{
		addRandomEvent(createRandomEventRecord(npc));
	}

	public void addRandomEvent(final RandomEventRecord record)
	{
		localStorage.addRandomEventRecord(record);
		panel.addRandom(record);
		panel.clearEventLog();
		secondsSinceLastRandomEvent = 0;
		if (record.npcInfoRecord.npcId == NpcID.STRANGE_PLANT) {
			secondsSinceLastRandomEvent = (int) ((new Date().getTime() / 1000) - record.spawnedTime);
		}
		localStorage.setSecondsSinceLastRandomEvent(secondsSinceLastRandomEvent);
	}

	private int getNextRandomEventEstimation() {
		XpInfoRecord xpInfoRecord = createXpInfoRecord();
		return secondsPerRandomEvent - secondsSinceLastRandomEvent;
	}

	private NpcInfoRecord createNpcInfoRecord(NPC npc) {
		LocalPoint npcLocalLocation = npc.getLocalLocation();
		WorldPoint npcWorldLocation = npc.getWorldLocation();
		return new NpcInfoRecord(
				npc.getId(),
				npc.getName(),
				npc.getCombatLevel(),
				npcLocalLocation.getX(),
				npcLocalLocation.getY(),
				npcWorldLocation.getX(),
				npcWorldLocation.getY(),
				npcWorldLocation.getPlane()
		);
	}

	private PlayerInfoRecord createPlayerInfoRecord(Player player) {
		LocalPoint playerLocalLocation = player.getLocalLocation();
		WorldPoint playerWorldLocation = player.getWorldLocation();
		return new PlayerInfoRecord(
				player.getCombatLevel(),
				playerLocalLocation.getX(),
				playerLocalLocation.getY(),
				playerWorldLocation.getX(),
				playerWorldLocation.getY(),
				playerWorldLocation.getPlane()
		);
	}

	private XpInfoRecord createXpInfoRecord() {
		Skill maximumActionsHrSkill = Skill.AGILITY;
		int maximumActionsHr = 0;
		Skill maximumXpHrSkill = Skill.AGILITY;
		int maximumXpHr = 0;

		for(Skill skill : Skill.values()) {
			if (skill.equals(Skill.OVERALL)) continue;
			int newSkillActionsHr = xpTrackerService.getActionsHr(skill);
			int newSkillXpHr = xpTrackerService.getXpHr(skill);
			if (newSkillActionsHr > xpTrackerService.getActionsHr(maximumActionsHrSkill)) {
				maximumActionsHrSkill = skill;
				maximumActionsHr = newSkillActionsHr;
			}
			if (newSkillXpHr > xpTrackerService.getXpHr(maximumXpHrSkill)) {
				maximumXpHrSkill = skill;
				maximumXpHr = newSkillXpHr;
			}
		}
		return new XpInfoRecord(
				xpTrackerService.getActionsHr(Skill.OVERALL),
				xpTrackerService.getXpHr(Skill.OVERALL),
				maximumActionsHrSkill.getName(),
				maximumActionsHr,
				maximumXpHrSkill.getName(),
				maximumXpHr,
				client.getOverallExperience()
		);

	}


}
