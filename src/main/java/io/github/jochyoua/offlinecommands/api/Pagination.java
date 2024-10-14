package io.github.jochyoua.offlinecommands.api;

import io.github.jochyoua.offlinecommands.storage.UserStorage;

import java.util.List;

/**
 * Handles pagination logic for lists of data.
 */
public class Pagination {
    private final int totalItems;
    private final int pageSize;

    /**
     * Constructs a new Pagination object with the specified total items and page size.
     *
     * @param totalItems the total number of items
     * @param pageSize   the number of items per page
     */
    public Pagination(int totalItems, int pageSize) {
        this.totalItems = totalItems;
        this.pageSize = pageSize;
    }

    /**
     * Checks if the given page number is within the valid range.
     *
     * @param page the page number to check
     * @return true if the page number is valid, false otherwise
     */
    public boolean isValidPage(int page) {
        return page >= 1 && page <= getTotalPages();
    }

    /**
     * Calculates the total number of pages based on the total items and page size.
     *
     * @return the total number of pages
     */
    public int getTotalPages() {
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    /**
     * Determines the next page number after the current page.
     *
     * @param currentPage the current page number
     * @return the next page number if there is one, otherwise the current page
     */
    public int getNextPage(int currentPage) {
        int totalPages = getTotalPages();
        return (currentPage < totalPages) ? currentPage + 1 : totalPages;
    }

    /**
     * Retrieves a sublist of items for the specified page.
     *
     * @param list the full list of items
     * @param page the page number
     * @return the sublist of items for the specified page
     */
    public List<UserStorage> getSubList(List<UserStorage> list, int page) {
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return list.subList(fromIndex, toIndex);
    }
}