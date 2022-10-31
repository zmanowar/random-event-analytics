package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetModelType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FreakyForesterSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;
	private static final Integer FREAKY_FORESTER_MAP_REGION = 10314;
	private static final Pattern FORESTER_CHAT_TAIL_PATTERN_1 = Pattern.compile(" I need you to kill (?:the|a) pheasant that has (?<numberOfTails>[1-4\\w]+) tail[s]?");

	private static final Pattern FORESTER_CHAT_TAIL_PATTERN_2 = Pattern.compile("Can you kill (?:the|a) (?<numberOfTails>[1-4\\w]+)-tailed pheasant");
	private static final Pattern FORESTER_CHAT_TAIL_PATTERN_3 = Pattern.compile("Could you kill (?:the|a) pheasant with (?<numberOfTails>\\w+) tail[s]?");
	private static final HashMap<Integer, Integer> PHEASANT_TAIL_TO_NPC_ID = new HashMap<Integer, Integer>()
	{{
		put(1, 373);
		put(2, 5500);
		put(3, 374);
		put(4, 5502);
	}};

	private final HashMap<NPC, Boolean> pheasants = new HashMap<>();

	public Set<NPC> getValidPheasants() {
		return pheasants.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
	}

	@Override
	public boolean isEnabled()
	{
		return client.getMapRegions() != null &&
			ArrayUtils.contains(client.getMapRegions(), FREAKY_FORESTER_MAP_REGION);
	}

	private void setValidPheasants(Integer tailCount) {
		if (tailCount == null) return;

		Integer pheasantNPCId = PHEASANT_TAIL_TO_NPC_ID.get(tailCount);
		if (pheasantNPCId == null) return;

		pheasants.replaceAll((npc, isCorrectPheasant) -> npc.getId() == pheasantNPCId);
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!isEnabled()) return;
		setValidPheasants(getTailCount());
	}

	private Matcher getValidMatcher(String npcText)
	{
		Matcher matcher = FORESTER_CHAT_TAIL_PATTERN_1.matcher(npcText);
		if (matcher.find())
		{
			return matcher;
		}

		matcher = FORESTER_CHAT_TAIL_PATTERN_2.matcher(npcText);
		if (matcher.find())
		{
			return matcher;
		}

		matcher = FORESTER_CHAT_TAIL_PATTERN_3.matcher(npcText);
		if (matcher.find()) {
			return matcher;
		}

		return null;
	}

	private Integer getTailCount()
	{
		Widget npcDialog = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
		if (npcDialog == null)
		{
			return null;
		}

		Widget name = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
		Widget head = client.getWidget(WidgetInfo.DIALOG_NPC_HEAD_MODEL);
		if (name == null || head == null || head.getModelType() != WidgetModelType.NPC_CHATHEAD)
		{
			return null;
		}

		final int npcId = head.getModelId();
		if (npcId != NpcID.FREAKY_FORESTER && npcId != NpcID.FREAKY_FORESTER_6748)
		{
			return null;
		}

		String npcText = Text.sanitizeMultilineText(npcDialog.getText());
		Matcher matcher = getValidMatcher(npcText);
		if (matcher == null) {
			return null;
		}

		return getIntFromText(matcher.group("numberOfTails"));
	}

	private Integer getIntFromText(String text)
	{
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return text.equalsIgnoreCase("one") ? 1
				: text.equalsIgnoreCase("two") ? 2
				: text.equalsIgnoreCase("three") ? 3
				: text.equalsIgnoreCase("four") ? 4 : null;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!isEnabled())
		{
			return;
		}
		NPC npc = event.getNpc();
		if (PHEASANT_TAIL_TO_NPC_ID.containsValue(npc.getId()))
		{
			pheasants.put(npc, false);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!isEnabled())
		{
			return;
		}
		NPC npc = event.getNpc();
		if (PHEASANT_TAIL_TO_NPC_ID.containsValue(npc.getId()))
		{
			pheasants.remove(npc);
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

	/**
	 * TODO: Implement this in the other solvers as well. May need different
	 *  checks if the random is in multiple chunks as LOADING/UNKNOWN will
	 *  break it
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState state = gameStateChanged.getGameState();
		if (state == GameState.CONNECTION_LOST || state == GameState.HOPPING
			|| state == GameState.LOGIN_SCREEN || state == GameState.UNKNOWN
			|| state == GameState.LOADING
		) {
			pheasants.clear();
		}
	}
}
