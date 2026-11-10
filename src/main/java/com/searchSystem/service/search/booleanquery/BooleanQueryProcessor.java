package com.searchSystem.service.search.booleanquery;

import com.searchSystem.model.SearchResult;
import com.searchSystem.service.search.QueryCoordinatorService;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BooleanQueryProcessor {
    private final QueryCoordinatorService queryCoordinatorService;

    public BooleanQueryProcessor(
            QueryCoordinatorService queryCoordinatorService) {

        this.queryCoordinatorService = queryCoordinatorService;
    }

    public List<SearchResult> execute(
            List<BooleanToken> tokens, int page, int size) {

        Set<String> currentDocuments = new HashSet<>();

        boolean initialized = false;

        for (BooleanToken token : tokens) {

            List<SearchResult> results = queryCoordinatorService
                    .executeDistributedQuery(
                            List.of(token.getValue()),
                            page,
                            size,
                            false);

            Set<String> documents = new HashSet<>();

            for (SearchResult result : results) {
                documents.add(
                        result.getDocumentId());
            }

            if (!initialized) {

                currentDocuments.addAll(
                        documents);

                initialized = true;
                continue;
            }

            BooleanOperator operator = token.getOperator();

            if (operator == BooleanOperator.AND) {

                currentDocuments.retainAll(
                        documents);

            } else if (operator == BooleanOperator.OR) {

                currentDocuments.addAll(
                        documents);

            } else if (operator == BooleanOperator.NOT) {

                currentDocuments.removeAll(
                        documents);
            }
        }

        List<SearchResult> finalResults = new ArrayList<>();

        for (String docId : currentDocuments) {

            finalResults.add(
                    new SearchResult(
                            docId,
                            1.0));
        }

        return finalResults;
    }
}
