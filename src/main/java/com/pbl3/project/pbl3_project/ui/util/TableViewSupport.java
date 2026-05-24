package com.pbl3.project.pbl3_project.ui.util;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

public final class TableViewSupport {

    private static final String COLUMN_REORDER_GUARD_KEY = "columnReorderGuardInstalled";

    private TableViewSupport() {
    }

    public static <T> void enableDragSelection(TableView<T> table) {
        prepareNonReorderableTable(table);
        final int[] dragAnchor = new int[] { -1 };

        // Repeated clicks should not clear the selected row before a double-click action opens.
        table.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Node node = event.getPickResult().getIntersectedNode();
            while (node != null && node != table && !(node instanceof TableRow)) {
                node = node.getParent();
            }

            if (node instanceof TableRow<?> row && !row.isEmpty()) {
                dragAnchor[0] = row.getIndex();
            }
        });

        table.setOnMouseDragged(event -> {
            if (event.getButton() != MouseButton.PRIMARY || dragAnchor[0] < 0) {
                return;
            }

            Node node = event.getPickResult().getIntersectedNode();
            while (node != null && node != table && !(node instanceof TableRow)) {
                node = node.getParent();
            }
            if (node instanceof TableRow<?> row && !row.isEmpty()) {
                int currentIndex = row.getIndex();
                table.getSelectionModel().clearSelection();
                int start = Math.min(dragAnchor[0], currentIndex);
                int end = Math.max(dragAnchor[0], currentIndex);
                table.getSelectionModel().selectRange(start, end + 1);
            }
        });
    }

    public static void prepareNonReorderableTable(TableView<?> table) {
        if (table == null) {
            return;
        }
        table.getSelectionModel().setCellSelectionEnabled(false);
        disableColumnReordering(table);
    }

    public static void enableDeselectOnOutsideClick(Pane root, TableView<?> table) {
        enableDeselectOnOutsideClick(root, new TableView<?>[]{table});
    }

    public static void enableDeselectOnOutsideClick(Pane root, TableView<?>... tables) {
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            boolean isSafe = false;
            Node curr = (Node) event.getTarget();

            boolean clickedEmptyTableArea = clickedEmptyTableArea(curr, root);
            while (curr != null && curr != root) {
                if (isInsideAnyTable(curr, tables) && !clickedEmptyTableArea) {
                    isSafe = true;
                    break;
                }
                if (isInteractiveControl(curr)) {
                    isSafe = true;
                    break;
                }
                curr = curr.getParent();
            }

            if (!isSafe) {
                clearSelections(tables);
                root.requestFocus();
            }
        });

        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                clearSelections(tables);
                root.requestFocus();
            }
        });
    }

    public static TableView<?> findFirstTableView(Node node) {
        if (node instanceof TableView<?> tableView) {
            return tableView;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TableView<?> childTable = findFirstTableView(child);
                if (childTable != null) {
                    return childTable;
                }
            }
        }
        return null;
    }

    private static void disableColumnReordering(TableView<?> table) {
        if (table == null) {
            return;
        }
        installColumnReorderGuard(table.getColumns());
    }

    private static void installColumnReorderGuard(ObservableList<? extends TableColumnBase<?, ?>> columns) {
        if (columns == null) {
            return;
        }
        for (TableColumnBase<?, ?> column : columns) {
            installColumnReorderGuard(column);
        }
        columns.addListener((ListChangeListener<TableColumnBase<?, ?>>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (TableColumnBase<?, ?> column : change.getAddedSubList()) {
                        installColumnReorderGuard(column);
                    }
                }
            }
        });
    }

    private static void installColumnReorderGuard(TableColumnBase<?, ?> column) {
        if (column == null) {
            return;
        }
        column.setReorderable(false);
        if (Boolean.TRUE.equals(column.getProperties().get(COLUMN_REORDER_GUARD_KEY))) {
            return;
        }
        column.getProperties().put(COLUMN_REORDER_GUARD_KEY, Boolean.TRUE);
        installColumnReorderGuard(column.getColumns());
    }

    private static boolean clickedEmptyTableArea(Node node, Pane root) {
        Node checkNode = node;
        while (checkNode != null && checkNode != root) {
            if (checkNode instanceof IndexedCell<?> cell) {
                return cell.isEmpty();
            }
            if (checkNode.getClass().getSimpleName().equals("TableBodyStack")) {
                return true;
            }
            checkNode = checkNode.getParent();
        }
        return false;
    }

    private static boolean isInteractiveControl(Node node) {
        return node instanceof Button
            || node instanceof TextField
            || node instanceof ComboBox
            || node instanceof DatePicker
            || node instanceof MenuBar
            || node.getStyleClass().contains("expandable-search-box")
            || node.getStyleClass().contains("search-field")
            || node.getStyleClass().contains("search-text-field");
    }

    private static boolean isInsideAnyTable(Node node, TableView<?>... tables) {
        if (node == null || tables == null) {
            return false;
        }
        for (TableView<?> table : tables) {
            if (table != null && node == table) {
                return true;
            }
        }
        return false;
    }

    private static void clearSelections(TableView<?>... tables) {
        if (tables == null) {
            return;
        }
        for (TableView<?> table : tables) {
            if (table != null) {
                table.getSelectionModel().clearSelection();
            }
        }
    }
}
