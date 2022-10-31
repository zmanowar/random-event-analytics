package com.randomEventAnalytics.helpers;

import com.google.inject.Inject;
import com.randomEventAnalytics.module.SolverModuleComponent;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SolverOverlayManager implements SolverModuleComponent
{
	private final EventBus eventBus;
	private final OverlayManager overlayManager;
	private final SolverOverlay solverOverlay;
	private final SolverItemOverlay solverItemOverlay;

	@Override
	public void startUp()
	{
		eventBus.register(this);
		overlayManager.add(solverOverlay);
		overlayManager.add(solverItemOverlay);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
		overlayManager.remove(solverOverlay);
		overlayManager.remove(solverItemOverlay);
	}
}
