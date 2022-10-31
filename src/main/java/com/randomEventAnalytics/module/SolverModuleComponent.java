package com.randomEventAnalytics.module;

public interface SolverModuleComponent
{

	default boolean isEnabled() {
		return true;
	}

	void startUp();
	void shutDown();

}
