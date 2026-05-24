package com.pbl3.project.pbl3_project.ui.util;

import java.util.ArrayList;
import java.util.List;

public final class TableSortState {

    private final List<SortCriterion> defaultCriteria;
    private final List<SortCriterion> criteria = new ArrayList<>();

    public TableSortState(List<SortCriterion> defaultCriteria) {
        this.defaultCriteria = copyCriteria(defaultCriteria);
        resetToDefault();
    }

    public List<SortCriterion> snapshot() {
        return copyCriteria(criteria);
    }

    public void replace(List<SortCriterion> nextCriteria) {
        criteria.clear();
        criteria.addAll(copyCriteria(nextCriteria));
    }

    public void resetToDefault() {
        replace(defaultCriteria);
    }

    public void clear() {
        criteria.clear();
    }

    public boolean isEmpty() {
        return criteria.isEmpty();
    }

    private static List<SortCriterion> copyCriteria(List<SortCriterion> criteria) {
        List<SortCriterion> copy = new ArrayList<>();
        if (criteria == null) {
            return copy;
        }
        for (SortCriterion criterion : criteria) {
            if (criterion == null || criterion.uiKey() == null || criterion.direction() == null) {
                continue;
            }
            copy.add(new SortCriterion(criterion.uiKey(), criterion.direction()));
        }
        return copy;
    }
}
