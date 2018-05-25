/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.services.cnd;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


/**
 *
 * @author GM
 */
public class CNDCustomFitPanelFor4Graphs extends JPanel {
	
	JTextField[] textFields;

        
	public CNDCustomFitPanelFor4Graphs(String[] fields){
		
        // Initialize the text fields            
        JTextField[] newTextFields = new JTextField[9];
        textFields = newTextFields;
        for (int i = 0; i < 9; i++) {
            textFields[i] = new JTextField(5);
        }

        //Set layout
        this.setLayout(new BorderLayout());

        //Add labels and fields
        JPanel panelNorthTop = new JPanel();
        JPanel panelNorthBottom = new JPanel();
        JPanel panelNorth = new JPanel(new BorderLayout());
        panelNorth.add(panelNorthTop, BorderLayout.NORTH);
        panelNorth.add(panelNorthBottom, BorderLayout.CENTER);
        add(panelNorth, BorderLayout.NORTH);                
               
        //**********************
        //**********************
        //North
        
        //********
        //NorthTop
        panelNorthTop.setLayout(new GridLayout(1, 3));
        panelNorthTop.add(new JLabel());  //Line 0
        JLabel labelLeft = new JLabel("Left",SwingConstants.CENTER);
        Font font = labelLeft.getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        labelLeft.setFont(font.deriveFont(attributes));
        panelNorthTop.add(labelLeft);  //Line 0
        JLabel labelRight = new JLabel("Right",SwingConstants.CENTER);
        font = labelRight.getFont();
        attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        labelRight.setFont(font.deriveFont(attributes));
        panelNorthTop.add(labelRight);  //Line 0
        
        //********        
        //NorthBottom
        panelNorthBottom.setLayout(new GridLayout(6, 6));
        
        panelNorthBottom.add(new JLabel());  //Line 0
        panelNorthBottom.add(new JLabel());  //Line 0
        panelNorthBottom.add(new JLabel("top graph xmin", SwingConstants.CENTER));  //Line 0
        panelNorthBottom.add(new JLabel("top graph xmax", SwingConstants.CENTER));  //Line 0            
        panelNorthBottom.add(new JLabel("top graph xmin", SwingConstants.CENTER));  //Line 0
        panelNorthBottom.add(new JLabel("top graph xmax", SwingConstants.CENTER));  //Line 0
        
        panelNorthBottom.add(new JLabel());  //Line 1
        panelNorthBottom.add(new JLabel());  //Line 1
        panelNorthBottom.add(new JLabel("(current = "+fields[0]+")", SwingConstants.CENTER));  //Line 1
        panelNorthBottom.add(new JLabel("(current = "+fields[1]+")", SwingConstants.CENTER));  //Line 1            
        panelNorthBottom.add(new JLabel("(current = "+fields[2]+")", SwingConstants.CENTER));  //Line 1
        panelNorthBottom.add(new JLabel("(current = "+fields[3]+")", SwingConstants.CENTER));  //Line 1
        
        JLabel labelCurrentComponent = new JLabel("Current component:",SwingConstants.LEFT);
        font = labelCurrentComponent.getFont();
        attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        labelCurrentComponent.setFont(font.deriveFont(attributes));
        
        panelNorthBottom.add(labelCurrentComponent);  //Line 2
        panelNorthBottom.add(new JLabel());  //Line 2
        panelNorthBottom.add(textFields[0]);  //Line 2
        panelNorthBottom.add(textFields[1]);  //Line 2
        panelNorthBottom.add(textFields[2]);  //Line 2
        panelNorthBottom.add(textFields[3]);  //Line 2
        
        panelNorthBottom.add(new JLabel());  //Line 3
        panelNorthBottom.add(new JLabel());  //Line 3
        panelNorthBottom.add(new JLabel("bottom graph xmin", SwingConstants.CENTER));  //Line 3
        panelNorthBottom.add(new JLabel("bottom graph xmax", SwingConstants.CENTER));  //Line 3            
        panelNorthBottom.add(new JLabel("bottom graph xmin", SwingConstants.CENTER));  //Line 3
        panelNorthBottom.add(new JLabel("bottom graph xmax", SwingConstants.CENTER));  //Line 3
        
        panelNorthBottom.add(new JLabel());  //Line 4
        panelNorthBottom.add(new JLabel());  //Line 4
        panelNorthBottom.add(new JLabel("(current = "+fields[4]+")", SwingConstants.CENTER));  //Line 4
        panelNorthBottom.add(new JLabel("(current = "+fields[5]+")", SwingConstants.CENTER));  //Line 4           
        panelNorthBottom.add(new JLabel("(current = "+fields[6]+")", SwingConstants.CENTER));  //Line 4
        panelNorthBottom.add(new JLabel("(current = "+fields[7]+")", SwingConstants.CENTER));  //Line 4
        
       
        panelNorthBottom.add(new JLabel());  //Line 5
        panelNorthBottom.add(new JLabel());  //Line 5
        panelNorthBottom.add(textFields[4]);  //Line 5
        panelNorthBottom.add(textFields[5]);  //Line 5 
        panelNorthBottom.add(textFields[6]);  //Line 5
        panelNorthBottom.add(textFields[7]);  //Line 5
        

        
	}

}
