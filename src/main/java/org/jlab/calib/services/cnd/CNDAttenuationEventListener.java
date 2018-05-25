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

public class CNDAttenuationEventListener extends CNDCalibrationEngine {

	private static boolean test = false;  //Test working for raw ADC and TDC values

	// hists
	//	public final int GEOMEAN = 0;
	//	public final int LOGRATIO = 1;
	public final int ATTLEN_L = 0;
	public final int ATTLEN_R = 1;
	public final int ATTLEN_L_GRAPH = 2;
	public final int ATTLEN_R_GRAPH = 3;

	public final int OVERRIDE_LEFT = 0;
	public final int OVERRIDE_RIGHT = 0;

	public final double EXPECTED_ATTLEN = 150.;
	public final double ALLOWED_DIFF = 15.;

	//    private String fitOption = "RNQ";
	private String fitOption = "RQ";
	private String showPlotType = "EFFV_L";
	int backgroundSF = 0;
	int fitMethod = 0;
	public String fitMode = "RQ";
	boolean showSlices = false;
	private int FIT_METHOD_SF = 0;
	private int FIT_METHOD_MAX = 1;
	public int fitMinEvents = 20;
	public final double MAX_CHI = 20;

	//GEMC IDEAL LIMITS
	//        public int HIST_BINS = 600;
	//        public double HIST_X_MIN = 0;
	//        public double HIST_X_MAX = HIST_X_MIN + ((double) HIST_BINS * (CNDPaddlePair.NS_PER_CH));
	//CALIB CHALL SAMPLE
	//        public int HIST_BINS = 640;
	//        public double HIST_X_MIN = 0;
	//        public double HIST_X_MAX = 640;
	public int HIST_X_BINS = 40;
	public double HIST_X_MIN = -5;
	public double HIST_X_MAX = 80;
	//        public int HIST_Y_BINS = 150;
	//        public double HIST_Y_MIN = 0;
	//        public double HIST_Y_MAX = 3000;
	// Log
	//        public int HIST_Y_BINS = 100;
	public int HIST_Y_BINS = 40;
	public double HIST_Y_MIN = -0.5;
	public double HIST_Y_MAX = 2.5;
//	public double HIST_Y_MIN = 6;
//	public double HIST_Y_MAX = 10;

	//        public int HIST_Y_BINS = 300;
	//        public double HIST_Y_MIN = 2.4;
	//        public double HIST_Y_MAX = 8.4;
	private final double FIT_MIN = HIST_X_MIN;
	private final double FIT_MAX = HIST_X_MAX;
	private final double[] TDC_MIN = {0.0, 25000.0, 24000.0, 25000.0};
	private final double[] TDC_MAX = {0.0, 28000.0, 27000.0, 28000.0};

	public CNDAttenuationEventListener() {

		stepName = "Attenuation";
		fileNamePrefix = "CND_CALIB_ATTENUATION_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"attlen_L/F:attlen_L_err/F:attlen_R/F:attlen_R_err/F");

		//                calib = new CalibrationConstants(3,
		//                "tdc_conv_left/F:tdc_conv_right/F");
		calib.setName("/calibration/cnd/Attenuation");
		calib.setPrecision(5);

		// assign constraints to all paddles
//		        calib.addConstraint(3, EXPECTED_ATTLEN - ALLOWED_DIFF,
//		                EXPECTED_ATTLEN + ALLOWED_DIFF);
//		        calib.addConstraint(4, (-1) * ALLOWED_DIFF,
//		                (1) * ALLOWED_DIFF);
//		        calib.addConstraint(5, EXPECTED_ATTLEN - ALLOWED_DIFF,
//		                EXPECTED_ATTLEN + ALLOWED_DIFF);
//		        calib.addConstraint(6, (-1) * ALLOWED_DIFF,
//		                (1) * ALLOWED_DIFF);

	}

	@Override
	public void populatePrevCalib() {
		prevCalRead = true;
	}

	//    @Override
	//    public void populatePrevCalib() {
	public void populatePrevCalib2() {

		//        System.out.println("!!!!!IN populatePrevCalib2 FOR TDC CONV!!!!!");
		if (calDBSource == CAL_FILE) {

			// read in the values from the text file            
			String line = null;
			try {

				// Open the file
				FileReader fileReader
				= new FileReader(prevCalFilename);

				// Always wrap FileReader in BufferedReader
				BufferedReader bufferedReader
				= new BufferedReader(fileReader);

				line = bufferedReader.readLine();

				while (line != null) {

					String[] lineValues;
					lineValues = line.split(" ");

					int sector = Integer.parseInt(lineValues[0]);
					int layer = Integer.parseInt(lineValues[1]);
					int component = Integer.parseInt(lineValues[2]);
					double attlenLeft = Double.parseDouble(lineValues[3]);
					double attlenErrLeft = Double.parseDouble(lineValues[4]);
					double attlenRight = Double.parseDouble(lineValues[5]);
					double attlenErrRight = Double.parseDouble(lineValues[6]);

					convValues.addEntry(sector, layer, component);
					convValues.setDoubleValue(attlenLeft,
							"attlen_L", sector, layer, component);
					convValues.setDoubleValue(attlenErrLeft,
							"attlen_L_err", sector, layer, component);
					convValues.setDoubleValue(attlenRight,
							"attlen_R", sector, layer, component);
					convValues.setDoubleValue(attlenErrRight,
							"attlen_R_err", sector, layer, component);

					line = bufferedReader.readLine();
				}

				bufferedReader.close();
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
				System.out.println(
						"Unable to open file '"
								+ prevCalFilename + "'");
			} catch (IOException ex) {
				System.out.println(
						"Error reading file '"
								+ prevCalFilename + "'");
				ex.printStackTrace();
			}
		} else if (calDBSource == CAL_DEFAULT) {
			for (int sector = 1; sector <= 24; sector++) {
				for (int layer = 1; layer <= 3; layer++) {
					int component = 1;

					attenuationValues.addEntry(sector, layer, component);                    
					attenuationValues.setDoubleValue(EXPECTED_ATTLEN,
							"attlen_L", sector, layer, component);
					attenuationValues.setDoubleValue(ALLOWED_DIFF,
							"attlen_L_err", sector, layer, component);
					attenuationValues.setDoubleValue(EXPECTED_ATTLEN,
							"attlen_R", sector, layer, component);
					attenuationValues.setDoubleValue((-1) * ALLOWED_DIFF,
							"attlen_R_err", sector, layer, component);

				}
			}
		} else if (calDBSource == CAL_DB) {
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			convValues = dcp.readConstants("/calibration/cnd/Attenuation");
			dcp.disconnect();
		}
	}

	@Override
	public void resetEventListener() {

		if(CNDCalibration.inspectatt!=0){
			showSlices=true;
		}
		if(CNDCalibration.modeatt!=0){
			fitMethod=1;
		}
		if(CNDCalibration.minYaxisatt!=0){
			HIST_Y_MIN=CNDCalibration.minYaxisatt;
		}
		if(CNDCalibration.maxYaxisatt!=0){
			HIST_Y_MAX=CNDCalibration.maxYaxisatt;
		}
		if(CNDCalibration.binYaxisatt!=0){
			HIST_Y_BINS=CNDCalibration.binYaxisatt;
		}
		if(CNDCalibration.binXaxisatt!=0){
			HIST_X_BINS=CNDCalibration.binXaxisatt;
		}
		
		// GM perform init processing
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				// create all the histograms
				//            int numBins = (int) (paddleLength(layer)*0.6);  // 1 bin per 2cm + 10% either side
				//            double min = paddleLength(layer) * -0.6;
				//            double max = paddleLength(layer) * 0.6;
				H2F histL
				= new H2F("adcL_vs_z",
						"adcL_vs_z",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);
				H2F histR
				= new H2F("adcR_vs_z",
						"adcR_vs_z",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);

				F1D funcL = new F1D("funcL", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);
				F1D funcR = new F1D("funcR", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);

				GraphErrors graphL = new GraphErrors("adcL_vs_z_graph");
				graphL.setName("adcL_vs_z_graph");
				graphL.setMarkerSize(MARKER_SIZE);
				graphL.setLineThickness(MARKER_LINE_WIDTH);

				GraphErrors graphR = new GraphErrors("adcR_vs_z_graph");
				graphR.setName("adcR_vs_z_graph");
				graphR.setMarkerSize(MARKER_SIZE);
				graphR.setLineThickness(MARKER_LINE_WIDTH);

				DataGroup dg = new DataGroup(2, 2);
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

			if (paddlePair.includeInCalib()) {

				int sector = paddlePair.getDescriptor().getSector();
				int layer = paddlePair.getDescriptor().getLayer();
				int component = paddlePair.getDescriptor().getComponent();

				int layer_index = layer - 1;

				double hitPosition = 0.0;
				double pathInPaddle = 0.0;

				int directHitPaddle = 0;
				int neighbourHitPaddle = 1;

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
					double[] veffLgArray = {16.0, 16.0};  //cm/ns, left and right paddles

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
					if (directHitPaddle == 0) {
						dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").fill(hitPosition, Math.log(paddlePair.ADCL));
					} else if (directHitPaddle == 1) {
						dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").fill(hitPosition, Math.log(paddlePair.ADCR));
					}

				} else if (!(test)) {

					// hitPosition from tracking:
					hitPosition = paddlePair.zPosCND();

					pathInPaddle = paddlePair.T_LENGTH;

					// CND paddle thickness is 3.0cm
					// Normalise the ADC values as if they were verticle tracks traversing only 3.0cm of paddle thickness
					double normalisingADCFactor = 0.0;
					normalisingADCFactor = 3.0 / pathInPaddle;

					//                    dataGroups.getItem(sector,layer,component).getH2F("adcL_vs_z").fill( hitPosition, normalisingADCFactor*paddlePair.ADCL);
					//                    dataGroups.getItem(sector,layer,component).getH2F("adcR_vs_z").fill( hitPosition, normalisingADCFactor*paddlePair.ADCR);
					//                    if (directHitPaddle == 0 && paddlePair.ZPOS!=0.0) {
					//                        dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").fill(hitPosition, Math.log(normalisingADCFactor * paddlePair.ADCL));
					//                    } else if (directHitPaddle == 1 && paddlePair.ZPOS!=0.0) {
					//                        dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").fill(hitPosition, Math.log(normalisingADCFactor * paddlePair.ADCR));
					//                    }

					if (directHitPaddle == 0 && paddlePair.ZPOS!=0.0) {
						dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").fill(hitPosition, Math.log((float)paddlePair.ADCL/paddlePair.ADCR));
						//dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").fill(hitPosition, Math.log(paddlePair.ADCR));

						//                      System.out.println();
						                     //    System.out.println("ADCL = " + (float)paddlePair.ADCL+" ADCR = " + (float)paddlePair.ADCR);
						//                      System.out.println("logratio = " +  Math.log((float)paddlePair.ADCL/(float)paddlePair.ADCR));
					} else if (directHitPaddle == 1 && paddlePair.ZPOS!=0.0) {
						dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").fill(hitPosition, Math.log((float)paddlePair.ADCR/paddlePair.ADCL));
						//dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").fill(hitPosition, Math.log(paddlePair.ADCL));

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

		System.out.println("IN ATTENUATION FIT for S" + sector + " L" + layer + " C" + component);
		//        System.out.println("minRange and maxRange = " + minRange + " " + maxRange);

		double paddlelength=0.0;

		if (layer == 1) {
			paddlelength = 66.572;
		} else if (layer == 2) {
			paddlelength = 70.000;
		} else if (layer == 3) {
			paddlelength = 73.428;
		}

		H2F histL = dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z");
		H2F histR = dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z");
		GraphErrors graphL = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcL_vs_z_graph");
		GraphErrors graphR = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcR_vs_z_graph");


		int fitMinEventsL = 2*histL.getEntries()/HIST_X_BINS;
		int fitMinEventsR = 2*histR.getEntries()/HIST_X_BINS;
		
		if(CNDCalibration.mineventatt!=0){
			fitMinEventsL = CNDCalibration.mineventatt;
			fitMinEventsR = CNDCalibration.mineventatt;
		}

		// fit function to the graph of means
		if (fitMethod == FIT_METHOD_SF) {
			ParallelSliceFitter psfL = new ParallelSliceFitter(histL);
			psfL.setFitMode(fitMode);
			psfL.setMinEvents(fitMinEventsL);
			psfL.setBackgroundOrder(backgroundSF);
			psfL.setNthreads(1);
			//double maxhistL = histL.getProfileX().getMax();
			//System.out.println("max "+maxhistL);
			//psfL.setRange(maxhistL-1., maxhistL+1);
			//psfL.setShowProgress(false);
			//psfL.setMaxChiSquare(MAX_CHI);
			setOutput(false);
			psfL.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfL.inspectFits();
			}
			graphL.copy(fixGraph(psfL.getMeanSlices(), "adcL_vs_z_graph"));

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
			//psfR.setMaxChiSquare(MAX_CHI);
			//psfR.setShowProgress(false);
			setOutput(false);
			psfR.fitSlicesX();
			setOutput(true);
			if (showSlices ) {
				psfR.inspectFits();
				//showSlices = false;
			}
			graphR.copy(fixGraph(psfR.getMeanSlices(), "adcR_vs_z_graph"));
		} else if (fitMethod == FIT_METHOD_MAX) {

			// ****
			// ****NEED TO EDIT THIS****
			// ****
			maxGraphError = 0.10;
			graphL.copy(maxGraph(histL, "adcL_vs_z_graph"));
			graphR.copy(maxGraph(histR, "adcR_vs_z_graph"));

		} else {
			//            graphL.copy(histL.getProfileX());
			//            graphR.copy(histR.getProfileX());
		}

		// ****
		// ****LEFT GRAPH
		// ****
		// find the range for the fit
		double lowLimit = FIT_MIN;
		double highLimit = FIT_MAX;
		int lowLimitBin = 0;

		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		} else {
			// Fit over filled data only:
			if (graphL.getDataSize(0) >= 2) {
				for (int i = 0; i < graphL.getDataSize(0); i++) {
					if (graphL.getDataX(i) > 0.0) {
						lowLimit = graphL.getDataX(i);
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
		} else {
			// Fit over filled data only:
			if (graphL.getDataSize(0) >= 2) {
				for (int i = (graphL.getDataSize(0) - 1); i > lowLimitBin; i--) {
					if (graphL.getDataX(i) > 0.0) {
						if(graphL.getDataX(i)<paddlelength){
							highLimit = graphL.getDataX(i);
							break;
						}
						else
						{
							highLimit = paddlelength;
							break;
						}
					}
				}
			} else {
				highLimit = FIT_MAX;
			}
		}

		F1D funcL = dataGroups.getItem(sector, layer, component).getF1D("funcL");
		funcL.setRange(lowLimit, highLimit);
		//        if (sector==1 && layer ==1){
		//            System.out.println("lowLimit = " + lowLimit);
		//            System.out.println("highLimit = " + highLimit);
		//            graphL.getData(0, 0);
		//            graphL.getData(1, 1);
		//        }
		funcL.setParameter(0, 7.0);  //offset estimate
		funcL.setParameter(1, (1. / EXPECTED_ATTLEN));  //gradient estimate

		try {
			DataFitter.fit(funcL, graphL, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}
		
		// ****
		// ****RIGHT GRAPH
		// ****
		lowLimit = FIT_MIN;
		highLimit = FIT_MAX;
		lowLimitBin = 0;
		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		} else {
			// Fit over filled data only:
			if (graphR.getDataSize(0) >= 2) {
				for (int i = 0; i < graphR.getDataSize(0); i++) {
					if (graphR.getDataX(i) > 0.0) {
						lowLimit = graphR.getDataX(i);
						lowLimitBin = i;
						break;
					}
				}
			} else {
				lowLimit = FIT_MIN;
			}
		}

//		if (maxRange != UNDEFINED_OVERRIDE) {
//			// use custom values for fit
//			highLimit = maxRange;
//		} else {
//			// Fit over filled data only:
//			if (graphR.getDataSize(0) >= 2) {
//				for (int i = (graphR.getDataSize(0) - 1); i > lowLimitBin; i--) {
//					if (graphR.getDataX(i) > 0.0) {
//						if(graphL.getDataX(i)<paddlelength){
//							highLimit = graphR.getDataX(i);
//							break;
//						}
//						else if(graphL.getDataX(i)>paddlelength)
//						{
//							highLimit = paddlelength;
//							break;
//						}
//					}
//				}
//			} else {
//				highLimit = FIT_MAX;
//			}
//		}
		
		if (maxRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			highLimit = maxRange;
		} else {
			// Fit over filled data only:
			if (graphR.getDataSize(0) >= 2) {
				for (int i = (graphR.getDataSize(0) - 1); i > lowLimitBin; i--) {
					if (graphR.getDataX(i) > 0.0) {
						if(graphR.getDataX(i)<paddlelength){
							highLimit = graphR.getDataX(i);
							break;
						}
						else
						{
							highLimit = paddlelength;
							break;
						}
					}
				}
			} else {
				highLimit = FIT_MAX;
			}
		}

		F1D funcR = dataGroups.getItem(sector, layer, component).getF1D("funcR");

		funcR.setRange(lowLimit, highLimit);

		funcR.setParameter(0, 7.0);  //offset estimate
		funcR.setParameter(1, (1. / EXPECTED_ATTLEN));  //gradient estimate

		try {
			DataFitter.fit(funcR, graphR, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
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
			graph = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcL_vs_z_graph");
		} else if (side == 2){
			func = dataGroups.getItem(sector, layer, component).getF1D("funcR");
			graph = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcR_vs_z_graph");
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
	public Double getAttLenLeft(int sector, int layer, int component) {

		double attLenLeft = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			attLenLeft = overrideVal;
		} else {
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcL").getParameter(1);
			attLenLeft = -2. / gradient;
		}
		return attLenLeft;
	}

	public Double getAttLenLeftError(int sector, int layer, int component) {

		double attLenLeft = 0.0;
		double attLenLefterror = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			attLenLeft = overrideVal;
		} else {
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcL").getParameter(1);
			double gradienterror = dataGroups.getItem(sector, layer, component).getF1D("funcL").parameter(1).error();
			attLenLeft = -2. / gradient;
			attLenLefterror = gradienterror*attLenLeft*attLenLeft/2.;
		}
		return attLenLefterror;
	}

	public Double getAttLenRight(int sector, int layer, int component) {

		double attLenRight = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_RIGHT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			attLenRight = overrideVal;
		} else {
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcR").getParameter(1);
			attLenRight = -2. / gradient;
		}
		return attLenRight;
	}

	public Double getAttLenRightError(int sector, int layer, int component) {

		double attLenRight = 0.0;
		double attLenRighterror = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_RIGHT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			attLenRight = overrideVal;
		} else {
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcR").getParameter(1);
			double gradienterror = dataGroups.getItem(sector, layer, component).getF1D("funcR").parameter(1).error();
			attLenRight = -2. / gradient;
			attLenRighterror = gradienterror* attLenRight* attLenRight/2.;
		}
		return attLenRighterror;
	}

	@Override
	public void saveRow(int sector, int layer, int component) {

		calib.setDoubleValue(getAttLenLeft(sector, layer, component),
				"attlen_L", sector, layer, component);
		calib.setDoubleValue(getAttLenLeftError(sector, layer, component),
				"attlen_L_err", sector, layer, component);
		calib.setDoubleValue(getAttLenRight(sector, layer, component),
				"attlen_R", sector, layer, component);
		calib.setDoubleValue(getAttLenRightError(sector, layer, component),
				"attlen_R_err", sector, layer, component);

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
		dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").setTitle("ADCL vs Position - S" + sector + " L" + layer + " C" + component);
		dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").setTitle("ADCR vs Position - S" + sector + " L" + layer + " C" + component);
		if (test) {
			dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcL_vs_z_graph").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcR_vs_z_graph").setTitleX("Calculated hit position (cm)");
		} else {
			dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcL_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcR_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
		}

		dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z").setTitleY("Log(ADCL) (log(channels))");
		dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z").setTitleY("Log(ADCR) (log(channels))");
		dataGroups.getItem(sector, layer, component).getGraph("adcL_vs_z_graph").setTitle("Log(ADCL) vs Position");
		dataGroups.getItem(sector, layer, component).getGraph("adcR_vs_z_graph").setTitle("Log(ADCR) vs Position");
		dataGroups.getItem(sector, layer, component).getGraph("adcL_vs_z_graph").setTitleY("ADC slice peak(log(channels))");
		dataGroups.getItem(sector, layer, component).getGraph("adcR_vs_z_graph").setTitleY("ADC slice peak (log(channels))");

	}

	@Override
	public void drawPlots(int sector, int layer, int component, EmbeddedCanvas canvas) {

		if (showPlotType == "ATTLEN_L") {
			H2F histL = dataGroups.getItem(sector, layer, component).getH2F("adcL_vs_z");
			histL.setTitle("");
			histL.setTitleX("");
			histL.setTitleY("");
			canvas.draw(histL);
		} else if (showPlotType == "ATTLEN_R") {
			H2F histR = dataGroups.getItem(sector, layer, component).getH2F("adcR_vs_z");
			histR.setTitle("");
			histR.setTitleX("");
			histR.setTitleY("");
			canvas.draw(histR);
		} else if (showPlotType == "ATTLEN_L_GRAPH") {
			GraphErrors graphL = dataGroups.getItem(sector, layer, component).getGraph("adcL_vs_z_graph");
			if (graphL.getDataSize(0) != 0) {
				graphL.setTitle("");
				graphL.setTitleX("");
				graphL.setTitleY("");
				canvas.draw(graphL);
				canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("funcL"), "same");
			}
		} else if (showPlotType == "ATTLEN_R_GRAPH") {
			GraphErrors graphR = dataGroups.getItem(sector, layer, component).getGraph("adcR_vs_z_graph");
			if (graphR.getDataSize(0) != 0) {
				graphR.setTitle("");
				graphR.setTitleX("");
				graphR.setTitleY("");
				canvas.draw(graphR);
				canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("funcR"), "same");
			}
		}
	}

	@Override
	public void showPlots(int sector, int layer) {

		showPlotType = "ATTLEN_L";
		stepName = "Attenuation - Left";
		super.showPlots(sector, layer);
		showPlotType = "ATTLEN_R";
		stepName = "Attenuation - Right";
		super.showPlots(sector, layer);
		showPlotType = "ATTLEN_L_GRAPH";
		stepName = "Attenuation Graph - Left";
		super.showPlots(sector, layer);
		showPlotType = "ATTLEN_R_GRAPH";
		stepName = "Attenuation Graph - Right";
		super.showPlots(sector, layer);

	}

	@Override
	public DataGroup getSummary() {

		double[] sectorNumber = new double[24];
		double[] zeroUncs = new double[24];        
		double[] attLenL3L = new double[24];
		double[] attLenL3R = new double[24];
		double[] attLenL2L = new double[24];
		double[] attLenL2R = new double[24];
		double[] attLenL1L = new double[24];
		double[] attLenL1R = new double[24];


		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				sectorNumber[sector - 1] = (double) sector;
				zeroUncs[sector - 1] = 0.0;

				if (layer == 3){
					attLenL3L[sector - 1] = getAttLenLeft(sector, layer, component);
					attLenL3R[sector - 1] = getAttLenRight(sector, layer, component);
				} else if (layer == 2){
					attLenL2L[sector - 1] = getAttLenLeft(sector, layer, component);
					attLenL2R[sector - 1] = getAttLenRight(sector, layer, component);
				} else if (layer == 1){
					attLenL1L[sector - 1] = getAttLenLeft(sector, layer, component);
					attLenL1R[sector - 1] = getAttLenRight(sector, layer, component);
				}
			}
		}

		GraphErrors allL3L = new GraphErrors("allL3L", sectorNumber,
				attLenL3L, zeroUncs, zeroUncs);

		allL3L.setTitleX("Sector Number");
		allL3L.setTitleY("Att Len (cm)");
		allL3L.setMarkerSize(MARKER_SIZE);
		allL3L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL3R = new GraphErrors("allL3R", sectorNumber,
				attLenL3R, zeroUncs, zeroUncs);

		allL3R.setTitleX("Sector Number");
		allL3R.setTitleY("Att Len (cm)");
		allL3R.setMarkerSize(MARKER_SIZE);
		allL3R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL2L = new GraphErrors("allL2L", sectorNumber,
				attLenL2L, zeroUncs, zeroUncs);

		allL2L.setTitleX("Sector Number");
		allL2L.setTitleY("Att Len (cm)");
		allL2L.setMarkerSize(MARKER_SIZE);
		allL2L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL2R = new GraphErrors("allL2R", sectorNumber,
				attLenL2R, zeroUncs, zeroUncs);

		allL2R.setTitleX("Sector Number");
		allL2R.setTitleY("Att Len (cm)");
		allL2R.setMarkerSize(MARKER_SIZE);
		allL2R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1L = new GraphErrors("allL1L", sectorNumber,
				attLenL1L, zeroUncs, zeroUncs);

		allL1L.setTitleX("Sector Number");
		allL1L.setTitleY("Att Len (cm)");
		allL1L.setMarkerSize(MARKER_SIZE);
		allL1L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1R = new GraphErrors("allL1R", sectorNumber,
				attLenL1R, zeroUncs, zeroUncs);

		allL1R.setTitleX("Sector Number");
		allL1R.setTitleY("Att Len (cm)");
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
