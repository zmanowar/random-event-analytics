package com.randomEventAnalytics;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RandomEventInfoPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RandomEventAnalyticsPlugin.class);
		RuneLite.main(args);
	}
}