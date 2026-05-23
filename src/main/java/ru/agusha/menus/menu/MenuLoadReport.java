package ru.agusha.menus.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MenuLoadReport {

    private int loadedMenus;
    private int failedMenus;
    private int skippedItems;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void loadedMenu() {
        loadedMenus++;
    }

    public void skippedItem() {
        skippedItems++;
    }

    public void failedMenu() {
        failedMenus++;
    }

    public void warning(String warning) {
        warnings.add(warning);
    }

    public void error(String error) {
        errors.add(error);
    }

    public int loadedMenus() {
        return loadedMenus;
    }

    public int skippedItems() {
        return skippedItems;
    }

    public int failedMenus() {
        return failedMenus;
    }

    public int warningCount() {
        return warnings.size();
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public int errorCount() {
        return errors.size();
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }
}
