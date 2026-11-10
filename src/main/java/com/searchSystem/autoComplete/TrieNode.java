package com.searchSystem.autoComplete;

import java.util.*;

public class TrieNode {
    Map<Character, TrieNode> children;
    boolean isWord = false;

    public TrieNode() {
        this.children = new HashMap<>();
        isWord = false;
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }
}
