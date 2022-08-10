package com.randomEventAnalytics.localstorage;

import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class XpInfoRecord
{
	public final int totalActionsHr;
	public final int totalXpHr;
	public final String maximumActionsHrSkillName;
	public final int maximumActionsHr;
	public final String maximumXpHrSkillName;
	public final int maximumXpHr;
	public final long overallExperience;
	public final HashMap<String, Integer> xpPerSkill;
}
