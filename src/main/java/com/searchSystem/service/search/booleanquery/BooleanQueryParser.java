package com.searchSystem.service.search.booleanquery;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BooleanQueryParser {
    public List<BooleanToken> parse(
            String query) {

        String[] parts = query.split("\\s+");

        List<BooleanToken> tokens = new ArrayList<>();

        BooleanOperator currentOperator = null;

        for (String part : parts) {

            String normalized = part.toUpperCase();

            switch (normalized) {

                case "AND":
                    currentOperator = BooleanOperator.AND;
                    break;

                case "OR":
                    currentOperator = BooleanOperator.OR;
                    break;

                case "NOT":
                    currentOperator = BooleanOperator.NOT;
                    break;

                default:

                    tokens.add(
                            new BooleanToken(
                                    part.toLowerCase(),
                                    currentOperator));

                    currentOperator = null;
            }
        }

        return tokens;
    }
}
