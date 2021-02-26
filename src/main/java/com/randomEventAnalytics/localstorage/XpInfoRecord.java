package com.randomEventAnalytics.localstorage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class XpInfoRecord {
    public final int totalActionsHr;
    public final int totalXpHr;
    public final String maximumActionsHrSkillName;
    public final int maximumActionsHr;
    public final String maximumXpHrSkillName;
    public final int maximumXpHr;
    public final long overallExperience;
}
