/*
 * Copyright (c) 2013 Philippe VIENNE
 *
 * This file is a part of SpeleoGraph
 *
 * SpeleoGraph is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * SpeleoGraph is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with SpeleoGraph.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.cds06.speleograph.actions;

import org.cds06.speleograph.data.ReefnetFileReader;
import org.cds06.speleograph.data.ImportTable;
import org.cds06.speleograph.utils.AcceptedFileFilter;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * Action used to describe a file import.
 *
 * @author Philippe VIENNE
 * @since 1.0
 */
public class ImportAction extends AbstractAction {

    /**
     * Logger for info and errors.
     */ @NonNls
    private static final Logger log=LoggerFactory.getLogger(ImportAction.class);

    /**
     * Parent component for dialog display.
     */
    private final JComponent parent;

    /**
     * Construct the import action.
     * @param component The parent component used to display dialogs.
     */
    public ImportAction(JComponent component) {
        super("Importer");
        parent = component;
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new AcceptedFileFilter());
        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (ReefnetFileReader.isReefnetFile(file)) { // We must convert it before using
                try {
                    ReefnetFileReader converter = new ReefnetFileReader(file);
                    converter.convert();
                    file = converter.getCsvTempFile();
                } catch (IOException e) {
                    log.error("Can not convert the ReefNet file, we stop the action.", e);
                    return;
                }
            }
            log.debug("Start reading file " + file.getName());
            try {
                ImportTable.openImportWizardFor(parent, file);
            } catch (IOException e) {
                log.error("Error on file reading", e);
            }
        }
    }
}
