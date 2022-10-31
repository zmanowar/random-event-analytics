package com.randomEventAnalytics;

import com.google.inject.Singleton;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;

@Singleton
public class RandomEventAnalyticsTimeTracking
{
	private static final int SECONDS_PER_RANDOM_EVENT = 60 * 60;
	@Inject
	private Client client;
	@Getter
	@Setter
	private int secondsSinceLastRandomEvent;
	@Getter
	private int secondsInInstance;
	@Getter
	private int ticksSinceLastRandomEvent;

	public void init(int secondsSinceLastRandomEvent, int secondsInInstance, int ticksSinceLastRandomEvent)
	{
		this.secondsSinceLastRandomEvent = secondsSinceLastRandomEvent;
		this.secondsInInstance = secondsInInstance;
		this.ticksSinceLastRandomEvent = ticksSinceLastRandomEvent;
	}

	public void incrementSeconds()
	{
		secondsSinceLastRandomEvent += 1;
		if (client.isInInstancedRegion())
		{
			secondsInInstance += 1;
		}
	}

	public void incrementTicks()
	{
		ticksSinceLastRandomEvent += 1;
	}

	public int getNextRandomEventEstimation()
	{
		return SECONDS_PER_RANDOM_EVENT - secondsSinceLastRandomEvent;
	}

	public void reset()
	{
		secondsInInstance = 0;
		ticksSinceLastRandomEvent = 0;
		secondsSinceLastRandomEvent = 0;
	}
}
