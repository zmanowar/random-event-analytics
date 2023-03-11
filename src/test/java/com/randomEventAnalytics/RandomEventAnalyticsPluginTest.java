package com.randomEventAnalytics;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import com.randomEventAnalytics.localstorage.RandomEventAnalyticsLocalStorage;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.xptracker.XpTrackerService;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RandomEventAnalyticsPluginTest
{
	@Mock
	@Bind
	Client client;
	@Mock
	@Bind
	ConfigManager configManager;
	@Mock
	@Bind
	RandomEventAnalyticsConfig config;
	@Mock
	@Bind
	OverlayManager overlayManager;
	@Mock
	@Bind
	RandomEventAnalyticsOverlay overlay;
	@Mock
	@Bind
	RandomEventAnalyticsPanel panel;
	@Mock
	@Bind
	RandomEventAnalyticsLocalStorage localStorage;
	@Mock
	@Bind
	TimeTracking timeTracking;
	@Mock
	@Bind
	ChatMessageManager chatMessageManager;
	@Mock
	@Bind
	XpTrackerService xpTrackerService;

	@Mock
	@Bind
	Notifier notifier;

	@Inject
	RandomEventAnalyticsPlugin plugin;

	@Before
	public void before() {
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
		mockLocalStorage();
	}

	@Test
	public void testAddsRandomEventWhenValidNPCInteractionChanged()
	{
		doNothing().when(panel).addRandom(any());
		plugin.setPanel(panel);

		final Player player = getMockedPlayer();
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getTickCount()).thenReturn(10);

		NPC npc1 = getMockedNPC(NpcID.BEE_KEEPER_6747);
		interactingChanged(npc1, player);

		verify(localStorage, times(1)).addRandomEventRecord(any());
		verify(panel, times(1)).addRandom(any());
		verify(timeTracking, times(1)).setRandomEventSpawned();
	}

	@Test
	public void testDoesNotAddOtherPlayersRandoms()
	{
		final Player player = getMockedPlayer();
		when(client.getLocalPlayer()).thenReturn(player);

		Player otherPlayer = getMockedPlayer();
		NPC npc1 = getMockedNPC(NpcID.BEE_KEEPER_6747);
		interactingChanged(npc1, otherPlayer);

		verify(localStorage, times(0)).addRandomEventRecord(any());
		verify(panel, times(0)).addRandom(any());
		verify(timeTracking, times(0)).setRandomEventSpawned();
	}

	private void mockLocalStorage() {
		when(localStorage.addRandomEventRecord(any())).thenReturn(true);
	}

	private NPC getMockedNPC(int npcID) {
		final NPC npc = mock(NPC.class);
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(0, 0, 0));
		when(npc.getLocalLocation()).thenReturn(new LocalPoint(0,0));
		when(npc.getName()).thenReturn("MockedNPCName");
		when(npc.getCombatLevel()).thenReturn(1);
		when(npc.getId()).thenReturn(npcID);
		return npc;
	}

	private Player getMockedPlayer() {
		final Player player = mock(Player.class);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(0, 0, 0));
		when(player.getLocalLocation()).thenReturn(new LocalPoint(0,0));
		when(player.getCombatLevel()).thenReturn(126);
		return player;
	}

	private void interactingChanged(final Actor source, final Actor target) {
		lenient().when(source.getInteracting()).thenReturn(target);
		plugin.onInteractingChanged(new InteractingChanged(source, target));
	}
}
