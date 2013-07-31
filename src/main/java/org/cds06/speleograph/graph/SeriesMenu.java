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

package org.cds06.speleograph.graph;

import org.apache.commons.lang3.Validate;
import org.cds06.speleograph.I18nSupport;
import org.cds06.speleograph.SpeleoGraphApp;
import org.cds06.speleograph.actions.LimitDateRangeAction;
import org.cds06.speleograph.actions.SamplingAction;
import org.cds06.speleograph.data.Series;
import org.cds06.speleograph.data.Type;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * This class help to create a menu to edit the opened series.
 * <p>It can be used in a {@link javax.swing.JMenuBar} or as a simple {@link javax.swing.JPopupMenu}</p>
 *
 * @author Philippe VIENNE
 * @since 1.0
 */
public class SeriesMenu implements DatasetChangeListener {

    private SpeleoGraphApp application;

    private JMenu menu = new JMenu("Séries");
    private HashMap<Series, JPopupMenu> menus = new HashMap<>(20);
    private List<Series> series = Series.getInstances();

    public SeriesMenu(SpeleoGraphApp app) {
        Validate.notNull(app);
        this.application = app;
        Series.addListener(this);
    }

    /**
     * Receives notification when a Series has been edited or the Series list has changed.
     *
     * @param event information about the event.
     */
    @Override
    public void datasetChanged(DatasetChangeEvent event) {
        menu.removeAll();
        for (Series s : series.toArray(new Series[series.size()])) {
            this.menus.put(s, createPopupMenuForSeries(s));
            JMenu jMenu = new JMenu(s.getName());
            for (Component item : createPopupMenuForSeries(s).getComponents()) {
                if (item instanceof JMenuItem || item instanceof JSeparator) {
                    jMenu.add(item);
                }
            }
            menu.add(jMenu);
        }
        menu.setVisible(menu.getMenuComponentCount() > 0);
    }

    private JPopupMenu createPopupMenuForSeries(final Series series) {

        if (series == null) return new JPopupMenu();

        final JPopupMenu menu = new JPopupMenu(series.getName());

        menu.removeAll();
        final JMenuItem renameItem = new JMenuItem("Renommer la série");
        renameItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                menu.setVisible(false);
                String newName = "";
                while (newName != null && newName.equals("")) {
                    newName = (String) JOptionPane.showInputDialog(
                            application,
                            "Entrez un nouveau nom pour la série",
                            null,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            series.getName()
                    );
                }
                series.setName(newName);

            }
        });
        menu.add(renameItem);

        if (series.hasOwnAxis()) {
            menu.add(new AbstractAction() {

                {
                    putValue(NAME, "Supprimer l'axe spécifique");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (JOptionPane.showConfirmDialog(
                            application,
                            "Êtes vous sûr de vouloir supprimer cet axe ?",
                            "Confirmation",
                            JOptionPane.OK_CANCEL_OPTION
                    ) == JOptionPane.OK_OPTION) {
                        series.setAxis(null);
                    }
                }
            });
        } else {
            menu.add(new JMenuItem(new AbstractAction() {

                {
                    putValue(NAME, "Créer un axe spécifique pour la série");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    String name = JOptionPane.showInputDialog(
                            application,
                            "Quel titre pour cet axe ?",
                            series.getAxis().getLabel());
                    if (name == null || "".equals(name)) return; // User has canceled
                    series.setAxis(new NumberAxis(name));
                }
            }));
        }

        if (series.getType().equals(Type.WATER)) {
            menu.add(new SamplingAction(series));
        }
        menu.add(new LimitDateRangeAction(series));

        {
            JMenuItem samplingItem = new JMenuItem("Supprimer la série");
            samplingItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (JOptionPane.showConfirmDialog(
                            application,
                            "Etes-vous sur de vouloir supprimer cette série",
                            "Confirmation",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                        series.delete();
                    }
                }
            });
            menu.add(samplingItem);
        }

        {
            final JMenuItem up = new JMenuItem("Remonter dans la liste"),
                    down = new JMenuItem("Descendre dans la liste");
            ActionListener listener = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource().equals(up)) {
                        series.upSeriesInList();
                    } else {
                        series.downSeriesInList();
                    }
                }
            };
            up.addActionListener(listener);
            down.addActionListener(listener);
            menu.addSeparator();
            if (series.isFirst()) {
                menu.add(down);
            } else if (series.isLast()) {
                menu.add(up);
            } else {
                menu.add(up);
                menu.add(down);
            }
            menu.addSeparator();
        }

        {
            JMenuItem colorItem = new JMenuItem("Couleur de la série");
            colorItem.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    series.setColor(JColorChooser.showDialog(
                            application,
                            I18nSupport.translate("actions.selectColorForSeries"),
                            series.getColor()));
                }
            });
            menu.add(colorItem);
        }

        {
            JMenu plotRenderer = new JMenu("Affichage de la série");
            final ButtonGroup modes = new ButtonGroup();
            java.util.List<DrawStyle> availableStyles;
            if (series.isMinMax()) {
                availableStyles = DrawStyles.getDrawableStylesForHighLow();
            } else {
                availableStyles = DrawStyles.getDrawableStyles();
            }
            for (final DrawStyle s : availableStyles) {
                final JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                        DrawStyles.getHumanCheckboxText(s)
                );
                item.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        if (item.isSelected())
                            series.setStyle(s);
                    }
                });
                modes.add(item);
                if (s.equals(series.getStyle())) {
                    modes.setSelected(item.getModel(), true);
                }
                plotRenderer.add(item);
            }
            menu.add(plotRenderer);
        }
        menu.addSeparator();
        menu.add(new AbstractAction() {

            {
                putValue(Action.NAME, "Fermer le fichier");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(
                        application,
                        "Etes-vous sur de vouloir fermer toutes les séries du fichier ?",
                        "Confirmation",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                    final File f = series.getOrigin();
                    for (final Series s : Series.getInstances().toArray(new Series[Series.getInstances().size()])) {
                        if (s.getOrigin().equals(f))
                            s.delete();
                    }
                }
            }
        });

        return menu;
    }

    public JMenu getMenu() {
        return menu;
    }

    public JPopupMenu getPopupMenu(Series s) {
        if (!menus.containsKey(s)) {
            createPopupMenuForSeries(s);
        }
        return menus.get(s);
    }
}