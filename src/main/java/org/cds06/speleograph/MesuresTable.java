/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Léa
 */
@SuppressWarnings("FieldCanBeLocal")
public class MesuresTable extends JPanel{
    
    /**
    * Logger for debug and errors.
    */
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);

    private JTable table;
    
    public MesuresTable(List<Series_de_Mesure> data) 
    {
        //En-têtes pour JTable 
        String[] columns = {"Sélectionner", "Nom de fichier", "Date de début", "Date de fin","Type de mesure", "URL", "Capteur"};
        
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        for(Series_de_Mesure c : data){
            model.addRow(new Object[]{
                    false,
                    c.nom_de_fichier,
                    c.date_debut,
                    c.date_fin,
                    c.mesure,
                    c.url,
                    c.capteur
                });
        }

        table = new JTable(model) {
            public Class getColumnClass(int column) {
              //renvoie Boolean.class
              return getValueAt(0, column).getClass(); 
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        
    }
    
    List get_url_selected(){
        List<String> list = new ArrayList();
        for (int i = 0 ; i<table.getRowCount(); i++){
            Boolean tr = true;
            if(table.getValueAt(i, 0)== tr ){
                list.add(table.getValueAt(i,5).toString());
            }
        }
        return list;
    }
}
