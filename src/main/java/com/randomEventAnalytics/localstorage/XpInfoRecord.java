package com.randomEventAnalytics.localstorage;

import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.xptracker.XpTrackerService;

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

	public static XpInfoRecord create(Client client, XpTrackerService xpTrackerService) {
		Skill maximumActionsHrSkill = Skill.AGILITY;
		int maximumActionsHr = xpTrackerService.getActionsHr(Skill.AGILITY);
		Skill maximumXpHrSkill = Skill.AGILITY;
		int maximumXpHr = xpTrackerService.getXpHr(Skill.AGILITY);
		int newSkillActionsHr = -1;
		int newSkillXpHr = -1;
		HashMap<String, Integer> xpPerSkill = new HashMap<>();

		for (Skill skill : Skill.values())
		{
			if (skill.equals(Skill.OVERALL))
			{
				continue;
			}
			xpPerSkill.put(skill.getName(), client.getSkillExperience(skill));
			newSkillActionsHr = xpTrackerService.getActionsHr(skill);
			newSkillXpHr = xpTrackerService.getXpHr(skill);
			if (newSkillActionsHr > xpTrackerService.getActionsHr(maximumActionsHrSkill))
			{
				maximumActionsHrSkill = skill;
				maximumActionsHr = newSkillActionsHr;
			}
			if (newSkillXpHr > xpTrackerService.getXpHr(maximumXpHrSkill))
			{
				maximumXpHrSkill = skill;
				maximumXpHr = newSkillXpHr;
			}
		}
		return new XpInfoRecord(
			xpTrackerService.getActionsHr(Skill.OVERALL),
			xpTrackerService.getXpHr(Skill.OVERALL),
			maximumActionsHrSkill.getName(),
			maximumActionsHr,
			maximumXpHrSkill.getName(),
			maximumXpHr,
			client.getOverallExperience(),
			xpPerSkill
		);
	}
}
