package com.randomEventAnalytics.helpers;

import com.randomEventAnalytics.module.SolverModuleComponent;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MimeSolver implements SolverModuleComponent
{
	private final Client client;
	private final EventBus eventBus;

	private static Integer MIME_NPC_ID = 321;
	private static Integer MIME_ACTION_WIDGET_GROUP_ID = 188;
	private static Integer MIME_MAP_REGION = 8010;

	private static HashMap<Integer, Integer> MIME_ANIMATION_TO_CHILD_ID_MAP = new HashMap<Integer, Integer>(){{
		put(857, 2);
		put(861, 3);
		put(1130, 4);
		put(1131, 5);
		put(860, 6);
		put(866, 7);
		put(1129, 8);
		put(1128, 9);
	}};

	private Integer mimeAnimationId;

	@Override
	public boolean isEnabled() {
		return client.getMapRegions() != null &&
			ArrayUtils.contains(client.getMapRegions(), MIME_MAP_REGION);
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

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		if (!isEnabled()) return;
		if (!(event.getActor() instanceof NPC)) {
			return;
		}

		NPC npc = (NPC) event.getActor();
		if(npc.getId() == MIME_NPC_ID) {
			int animationId = npc.getAnimation();
			if (MIME_ANIMATION_TO_CHILD_ID_MAP.containsKey(animationId)) {
				mimeAnimationId = animationId;
			}
		}
	}

	public Widget getMimeEmoteWidget() {
		Integer childWidgetId = MIME_ANIMATION_TO_CHILD_ID_MAP.get(mimeAnimationId);
		if (childWidgetId == null) return null;
		return client.getWidget(MIME_ACTION_WIDGET_GROUP_ID, childWidgetId);
	}

}
