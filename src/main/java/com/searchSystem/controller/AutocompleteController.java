package com.searchSystem.controller;

import com.searchSystem.model.Suggestion;
import com.searchSystem.service.search.AutoCompleteService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/autocomplete")
public class AutocompleteController {
    private final AutoCompleteService service;

    public AutocompleteController(AutoCompleteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Suggestion> autocomplete(
            @RequestParam String prefix) {

        return service.suggest(prefix, 10);
    }
}
