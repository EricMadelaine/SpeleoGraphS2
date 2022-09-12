/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Léa
 */

@SuppressWarnings("FieldCanBeLocal")
public class ComboCheckBox extends JPanel {
    /**
    * Logger for debug and errors.
    */
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);

    public List boxselected;
    
    public ComboCheckBox(){
        
        Vector v = new Vector();
        v.add("Mesures");
        v.add(new JCheckBox("Température",false));
        v.add(new JCheckBox("Conductivité",false));
        v.add(new JCheckBox("Pression",false));
        v.add(new JCheckBox("Pluviométrie",false));
        
        CustomComboCheck myccc = new CustomComboCheck(v);
        add(myccc);
        boxselected = new ArrayList();
        boxselected = myccc.boxselected;
        
    }
}

class CustomComboCheck extends JComboBox {
    /**
    * Logger for debug and errors.
    */
    public List boxselected;
    
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);
    public CustomComboCheck(Vector v){
        super(v);
        boxselected = new ArrayList();
        setRenderer(new Comborenderer());
        
        addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                ourItemSelected();
            }
        });
    }
    
    private void ourItemSelected(){
        Object selected = getSelectedItem();
        if(selected instanceof JCheckBox){
            JCheckBox chk =(JCheckBox) selected;
            chk.setSelected(!chk.isSelected());
            repaint();
            String name = chk.getText();
            Object[] selections = chk.getSelectedObjects();
            if(chk.isSelected()){
                if (!boxselected.contains(name)) {
                    boxselected.add(name);
                }
            }
            else{
                boxselected.remove(name);
            }
            if(selections != null){
                for(Object lastItem : selections){
                    log.debug("je check:" + lastItem.toString());
                }
            }
        }
    }
}

class Comborenderer implements ListCellRenderer{

    private JLabel label;
    
    @Override
    public Component getListCellRendererComponent(JList list, Object val, int index, boolean selected, boolean focused) {
        if(val instanceof Component){
            Component c=(Component) val;
            if(selected){
                c.setBackground(list.getSelectionBackground());
                c.setForeground(list.getSelectionForeground());
            }
            else{
                c.setBackground(list.getBackground());
                c.setForeground(list.getForeground());
            }
            
            return c;
        }
        else{
            if(label ==null){
                label = new JLabel(val.toString());
            }
            else{
                label.setText(val.toString());
            }
            return label;
        }
    }
}