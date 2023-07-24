package com.example.demo.model;

import java.util.List;

public class Page<T> {
    private final List<T> items;
    private final int currentPage;
    private final int totalPages;
    private final long totalItems;

    public Page(List<T> items, int currentPage, int totalPages, long totalItems) {
        this.items = items;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
    }

    public List<T> getItems() {
        return items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalItems() {
        return totalItems;
    }
}