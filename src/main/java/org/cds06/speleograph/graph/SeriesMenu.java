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
import org.cds06.speleograph.actions.SeriesInfoAction;
import org.cds06.speleograph.actions.data.*;
import org.cds06.speleograph.data.Series;
import org.jetbrains.annotations.NonNls;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @SuppressWarnings("UnusedDeclaration")
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(SeriesMenu.class);

    private SpeleoGraphApp application;

    private JMenu menu = new JMenu("S??ries");
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

        menu.add(new AbstractAction() {
            {
                putValue(NAME, "Renommer la s??rie");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                menu.setVisible(false);
                String newName = "";
                while (newName == null || newName.equals("")) {
                    newName = (String) JOptionPane.showInputDialog(
                            application,
                            "Entrez un nouveau nom pour la s??rie",
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

        if (series.hasOwnAxis()) {
            menu.add(new AbstractAction() {

                {
                    putValue(NAME, "Supprimer l'axe sp??cifique");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (JOptionPane.showConfirmDialog(
                            application,
                            "??tes vous s??r de vouloir supprimer cet axe ?",
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
                    putValue(NAME, "Cr??er un axe sp??cifique pour la s??rie");
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

        menu.add(new SetTypeMenu(series));


        if (series.isWater()) {
            menu.addSeparator();
            menu.add(new SumOnPeriodAction(series));
            menu.add(new CreateCumulAction(series));
        }
        if (series.isWaterCumul()) {
            menu.addSeparator();
            menu.add(new SamplingAction(series));
        }

        if (series.isPressure()) {
            menu.addSeparator();
            menu.add(new CorrelateAction(series));
            menu.add(new WaterHeightAction(series));
        }

        menu.addSeparator();

        menu.add(new AbstractAction() {
            {
                String name;
                if (series.canUndo()) name = "Annuler " + series.getItemsName();
                else name = series.getLastUndoName();

                putValue(NAME, name);

                if (series.canUndo())
                    setEnabled(true);
                else {
                    setEnabled(false);
                }

            }
            @Override
            public void actionPerformed(ActionEvent e) {
                series.undo();
            }
        });

        menu.add(new AbstractAction() {
            {
                String name;
                if (series.canRedo()){
                    name = "Refaire " + series.getNextRedoName();
                    setEnabled(true);
                }
                else{
                    name = series.getNextRedoName();
                    setEnabled(false);
                }

                putValue(NAME, name);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                series.redo();
            }
        });

        menu.add(new AbstractAction() {
            {
                putValue(NAME, I18nSupport.translate("menus.serie.resetSerie"));
                if (series.canUndo())
                    setEnabled(true);
                else
                    setEnabled(false);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                series.reset();
            }
        });

        menu.add(new LimitDateRangeAction(series));

        menu.add(new HourSettingAction(series));

        menu.addSeparator();

        {
            JMenuItem deleteItem = new JMenuItem("Supprimer la s??rie");
            deleteItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (JOptionPane.showConfirmDialog(
                            application,
                            "??tes-vous sur de vouloir supprimer cette s??rie ?\n" +
                                    "Cette action est d??finitive.",
                            "Confirmation",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                        series.delete();
                    }
                }
            });
            menu.add(deleteItem);
        }

        menu.addSeparator();

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
            if (series.isFirst()) {
                menu.add(down);
            } else if (series.isLast()) {
                menu.add(up);
            } else {
                menu.add(up);
                menu.add(down);
            }
        }

        menu.addSeparator();

        {
            menu.add(new SeriesInfoAction(series));
        }

        {
            JMenuItem colorItem = new JMenuItem("Couleur de la s??rie");
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
            JMenu plotRenderer = new JMenu("Affichage de la s??rie");
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
                        "??tes-vous sur de vouloir fermer toutes les s??ries du fichier ?",
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
