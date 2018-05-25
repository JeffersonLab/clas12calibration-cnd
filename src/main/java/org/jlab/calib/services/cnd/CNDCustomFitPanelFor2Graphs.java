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
public class CNDCustomFitPanelFor2Graphs extends JPanel {
	
	JTextField[] textFields;

        
	public CNDCustomFitPanelFor2Graphs(String[] fields){
		
        // Initialize the text fields            
        JTextField[] newTextFields = new JTextField[16];
        textFields = newTextFields;
        for (int i = 0; i < 16; i++) {
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
        

        JPanel panelSouthTop = new JPanel();
        JPanel panelSouthMiddle = new JPanel();
        JPanel panelSouth = new JPanel(new BorderLayout());
        panelSouth.add(panelSouthTop, BorderLayout.NORTH);
        panelSouth.add(panelSouthMiddle, BorderLayout.SOUTH);
        add(panelSouth, BorderLayout.SOUTH);        
        
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
        panelNorthBottom.setLayout(new GridLayout(3, 6));
        panelNorthBottom.add(new JLabel());  //Line 0
        panelNorthBottom.add(new JLabel());  //Line 0
        panelNorthBottom.add(new JLabel("xmin", SwingConstants.CENTER));  //Line 0
        panelNorthBottom.add(new JLabel("xmax", SwingConstants.CENTER));  //Line 0            
        panelNorthBottom.add(new JLabel("xmin", SwingConstants.CENTER));  //Line 0
        panelNorthBottom.add(new JLabel("xmax", SwingConstants.CENTER));  //Line 0
        
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
        
        //**********************
        //**********************
        //South
        
        //********
        //SouthTop
        panelSouthTop.setLayout(new GridLayout(4, 1));  
        panelSouthTop.add(new JLabel());  //Line 0
        panelSouthTop.add(new JLabel());  //Line 1
        panelSouthTop.add(new JLabel());  //Line 2
        panelSouthTop.add(new JLabel("Adjust limits for all sectors (supply both min and max): "));  //Line 3
        
        //********        
        //SouthMiddle
        panelSouthMiddle.setLayout(new GridLayout(4, 6));
        panelSouthMiddle.add(new JLabel());  //Line 0
        panelSouthMiddle.add(new JLabel());  //Line 0
        panelSouthMiddle.add(new JLabel("xmin", SwingConstants.CENTER));  //Line 0
        panelSouthMiddle.add(new JLabel("xmax", SwingConstants.CENTER));  //Line 0            
        panelSouthMiddle.add(new JLabel("xmin", SwingConstants.CENTER));  //Line 0
        panelSouthMiddle.add(new JLabel("xmax", SwingConstants.CENTER));  //Line 0  
        
        JLabel labelLayer3 = new JLabel("Layer 3:",SwingConstants.LEFT);
        font = labelLayer3.getFont();
        attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        labelLayer3.setFont(font.deriveFont(attributes));
        
        panelSouthMiddle.add(labelLayer3);  //Line 1
        panelSouthMiddle.add(new JLabel());  //Line 1
        panelSouthMiddle.add(textFields[4]);  //Line 1
        panelSouthMiddle.add(textFields[5]);  //Line 1
        panelSouthMiddle.add(textFields[6]);  //Line 1
        panelSouthMiddle.add(textFields[7]);  //Line 1  
        
        JLabel labelLayer2 = new JLabel("Layer 2:",SwingConstants.LEFT);
        font = labelLayer2.getFont();
        attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        labelLayer2.setFont(font.deriveFont(attributes));        

        panelSouthMiddle.add(labelLayer2);  //Line 2
        panelSouthMiddle.add(new JLabel());  //Line 2
        panelSouthMiddle.add(textFields[8]);  //Line 2
        panelSouthMiddle.add(textFields[9]);  //Line 2
        panelSouthMiddle.add(textFields[10]);  //Line 2
        panelSouthMiddle.add(textFields[11]);  //Line 2   
        
        JLabel labelLayer1 = new JLabel("Layer 1:",SwingConstants.LEFT);
        font = labelLayer1.getFont();
        attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        labelLayer1.setFont(font.deriveFont(attributes));        

        panelSouthMiddle.add(labelLayer1);  //Line 2
        panelSouthMiddle.add(new JLabel());  //Line 2
        panelSouthMiddle.add(textFields[12]);  //Line 2
        panelSouthMiddle.add(textFields[13]);  //Line 2
        panelSouthMiddle.add(textFields[14]);  //Line 2
        panelSouthMiddle.add(textFields[15]);  //Line 2    

        //**********************
        //**********************
        
	}

}
