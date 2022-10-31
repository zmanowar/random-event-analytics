package com.randomEventAnalytics.module;

import com.google.inject.Inject;
import java.util.Set;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Slf4j
public class ComponentManager
{
	private final EventBus eventBus;
	private final Set<SolverModuleComponent> components;

	public void onPluginStart() {
		eventBus.register(this);
		components.forEach(c -> c.startUp());
	}

	public void onPluginStop()
	{
		eventBus.unregister(this);
		components.forEach(c -> c.shutDown());
	}
}
