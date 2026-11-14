package com.smartats.engine;

import com.smartats.model.JobPosting;

import java.util.*;
import java.util.regex.Pattern;

public class ATSEngine {

    public static class ScoreResult {
        private final double totalAtsScore;
        private final double keywordScore;
        private final double experienceScore;
        private final List<String> matchedSkills;
        private final List<String> missingSkills;

        public ScoreResult(double totalAtsScore, double keywordScore, double experienceScore,
                           List<String> matchedSkills, List<String> missingSkills) {
            this.totalAtsScore = Math.round(totalAtsScore * 10.0) / 10.0;
            this.keywordScore = Math.round(keywordScore * 10.0) / 10.0;
            this.experienceScore = Math.round(experienceScore * 10.0) / 10.0;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
        }

        public double getTotalAtsScore() {
            return totalAtsScore;
        }

        public double getKeywordScore() {
            return keywordScore;
        }

        public double getExperienceScore() {
            return experienceScore;
        }

        public List<String> getMatchedSkills() {
            return matchedSkills;
        }

        public List<String> getMissingSkills() {
            return missingSkills;
        }

        public String getMatchedSkillsCsv() {
            return String.join(", ", matchedSkills);
        }

        public String getMissingSkillsCsv() {
            return String.join(", ", missingSkills);
        }
    }

    public static ScoreResult evaluateResume(String resumeText, int candidateExpYears, JobPosting job) {
        if (resumeText == null) {
            resumeText = "";
        }
        String normalizedResume = " " + resumeText.toLowerCase().replaceAll("[^a-z0-9#+.-]", " ") + " ";

        // 1. Gather all required skills for this job
        // From XML taxonomy
        XMLTaxonomyParser taxonomy = XMLTaxonomyParser.getInstance();
        XMLTaxonomyParser.RoleDefinition roleDef = taxonomy.getRoleDefinition(job.getJobRole());

        Map<String, Integer> requiredSkillsWithWeights = new LinkedHashMap<>();
        Map<String, List<String>> skillSynonyms = new HashMap<>();

        if (roleDef != null) {
            for (XMLTaxonomyParser.Skill s : roleDef.getSkills()) {
                requiredSkillsWithWeights.put(s.getName(), s.getWeight());
                skillSynonyms.put(s.getName(), s.getSynonyms());
            }
        }

        // Add custom skills specified by the recruiter in the sentence builder
        if (job.getCustomSkillsCsv() != null && !job.getCustomSkillsCsv().trim().isEmpty()) {
            String[] custom = job.getCustomSkillsCsv().split("[,;]");
            for (String c : custom) {
                String trimmed = c.trim();
                if (!trimmed.isEmpty() && !requiredSkillsWithWeights.containsKey(trimmed)) {
                    requiredSkillsWithWeights.put(trimmed, 7); // Default weight of 7 for custom skills
                    skillSynonyms.put(trimmed, Collections.emptyList());
                }
            }
        }

        // If no skills found anywhere, provide generic programming skills
        if (requiredSkillsWithWeights.isEmpty()) {
            requiredSkillsWithWeights.put("Java", 8);
            requiredSkillsWithWeights.put("SQL", 7);
            requiredSkillsWithWeights.put("Problem Solving", 5);
        }

        // 2. Perform Keyword & Synonym Matching
        int totalWeight = 0;
        int matchedWeight = 0;
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : requiredSkillsWithWeights.entrySet()) {
            String skillName = entry.getKey();
            int weight = entry.getValue();
            totalWeight += weight;

            boolean isMatched = false;

            // Check primary skill name
            if (containsWord(normalizedResume, skillName.toLowerCase())) {
                isMatched = true;
            } else {
                // Check synonyms
                List<String> syns = skillSynonyms.get(skillName);
                if (syns != null) {
                    for (String syn : syns) {
                        if (containsWord(normalizedResume, syn.toLowerCase())) {
                            isMatched = true;
                            break;
                        }
                    }
                }
            }

            if (isMatched) {
                matchedWeight += weight;
                matched.add(skillName);
            } else {
                missing.add(skillName);
            }
        }

        // Keyword component (70% weight)
        double keywordScore = (totalWeight > 0)
                ? ((double) matchedWeight / totalWeight) * 70.0
                : 0.0;

        // 3. Experience component (30% weight)
        double expScore;
        int requiredExp = job.getMinExpYears();
        if (requiredExp <= 0) {
            expScore = 30.0; // No minimum experience requested, full marks awarded
        } else if (candidateExpYears >= requiredExp) {
            expScore = 30.0;
        } else {
            expScore = Math.max(0.0, ((double) candidateExpYears / requiredExp) * 30.0);
        }

        double totalScore = Math.min(100.0, keywordScore + expScore);

        return new ScoreResult(totalScore, keywordScore, expScore, matched, missing);
    }

    private static boolean containsWord(String text, String word) {
        if (word == null || word.trim().isEmpty()) return false;
        word = word.trim();
        // Regex word boundary matching with support for C++, C#, .NET
        String escapedWord = Pattern.quote(word);
        Pattern p = Pattern.compile("(?:^|[^a-z0-9#+.-])" + escapedWord + "(?:$|[^a-z0-9#+.-])", Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }
}
