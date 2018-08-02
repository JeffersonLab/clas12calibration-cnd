package org.jlab.calib.services.cnd;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class CNDCustomFitPanelTimeOffsetsLR extends JPanel implements ActionListener {
	
	public JTextField[] textFields;
	public boolean applyToAll = false;

	public CNDCustomFitPanelTimeOffsetsLR(int sector, int layer, int component, String[] fields){
		
        // Initialize the text fields            
        JTextField[] newTextFields = new JTextField[7];
        textFields = newTextFields;
        for (int i = 0; i < 7; i++) {
            textFields[i] = new JTextField(5);
        }

        //Set layout
        this.setLayout(new BorderLayout());

        JPanel panelNorth = new JPanel();
        add(panelNorth, BorderLayout.NORTH);
        JPanel panelSouth = new JPanel();
        add(panelSouth, BorderLayout.SOUTH);
        
        //Add labels and fields
        
        // North
        panelNorth.setLayout(new GridLayout(3, 3)); 
        
        // 1st line
        panelNorth.add(new JLabel());
        JLabel labelLowerLimit = new JLabel(fields[0], SwingConstants.CENTER);
        panelNorth.add(labelLowerLimit);
        JLabel labelUpperLimit = new JLabel(fields[1], SwingConstants.CENTER);                  
        panelNorth.add(labelUpperLimit);        
        // 2nd line        
        JLabel labelLeft = new JLabel("Fit limits:");
        Font font = labelLeft.getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        labelLeft.setFont(font.deriveFont(attributes));        
        panelNorth.add(labelLeft);    
        panelNorth.add(textFields[0]);
        panelNorth.add(textFields[1]);   
        // 3rd line
        panelNorth.add(new JLabel());        
        panelNorth.add(new JLabel());        
        panelNorth.add(new JLabel());        
        
        // South
        panelSouth.setLayout(new GridLayout(7, 2)); 
//
//        // 1st line
        panelSouth.add(new JLabel("Change height of fit"));
        JLabel labelHeightOfFit = new JLabel(fields[2], SwingConstants.CENTER);   
        panelSouth.add(labelHeightOfFit);          
        // 2nd line
        panelSouth.add(new JLabel("Fit height:"));
        panelSouth.add(textFields[3]);     
        // 3rd line
        panelSouth.add(new JLabel());        
        panelSouth.add(new JLabel());   
        // 4th line        
        panelSouth.add(new JLabel("Change height for each layer"));
        panelSouth.add(new JLabel());   
        // 5th line        
        panelSouth.add(new JLabel("Upper: "));
        panelSouth.add(textFields[4]);
        // 6th line              
        panelSouth.add(new JLabel("Middle: "));   
        panelSouth.add(textFields[5]);        
        // 7th line              
        panelSouth.add(new JLabel("Lower: "));   
        panelSouth.add(textFields[6]);   
				
	}

	public void actionPerformed(ActionEvent e) {

//		if (e.getActionCommand().startsWith("Single")) {
//			applyToAll = false;
//		}
//		else if (e.getActionCommand().startsWith("All paddles")) {
//			applyToAll = true;
//		}
		
		
	}	
	
}

