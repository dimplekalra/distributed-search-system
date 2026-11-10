package com.searchSystem.service.search;

import org.springframework.stereotype.Service;

@Service
public class RankingService {
        private static final double COVERAGE_WEIGHT = 2.0;
        private static final double PHRASE_MATCH_BOOST = 5.0;
        private static final double POPULARITY_WEIGHT = 0.25;

        public double calculateFinalScore(
                        double bm25Score,
                        int matchedTerms,
                        int totalQueryTerms,
                        boolean phraseMatch,
                        long popularityScore) {

                double coverageRatio = (double) matchedTerms / totalQueryTerms;

                double coverageBoost = coverageRatio * COVERAGE_WEIGHT;

                double phraseBoost = phraseMatch
                                ? PHRASE_MATCH_BOOST
                                : 0.0;

                double popularityBoost = Math.log(1 + popularityScore)
                                * POPULARITY_WEIGHT;

                return bm25Score
                                + coverageBoost
                                + phraseBoost
                                + popularityBoost;
        }
}
