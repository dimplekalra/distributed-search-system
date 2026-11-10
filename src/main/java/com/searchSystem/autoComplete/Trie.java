package com.searchSystem.autoComplete;

import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class Trie {

    private final TrieNode root = new TrieNode();

    public void insert(String word) {

        TrieNode current = root;

        for (char c : word.toCharArray()) {

            current.children.putIfAbsent(
                    c,
                    new TrieNode());

            current = current.children.get(c);
        }

        current.isWord = true;
    }

    public List<String> searchPrefix(String prefix) {

        TrieNode current = root;

        for (char c : prefix.toCharArray()) {

            current = current.children.get(c);

            if (current == null) {
                return Collections.emptyList();
            }
        }

        List<String> results = new ArrayList<>();

        collectWords(current, prefix, results);

        return results;
    }

    public List<String> getAllWords() {

        List<String> words = new ArrayList<>();

        TrieNode current = root;

        collectWords(
                current,
                "",
                words);

        return words;
    }

    private void collectWords(
            TrieNode node,
            String word,
            List<String> results) {

        if (node.isWord) {
            results.add(word);
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {

            collectWords(
                    entry.getValue(),
                    word + entry.getKey(),
                    results);

        }
    }
}
