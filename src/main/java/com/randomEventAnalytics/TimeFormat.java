package com.randomEventAnalytics;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TimeFormat
{
	TIME_12H("12-hour"),
	TIME_24H("24-hour");

	private final String name;

	@Override
	public String toString()
	{
		return this.name;
	}
}
