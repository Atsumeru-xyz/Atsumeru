package com.atsumeru.web.filewatch;

import java.util.Set;

/**
 * Callback interface when file changes are detected.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @since 1.3.0
 */
@FunctionalInterface
public interface FileChangeListener {

    /**
     * Called when files have been changed.
     * @param changeSet a set of the {@link ChangedFiles}
     */
    void onChange(Set<ChangedFiles> changeSet);

}
