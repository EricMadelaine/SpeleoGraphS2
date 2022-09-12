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

package org.cds06.speleograph;

import org.cds06.speleograph.actions.*;
import org.cds06.speleograph.actions.data.ImportAction;
import org.cds06.speleograph.data.fileio.*;
import org.cds06.speleograph.graph.EditMenu;
import org.cds06.speleograph.graph.GraphEditor;
import org.cds06.speleograph.graph.SeriesMenu;
import org.cds06.speleograph.utils.About;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.prefs.Preferences;

/**
 * SpeleoGraph Main Frame. This class is the enter point for a graphical utilisation and is the JFrame show. It create
 * all sub-elements for UI like graph, lists, menus ...
 *
 * @author Philippe VIENNE
 */
public class SpeleoGraphApp extends JFrame {
    public static final String APP_VERSION = "S2-V0.9";
    public static final String AUTHORS = "Philippe Vienne, Gabriel Augendre";
    public static final String CONTACT = "speleograph@free.fr";
    public static final String APP_NAME = "SpeleoGraph"; // NON-NLS

    /**
     * Instance of SpeleoGraph in the current JVM.
     */
    private static SpeleoGraphApp instance;

    public static SpeleoGraphApp getInstance() {
        return instance;
    }

    /**
     * Logger for errors and info.
     */
    @SuppressWarnings("UnusedDeclaration")
    @NonNls
    private static Logger log = LoggerFactory.getLogger(SpeleoGraphApp.class);

    /**
     * Main panel for the application.
     */
    private final JPanel panel;
    /*
    MAin panel for the resaerch
    */
    private final JPanel panelV2;

    /**
     * The splitPane to divide space between Graph and Series' List.
     */
    private final JSplitPane splitPane;
    
    private final JTabbedPane onglets;
    
    /**
     * The class who manages menus for Series.
     */
    private final SeriesMenu seriesMenu;

    public SeriesMenu getSeriesMenu() {
        return seriesMenu;
    }

    private final CheckBoxList list;

    public CheckBoxList getSeriesList() {
        return list;
    }

    public SpeleoGraphApp() {
        super(APP_NAME + APP_VERSION); // NON-NLS

        try {
            setIconImage(new ImageIcon(SpeleoGraphApp.class.getResource("SpeleoGraph_icon.png")).getImage()); //NON-NLS
        } catch (Exception e) {
            log.info("No logo for " + APP_NAME);
        }

        // Initialize Graphic elements
        
        panel = new JPanel(new BorderLayout(2, 2));
        panelV2 = new JPanel(new BorderLayout(2, 2));
        onglets = new JTabbedPane();
        onglets.setBounds(40,20,300,300);
        onglets.add("KarstlinkS2",panelV2);
        onglets.add("Spéléograph",panel);
        SpeleoSeriesListModel listModel = new SpeleoSeriesListModel();
        list = new CheckBoxList(listModel);
        JScrollPane scrollPane = new JScrollPane(list);

        JPanel listAndButtons = new JPanel(new BorderLayout(2,2));
        listAndButtons.add(scrollPane, BorderLayout.CENTER);

        //Ajout des boutons de contrôle de sélection (tout cocher/décocher, inverser)
        listAndButtons.add(new SelectButtonsPanel(list), BorderLayout.SOUTH);

        GraphPanel graphPanel = new GraphPanel(this);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, graphPanel, listAndButtons);

        // Configure and add the splitPane
        splitPane.setResizeWeight(1.0);
        panel.add(splitPane, BorderLayout.CENTER);
        
        //construction of the form
        FormPanel formPanel = new FormPanel(this, "");
        panelV2.add(formPanel);
        
        // Configure the frame
        //setContentPane(panel);
        seriesMenu = new SeriesMenu(this);
        final JMenuBar menus = createMenus();
        setJMenuBar(menus);
        // Positioning
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenSize.height = screenSize.height - 100;
        screenSize.width = screenSize.width - 100;
        setSize(screenSize);
        setLocation(50, 50);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        add(onglets);
        
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    /**
     * Create a JMenuBar for this application.
     * @return the JMenuBar containg all the wanted menus.
     */
    public JMenuBar createMenus() {
        final JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu(I18nSupport.translate("menus.file"));
        fileMenu.add(new OpenAction(panel, SpeleoFileReader.class));
        fileMenu.add(new SaveAction(panel));
        JMenu importMenu = new JMenu(I18nSupport.translate("menus.import"));
        importMenu.add(new OpenAction(panel, ReefnetFileReader.class));
        importMenu.add(new OpenAction(panel, ReefnetFileReaderV2.class));
        importMenu.add(new OpenAction(panel, HoboFileReader.class));
        importMenu.add(new OpenAction(panel, WundergroundFileReader.class));
        importMenu.add(new OpenAction(panel, CTDFileReader.class));
        importMenu.addSeparator();
        importMenu.add(new ImportAction(panel));
        fileMenu.add(importMenu);
        fileMenu.add(((GraphPanel) getSplitPane().getLeftComponent()).saveImageAction);
        fileMenu.addSeparator();
        fileMenu.add(new QuitAction(panel, this));
        bar.add(fileMenu);

        bar.add(new EditMenu());

        {
            JMenu menu = new JMenu(I18nSupport.translate("menus.graph"));

            menu.add(new AbstractAction() {
                final GraphEditor editor = new GraphEditor((GraphPanel) getSplitPane().getLeftComponent());

                @Override
                public void actionPerformed(ActionEvent e) {
                    editor.setLocation(
                            SpeleoGraphApp.this.getX() + (SpeleoGraphApp.this.getWidth() / 2 - editor.getWidth() / 2),
                            SpeleoGraphApp.this.getY() + (SpeleoGraphApp.this.getHeight() / 2 - editor.getHeight() / 2)
                    );
                    editor.setVisible(true);
                }

                {
                    putValue(NAME, I18nSupport.translate("menus.graph.graphEditor"));
                }
            });
            menu.add(new ResetAxisAction((GraphPanel) getSplitPane().getLeftComponent()));

            bar.add(menu);
        }

        {
            final JMenu menu = seriesMenu.getMenu();
            menu.setVisible(false);
            bar.add(menu);
        }

        {
            JMenu menu = new JMenu(I18nSupport.translate("menus.help"));
            menu.add(new AbstractAction() {

                {
                    putValue(NAME, I18nSupport.translate("menus.help.about"));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(SpeleoGraphApp.this,
                            new About(APP_NAME, APP_VERSION, AUTHORS, CONTACT, "http://speleograph.free.fr",
                                    I18nSupport.translate("menus.help.about.disclaimer"),
                                    new ImageIcon(SpeleoGraphApp.class.getResource("SpeleoGraph_icon.png")),
                                    new ImageIcon(SpeleoGraphApp.class.getResource("CDS06_icon.png"))),
                            I18nSupport.translate("menus.help.about"), JOptionPane.INFORMATION_MESSAGE, new ImageIcon());
                }
            });
            bar.add(menu);
        }

        return bar;
    }

    /**
     * Detect if we are on a Mac OS X.
     */
    public static boolean isMac() {
        return System.getProperty("os.name").contains("Mac");
    }

    /**
     * Start the application using this function.
     * No arguments are currently read by the program.
     * This function try to use the Nimbus LaF or System if not found.
     *
     * @param args Arguments sent to the JVM (not used)
     */
    @NonNls
    public static void main(String... args) throws IOException {

        if (isMac()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true"); //On déporte la barre de menus
            try { //On essaie de spécifier à Mac que l'image du dock est celle des ressources avec la reflective API
                final Class<?> ApplicationClass =
                        ClassLoader.getSystemClassLoader().loadClass("com.apple.eawt.Application");
                Object app =
                        ApplicationClass.getMethod("getApplication").invoke(null);
                ApplicationClass.getMethod("setDockIconImage", Image.class).invoke(app,
                        new ImageIcon(SpeleoGraphApp.class.getResource("SpeleoGraph_icon.png")).getImage());
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                log.error("We are on mac without Mac support !", e);
            }
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (
                    UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                            IllegalAccessException e2) {
                System.out.println("Leave default Java LookAndFeel"); // NON-NLS
            }
        } else {

            // Setup Look and Feels
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) { // NON-NLS
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                        IllegalAccessException e2) {
                    System.out.println("Leave default Java LookAndFeel"); // NON-NLS
                }
            }
        }
        

        instance = new SpeleoGraphApp();

        // Start application
        instance.setVisible(true);
    }

    /**
     * Open a SpeleoGraph file.
     *
     * @param file The file to open.
     */
    public static void openFile(File file) throws IOException, ParseException {
        try {
            SpeleoFileReader.getInstance().readFile(file);
        } catch (FileReadingError fileReadingError) {
            log.error("Error on file reading", fileReadingError);
        }
    }

    private static Preferences configuration = Preferences.userNodeForPackage(SpeleoGraphApp.class);

    /**
     * Get the location to open in a FileChooser.
     *
     * @return A non-null directory to display to the user.
     */
    public static File getWorkingDirectory() {
        return new File(configuration.get(
                "workingDirectory",   // NON-NLS
                OpenAction.class.getProtectionDomain().getCodeSource().getLocation().getPath()));
    }

    /**
     * Save the working directory.
     *
     * @param dir The directory to register.
     */
    public static void setWorkingDirectory(File dir) {
        configuration.put("workingDirectory", dir.getAbsolutePath()); // NON-NLS
    }
}
