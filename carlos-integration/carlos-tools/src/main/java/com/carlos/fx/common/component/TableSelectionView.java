package com.carlos.fx.common.component;

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable Table Selection View with checkboxes
 *
 * @author Carlos
 * @since 3.0.0
 */
public class TableSelectionView extends TreeView<String> {

    private final CheckBoxTreeItem<String> rootItem;

    public TableSelectionView(String rootLabel) {
        rootItem = new CheckBoxTreeItem<>(rootLabel);
        rootItem.setExpanded(true);

        setRoot(rootItem);
        setShowRoot(true);
        setCellFactory(CheckBoxTreeCell.forTreeView());
        setEditable(true);
    }

    /**
     * Add table with fields
     */
    public void addTable(String tableName, List<String> fields) {
        CheckBoxTreeItem<String> tableItem = new CheckBoxTreeItem<>(tableName);

        for (String field : fields) {
            CheckBoxTreeItem<String> fieldItem = new CheckBoxTreeItem<>(field);
            tableItem.getChildren().add(fieldItem);
        }

        tableItem.setExpanded(false);
        rootItem.getChildren().add(tableItem);
    }

    /**
     * Get selected tables
     */
    public List<String> getSelectedTables() {
        List<String> selected = new ArrayList<>();

        for (var child : rootItem.getChildren()) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) child;
            if (item.isSelected() || item.isIndeterminate()) {
                selected.add(item.getValue());
            }
        }

        return selected;
    }

    /**
     * Get selected fields for a table
     */
    public List<String> getSelectedFields(String tableName) {
        List<String> selected = new ArrayList<>();

        for (var child : rootItem.getChildren()) {
            CheckBoxTreeItem<String> tableItem = (CheckBoxTreeItem<String>) child;
            if (tableItem.getValue().equals(tableName)) {
                for (var fieldChild : tableItem.getChildren()) {
                    CheckBoxTreeItem<String> fieldItem = (CheckBoxTreeItem<String>) fieldChild;
                    if (fieldItem.isSelected()) {
                        selected.add(fieldItem.getValue());
                    }
                }
                break;
            }
        }

        return selected;
    }

    /**
     * Clear all items
     */
    public void clearItems() {
        rootItem.getChildren().clear();
    }

    /**
     * Select all items
     */
    public void selectAll() {
        rootItem.setSelected(true);
    }

    /**
     * Deselect all items
     */
    public void deselectAll() {
        rootItem.setSelected(false);
    }
}
