package org.jlab.calib.services.cnd;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.tasks.CalibrationEngineView;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.view.DetectorListener;
import org.jlab.detector.view.DetectorPane2D;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

/**
 * CND Calibration suite
 * Based on the work of Louise Clark thanks!
 *
 * @author  Gavin Murdoch
 */


public class CNDCalibration implements IDataEventListener, ActionListener, 
CalibrationConstantsListener, DetectorListener,
ChangeListener {

	// main panel
	JPanel          pane         = null;
	JFrame  innerConfigFrame = new JFrame("Configure CND calibration settings");
	JDialog configFrame = new JDialog(innerConfigFrame, "Configure CND calibration settings");
	JTabbedPane configPane = new JTabbedPane();

	// detector panel
	DetectorPane2D            detectorView        = null;

	// event reading panel
	DataSourceProcessorPane processorPane = null;
	public final int UPDATE_RATE = 10000;

	// calibration view
	EmbeddedCanvas     canvas = null;   
	CalibrationConstantsView ccview = null;

	CNDCalibrationEngine[] engines = {
			new CNDStatusEventListener(),
			new CNDTDCconvEventListener(),
			new CNDTimeOffsetsLREventListener(),
			new CNDEffVEventListener(),
			new CNDUturnTlossEventListener(),
			new CNDAttenuationEventListener(),
			new CNDTimeOffsetslayerEventListener(),
			new CNDTimeOffsetsRFEventListener(),
			new CNDEnergy(),
			new CNDHVSetting(),
			//            new CNDUturntlossEventListener()
			//            new TofTdcConvEventListener(),
			//            new TofLeftRightEventListener(),
			//            new TofVeffEventListener(),
			//            new TofTimeWalkEventListener(),
			//            new TofRFPadEventListener(),
			//            new TofP2PEventListener()
	};

	// engine indices
	public final int STATUS = 0;
	public final int TDC_CONV = 1;
	public final int TIMEOFFSETS_LR = 2;    
	public final int EFFV = 3;
	public final int UTURN_TLOSS = 4;
	public final int ATTENUATION = 5;
	public final int TIME_OFFSET_LAYER = 6;
	public final int TIME_OFFSETS_RF = 7;  //Not used right now though
	public final int ENERGY = 8;  
	//public final int UTURN_ELOSS = 9;  //Not used right now though*
	public final int HV = 9;

	String[] dirs = {"/calibration/cnd/Status_LR",
			"/calibration/cnd/TDC_conv",
			"/calibration/cnd/TimeOffsets_LR",
			"/calibration/cnd/EffV",                     
			"/calibration/cnd/UturnTloss",
			"/calibration/cnd/Attenuation",
			"/calibration/cnd/TimeOffsets_layer",
			"/calibration/cnd/TimeOffsets_RF",
			"/calibration/cnd/Energy",
	"/calibration/cnd/HVSetting"};

	String selectedDir = "None";
	int selectedSector = 1;
	int selectedLayer = 1;
	int selectedComponent = 1;

	String[] buttons = {"View all", "Adjust Fit/Override", "Write"};
	// button indices
	public final int VIEW_ALL = 0;
	public final int FIT_OVERRIDE = 1;
	public final int WRITE = 2;

	// configuration settings
	JCheckBox[] stepChecks = {new JCheckBox(),new JCheckBox(),new JCheckBox(),new JCheckBox(),
			new JCheckBox(),new JCheckBox(),new JCheckBox(),new JCheckBox(),new JCheckBox(),new JCheckBox()};    
	private JTextField targetCCDB = new JTextField(5);
	public static String targetVariation = "default";
	JComboBox<String> hipoVersion = new JComboBox<String>();
	public static int hipoV=0;
	public final static int hipo4 = 0;
	public final static int hipo3 = 1;
	
	//Tracking/General
	JComboBox<String> CVTmatch = new JComboBox<String>();
	public static int CVTmatchI = 0;
	public final static int CVTNO = 0;
	public final static int CVTTRUE = 1;
	private JTextField rcsText = new JTextField(5);
	public static double maxRcs = 0.0;
	private JTextField minVText = new JTextField(5);
	public static double minV = -9999.0;
	private JTextField maxVText = new JTextField(5);
	public static double maxV = 9999.0;
	private JTextField minPText = new JTextField(5);
	public static double minP = 0.0;
	JComboBox<String> trackChargeList = new JComboBox<String>();
	public static int trackCharge = 0;
	public final static int TRACK_BOTH = 0;
	public final static int TRACK_NEG = 1;
	public final static int TRACK_POS = 2;
	JComboBox<String> pidList = new JComboBox<String>();
	public static int trackPid = 2;
	public final static int PID_BOTH = 0;
	public final static int PID_E = 1;
	public final static int PID_PI = 2;
	private JTextField triggerText = new JTextField(10);
	public static int triggerBit = 0;    
	JComboBox<String> counterSectionList = new JComboBox<String>();
	private JTextField sectionWidthText = new JTextField(5);

	//Status
	JComboBox<String> StatusList = new JComboBox<String>();
	private JTextField minTimeText = new JTextField(5);
	private JTextField maxTimeText = new JTextField(5);
	private JTextField binTimeText = new JTextField(5);
	private JTextField minADCText = new JTextField(5);
	private JTextField maxADCText = new JTextField(5);
	private JTextField binADCText = new JTextField(5);
	public static int statusmode = 0;
	public static double mintimestatus = 0.0;
	public static double maxtimestatus = 0.0;
	public static int bintimestatus = 0;
	public static double minADCstatus = 0.0;
	public static double maxADCstatus =  0.0;
	public static int binADCstatus = 0;

	//LRoffset
	JComboBox<String> LRList = new JComboBox<String>();
	private JTextField minTimeLR = new JTextField(5);
	private JTextField maxTimeLR = new JTextField(5);
	private JTextField binTimeLR = new JTextField(5);
	public static int modeLR = 0;
	public static double mintimeLR = 0.0;
	public static double maxtimeLR = 0.0;
	public static int bintimeLR = 0;

	//Eff V
	JComboBox<String> effVFitList = new JComboBox<String>();
	JComboBox<String> effVFitInspect = new JComboBox<String>();
	private JTextField minEffVEventsText = new JTextField(5);
	private JTextField minYaxisVeff = new JTextField(5);
	private JTextField maxYaxisVeff = new JTextField(5);
	private JTextField binYaxisVeff = new JTextField(5);
	private JTextField binXaxisVeff = new JTextField(5);
	public static int modeVeff = 0;
	public static int inspectVeff = 0;
	public static int mineventVeff = 0;
	public static double minYaxisveff = 0.0;
	public static double maxYaxisveff = 0.0;
	public static int binYaxisveff = 0;
	public static int binXaxisveff = 0;

	//Utloss
	JComboBox<String> utFitList = new JComboBox<String>();
	JComboBox<String> utFitInspect = new JComboBox<String>();
	private JTextField utEventsText = new JTextField(5);
	private JTextField minYaxisUt = new JTextField(5);
	private JTextField maxYaxisUt = new JTextField(5);
	private JTextField binYaxisUt = new JTextField(5);
	private JTextField binXaxisUt = new JTextField(5);
	public static int modeut = 0;
	public static int inspectut = 0;
	public static int mineventut = 0;
	public static double minYaxisut = 0.0;
	public static double maxYaxisut = 0.0;
	public static int binYaxisut = 0;
	public static int binXaxisut = 0;

	//attlength
	JComboBox<String> attFitList = new JComboBox<String>();
	JComboBox<String> attFitInspect = new JComboBox<String>();
	private JTextField attEventsText = new JTextField(5);
	private JTextField minYaxisAtt = new JTextField(5);
	private JTextField maxYaxisAtt = new JTextField(5);
	private JTextField binYaxisAtt = new JTextField(5);
	private JTextField binXaxisAtt = new JTextField(5);
	public static int modeatt = 0;
	public static int inspectatt = 0;
	public static int mineventatt = 0;
	public static double minYaxisatt = 0.0;
	public static double maxYaxisatt = 0.0;
	public static int binYaxisatt = 0;
	public static int binXaxisatt = 0;

	//global timeoffset
	private JTextField minTime = new JTextField(5);
	private JTextField maxTime = new JTextField(5);
	private JTextField binTime = new JTextField(5);
	public static double mintime = 0.0;
	public static double maxtime= 0.0;
	public static int bintime= 0;


	//Energy
	JComboBox<String> EFitList = new JComboBox<String>();
	private JTextField mineventE = new JTextField(5);
	private JTextField minYaxisE = new JTextField(5);
	private JTextField maxYaxisE = new JTextField(5);
	private JTextField binYaxisE = new JTextField(5);
	private JTextField binXaxisE1 = new JTextField(5);
	private JTextField minXaxisE = new JTextField(5);
	private JTextField maxXaxisE = new JTextField(5);
	private JTextField binXaxisE = new JTextField(5);
	public static int modee = 0;
	public static int minevente = 0;
	public static double minYaxise = 0.0;
	public static double maxYaxise = 0.0;
	public static int binYaxise = 0;
	public static int binXaxise1 = 0;
	public static double minXaxise = 0.0;
	public static double maxXaxise = 0.0;
	public static int binXaxise = 0;





	public final static PrintStream oldStdout = System.out;

	public CNDCalibration() {

		configFrame.setModalityType(ModalityType.APPLICATION_MODAL);
		configure();

		pane = new JPanel();
		pane.setLayout(new BorderLayout());

		JSplitPane   splitPane = new JSplitPane();

		// combined panel for detector view and button panel
		JPanel combined = new JPanel();
		combined.setLayout(new BorderLayout());

		detectorView = new DetectorPane2D();
		detectorView.getView().addDetectorListener(this);

		JPanel butPanel = new JPanel();
		for (int i=0; i < buttons.length; i++) {
			JButton button = new JButton(buttons[i]);
			button.addActionListener(this);
			butPanel.add(button);            
		}
		combined.add(detectorView, BorderLayout.CENTER);
		combined.add(butPanel,BorderLayout.PAGE_END);

		this.updateDetectorView(true);

		splitPane.setLeftComponent(combined);

		// Create the engine views with this GUI as listener
		JPanel engineView = new JPanel();
		JSplitPane          enginePane = null;
		engineView.setLayout(new BorderLayout());
		enginePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		canvas = new EmbeddedCanvas();
		ccview = new CalibrationConstantsView();
		ccview.getTabbedPane().addChangeListener(this);

		for (int i=0; i < engines.length; i++) {
			if (engines[i].engineOn) {
				ccview.addConstants(engines[i].getCalibrationConstants().get(0),this);
				ccview.getTabbedPane().setEnabled(false);
			}
		}

		enginePane.setTopComponent(canvas);
		enginePane.setBottomComponent(ccview);
		enginePane.setDividerLocation(0.6);
		enginePane.setResizeWeight(0.6);
		engineView.add(splitPane,BorderLayout.CENTER);

		splitPane.setRightComponent(enginePane);
		pane.add(splitPane,BorderLayout.CENTER);

		processorPane = new DataSourceProcessorPane();
		processorPane.setUpdateRate(UPDATE_RATE);

		// only add the gui as listener so that extracting paddle list from event is only done once per event
		this.processorPane.addEventListener(this);

		//        this.processorPane.addEventListener(engines[0]);
		//        this.processorPane.addEventListener(this); // add gui listener second so detector view updates 
		//                                                   // as soon as 1st analyze is done
		//        for (int i=1; i< engines.length; i++) {
		//            this.processorPane.addEventListener(engines[i]);
		//        }
		pane.add(processorPane,BorderLayout.PAGE_END);

		JFrame frame = new JFrame("CND Calibration");
		frame.setSize(1800, 1000);

		frame.add(pane);
		//frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}

	public CNDCalibrationEngine getSelectedEngine() {

		CNDCalibrationEngine engine = null; // = engines[HV];

		//        System.out.println("selectedDir = "+selectedDir);
		//        for (int i = 0; i < dirs.length; i++ ){
		//            System.out.println(dirs[i]);
		//        }

		if (selectedDir == dirs[STATUS]) {
			engine = engines[STATUS];
		} else if (selectedDir == dirs[TDC_CONV]) {
			engine = engines[TDC_CONV];
		} else if (selectedDir == dirs[TIMEOFFSETS_LR]) {
			engine = engines[TIMEOFFSETS_LR];
		} else if (selectedDir == dirs[TIME_OFFSET_LAYER]) {
			engine = engines[TIME_OFFSET_LAYER];
		} else if (selectedDir == dirs[UTURN_TLOSS]) {
			engine = engines[UTURN_TLOSS];
		} else if (selectedDir == dirs[EFFV]) {
			engine = engines[EFFV];
		} else if (selectedDir == dirs[ATTENUATION]) {
			engine = engines[ATTENUATION];
		} else if (selectedDir == dirs[TIME_OFFSETS_RF]) {
			engine = engines[TIME_OFFSETS_RF];
		} else if (selectedDir == dirs[ENERGY]) {
			engine = engines[ENERGY];
		} else if (selectedDir == dirs[HV]) {
			engine = engines[HV];
			//        } else if (selectedDir == dirs[UTURN_ELOSS]) {
			//            engine = engines[UTURN_ELOSS];
		} else {
			System.out.println("NO TAB MATCHED???!!!"+selectedDir);            
		}
		return engine;
	}    

	public void actionPerformed(ActionEvent e) {

		CNDCalibrationEngine engine = getSelectedEngine();

		if (e.getActionCommand().compareTo(buttons[VIEW_ALL])==0) {

			engine.showPlots(selectedSector, selectedLayer);

		}
		else if (e.getActionCommand().compareTo(buttons[FIT_OVERRIDE])==0) {

			engine.customFit(selectedSector, selectedLayer, selectedComponent);
			updateDetectorView(false);
			this.updateCanvas();
		}

		else if (e.getActionCommand().compareTo(buttons[WRITE])==0) {

			String outputFilename = engine.nextFileName();
			engine.writeFile(outputFilename);
			JOptionPane.showMessageDialog(new JPanel(),
					engine.stepName + " calibration values written to "+outputFilename);
		}

		// config settings
		//        if (e.getSource() == stepChecks[TDC_CONV]) {
		//            configPane.setEnabledAt(3, stepChecks[TDC_CONV].isSelected());
		//        }
		//        if (e.getSource() == stepChecks[TW]) {
		//            configPane.setEnabledAt(4, stepChecks[TW].isSelected());
		//        }
		if (e.getActionCommand().compareTo("Next")==0) {
			int currentTab = configPane.getSelectedIndex();
			for (int i=currentTab+1; i<configPane.getTabCount(); i++) {
				if (configPane.isEnabledAt(i)) {
					configPane.setSelectedIndex(i);
					break;
				}
			}
		}
		if (e.getActionCommand().compareTo("Back")==0) {
			int currentTab = configPane.getSelectedIndex();
			for (int i=currentTab-1; i>=0; i--) {
				if (configPane.isEnabledAt(i)) {
					configPane.setSelectedIndex(i);
					break;
				}
			}        
		}
		if (e.getActionCommand().compareTo("Cancel")==0) {
			System.exit(0);
		}

		if (e.getActionCommand().compareTo("Finish")==0) {
			configFrame.setVisible(false);

			System.out.println("");
			System.out.println(todayString());
			System.out.println("Configuration settings - Selected steps");
			System.out.println("---------------------------------------");
			// step selection
			for (int i=0; i < engines.length; i++) {
				engines[i].engineOn = stepChecks[i].isSelected();
				if (selectedDir.compareTo("None")==0 && engines[i].engineOn) {
					selectedDir = dirs[i];
				}
				System.out.println(engines[i].stepName+" "+engines[i].engineOn);
			}

			System.out.println("");
			System.out.println("Configuration settings - Previous calibration values");
			System.out.println("----------------------------------------------------");
			// get the previous iteration calibration values
			for (int i=0; i< engines.length; i++) {
				engines[i].populatePrevCalib();

				if (!engines[i].prevCalRead) {
					System.out.println("Problem populating "+engines[i].stepName+" previous calibration values - exiting");
					System.exit(0);
				}
			}

			// set the config values tracking
			hipoV = hipoVersion.getSelectedIndex();
			System.out.println("HIPO V "+hipoV);
			if (targetCCDB.getText().compareTo("default") != 0) {
				targetVariation = targetCCDB.getText();
			}
			
			if (rcsText.getText().compareTo("") != 0) {
				maxRcs = Double.parseDouble(rcsText.getText());
			}
			if (minVText.getText().compareTo("") != 0) {
				minV = Double.parseDouble(minVText.getText());
			}
			if (maxVText.getText().compareTo("") != 0) {
				maxV = Double.parseDouble(maxVText.getText());
			}    
			if (minPText.getText().compareTo("") != 0) {
				minP = Double.parseDouble(minPText.getText());
			}
			trackCharge = trackChargeList.getSelectedIndex();
			trackPid = pidList.getSelectedIndex();
			CVTmatchI = CVTmatch.getSelectedIndex();
			if (triggerText.getText().compareTo("") != 0) {
				triggerBit = Integer.parseInt(triggerText.getText());
			}

			//Status
			statusmode=StatusList.getSelectedIndex();
			if (minTimeText.getText().compareTo("") != 0) {
				mintimestatus =  Double.parseDouble(minTimeText.getText()); 
			}
			if (maxTimeText.getText().compareTo("") != 0) {
				maxtimestatus =  Double.parseDouble(maxTimeText.getText()); 
			}
			if (binTimeText.getText().compareTo("") != 0) {
				bintimestatus =  Integer.parseInt(binTimeText.getText()); 
			}
			if (minADCText.getText().compareTo("") != 0) {
				minADCstatus =  Double.parseDouble(minADCText.getText()); 
			}
			if (maxADCText.getText().compareTo("") != 0) {
				maxADCstatus =  Double.parseDouble(maxADCText.getText()); 
			}
			if (binADCText.getText().compareTo("") != 0) {
				binADCstatus =  Integer.parseInt(binADCText.getText()); 
			}

			//LR offset
			modeLR = LRList.getSelectedIndex();
			if (minTimeLR.getText().compareTo("") != 0) {
				mintimeLR =  Double.parseDouble(minTimeLR.getText()); 
			}
			if (maxTimeLR.getText().compareTo("") != 0) {
				maxtimeLR =  Double.parseDouble(maxTimeLR.getText()); 
			}
			if (binTimeLR.getText().compareTo("") != 0) {
				bintimeLR =  Integer.parseInt(binTimeLR.getText()); 
			}


			// veff
			modeVeff = effVFitList.getSelectedIndex();
			inspectVeff = effVFitInspect.getSelectedIndex();
			if (minEffVEventsText.getText().compareTo("") != 0) {
				mineventVeff = Integer.parseInt(minEffVEventsText.getText());
			}
			if (minYaxisVeff.getText().compareTo("") != 0) {
				minYaxisveff = Double.parseDouble(minYaxisVeff.getText());
			}
			if (maxYaxisVeff.getText().compareTo("") != 0) {
				maxYaxisveff = Double.parseDouble(maxYaxisVeff.getText());
			}
			if (binYaxisVeff.getText().compareTo("") != 0) {
				binYaxisveff =Integer.parseInt(binYaxisVeff.getText());
			}
			if (binXaxisVeff.getText().compareTo("") != 0) {
				binXaxisveff = Integer.parseInt(binXaxisVeff.getText());
			}

			// utloss
			modeut = utFitList.getSelectedIndex();
			inspectut = utFitInspect.getSelectedIndex();
			if (utEventsText.getText().compareTo("") != 0) {
				mineventut = Integer.parseInt(utEventsText.getText());
			}
			if (minYaxisUt.getText().compareTo("") != 0) {
				minYaxisut = Double.parseDouble(minYaxisUt.getText());
			}
			if (maxYaxisUt.getText().compareTo("") != 0) {
				maxYaxisut = Double.parseDouble(maxYaxisUt.getText());
			}
			if (binYaxisUt.getText().compareTo("") != 0) {
				binYaxisut = Integer.parseInt(binYaxisUt.getText());
			}
			if (binXaxisUt.getText().compareTo("") != 0) {
				binXaxisut = Integer.parseInt(binXaxisUt.getText());
			}


			//ATT length
			modeatt = attFitList.getSelectedIndex();
			inspectatt = attFitInspect.getSelectedIndex();
			if (attEventsText.getText().compareTo("") != 0) {
				mineventatt = Integer.parseInt(attEventsText.getText());
			}
			if (minYaxisAtt.getText().compareTo("") != 0) {
				minYaxisatt = Double.parseDouble(minYaxisAtt.getText());
			}
			if (maxYaxisAtt.getText().compareTo("") != 0) {
				maxYaxisatt = Double.parseDouble(maxYaxisAtt.getText());
			}
			if (binYaxisAtt.getText().compareTo("") != 0) {
				binYaxisatt = Integer.parseInt(binYaxisAtt.getText());
			}
			if (binXaxisAtt.getText().compareTo("") != 0) {
				binXaxisatt = Integer.parseInt(binXaxisAtt.getText());
			}


			//time offset
			if (minTime.getText().compareTo("") != 0) {
				mintime = Double.parseDouble(minTime.getText());
			}
			if (maxTime.getText().compareTo("") != 0) {
				maxtime = Double.parseDouble(maxTime.getText());
			}
			if (binTime.getText().compareTo("") != 0) {
				bintime = Integer.parseInt(binTime.getText());
			}

			//energy
			modee = EFitList.getSelectedIndex();
			if (mineventE.getText().compareTo("") != 0) {
				minevente = Integer.parseInt(mineventE.getText());
			}
			if (minYaxisE.getText().compareTo("") != 0) {
				minYaxise = Double.parseDouble(minYaxisE.getText());
			}
			if (maxYaxisE.getText().compareTo("") != 0) {
				maxYaxise = Double.parseDouble(maxYaxisE.getText());
			}
			if (binYaxisE.getText().compareTo("") != 0) {
				binYaxise = Integer.parseInt(binYaxisE.getText());
			}
			if (minXaxisE.getText().compareTo("") != 0) {
				minXaxise = Double.parseDouble(minXaxisE.getText());
			}
			if (maxXaxisE.getText().compareTo("") != 0) {
				maxXaxise = Double.parseDouble(maxXaxisE.getText());
			}
			if (binXaxisE.getText().compareTo("") != 0) {
				binXaxise = Integer.parseInt(binXaxisE.getText());
			}
			if (binXaxisE1.getText().compareTo("") != 0) {
				binXaxise1 = Integer.parseInt(binXaxisE1.getText());
			}



			//
			//            engines[TDC_CONV].fitMethod = tdcFitList.getSelectedIndex();
			//            engines[TDC_CONV].fitMode = (String) tdcFitModeList.getSelectedItem();
			//            if (minTDCEventsText.getText().compareTo("") != 0) {
			//                engines[TDC_CONV].fitMinEvents = Integer.parseInt(minTDCEventsText.getText());
			//            }

			//            System.out.println("");
			//            System.out.println("Configuration settings - Tracking/General");
			//            System.out.println("-----------------------------------------");
			//            System.out.println("Maximum reduced chi squared for tracks: "+maxRcs);
			//            System.out.println("Minimum vertex z: "+minV);
			//            System.out.println("Maximum vertex z: "+maxV);
			//            System.out.println("Minimum momentum from tracking (GeV): "+minP);
			//            System.out.println("Track charge: "+trackChargeList.getItemAt(trackCharge));
			//            System.out.println("PID: "+pidList.getItemAt(trackPid));
			//            System.out.println("Trigger: "+triggerBit);
			//            System.out.println("Counter section: "+counterSectionList.getSelectedItem());
			//            System.out.println("Width of section (cm): "+engines[TW].sectionWidth);
			//            
			//            
			//			System.out.println("");
			//			System.out.println("Configuration settings - Attenuation length");
			//			System.out.println("-------------------------------------------");
			//			System.out.println("Attenuation length graph: "+attenFitList.getSelectedItem());
			//			System.out.println("Attenuation length slicefitter mode: "+engines[ATTENUATION].fitMode);
			//			System.out.println("Attenuation length minimum events per slice: "+engines[ATTENUATION].fitMinEvents);
			//            System.out.println("");
			//            System.out.println("Configuration settings - TDC conversion");
			//            System.out.println("---------------------------------------");
			//            System.out.println("TDC graph: "+tdcFitList.getSelectedItem());
			//            System.out.println("TDC slicefitter mode: "+engines[TDC_CONV].fitMode);
			//            System.out.println("TDC minimum events per slice: "+engines[TDC_CONV].fitMinEvents);
			System.out.println("");
			System.out.println("Configuration settings - Effective velocity");
			System.out.println("-------------------------------------------");
			System.out.println("Effective velocity graph: "+effVFitList.getSelectedItem());
			System.out.println("Effective velocity slicefitter mode: "+engines[EFFV].fitMode);
			System.out.println("Effective velocity minimum events per slice: "+engines[EFFV].fitMinEvents);
			System.out.println("");
			//			System.out.println("Configuration settings - Uturn Tloss");
			//			System.out.println("-------------------------------------------");
			//			System.out.println("Uturn Tloss graph: "+uturnTlossFitList.getSelectedItem());
			//			System.out.println("Uturn Tloss slicefitter mode: "+engines[UTURN_TLOSS].fitMode);
			//			System.out.println("Uturn Tloss minimum events per slice: "+engines[UTURN_TLOSS].fitMinEvents);
			//			System.out.println("");
			//			System.out.println("Configuration settings - Energy");
			//			System.out.println("-------------------------------------------");
			//			System.out.println("Energy graph: "+energyFitList.getSelectedItem());
			//			System.out.println("Energy slicefitter mode: "+engines[ENERGY].fitMode);
			//			System.out.println("Energy minimum events per slice: "+engines[ENERGY].fitMinEvents);
			//            System.out.println("");
			//            System.out.println("Configuration settings - Time walk");
			//            System.out.println("----------------------------------");
			//            System.out.println("Time walk graph: "+twFitList.getSelectedItem());
			//            System.out.println("Time walk slicefitter mode: "+engines[TW].fitMode);
			//            System.out.println("Time walk minimum events per slice: "+engines[TW].fitMinEvents);
			//            System.out.println("");
		}

	}

	public void dataEventAction(DataEvent event) {

		//DataProvider dp = new DataProvider();      
		List<CNDPaddlePair> paddlePairList = DataProvider.getPaddlePairList(event);


		for (int i=0; i< engines.length; i++) {

			if (engines[i].engineOn) {

				if (event.getType()==DataEventType.EVENT_START) {
					engines[i].resetEventListener();
					engines[i].processPaddlePairList(paddlePairList);

				}
				else if (event.getType()==DataEventType.EVENT_ACCUMULATE) {
					engines[i].processPaddlePairList(paddlePairList);
				}
				else if (event.getType()==DataEventType.EVENT_STOP) {
					System.setOut(oldStdout);
					System.out.println("EVENT_STOP for "+engines[i].stepName+" "+todayString());
					engines[i].analyze();
					ccview.getTabbedPane().setEnabled(true);
					//System.setOut(oldStdout);
					this.updateDetectorView(false);
					this.updateCanvas();
					System.out.println("NUM_PROCESSED = " + CNDStatusEventListener.NUM_PROCESSED);
				} 

				//				if (event.getType()==DataEventType.EVENT_STOP) {
				//					this.updateDetectorView(false);
				//					this.updateCanvas();
				//					System.out.println("NUM_PROCESSED = " + CNDStatusEventListener.NUM_PROCESSED);
				//
				//					//if (i==0) this.showTestHists();
				//				}
			}
		}
	}

	private String todayString() {
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
		String todayString = dateFormat.format(today);

		return todayString;
	}

	public void resetEventListener() {

	}

	public void timerUpdate() {

		for (int i=0; i< engines.length; i++) {
			if (engines[i].engineOn && i!=HV) { // 		 	fit for HV only when file is done
				engines[i].timerUpdate();
				ccview.getTabbedPane().setEnabled(true);
			}
		}


		this.updateDetectorView(false);
		//this.updateCanvas();
	}

	public final void updateDetectorView(boolean isNew){

		//        System.out.println(isNew);        

		CNDCalibrationEngine engine = getSelectedEngine();

		//****************
		//Geometry is 24 sectors, 3 layers, 1 component (consisting of a coupled pair of paddles)
		//****************        
		double phi_slice = 7.5;    // degrees corresponding to single pair
		double phi_first =   - 180.;  // start of first paddle pair at 9 o'clock looking downstream
		double r_inner = 30.;      // inner radius of inner layer (cm)
		double thickness = 3.;     // thickness of each paddle (cm)

		double radius_in = 0.;
		double radius_out = 0.;
		double phi_start = 0.;
		double phi_end = 0.;

		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {

				int component = 1;

				int sector_index = sector - 1;
				int layer_index = layer - 1;

				radius_in = r_inner + ((layer_index) * thickness);
				radius_out = radius_in + thickness;

				phi_start = phi_first + ((2*phi_slice) * (sector_index));
				phi_end = phi_start + (2*phi_slice);   

				//Subtract a small value from start and end:
				phi_start = phi_start + (phi_slice/10.);   
				phi_end = phi_end - (phi_slice/10.);                           

				DetectorShape2D shape = new DetectorShape2D();
				shape.getDescriptor().setType(DetectorType.CND);
				shape.getDescriptor().setSectorLayerComponent(sector, layer, component);

				shape.createArc(radius_in, radius_out, phi_start, phi_end);


				//**** This crashes when switching tab!!!**
				if (!isNew) {
					//    System.out.println("updateDetectorView for "+ engine.stepName);
					if (engine.isGoodComponent(sector, layer, component)) {
						shape.setColor(101,200,59); //green
					}
					else {
						shape.setColor(225,75,60); //red
					}
				}


				detectorView.getView().addShape("CND", shape);

			}
		}

		//        System.out.println("END3 LOOP ");
		if (isNew) {
			detectorView.updateBox();
		}
		//        System.out.println("before repaint");
		detectorView.repaint();
		//        System.out.println("after repaint");

	}



	//        double FTOFSize = 500.0;
	//        int[]     npaddles = new int[]{23,62,5};
	//        int[]     widths   = new int[]{6,15,25};
	//        int[]     lengths  = new int[]{6,15,25};
	//
	//        String[]  names    = new String[]{"FTOF 1A","FTOF 1B","FTOF 2"};
	//        for(int sector = 1; sector <= 6; sector++){
	//            double rotation = Math.toRadians((sector-1)*(360.0/6)+90.0);
	//
	//            for(int layer = 1; layer <=3; layer++){
	//
	//                int width  = widths[layer-1];
	//                int length = lengths[layer-1];
	//
	//                for(int paddle = 1; paddle <= npaddles[layer-1]; paddle++){
	//
	//                    DetectorShape2D shape = new DetectorShape2D();
	//                    shape.getDescriptor().setType(DetectorType.FTOF);
	//                    shape.getDescriptor().setSectorLayerComponent(sector, layer, paddle);
	//                    shape.createBarXY(20 + length*paddle, width);
	//                    shape.getShapePath().translateXYZ(0.0, 40 + width*paddle , 0.0);
	//                    shape.getShapePath().rotateZ(rotation);
	//                    if (!isNew) {
	//                        if (engine.isGoodPaddle(sector, layer, paddle)) {
	//                            shape.setColor(101,200,59); //green
	//                        }
	//                        else {
	//                            shape.setColor(225,75,60); //red
	//                        }
	//                    }    
	//                    detectorView.getView().addShape(names[layer-1], shape);
	//                }
	//            }
	//        }
	//
	//        if (isNew) {
	//            detectorView.updateBox();
	//        }
	//        detectorView.repaint();
	//
	//    }

	public JPanel  getPanel(){
		return pane;
	}

	public void constantsEvent(CalibrationConstants cc, int col, int row) {

		System.out.println("----------in constantsEvent----------- for "+cc.getName());

		String str_sector    = (String) cc.getValueAt(row, 0);
		String str_layer     = (String) cc.getValueAt(row, 1);
		String str_component = (String) cc.getValueAt(row, 2);

		if (cc.getName() != selectedDir) {
			selectedDir = cc.getName();
			this.updateDetectorView(false);
		}

		selectedSector    = Integer.parseInt(str_sector);
		selectedLayer     = Integer.parseInt(str_layer);
		selectedComponent = Integer.parseInt(str_component);

		updateCanvas();
	}

	public void updateCanvas() {

		System.out.println(" In CNDCalibration.updateCanvas()");

		IndexedList<DataGroup> group = getSelectedEngine().getDataGroup();

		getSelectedEngine().setPlotTitle(selectedSector,selectedLayer,selectedComponent);


		if(group.hasItem(selectedSector,selectedLayer,selectedComponent)==true){
			DataGroup dataGroup = group.getItem(selectedSector,selectedLayer,selectedComponent);
			this.canvas.clear();
			this.canvas.draw(dataGroup);
			getSelectedEngine().rescaleGraphs(canvas, selectedSector, selectedLayer, selectedComponent);
			//            System.out.println("canvas.getNColumns() = " + canvas.getNColumns());
			//            System.out.println("canvas.getNRows() = " + canvas.getNRows());

			//            canvas.getPad(0).setTitle("Sector "+selectedSector+" Layer "+selectedLayer+" Component "+selectedComponent);
			this.canvas.update();
		} else {
			System.out.println(" ERROR: can not find the data group");
		}

		System.out.println(" Out of CNDCalibration.updateCanvas()");

	}

	public void processShape(DetectorShape2D shape) {

		//        System.out.println(" ** In CNDCalibration.processShape()");

		// show summary
		selectedSector = shape.getDescriptor().getSector();
		selectedLayer = shape.getDescriptor().getLayer();
		selectedComponent = shape.getDescriptor().getComponent();


		this.canvas.clear();
		//this.canvas.draw(getSelectedEngine().getSummary(selectedSector, selectedLayer));
		this.canvas.draw(getSelectedEngine().getSummary());

		System.out.println(" here now!");

		//        canvas.getPad(0).setTitle("Calibration values for Sector "+selectedSector);
		System.out.println(" made it to here now!");
		this.canvas.update();
		//        System.out.println(" updated the canvas in processShape...");        

	}

	public void stateChanged(ChangeEvent e) {

		System.out.println(" In stateChanged()!!!!!!");

		int i = ccview.getTabbedPane().getSelectedIndex();
		String tabTitle = ccview.getTabbedPane().getTitleAt(i);

		System.out.println("    tabTitle= "+tabTitle);
		System.out.println("    selectedDir= "+selectedDir);


		if (tabTitle != selectedDir) {
			selectedDir = tabTitle;
			this.updateDetectorView(false);
			this.updateCanvas();
		}
	}

	//    public void showTestHists() {
	//        JFrame frame = new JFrame("Track Reduced Chi Squared");
	//        frame.setSize(1000, 800);
	//        EmbeddedCanvas canvas = new EmbeddedCanvas();
	//        canvas.cd(0);
	//        canvas.draw(trackRCS);
	//        frame.add(canvas);
	//        frame.setVisible(true);
	//        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	//        
	//        JFrame frame2 = new JFrame("Track Reduced Chi Squared");
	//        frame2.setSize(1000, 800);
	//        EmbeddedCanvas canvas2 = new EmbeddedCanvas();
	//        canvas2.cd(0);
	//        canvas2.draw(trackRCS2);
	//        frame2.add(canvas2);
	//        frame2.setVisible(true);
	//        frame2.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	//        
	//        JFrame frame3 = new JFrame("Vertex z");
	//        frame3.setSize(1000, 800);
	//        EmbeddedCanvas canvas3 = new EmbeddedCanvas();
	//        canvas3.cd(0);
	//        canvas3.draw(vertexHist);
	//        frame3.add(canvas3);
	//        frame3.setVisible(true);
	//        frame3.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	//
	//    }

	public void configure() {

		configFrame.setSize(1000, 650);

		//configFrame.setSize(1000, 600);
		configFrame.setLocationRelativeTo(pane);
		//configFrame.setDefaultCloseOperation(configFrame.DO_NOTHING_ON_CLOSE);

		// Tracking options
		JPanel OuterPanel = new JPanel(new BorderLayout());
		JPanel Panel = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		OuterPanel.add(Panel, BorderLayout.NORTH);
		c1.weighty = 1;
		c1.anchor = c1.NORTHWEST;
		c1.insets = new Insets(3,3,3,3);
		c1.gridx = 0;
		c1.gridy = 0;
		JLabel welcome= new JLabel("Welcome to the CND Calibration Suite");
		welcome.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		Border border = BorderFactory.createLineBorder(Color.BLUE, 2);
		welcome.setFont(new java.awt.Font("Times New Roman", 1, 35));
		welcome.setBorder(border);
		welcome.setSize(600, 200);
		Panel.add(welcome,c1);
		c1.gridx = 0;
		c1.gridy = 1;
		JLabel s= new JLabel("");
		Panel.add(s,c1);
		c1.gridx = 0;
		c1.gridy = 2;
		JLabel intro= new JLabel("In this window you can choose which constant you want to calibrate and several options to do it:");
		Panel.add(intro,c1);
		c1.gridx = 0;
		c1.gridy = 3;
		JLabel intro1= new JLabel("- Click on \"Next\" to go to the next tab. Once you reach the last tab, click on \"Finish\"");
		Panel.add(intro1,c1);
		c1.gridx = 0;
		c1.gridy = 4;
		JLabel intro2= new JLabel("- In \"Select Step\" choose the calibration you want to perform");
		Panel.add(intro2,c1);
		c1.gridx = 0;
		c1.gridy = 5;
		JLabel intro3= new JLabel("- In \"Previous calibration values\" load calibration constants as necessary");
		Panel.add(intro3,c1);
		c1.gridx = 0;
		c1.gridy = 6;
		JLabel intro4= new JLabel("- In \"Tracking / General\" add some cuts on the match CVT tracks. WARNING these cuts are used for every calibration step.");
		Panel.add(intro4,c1);
		c1.gridx = 0;
		c1.gridy = 7;
		JLabel intro5= new JLabel("- Then browse each tab to add option as desired");
		Panel.add(intro5,c1);
		c1.gridx = 0;
		c1.gridy = 8;
		JLabel intro6= new JLabel("- To leave the suite click \"Cancel\" or the normal exit button");
		Panel.add(intro6,c1);

		JPanel butPage = new configButtonPanel(this, false, "Next");
		OuterPanel.add(butPage, BorderLayout.SOUTH);

		configPane.add("CND Calibration Suite", OuterPanel);

		// Which steps    
		JPanel stepOuterPanel = new JPanel(new BorderLayout());
		JPanel stepPanel = new JPanel(new GridBagLayout());
		stepOuterPanel.add(stepPanel, BorderLayout.NORTH);
		GridBagConstraints c = new GridBagConstraints();

		int gridy=0;
		for (int i=0; i< engines.length; i++) {
			if(i== HV || i== TIME_OFFSETS_RF || i== TDC_CONV || i== UTURN_TLOSS || i== ATTENUATION)continue; //added exceptions for attenuation and LR_offset --PN
				c.gridx = 0; c.gridy = gridy;
				gridy++;
				c.anchor = c.WEST;
				stepChecks[i].setName(engines[i].stepName);
				stepChecks[i].setText(engines[i].stepName);
				stepChecks[i].setSelected(false);
				stepChecks[i].addActionListener(this);
				stepPanel.add(stepChecks[i],c);
			
		}
		c.gridx = 0; c.gridy = 20;
		stepPanel.add(new JLabel("Target CCDB"),c);
		targetCCDB.addActionListener(this);
		Border borderCCDB = BorderFactory.createLineBorder(Color.BLUE, 2);
		targetCCDB.setBorder(border);
		targetCCDB.setText("default");
		c.gridx = 1; c.gridy = 20;
		stepPanel.add(targetCCDB,c);
		// hipo version
				c.gridx = 0;
				c.gridy = 21;
				stepPanel.add(new JLabel("Hipo version:"),c);
				hipoVersion.addItem("hipo 4");
				hipoVersion.addItem("hipo 3");
				hipoVersion.addActionListener(this);
				c.gridx = 1;
				c.gridy = 21;
				stepPanel.add(hipoVersion,c);

		/*c.gridx = 0;
		c.gridy = 1;
		trPanel.add(new JLabel("Maximum reduced chi squared for track:"),c);
		rcsText.addActionListener(this);
		rcsText.setText("75");*/
		
		JPanel butPage1 = new configButtonPanel(this, true, "Next");
		stepOuterPanel.add(butPage1, BorderLayout.SOUTH);

		
		
		configPane.add("Select Step", stepOuterPanel);    

		// Previous calibration values
		JPanel confOuterPanel = new JPanel(new BorderLayout());
		Box confPanel = new Box(BoxLayout.Y_AXIS);
		CNDPrevConfigPanel[] engPanels = {new CNDPrevConfigPanel(new CNDCalibrationEngine()), 
				new CNDPrevConfigPanel(new CNDCalibrationEngine()), 
				new CNDPrevConfigPanel(new CNDCalibrationEngine()),
				new CNDPrevConfigPanel(new CNDCalibrationEngine()),
				new CNDPrevConfigPanel(new CNDCalibrationEngine()),
				new CNDPrevConfigPanel(new CNDCalibrationEngine()),
				new CNDPrevConfigPanel(new CNDCalibrationEngine()),
				new CNDPrevConfigPanel(new CNDCalibrationEngine())};

		//    public final int STATUS = 0;
		//    public final int TDC_CONV = 1;
		//    public final int TIMEOFFSETS_LR = 2;    
		//    public final int EFFV = 3;
		//    public final int UTURN_TLOSS = 4;
		//    public final int ATTENUATION = 5;
		//    public final int TIME_OFFSET_LAYER = 6;
		//    public final int TIME_OFFSET_LAYER = 7;
		//    public final int ENERGY = 8;
		//    public final int UTURN_ELOSS = 9;        

		for (int i=2; i< 5; i++) {
			engPanels[i-2] = new CNDPrevConfigPanel(engines[i]);
			confPanel.add(engPanels[i-2]);
		}

		/* YOU ARE HERE!
		 * PLAY AROUND WITH THIS LINE FROM CNDPrevConfigPanel:
		 * this.setBorder(BorderFactory.createTitledBorder(engine.stepName))
		 * CAN THIS BE USED TO MANUALLY SET THE LABLES ON THESE BORDERS? */
		
		//    Previously:
		//        for (int i=3; i< engines.length; i++) {
		//            engPanels[i-3] = new CNDPrevConfigPanel(engines[i]);
		//            confPanel.add(engPanels[i-3]);
		//        }

		JPanel butPage2 = new configButtonPanel(this, true, "Next");
		confOuterPanel.add(confPanel, BorderLayout.NORTH);
		confOuterPanel.add(butPage2, BorderLayout.SOUTH);

		configPane.add("Previous Calibration Values", confOuterPanel);

		// Tracking options
		JPanel trOuterPanel = new JPanel(new BorderLayout());
		JPanel trPanel = new JPanel(new GridBagLayout());
		trOuterPanel.add(trPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		//CVT match
		c.gridx = 0;
		c.gridy = 0;
		trPanel.add(new JLabel("Hits with CVT track matched only:"),c);
		CVTmatch.addItem("No");
		CVTmatch.addItem("Yes");
		CVTmatch.addActionListener(this);
		c.gridx = 1;
		c.gridy = 0;
		trPanel.add(CVTmatch,c);
		c.gridx = 2;
		c.gridy = 0;
		// Chi squared
		c.gridx = 0;
		c.gridy = 1;
		trPanel.add(new JLabel("Maximum reduced chi squared for track:"),c);
		rcsText.addActionListener(this);
		rcsText.setText("75");
		c.gridx = 1;
		c.gridy = 1;
		trPanel.add(rcsText,c);
		c.gridx = 2;
		c.gridy = 1;
		// vertex min
		c.gridx = 0;
		c.gridy = 2;
		trPanel.add(new JLabel("Minimum vertex z:"),c);
		minVText.addActionListener(this);
		minVText.setText("-10");
		c.gridx = 1;
		c.gridy = 2;
		trPanel.add(minVText,c);
		// vertex max
		c.gridx = 0;
		c.gridy = 3;
		trPanel.add(new JLabel("Maximum vertex z:"),c);
		maxVText.addActionListener(this);
		maxVText.setText("10");
		c.gridx = 1;
		c.gridy = 3;
		trPanel.add(maxVText,c);
		// p min
		c.gridx = 0;
		c.gridy = 4;
		trPanel.add(new JLabel("Minimum momentum from tracking (GeV):"),c);
		minPText.addActionListener(this);
		minPText.setText("1");
		c.gridx = 1;
		c.gridy = 4;
		trPanel.add(minPText,c);
		// track charge
		c.gridx = 0;
		c.gridy = 5;
		trPanel.add(new JLabel("Track charge:"),c);
		trackChargeList.addItem("Both");
		trackChargeList.addItem("Negative");
		trackChargeList.addItem("Positive");
		trackChargeList.addActionListener(this);
		c.gridx = 1;
		c.gridy = 5;
		trPanel.add(trackChargeList,c);


		JPanel butPage3 = new configButtonPanel(this, true, "Next");
		trOuterPanel.add(butPage3, BorderLayout.SOUTH);

		configPane.add("Tracking / General", trOuterPanel);


		//Status options

		JPanel StatusOuterPanel = new JPanel(new BorderLayout());
		JPanel StatusPanel = new JPanel(new GridBagLayout());
		StatusOuterPanel.add(StatusPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// graph type
		c.gridx = 0;
		c.gridy = 0;
		StatusPanel.add(new JLabel("Status Options:"),c);
		c.gridx = 1;
		c.gridy = 0;
		StatusList.addItem("1D");   
		StatusList.addItem("2D");  
		StatusList.addActionListener(this);
		StatusPanel.add(StatusList,c);
		//limit time
		c.gridx = 0;
		c.gridy = 1;
		StatusPanel.add(new JLabel("Min time :"),c);
		c.gridx = 1;
		c.gridy = 1;
		minTimeText.addActionListener(this);
		minTimeText.setText("");
		StatusPanel.add(minTimeText,c);
		c.gridx = 0;
		c.gridy = 2;
		StatusPanel.add(new JLabel("Max time :"),c);
		c.gridx = 1;
		c.gridy = 2;
		maxTimeText.addActionListener(this);
		maxTimeText.setText("");
		StatusPanel.add(maxTimeText,c);
		c.gridx = 0;
		c.gridy = 3;
		StatusPanel.add(new JLabel("Bin number time :"),c);
		c.gridx = 1;
		c.gridy = 3;
		binTimeText.addActionListener(this);
		binTimeText.setText("");
		StatusPanel.add(binTimeText,c);
		//limit ADC
		c.gridx = 0;
		c.gridy = 4;
		StatusPanel.add(new JLabel("Min ADC :"),c);
		c.gridx = 1;
		c.gridy = 4;
		minADCText.addActionListener(this);
		minADCText.setText("");
		StatusPanel.add(minADCText,c);
		c.gridx = 0;
		c.gridy = 5;
		StatusPanel.add(new JLabel("Max ADC :"),c);
		c.gridx = 1;
		c.gridy = 5;
		maxADCText.addActionListener(this);
		maxADCText.setText("");
		StatusPanel.add(maxADCText,c);
		c.gridx = 0;
		c.gridy = 6;
		StatusPanel.add(new JLabel("Bin number ADC :"),c);
		c.gridx = 1;
		c.gridy = 6;
		binADCText.addActionListener(this);
		binADCText.setText("");
		StatusPanel.add(binADCText,c);

		JPanel butPageStatus = new configButtonPanel(this, true, "Next");
		StatusOuterPanel.add(butPageStatus, BorderLayout.SOUTH);
		configPane.add("Status", StatusOuterPanel);    

		//LR offset
		JPanel LROuterPanel = new JPanel(new BorderLayout());
		JPanel LRPanel = new JPanel(new GridBagLayout());
		LROuterPanel.add(LRPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// graph type
		c.gridx = 0;
		c.gridy = 0;
		LRPanel.add(new JLabel("LR offset Options:"),c);
		c.gridx = 1;
		c.gridy = 0;
		LRList.addItem("magnetic field on"); 
		LRList.addItem("no field");   
		LRList.addActionListener(this);
		LRPanel.add(LRList,c);
		// time limit 
		c.gridx = 0;
		c.gridy = 1;
		LRPanel.add(new JLabel("minimum time :"),c);
		c.gridx = 1;
		c.gridy = 1;
		minTimeLR.setText("");
		minTimeLR.addActionListener(this);
		LRPanel.add(minTimeLR,c);
		c.gridx = 0;
		c.gridy = 2;
		LRPanel.add(new JLabel("maximum time :"),c);
		c.gridx = 1;
		c.gridy = 2;
		maxTimeLR.setText("");
		maxTimeLR.addActionListener(this);
		LRPanel.add(maxTimeLR,c);
		c.gridx = 0;
		c.gridy = 3;
		LRPanel.add(new JLabel("number of bin time :"),c);
		c.gridx = 1;
		c.gridy = 3;
		binTimeLR.setText("");
		binTimeLR.addActionListener(this);
		LRPanel.add(binTimeLR,c);

		JPanel butPageLR = new configButtonPanel(this, true, "Next");
		LROuterPanel.add(butPageLR, BorderLayout.SOUTH);
		configPane.add("LR Offset", LROuterPanel);    

		// effV options
		JPanel effVOuterPanel = new JPanel(new BorderLayout());
		JPanel effVPanel = new JPanel(new GridBagLayout());
		effVOuterPanel.add(effVPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// graph type
		c.gridx = 0;
		c.gridy = 0;
		effVPanel.add(new JLabel("Effective velocity graph:"),c);
		c.gridx = 1;
		c.gridy = 0; 
		effVFitList.addItem("Max position of slices");
		effVFitList.addItem("Gaussian mean of slices");
		effVFitList.addActionListener(this);
		effVPanel.add(effVFitList,c);
		// fit mode
		c.gridx = 0;
		c.gridy = 1;
		effVPanel.add(new JLabel("Inspect fit (WARNING opens 72 windows) : "),c);
		c.gridx = 1;
		c.gridy = 1;
		effVFitInspect.addItem("No");
		effVFitInspect.addItem("Yes");
		effVFitInspect.addActionListener(this);
		effVPanel.add(effVFitInspect,c);
		// min events
		c.gridx = 0;
		c.gridy = 2;
		effVPanel.add(new JLabel("Minimum events per slice (for Gaussian slice fitter only):"),c);
		minEffVEventsText.addActionListener(this);
		minEffVEventsText.setText("");
		c.gridx = 1;
		c.gridy = 2;
		effVPanel.add(minEffVEventsText,c);
		//Yaxis
		c.gridx = 0;
		c.gridy = 3;
		effVPanel.add(new JLabel("Minimum Y axis:"),c);
		minYaxisVeff.addActionListener(this);
		minYaxisVeff.setText("");
		c.gridx = 1;
		c.gridy = 3;
		effVPanel.add(minYaxisVeff,c);
		c.gridx = 0;
		c.gridy = 4;
		effVPanel.add(new JLabel("Maximum Y axis:"),c);
		maxYaxisVeff.addActionListener(this);
		maxYaxisVeff.setText("");
		c.gridx = 1;
		c.gridy = 4;
		effVPanel.add(maxYaxisVeff,c);
		c.gridx = 0;
		c.gridy = 5;
		effVPanel.add(new JLabel("Bin Y axis:"),c);
		binYaxisVeff.addActionListener(this);
		binYaxisVeff.setText("");
		c.gridx = 1;
		c.gridy = 5;
		effVPanel.add(binYaxisVeff,c);
		c.gridx = 0;
		c.gridy = 6;
		effVPanel.add(new JLabel("Bin X axis:"),c);
		binXaxisVeff.addActionListener(this);
		binXaxisVeff.setText("");
		c.gridx = 1;
		c.gridy = 6;
		effVPanel.add(binXaxisVeff,c);


		JPanel butPage6 = new configButtonPanel(this, true, "Next");
		effVOuterPanel.add(butPage6, BorderLayout.SOUTH);
		configPane.add("Effective Velocity, Uturn-TimeLoss & Adjusted LR-Offset", effVOuterPanel);  

//Suppress tabs for uturn' and Attenuation. -- PN
/*		// ut options
		JPanel utOuterPanel = new JPanel(new BorderLayout());
		JPanel utPanel = new JPanel(new GridBagLayout());
		utOuterPanel.add(utPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// graph type
		c.gridx = 0;
		c.gridy = 0;
		utPanel.add(new JLabel("Uturn time loss graph:"),c);
		c.gridx = 1;
		c.gridy = 0;    
		utFitList.addItem("Max position of slices");
		utFitList.addItem("Gaussian mean of slices");
		utFitList.addActionListener(this);
		utPanel.add(utFitList,c);
		// fit mode
		c.gridx = 0;
		c.gridy = 1;
		utPanel.add(new JLabel("Inspect fit (WARNING opens 72 windows) : "),c);
		c.gridx = 1;
		c.gridy = 1;
		utFitInspect.addItem("No");
		utFitInspect.addItem("Yes");
		utFitInspect.addActionListener(this);
		utPanel.add(utFitInspect,c);
		// min events
		c.gridx = 0;
		c.gridy = 2;
		utPanel.add(new JLabel("Minimum events per slice (for Gaussian slice fitter only):"),c);
		utEventsText.addActionListener(this);
		utEventsText.setText("");
		c.gridx = 1;
		c.gridy = 2;
		utPanel.add(utEventsText,c);
		//Yaxis
		c.gridx = 0;
		c.gridy = 3;
		utPanel.add(new JLabel("Minimum Y axis:"),c);
		minYaxisUt.addActionListener(this);
		minYaxisUt.setText("");
		c.gridx = 1;
		c.gridy = 3;
		utPanel.add(minYaxisUt,c);
		c.gridx = 0;
		c.gridy = 4;
		utPanel.add(new JLabel("Maximum Y axis:"),c);
		maxYaxisUt.addActionListener(this);
		maxYaxisUt.setText("");
		c.gridx = 1;
		c.gridy = 4;
		utPanel.add(maxYaxisUt,c);
		c.gridx = 0;
		c.gridy = 5;
		utPanel.add(new JLabel("Bin Y axis:"),c);
		binYaxisUt.addActionListener(this);
		binYaxisUt.setText("");
		c.gridx = 1;
		c.gridy = 5;
		utPanel.add(binYaxisUt,c);
		c.gridx = 0;
		c.gridy = 6;
		utPanel.add(new JLabel("Bin X axis:"),c);
		binXaxisUt.addActionListener(this);
		binXaxisUt.setText("");
		c.gridx = 1;
		c.gridy = 6;
		utPanel.add(binXaxisUt,c);

		JPanel butPage7 = new configButtonPanel(this, true, "Next");
		utOuterPanel.add(butPage7, BorderLayout.SOUTH);
		configPane.add("UturnTimeLoss and LR adjusted", utOuterPanel); 

		// att options
		JPanel attOuterPanel = new JPanel(new BorderLayout());
		JPanel attPanel = new JPanel(new GridBagLayout());
		attOuterPanel.add(attPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// graph type
		c.gridx = 0;
		c.gridy = 0;
		attPanel.add(new JLabel("Attenuation length graph:"),c);
		c.gridx = 1;
		c.gridy = 0;        
		attFitList.addItem("Max position of slices");
		attFitList.addItem("Gaussian mean of slices");
		attFitList.addActionListener(this);
		attPanel.add(attFitList,c);
		// fit mode
		c.gridx = 0;
		c.gridy = 1;
		attPanel.add(new JLabel("Inspect fit (WARNING opens 72 windows) : "),c);
		c.gridx = 1;
		c.gridy = 1;
		attFitInspect.addItem("No");
		attFitInspect.addItem("Yes");
		attFitInspect.addActionListener(this);
		attPanel.add(attFitInspect,c);
		// min events
		c.gridx = 0;
		c.gridy = 2;
		attPanel.add(new JLabel("Minimum events per slice (for Gaussian slice fitter only):"),c);
		attEventsText.addActionListener(this);
		attEventsText.setText("");
		c.gridx = 1;
		c.gridy = 2;
		attPanel.add(attEventsText,c);
		//Yaxis
		c.gridx = 0;
		c.gridy = 3;
		attPanel.add(new JLabel("Minimum Y axis:"),c);
		minYaxisAtt.addActionListener(this);
		minYaxisAtt.setText("");
		c.gridx = 1;
		c.gridy = 3;
		attPanel.add(minYaxisAtt,c);
		c.gridx = 0;
		c.gridy = 4;
		attPanel.add(new JLabel("Maximum Y axis:"),c);
		maxYaxisAtt.addActionListener(this);
		maxYaxisAtt.setText("");
		c.gridx = 1;
		c.gridy = 4;
		attPanel.add(maxYaxisAtt,c);
		c.gridx = 0;
		c.gridy = 5;
		attPanel.add(new JLabel("Bin Y axis:"),c);
		binYaxisAtt.addActionListener(this);
		binYaxisAtt.setText("");
		c.gridx = 1;
		c.gridy = 5;
		attPanel.add(binYaxisAtt,c);
		c.gridx = 0;
		c.gridy = 6;
		attPanel.add(new JLabel("Bin X axis:"),c);
		binXaxisAtt.addActionListener(this);
		binXaxisAtt.setText("");
		c.gridx = 1;
		c.gridy = 6;
		attPanel.add(binXaxisAtt,c);

		JPanel butPage8= new configButtonPanel(this, true, "Next");
		attOuterPanel.add(butPage8, BorderLayout.SOUTH);
		configPane.add("Attenuation", attOuterPanel); 
*/                                                                    //Suppress tabs for uturn' and Attenuation. -- PN

		//LR offset
		JPanel timeOuterPanel = new JPanel(new BorderLayout());
		JPanel timePanel = new JPanel(new GridBagLayout());
		timeOuterPanel.add(timePanel, BorderLayout.NORTH);
		// time limit 
		c.gridx = 0;
		c.gridy = 0;
		timePanel.add(new JLabel("Minimum time :"),c);
		c.gridx = 1;
		c.gridy = 0;
		minTime.setText("");
		minTime.addActionListener(this);
		timePanel.add(minTime,c);
		c.gridx = 0;
		c.gridy = 1;
		timePanel.add(new JLabel("Maximum time :"),c);
		c.gridx = 1;
		c.gridy = 1;
		maxTime.setText("");
		maxTime.addActionListener(this);
		timePanel.add(maxTime,c);
		c.gridx = 0;
		c.gridy = 2;
		timePanel.add(new JLabel("Number of bin :"),c);
		c.gridx = 1;
		c.gridy = 2;
		binTime.setText("");
		binTime.addActionListener(this);
		timePanel.add(binTime,c);

		JPanel butPagetime = new configButtonPanel(this, true, "Next");
		timeOuterPanel.add(butPagetime, BorderLayout.SOUTH);
		configPane.add("Global TimeOffset", timeOuterPanel);    



		// energy options
		JPanel energyOuterPanel = new JPanel(new BorderLayout());
		JPanel energyPanel = new JPanel(new GridBagLayout());
		energyOuterPanel.add(energyPanel, BorderLayout.NORTH);
		c.weighty = 1;
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(3,3,3,3);
		// graph type
		c.gridx = 0;
		c.gridy = 0;
		energyPanel.add(new JLabel("Log ratio graph:"),c);
		c.gridx = 1;
		c.gridy = 0;
		EFitList.addItem("Max position of slices");
		EFitList.addItem("Gaussian mean of slices");
		EFitList.addActionListener(this);
		energyPanel.add(EFitList,c);
		// min events
		c.gridx = 0;
		c.gridy = 1;
		energyPanel.add(new JLabel("Min number of event per slice (only Gaussian Mean Mode):"),c);
		c.gridx = 1;
		c.gridy = 1;
		mineventE.setText("");
		mineventE.addActionListener(this);
		energyPanel.add(mineventE,c);
		// min Y axis
		c.gridx = 0;
		c.gridy = 2;
		energyPanel.add(new JLabel("Min Y for log ratio:"),c);
		c.gridx = 1;
		c.gridy = 2;
		minYaxisE.setText("");
		minYaxisE.addActionListener(this);
		energyPanel.add(minYaxisE,c);
		// max Y axis
		c.gridx = 0;
		c.gridy = 3;
		energyPanel.add(new JLabel("Max Y for log ratio:"),c);
		c.gridx = 1;
		c.gridy = 3;
		maxYaxisE.setText("");
		maxYaxisE.addActionListener(this);
		energyPanel.add(maxYaxisE,c);
		// binY axis
		c.gridx = 0;
		c.gridy = 4;
		energyPanel.add(new JLabel("Bin number Y for log ratio:"),c);
		c.gridx = 1;
		c.gridy = 4;
		binYaxisE.setText("");
		binYaxisE.addActionListener(this);
		energyPanel.add(binYaxisE,c);
		// binY axis
		c.gridx = 0;
		c.gridy = 5;
		energyPanel.add(new JLabel("Bin number X for log ratio:"),c);
		c.gridx = 1;
		c.gridy = 5;
		binXaxisE1.setText("");
		binXaxisE1.addActionListener(this);
		energyPanel.add(binXaxisE1,c);
		// min X axis
		c.gridx = 0;
		c.gridy = 6;
		energyPanel.add(new JLabel("Min X for geometric mean:"),c);
		c.gridx = 1;
		c.gridy = 6;
		minXaxisE.setText("");
		minXaxisE.addActionListener(this);
		energyPanel.add(minXaxisE,c);
		// max X axis
		c.gridx = 0;
		c.gridy = 7;
		energyPanel.add(new JLabel("Max X for geometric mean:"),c);
		c.gridx = 1;
		c.gridy = 7;
		maxXaxisE.setText("");
		maxXaxisE.addActionListener(this);
		energyPanel.add(maxXaxisE,c);
		// binX axis
		c.gridx = 0;
		c.gridy = 8;
		energyPanel.add(new JLabel("Bin number X for geometric mean:"),c);
		c.gridx = 1;
		c.gridy = 8;
		binXaxisE.setText("");
		binXaxisE.addActionListener(this);
		energyPanel.add(binXaxisE,c);





		JPanel butPage9 = new configButtonPanel(this, true, "Finish");        
		energyOuterPanel.add(butPage9, BorderLayout.SOUTH);
		configPane.add("Attenuation & Energy", energyOuterPanel);




		// Time walk options
		//        JPanel twOuterPanel = new JPanel(new BorderLayout());
		//        JPanel twPanel = new JPanel(new GridBagLayout());
		//        twOuterPanel.add(twPanel, BorderLayout.NORTH);
		//        c.weighty = 1;
		//        c.anchor = c.NORTHWEST;
		//        c.insets = new Insets(3,3,3,3);
		//        // graph type
		//        c.gridx = 0;
		//        c.gridy = 0;
		//        twPanel.add(new JLabel("Time walk graph:"),c);
		//        c.gridx = 1;
		//        c.gridy = 0;
		//        twFitList.addItem("Gaussian mean of slices");
		//        twFitList.addItem("Max position of slices");
		//        //twFitList.addItem("Profile");
		//        twFitList.addActionListener(this);
		//        twPanel.add(twFitList,c);
		//        // fit mode
		//        c.gridx = 0;
		//        c.gridy = 1;
		//        twPanel.add(new JLabel("Slicefitter mode:"),c);
		//        c.gridx = 1;
		//        c.gridy = 1;
		//        //twFitModeList.addItem("L");
		//        twFitModeList.addItem("");
		//        twPanel.add(twFitModeList,c);
		//        twFitModeList.addActionListener(this);
		//        // min events
		//        c.gridx = 0;
		//        c.gridy = 2;
		//        twPanel.add(new JLabel("Minimum events per slice:"),c);
		//        minTWEventsText.addActionListener(this);
		//        minTWEventsText.setText("100");
		//        c.gridx = 1;
		//        c.gridy = 2;
		//        twPanel.add(minTWEventsText,c);
		//        
		//        JPanel butPage8 = new configButtonPanel(this, true, "Finish");
		//        twOuterPanel.add(butPage8, BorderLayout.SOUTH);
		//        configPane.add("Time walk", twOuterPanel);
		//        
		configFrame.add(configPane);
		configFrame.setVisible(true);

	}

	public static void main(String[] args) {

		CNDCalibration calibGUI = new CNDCalibration();

	}

}
