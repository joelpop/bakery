package org.vaadin.bakery.ui.component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Shared helper for stale data detection in edit dialogs.
 */
public final class StaleDataHelper {

    private StaleDataHelper() {}

    /**
     * Checks for external changes and shows the appropriate banner.
     * Used by Effect for live change detection while the dialog is open.
     */
    public static void checkForExternalChanges(
            Supplier<Optional<Integer>> versionSupplier,
            Integer expectedVersion,
            StaleDataBanner banner,
            Runnable onReload,
            Runnable onClose) {
        var currentVersion = versionSupplier.get();
        if (currentVersion.isEmpty()) {
            banner.showDeleted(onClose);
        } else if (!currentVersion.get().equals(expectedVersion)) {
            banner.showModified(onReload);
        }
    }

    /**
     * Pre-save version check. Returns true if data is stale (save should be aborted).
     */
    public static boolean isStale(
            Supplier<Optional<Integer>> versionSupplier,
            Integer expectedVersion,
            StaleDataBanner banner,
            Runnable onReload,
            Runnable onClose) {
        var currentVersion = versionSupplier.get();
        if (currentVersion.isEmpty()) {
            banner.showDeleted(onClose);
            return true;
        }
        if (!currentVersion.get().equals(expectedVersion)) {
            banner.showModified(onReload);
            return true;
        }
        return false;
    }
}
