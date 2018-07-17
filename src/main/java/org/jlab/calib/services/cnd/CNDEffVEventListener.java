package org.jlab.calib.services.cnd; 

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorDecoderView;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.detector.examples.RawEventViewer;
import org.jlab.detector.view.DetectorPane2D;
import org.jlab.detector.view.DetectorShape2D;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.task.DataSourceProcessorPane;
import org.jlab.io.task.IDataEventListener;
import org.jlab.utils.groups.IndexedList;

/**
 * CND Calibration suite
 * Based on the work of Louise Clark thanks!
 *
 * @author  Gavin Murdoch
 */

public class CNDEffVEventListener extends CNDCalibrationEngine {

	private static	boolean test = false;  //Test working for raw ADC and TDC values

	// hists
	//	public final int GEOMEAN = 0;
	//	public final int LOGRATIO = 1;
	public final int EFFV_L = 0;
	public final int EFFV_R = 1;
	public final int EFFV_L_GRAPH = 2;
	public final int EFFV_R_GRAPH = 3;


	public final int OVERRIDE_LEFT = 0;
	public final int OVERRIDE_RIGHT = 0;

	public final double EXPECTED_EFFV = 16.0;
	public final double ALLOWED_DIFF = 1.6;


	private String fitOption = "RQ";
	private String showPlotType = "EFFV_L";
	int backgroundSF = 0;
	int fitMethod = 0;
	public String fitMode = "RQ";
	boolean showSlices = false;
	private int FIT_METHOD_SF = 0;
	private int FIT_METHOD_MAX = 1;
	public int fitMinEvents = 20;
	public final double MAX_CHI = 10;

	//GEMC IDEAL LIMITS
	//        public int HIST_BINS = 600;
	//        public double HIST_X_MIN = 0;
	//        public double HIST_X_MAX = HIST_X_MIN + ((double) HIST_BINS * (CNDPaddlePair.NS_PER_CH));

	//CALIB CHALL SAMPLE
	//        public int HIST_BINS = 640;
	//        public double HIST_X_MIN = 0;
	//        public double HIST_X_MAX = 640;
	public int HIST_X_BINS = 100;
	public double HIST_X_MIN = -5;
	public double HIST_X_MAX = 75;

	//        public int HIST_Y_BINS = 150;
	//        public double HIST_Y_MIN = 0;
	//        public double HIST_Y_MAX = 3000;
	//        public int HIST_Y_BINS = 100;
	// Time
	public int HIST_Y_BINS = 70;
	public double HIST_Y_MIN = -6;
	public double HIST_Y_MAX = -0.5;

	// TDC
	//        public int HIST_Y_BINS = 28;
	//        public double HIST_Y_MIN = 42;
	//        public double HIST_Y_MAX = -256;

	//        public int HIST_Y_BINS = 300;
	//        public double HIST_Y_MIN = 2.4;
	//        public double HIST_Y_MAX = 8.4;

	private final double FIT_MIN = HIST_X_MIN;
	private final double FIT_MAX = HIST_X_MAX;
	private final double[]        TDC_MIN = {0.0, 25000.0,  24000.0, 25000.0};
	private final double[]        TDC_MAX = {0.0, 28000.0, 27000.0, 28000.0};	

	public CNDEffVEventListener() {

		stepName = "EffV";
		fileNamePrefix = "CND_CALIB_EFFV_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"veff_L/F:veff_L_err/F:veff_R/F:veff_R_err/F");

		//                calib = new CalibrationConstants(3,
		//                "tdc_conv_left/F:tdc_conv_right/F");

		calib.setName("/calibration/cnd/EffV");
		calib.setPrecision(5);

		// assign constraints to all paddles
		//        calib.addConstraint(3, EXPECTED_EFFV-ALLOWED_DIFF,
		//                EXPECTED_EFFV+ALLOWED_DIFF);
		//        calib.addConstraint(4, (-1)*ALLOWED_DIFF,
		//                (1)*ALLOWED_DIFF);
		//        calib.addConstraint(5, EXPECTED_EFFV-ALLOWED_DIFF,
		//                EXPECTED_EFFV+ALLOWED_DIFF);
		//        calib.addConstraint(6, (-1)*ALLOWED_DIFF,
		//                (1)*ALLOWED_DIFF);        

	}

	public void setConstraints() {
		calib.addConstraint(3, EXPECTED_EFFV-ALLOWED_DIFF,
				EXPECTED_EFFV+ALLOWED_DIFF);
		calib.addConstraint(4, (-1)*ALLOWED_DIFF,
				(1)*ALLOWED_DIFF);
		calib.addConstraint(5, EXPECTED_EFFV-ALLOWED_DIFF,
				EXPECTED_EFFV+ALLOWED_DIFF);
		calib.addConstraint(6, (-1)*ALLOWED_DIFF,
				(1)*ALLOWED_DIFF);        
	}


//	@Override
//	public void populatePrevCalib() {
//		prevCalRead = true;
//	}    

	    @Override
	//    public void populatePrevCalib() {
	public void populatePrevCalib() {

		//        System.out.println("!!!!!IN populatePrevCalib2 FOR TDC CONV!!!!!");
		System.out.println("Populating " + stepName + " previous calibration values");


		if (calDBSource==CAL_FILE) {

			// read in the values from the text file            
			String line = null;
			try { 

				// Open the file
				FileReader fileReader = 
						new FileReader(prevCalFilename);

				// Always wrap FileReader in BufferedReader
				BufferedReader bufferedReader = 
						new BufferedReader(fileReader);            

				line = bufferedReader.readLine();

				while (line != null) {

					String[] lineValues;
					lineValues = line.split(" ");

					int sector = Integer.parseInt(lineValues[0]);
					int layer = Integer.parseInt(lineValues[1]);
					int component = Integer.parseInt(lineValues[2]);
					double effVLeft = Double.parseDouble(lineValues[3]);
					double effVErrLeft = Double.parseDouble(lineValues[4]);
					double effVRight = Double.parseDouble(lineValues[5]);
					double effVErrRight = Double.parseDouble(lineValues[6]);

					veffValues.addEntry(sector, layer, component);
					veffValues.setDoubleValue(effVLeft,
							"veff_L", sector, layer, component);
					veffValues.setDoubleValue(effVErrLeft,
							"veff_L_err", sector, layer, component);                    
					veffValues.setDoubleValue(effVRight,
							"veff_R", sector, layer, component);                    
					veffValues.setDoubleValue(effVErrRight,
							"veff_R_err", sector, layer, component);
					System.out.println("veff: " + effVLeft);
					line = bufferedReader.readLine();
				}

				bufferedReader.close();            
			}
			catch(FileNotFoundException ex) {
				ex.printStackTrace();
				System.out.println(
						"Unable to open file '" + 
								prevCalFilename + "'");                
			}
			catch(IOException ex) {
				System.out.println(
						"Error reading file '" 
								+ prevCalFilename + "'");                   
				ex.printStackTrace();
			}            
		}
		else if (calDBSource==CAL_DEFAULT) {


			for (int sector = 1; sector <= 24; sector++) {
				for (int layer = 1; layer <= 3; layer++) {
					int component = 1;

					veffValues.setDoubleValue(EXPECTED_EFFV,
							"veff_L", sector, layer, component);                    
					veffValues.setDoubleValue(ALLOWED_DIFF,
							"veff_L_err", sector, layer, component);                    
					veffValues.setDoubleValue(EXPECTED_EFFV,
							"veff_R", sector, layer, component);                    
					veffValues.setDoubleValue((-1)*ALLOWED_DIFF,
							"veff_R_err", sector, layer, component);

				}
			}            
		}
		else if (calDBSource==CAL_DB) {
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			veffValues = dcp.readConstants("/calibration/cnd/EffV");   
			dcp.disconnect();
		}
		prevCalRead = true;
	}

	@Override
	public void resetEventListener() {

		if(CNDCalibration.inspectVeff!=0){
			showSlices=true;
		}
		if(CNDCalibration.modeVeff!=0){
			fitMethod=1;
		}
		if(CNDCalibration.minYaxisveff!=0){
			HIST_Y_MIN=CNDCalibration.minYaxisveff;
		}
		if(CNDCalibration.maxYaxisveff!=0){
			HIST_Y_MAX=CNDCalibration.maxYaxisveff;
		}
		if(CNDCalibration.binYaxisveff!=0){
			HIST_Y_BINS=CNDCalibration.binYaxisveff;
		}
		if(CNDCalibration.binXaxisveff!=0){
			HIST_X_BINS=CNDCalibration.binXaxisveff;
		}
		

	
		// GM perform init processing
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				// create all the histograms


				//            int numBins = (int) (paddleLength(layer)*0.6);  // 1 bin per 2cm + 10% either side
				//            double min = paddleLength(layer) * -0.6;
				//            double max = paddleLength(layer) * 0.6;

				H2F histL = 
						new H2F("effVLHist",
								"effVLHist",
								HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
								HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);
				H2F histR = 
						new H2F("effVRHist",
								"effVRHist",
								HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
								HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);

				F1D funcL = new F1D("funcL", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);
				F1D funcR = new F1D("funcR", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);

				GraphErrors graphL = new GraphErrors("effVLGraphh");
				graphL.setName("effVLGraph");
				graphL.setMarkerSize(MARKER_SIZE);
				graphL.setLineThickness(MARKER_LINE_WIDTH);    

				GraphErrors graphR = new GraphErrors("effVRGraph");
				graphR.setName("effVRGraph");
				graphR.setMarkerSize(MARKER_SIZE);
				graphR.setLineThickness(MARKER_LINE_WIDTH);            

				DataGroup dg = new DataGroup(2,2);
				dg.addDataSet(histL, 0);
				dg.addDataSet(histR, 1);
				dg.addDataSet(graphL, 2);
				dg.addDataSet(funcL, 2);
				dg.addDataSet(graphR, 3);
				dg.addDataSet(funcR, 3);
				dataGroups.add(dg, sector, layer, component);

				setPlotTitle(sector, layer, component);

				// initialize the constants array
				Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};                
				// override values

				constants.add(consts, sector, layer, component);


			}
		}
	}

	@Override
	public void processEvent(DataEvent event) {

		//DataProvider dp = new DataProvider();
		List<CNDPaddlePair> paddlePairList = DataProvider.getPaddlePairList(event);
		processPaddlePairList(paddlePairList);
	}

	@Override
	public void processPaddlePairList(List<CNDPaddlePair> paddlePairList) {

		for (CNDPaddlePair paddlePair : paddlePairList) {

			if (paddlePair.includeInCalib()){

				int sector = paddlePair.getDescriptor().getSector();
				int layer = paddlePair.getDescriptor().getLayer();
				int component = paddlePair.getDescriptor().getComponent();

				int layer_index = layer - 1;

				double hitPosition = 0.0;
				double pathInPaddle = 0.0;

				int directHitPaddle = 0;
				int neighbourHitPaddle = 1;

				double tdc[] = {paddlePair.TDCL, paddlePair.TDCR};
				double time[] = {paddlePair.tdcToTime(paddlePair.TDCL), paddlePair.tdcToTime(paddlePair.TDCR)};

				// Only going to deal with direct hits!!
				if (time[0] < time[1]-leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)){

					directHitPaddle = 0;
					neighbourHitPaddle = 1;

				} else if (time[1] < time[0]+leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)){

					directHitPaddle = 1;
					neighbourHitPaddle = 0;
				}

				if (test) {

					//****************************
					// CONSTANTS TO USE
					//****************************
					// Use nominal values
					// Use calculated veff values if previously saved, otherwise use default/nominal values
					double[] veffPaddle = {16.0, 16.0};

					// length of the light guides
					double[] Lg = {139.56, 138.89, 136.88};  //cm, length of lower/middle/higher light guides
					double[] veffLgArray= {16.0, 16.0};  //cm/ns, left and right paddles

					// time around the u-turn
					double[] tU = {0.6, 1.1, 1.55};  //time to travel round the u-turn, lower/middle/higher layers.  Daria deduced these from plots.
					//Or can use the outer radius as an estimate for the lightpath in the u-turn (when multiplied by PI)
					double[] rU = {3.808, 4.216, 4.624};  //u-turn radius at height of 1.5cm for each paddle, lower/middle/higher layers.        

					hitPosition = ((time[directHitPaddle] - time[neighbourHitPaddle])
							+ Lg[layer_index] * ((1 / veffLgArray[neighbourHitPaddle]) - (1 / veffLgArray[directHitPaddle]))
							+ paddleLength(layer) * ((1 / (veffPaddle[directHitPaddle])) + (1 / (veffPaddle[neighbourHitPaddle])))
							+ tU[layer_index])
							* ((veffPaddle[directHitPaddle]) / 2);

					// **ADC values are not normalised!!!**
					if (directHitPaddle == 0){
						dataGroups.getItem(sector,layer,component).getH2F("effVLHist").fill( hitPosition, 0.5*(time[0]-time[1]));
					} else if (directHitPaddle == 1){
						dataGroups.getItem(sector,layer,component).getH2F("effVRHist").fill( hitPosition, 0.5*(time[1]-time[0]));
					}

				} else if (!(test)){

					// hitPosition from tracking:
					hitPosition = paddlePair.zPosCND();                    
					if (paddlePair.COMP==1 && paddlePair.ZPOS!=0.0  && Math.abs((time[0]-time[1]+leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)))>1.5){
						dataGroups.getItem(sector,layer,component).getH2F("effVLHist").fill( hitPosition, 0.5*(time[0]-time[1]+leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)));
						//                        System.out.println("(tdc[0]-tdc[1]) = " + (tdc[0]-tdc[1]));
						//                        System.out.println("(time[0]-time[1]) = " + (time[0]-time[1]));
						//                        System.out.println("");
						//                        dataGroups.getItem(sector,layer,component).getH2F("effVLHist").fill( hitPosition, 0.5*(tdc[0]-tdc[1]));
					} else if (paddlePair.COMP==2 && paddlePair.ZPOS!=0.0 && Math.abs((time[1]-time[0]-leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)))>1.5){
						dataGroups.getItem(sector,layer,component).getH2F("effVRHist").fill( hitPosition, 0.5*(time[1]-time[0]-leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)));
						//                        dataGroups.getItem(sector,layer,component).getH2F("effVRHist").fill( hitPosition, 0.5*(tdc[1]-tdc[0]));
					}                    

				}
			}
		}
	}

	//GM commented this out    
	//	@Override
	//	public void timerUpdate() {
	//		if (fitMethod!=FIT_METHOD_SF) {
	//			 // only analyze at end of file for slice fitter - takes too long
	//			analyze();
	//		}
	//		save();
	//		calib.fireTableDataChanged();
	//	}

	@Override
	public void fit(int sector, int layer, int component,
			double minRange, double maxRange) {

		System.out.println("IN EFFV FIT for S"+sector+" L"+layer+" C"+component);
		//        System.out.println("minRange and maxRange = " + minRange + " " + maxRange);


		H2F histL = dataGroups.getItem(sector,layer,component).getH2F("effVLHist");
		H2F histR = dataGroups.getItem(sector,layer,component).getH2F("effVRHist");
		GraphErrors graphL = (GraphErrors) dataGroups.getItem(sector,layer,component).getData("effVLGraph");
		GraphErrors graphR = (GraphErrors) dataGroups.getItem(sector,layer,component).getData("effVRGraph");

		
		int fitMinEventsL = 2*histL.getEntries()/HIST_X_BINS;
		int fitMinEventsR = 2*histR.getEntries()/HIST_X_BINS;
		
		if(CNDCalibration.mineventVeff!=0){
			fitMinEventsL = CNDCalibration.mineventVeff;
			fitMinEventsR = CNDCalibration.mineventVeff;
		}
		
		// fit function to the graph of means
		if (fitMethod == FIT_METHOD_SF) {
			System.out.println("in Gfit");
			ParallelSliceFitter psfL = new ParallelSliceFitter(histL);
			psfL.setFitMode(fitMode);
			psfL.setMinEvents(fitMinEventsL);
			psfL.setBackgroundOrder(backgroundSF);
			psfL.setNthreads(1);
			psfL.setMaxChiSquare(MAX_CHI);
			//psfL.setShowProgress(false);
			setOutput(false);
			psfL.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfL.inspectFits();
			}
			graphL.copy(fixGraph(psfL.getMeanSlices(), "effVLGraph"));

			//            if ( sector == 1 && layer == 1) {
			//                for (int i = 0; i< adcL_vs_z_graph.getDataSize(0); i++ ){
			//                    System.out.println("X["+i+"] = "+adcL_vs_z_graph.getDataX(i));
			//                    System.out.println("Y["+i+"] = "+adcL_vs_z_graph.getDataY(i));
			//                }
			//            }

			ParallelSliceFitter psfR = new ParallelSliceFitter(histR);
			psfR.setFitMode(fitMode);
			psfR.setMinEvents(fitMinEventsR);
			psfR.setBackgroundOrder(backgroundSF);
			psfR.setNthreads(1);
			psfR.setMaxChiSquare(MAX_CHI);
			
			//psfR.setShowProgress(false);
			setOutput(false);
			psfR.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfR.inspectFits();
				showSlices = false;
			}
			graphR.copy(fixGraph(psfR.getMeanSlices(), "effVRGraph"));
		} else if (fitMethod == FIT_METHOD_MAX) {
			System.out.println("in maxfit");
			// ****
			// ****NEED TO EDIT THIS****
			// ****
			maxGraphError = 0.15;
			graphL.copy(fixGraph(maxGraph(histL, "effVLGraph"),"effVLGraph"));
			graphR.copy(fixGraph(maxGraph(histR, "effVRGraph"),"effVRGraph"));


			//            graphL.copy(maxGraph(histL, "graphL"));
			//            graphR.copy(maxGraph(histR, "graphR"));
		} else {
			graphL.copy(histL.getProfileX());
			graphR.copy(histR.getProfileX());
		}

		// ****
		// ****LEFT GRAPH
		// ****        

		// find the range for the fit
		double lowLimit = 0.0;
		double highLimit = 0.0;
		int lowLimitBin = 0;

		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		}
		else {
			            // Fit over filled data only:
			            if ( graphL.getDataSize(0) >= 2){
			                for (int i = 0; i < graphL.getDataSize(0); i++){
			                    if ( graphL.getDataX(i) > 0.0 ) {
			                        lowLimit = graphL.getDataX(i);
			                        lowLimitBin = i;
			                        break;
			                    }
			                }
			}
			
			            
			// A FINIR            
			            
//			System.out.println("in lowlimit fit ");
//			int lowIndex = 20;
//
//			int graphMaxIndex = graphL.getDataSize(0);
//			if(graphMaxIndex>=2){
//				// get the position with max entries
//				int maxCounts = 0;
//				int centreIndex = graphL.getDataSize(0)/2;
//
//				int maxIndex = 0;
//				for (int i=0; i<graphL.getDataSize(0); i++) {
//					if (histL.getSlicesX().get(i).getEntries() > maxCounts) {
//						maxIndex = i;
//						maxCounts = histL.getSlicesX().get(i).getEntries(); 
//					}
//				}
//
//				for (int pos=centreIndex; pos>=1; pos--) {
//					if(histL.getSlicesX().get(pos).getEntries() < 0.2*maxCounts){
//						lowIndex = pos;
//						break;
//					}
//				}
//
//				lowLimit = graphL.getDataX(lowIndex);
//			}
			else {
				lowLimit = FIT_MIN;                
			}

			System.out.println("lowlimit fit "+lowLimit);

		}

		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		}
		else {
			// Fit over filled data only:
			if ( graphL.getDataSize(0) >= 2){
				for (int i = (graphL.getDataSize(0) - 1); i > lowLimitBin; i--){
					if ( graphL.getDataX( i ) > 0.0 ) {
						highLimit = graphL.getDataX(i);
						break;
					}
				}                
			} else {
				highLimit = FIT_MAX;
			}
		}        

		F1D funcL = dataGroups.getItem(sector,layer,component).getF1D("funcL");
		funcL.setRange(lowLimit, highLimit);

		if (layer == 1){
			funcL.setParameter(0, -4.1);  //offset estimate
		} else if (layer == 2){
			funcL.setParameter(0, -4.79);  //offset estimate
		} else if (layer == 3){
			funcL.setParameter(0, -5.35);  //offset estimate
		}

		funcL.setParameter(1, (1./EXPECTED_EFFV));  //gradient estimate
		
		
		try {
			DataFitter.fit(funcL, graphL, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector "+sector+" layer "+layer+" component "+component);
			e.printStackTrace();
		}


		// ****
		// ****RIGHT GRAPH
		// ****

		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		}
		else {
			// Fit over filled data only:
			if ( graphR.getDataSize(0) >= 2){
				for (int i = 0; i < graphR.getDataSize(0); i++){
					if ( graphR.getDataX(i) > 0.0 ) {
						lowLimit = graphR.getDataX(i);
						lowLimitBin = i;
						break;
					}
				}
			} else {
				lowLimit = FIT_MIN;                
			}
		}

		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		}
		else {
			// Fit over filled data only:
			if ( graphR.getDataSize(0) >= 2){
				for (int i = (graphR.getDataSize(0) - 1); i > lowLimitBin; i--){
					if ( graphR.getDataX( i ) > 0.0 ) {
						highLimit = graphR.getDataX(i);
						break;
					}
				}                
			} else {
				highLimit = FIT_MAX;
			}
		}        

		F1D funcR = dataGroups.getItem(sector,layer,component).getF1D("funcR");
		funcR.setRange(lowLimit, highLimit);

		if (layer == 1){
			funcR.setParameter(0, -4.1);  //offset estimate
		} else if (layer == 2){
			funcR.setParameter(0, -4.79);  //offset estimate
		} else if (layer == 3){
			funcR.setParameter(0, -5.35);  //offset estimate
		}

		funcR.setParameter(1, (1./EXPECTED_EFFV));  //gradient estimate

		try {
			DataFitter.fit(funcR, graphR, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector "+sector+" layer "+layer+" component "+component);
			e.printStackTrace();
		}

	}


	public void adjustFit(int sector, int layer, int component,
			double minRange, double maxRange, int side) {

		//        System.out.println("IN adjustFit FIT for S" + sector + " L" + layer + " C" + component);

		F1D func = null;
		GraphErrors graph = null;

		if (side == 1){
			func = dataGroups.getItem(sector, layer, component).getF1D("funcL");
			graph = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("effVLGraph");
		} else if (side == 2){
			func = dataGroups.getItem(sector, layer, component).getF1D("funcR");
			graph = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("effVRGraph");
		}


		func.setRange(minRange, maxRange);

		try {
			DataFitter.fit(func, graph, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}        

		// update the table
		saveRow(sector,layer,component);		
		calib.fireTableDataChanged();        

	}

	public void customFit(int sector, int layer, int component) {

		F1D funcL = dataGroups.getItem(sector, layer, component).getF1D("funcL");   
		F1D funcR = dataGroups.getItem(sector, layer, component).getF1D("funcR");   

		String[] fields = {""+funcL.getMin(),
				""+funcL.getMax(),
				""+funcR.getMin(),
				""+funcR.getMax()};

		CNDCustomFitPanelFor2Graphs panel = new CNDCustomFitPanelFor2Graphs(fields);

		int result = JOptionPane
				.showConfirmDialog(null, panel, "Adjust fit for S" + sector + " L" + layer + " C" + component,
						JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION) {

			double[] fitLimitsLeft = new double[2];
			double[] fitLimitsRight = new double[2];

			for (int j = 0; j < 2; j++) {
				fitLimitsLeft[j] = toDouble(panel.textFields[j].getText());
				fitLimitsRight[j] = toDouble(panel.textFields[2+j].getText());
			}


			//Check for new limits for the LEFT histogram, only when a lower OR an upper have been provided
			if (fitLimitsLeft[0] != 0.0 || fitLimitsLeft[1] != 0.0) {

				//Use previous fit limits if required
				for (int p = 0; p < 2; p++) {
					if (fitLimitsLeft[p] == 0.0) {
						if (p % 2 == 0) {
							fitLimitsLeft[p] = funcL.getMin();
						} else {
							fitLimitsLeft[p] = funcL.getMax();                            
						}
					}
				}
				adjustFit(sector, layer, component, fitLimitsLeft[0], fitLimitsLeft[1], 1);
			}    

			//Check for new limits for the RIGHT histogram, only when a lower OR an upper have been provided
			if (fitLimitsRight[0] != 0.0 || fitLimitsRight[1] != 0.0) {

				//Use previous fit limits if required
				for (int p = 0; p < 2; p++) {
					if (fitLimitsRight[p] == 0.0) {
						if (p % 2 == 0) {
							fitLimitsRight[p] = funcR.getMin();
						} else {
							fitLimitsRight[p] = funcR.getMax();                            
						}
					}
				}
				adjustFit(sector, layer, component, fitLimitsRight[0], fitLimitsRight[1], 2);
			}            

			//Now check for fit limit changes for all sectors
			double[][][] allSectorLimits = new double[3][2][2];
			for (int layerCount = 0; layerCount < 3; layerCount++) {
				//0 = layer 1, 1 = layer 2, 2 = layer2.  Left (0) and right (1) paddles.  Lower (0) and upper (1) limits.
				allSectorLimits[(2 - layerCount)][0][0] = toDouble(panel.textFields[4 + (layerCount * 4)].getText());
				allSectorLimits[(2 - layerCount)][0][1] = toDouble(panel.textFields[5 + (layerCount * 4)].getText());
				allSectorLimits[(2 - layerCount)][1][0] = toDouble(panel.textFields[6 + (layerCount * 4)].getText());
				allSectorLimits[(2 - layerCount)][1][1] = toDouble(panel.textFields[7 + (layerCount * 4)].getText());
			}

			for (int layer_index = 0; layer_index < 3; layer_index++) {
				for (int paddle_index = 0; paddle_index < 2; paddle_index++) {
					if (allSectorLimits[layer_index][paddle_index][0] != 0.0 && allSectorLimits[layer_index][paddle_index][1] != 0.0) {

						//Perform the fits for all sectors for this layer
						int component_index = 1;
						int paddleNum = paddle_index + 1;
						int layerNum = layer_index + 1;

						for (int sector_index = 0; sector_index < 24; sector_index++) {
							int sectorNum = sector_index + 1;
							adjustFit(sectorNum, layerNum, component_index, allSectorLimits[layer_index][paddle_index][0], allSectorLimits[layer_index][paddle_index][1],paddleNum);
						}
					}
				}
			}

		}
	}



	//    public void customFit(int sector, int layer, int paddle){
	//
	//        String[] fields = { "Min range for fit:", "Max range for fit:", "SPACE",
	//				"Min Events per slice:", "Background order for slicefitter(-1=no background, 0=p0 etc):","SPACE",
	//                "Override TDC_conv_left:", "Override TDC_conv_right"};
	//
	//        TOFCustomFitPanel panel = new TOFCustomFitPanel(fields,sector,layer);
	//
	//        int result = JOptionPane.showConfirmDialog(null, panel, 
	//                "Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
	//        if (result == JOptionPane.OK_OPTION) {
	//
	//            double minRange = toDouble(panel.textFields[0].getText());
	//            double maxRange = toDouble(panel.textFields[1].getText());
	//			if (panel.textFields[2].getText().compareTo("") !=0) {
	//				fitMinEvents = Integer.parseInt(panel.textFields[2].getText());
	//			}
	//			if (panel.textFields[3].getText().compareTo("") !=0) {
	//				backgroundSF = Integer.parseInt(panel.textFields[3].getText());
	//			}            
	//            double overrideValueL = toDouble(panel.textFields[4].getText());
	//            double overrideValueR = toDouble(panel.textFields[5].getText());
	//
	//			int minP = paddle;
	//			int maxP = paddle;
	//			if (panel.applyToAll) {
	//				minP = 1;
	//				maxP = NUM_PADDLES[layer-1];
	//			}
	//			else {
	//				// if fitting one panel then show inspectFits view
	//				showSlices = true;
	//			}
	//			
	//			for (int p=minP; p<=maxP; p++) {
	//				// save the override values
	//				Double[] consts = constants.getItem(sector, layer, paddle);
	//				consts[OVERRIDE_LEFT] = overrideValueL;
	//				consts[OVERRIDE_RIGHT] = overrideValueR;
	//
	//				fit(sector, layer, p, minRange, maxRange);
	//
	//				// update the table
	//				saveRow(sector,layer,p);
	//			}
	//            calib.fireTableDataChanged();
	//
	//        }     
	//    }


	public Double getEffVLeft(int sector, int layer, int component) {

		double effVLeft = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			effVLeft = overrideVal;
		}
		else {
			double gradient = dataGroups.getItem(sector,layer,component).getF1D("funcL").getParameter(1);
			effVLeft = 1. / gradient;
		}
		return effVLeft;
	}

	public Double getEffVRight(int sector, int layer, int component) {

		double effVRight = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_RIGHT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			effVRight = overrideVal;
		}
		else {
			double gradient = dataGroups.getItem(sector,layer,component).getF1D("funcR").getParameter(1);
			effVRight = 1. / gradient;
		}
		return effVRight;
	}

	public Double getVeffLError(int sector, int layer, int paddle){

		double veffError = 0.0;

		// Calculate the error
		// fractional error in veff = fractional error in 1/veff

		double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("funcL")
				.getParameter(1);
		double gradientErr = dataGroups.getItem(sector,layer,paddle).getF1D("funcL")
				.parameter(1).error();
		double veff = getEffVLeft(sector, layer, paddle);

		if (gradient==0.0) {
			veffError = 0.0;
		}
		else {
			veffError = (gradientErr/gradient) * veff;
		}

		return veffError;
	} 

	public Double getVeffRError(int sector, int layer, int paddle){

		double veffError = 0.0;

		// Calculate the error
		// fractional error in veff = fractional error in 1/veff

		double gradient = dataGroups.getItem(sector,layer,paddle).getF1D("funcR")
				.getParameter(1);
		double gradientErr = dataGroups.getItem(sector,layer,paddle).getF1D("funcR")
				.parameter(1).error();
		double chi2 = dataGroups.getItem(sector,layer,paddle).getF1D("funcR")
				.getChiSquare();
		double veff = getEffVRight(sector, layer, paddle);
		
		System.out.println();
		System.out.println("chi2= " + chi2);
		System.out.println("gradientR= " + gradient);
		System.out.println("gradientErrR = " + gradientErr);
		
		
		if (gradient==0.0) {
			veffError = 0.0;
		}
		else {
			veffError = (gradientErr/gradient) * veff;
		}

		return veffError;
	}

	@Override
	public void saveRow(int sector, int layer, int component) {

		//        if (sector == 1){
		//            System.out.println("veffL = " + getEffVLeft(sector, layer, component));
		//            System.out.println("veffR = " + getEffVRight(sector, layer, component));
		//        }

		calib.setDoubleValue(getEffVLeft(sector, layer, component),
				"veff_L", sector, layer, component);
		//        calib.setDoubleValue(ALLOWED_DIFF,
		//                        "veff_L_err", sector, layer, component);
		calib.setDoubleValue(getVeffLError(sector, layer, component),
				"veff_L_err", sector, layer, component);
		calib.setDoubleValue(getEffVRight(sector, layer, component),
				"veff_R", sector, layer, component);
		//        calib.setDoubleValue(ALLOWED_DIFF,
		//                        "veff_R_err", sector, layer, component);
		calib.setDoubleValue(getVeffRError(sector, layer, component),
				"veff_R_err", sector, layer, component);

	}

	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return (true);

		//        return (getConvLeft(sector,layer,paddle) >= EXPECTED_SLOPE*(1-ALLOWED_DIFF)
		//                &&
		//                getConvLeft(sector,layer,paddle) <= EXPECTED_SLOPE*(1+ALLOWED_DIFF)
		//                &&
		//                getConvRight(sector,layer,paddle) >= EXPECTED_SLOPE*(1-ALLOWED_DIFF)
		//                &&
		//                getConvRight(sector,layer,paddle) <= EXPECTED_SLOPE*(1+ALLOWED_DIFF)
		//                );

	}

	@Override
	public boolean isGoodComponent(int sector, int layer, int component) {

		return (true);

	}    

	@Override
	public void setPlotTitle(int sector, int layer, int component) {

		// reset hist title as may have been set to null by show all 
		dataGroups.getItem(sector, layer, component).getH2F("effVLHist").setTitle("0.5 * (tL-tR) vs z - S" + sector + " L" + layer + " C" + component);
		dataGroups.getItem(sector, layer, component).getH2F("effVRHist").setTitle("0.5 * (tR-tL) vs z - S" + sector + " L" + layer + " C" + component);
		if (test){
			dataGroups.getItem(sector, layer, component).getH2F("effVLHist").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("effVRHist").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("effVLGraph").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("effVRGraph").setTitleX("Calculated hit position (cm)");
		}
		else {
			dataGroups.getItem(sector, layer, component).getH2F("effVLHist").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("effVRHist").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("effVLGraph").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("effVRGraph").setTitleX("Local hit position from tracking (cm)");              
		}

		dataGroups.getItem(sector, layer, component).getH2F("effVLHist").setTitleY("0.5 * (tL-tR) (ns)");
		dataGroups.getItem(sector, layer, component).getH2F("effVRHist").setTitleY("0.5 * (tR-tL) (ns)");
		dataGroups.getItem(sector, layer, component).getGraph("effVLGraph").setTitle("0.5 * (tL-tR) vs z");
		dataGroups.getItem(sector, layer, component).getGraph("effVRGraph").setTitle("0.5 * (tR-tL) vs z");
		dataGroups.getItem(sector, layer, component).getGraph("effVLGraph").setTitleY("0.5 * (tL-tR) (ns)");
		dataGroups.getItem(sector, layer, component).getGraph("effVRGraph").setTitleY("0.5 * (tR-tL) (ns)");

	}

	@Override
	public void drawPlots(int sector, int layer, int component, EmbeddedCanvas canvas) {

		if (showPlotType == "EFFV_L") {
			H2F histL = dataGroups.getItem(sector,layer,component).getH2F("effVLHist");
			histL.setTitle("");
			histL.setTitleX("");
			histL.setTitleY("");    
			canvas.draw(histL);
		}
		else if (showPlotType == "EFFV_R") {
			H2F histR = dataGroups.getItem(sector,layer,component).getH2F("effVRHist");
			histR.setTitle("");
			histR.setTitleX("");
			histR.setTitleY("");                         
			canvas.draw(histR);
		}
		else if (showPlotType == "EFFV_L_GRAPH") {
			GraphErrors graphL = dataGroups.getItem(sector,layer,component).getGraph("effVLGraph");
			if (graphL.getDataSize(0) != 0) {
				graphL.setTitle("");
				graphL.setTitleX("");
				graphL.setTitleY("");                       
				canvas.draw(graphL);
				canvas.draw(dataGroups.getItem(sector,layer,component).getF1D("funcL"), "same");
			}
		}
		else if (showPlotType == "EFFV_R_GRAPH") {
			GraphErrors graphR = dataGroups.getItem(sector,layer,component).getGraph("effVRGraph");
			if (graphR.getDataSize(0) != 0) {
				graphR.setTitle("");
				graphR.setTitleX("");
				graphR.setTitleY("");         
				canvas.draw(graphR);
				canvas.draw(dataGroups.getItem(sector,layer,component).getF1D("funcR"), "same");
			}
		}
	}

	@Override
	public void showPlots(int sector, int layer) {

		showPlotType = "EFFV_L";
		stepName = "EffV - Left";
		super.showPlots(sector, layer);
		showPlotType = "EFFV_R";
		stepName = "EffV - Right";
		super.showPlots(sector, layer);
		showPlotType = "EFFV_L_GRAPH";
		stepName = "EffV Graph - Left";
		super.showPlots(sector, layer);
		showPlotType = "EFFV_R_GRAPH";
		stepName = "EffV Graph - Right";
		super.showPlots(sector, layer);

	}    

	@Override
	public DataGroup getSummary() {

		double[] sectorNumber = new double[24];
		double[] zeroUncs = new double[24];        
		double[] effVL3L = new double[24];
		double[] effVL3R = new double[24];
		double[] effVL2L = new double[24];
		double[] effVL2R = new double[24];
		double[] effVL1L = new double[24];
		double[] effVL1R = new double[24];


		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				sectorNumber[sector - 1] = (double) sector;
				zeroUncs[sector - 1] = 0.0;

				if (layer == 3){
					effVL3L[sector - 1] = getEffVLeft(sector, layer, component);
					effVL3R[sector - 1] = getEffVRight(sector, layer, component);
				} else if (layer == 2){
					effVL2L[sector - 1] = getEffVLeft(sector, layer, component);
					effVL2R[sector - 1] = getEffVRight(sector, layer, component);
				} else if (layer == 1){
					effVL1L[sector - 1] = getEffVLeft(sector, layer, component);
					effVL1R[sector - 1] = getEffVRight(sector, layer, component);
				}
			}
		}

		GraphErrors allL3L = new GraphErrors("allL3L", sectorNumber,
				effVL3L, zeroUncs, zeroUncs);

		allL3L.setTitleX("Sector Number");
		allL3L.setTitleY("EffV (cm/ns)");
		allL3L.setMarkerSize(MARKER_SIZE);
		allL3L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL3R = new GraphErrors("allL3R", sectorNumber,
				effVL3R, zeroUncs, zeroUncs);

		allL3R.setTitleX("Sector Number");
		allL3R.setTitleY("EffV (cm/ns)");
		allL3R.setMarkerSize(MARKER_SIZE);
		allL3R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL2L = new GraphErrors("allL2L", sectorNumber,
				effVL2L, zeroUncs, zeroUncs);

		allL2L.setTitleX("Sector Number");
		allL2L.setTitleY("EffV (cm/ns)");
		allL2L.setMarkerSize(MARKER_SIZE);
		allL2L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL2R = new GraphErrors("allL2R", sectorNumber,
				effVL2R, zeroUncs, zeroUncs);

		allL2R.setTitleX("Sector Number");
		allL2R.setTitleY("EffV (cm/ns)");
		allL2R.setMarkerSize(MARKER_SIZE);
		allL2R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1L = new GraphErrors("allL1L", sectorNumber,
				effVL1L, zeroUncs, zeroUncs);

		allL1L.setTitleX("Sector Number");
		allL1L.setTitleY("EffV (cm/ns)");
		allL1L.setMarkerSize(MARKER_SIZE);
		allL1L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1R = new GraphErrors("allL1R", sectorNumber,
				effVL1R, zeroUncs, zeroUncs);

		allL1R.setTitleX("Sector Number");
		allL1R.setTitleY("EffV (cm/ns)");
		allL1R.setMarkerSize(MARKER_SIZE);
		allL1R.setLineThickness(MARKER_LINE_WIDTH);           

		DataGroup dg = new DataGroup(2, 3);
		dg.addDataSet(allL3L, 0);
		dg.addDataSet(allL3R, 1);
		dg.addDataSet(allL2L, 2);
		dg.addDataSet(allL2R, 3);
		dg.addDataSet(allL1L, 4);
		dg.addDataSet(allL1R, 5);
		return dg;

	}    

	@Override
	public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {

		//    	canvas.getPad(2).setAxisRange(TDC_MIN[layer], TDC_MAX[layer], -1.0, 1.0);
		//    	canvas.getPad(3).setAxisRange(TDC_MIN[layer], TDC_MAX[layer], -1.0, 1.0);

	}

}
