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
 * Based on the work of Louise Clark and Gavin Murdoch thanks!
 *
 * @author  Pierre Chatagnon
 */

public class CNDEnergyEventListener extends CNDCalibrationEngine {

	private static boolean test = false;  //Test working for raw ADC and TDC values

	// hists
	//	public final int GEOMEAN = 0;
	//	public final int LOGRATIO = 1;
	public final int MIPDIR_L = 0;
	public final int MIPDIR_R = 1;
	public final int MIPDIR_L_GRAPH = 2;
	public final int MIPDIR_R_GRAPH = 3;
	public final int MIPINDIR_L = 4;
	public final int MIPINDIR_R = 5;
	public final int MIPINDIR_L_GRAPH = 6;
	public final int MIPINDIR_R_GRAPH = 7;

	public final int OVERRIDE_LEFT = 0;
	public final int OVERRIDE_RIGHT = 0;

	public final double[] EXPECTED_MIPDIR = {1200., 1400., 1800.};
	public final double EXPECTED_MIPINDIR = 500.;
	public final double ALLOWED_DIFF = 100.;


	// Switch to go to Cosmics mode

	public final boolean Cosmics = false;

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
	public final double MAX_CHI = 10;
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
	public double HIST_Y_MIN = 6.0;
	public double HIST_Y_MAX = 10.0;

	//        public int HIST_Y_BINS = 300;
	//        public double HIST_Y_MIN = 2.4;
	//        public double HIST_Y_MAX = 8.4;
	private final double FIT_MIN = HIST_X_MIN;
	private final double FIT_MAX = HIST_X_MAX;
	private final double[] TDC_MIN = {0.0, 25000.0, 24000.0, 25000.0};
	private final double[] TDC_MAX = {0.0, 28000.0, 27000.0, 28000.0};

	public CNDEnergyEventListener() {

		stepName = "Energy";
		fileNamePrefix = "CND_CALIB_Energy_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"mip_dir_L/F:mip_dir_L_err/F:mip_indir_L/F:mip_indir_L_err/F:mip_dir_R/F:mip_dir_R_err/F:mip_indir_R/F:mip_indir_R_err/F");

		//                calib = new CalibrationConstants(3,
		//                "tdc_conv_left/F:tdc_conv_right/F");
		calib.setName("/calibration/cnd/Energy");
		calib.setPrecision(5);


		// a finir modifier ou supprimer
		// assign constraints to all paddles
		//calib.addConstraint(3, EXPECTED_ATTLEN - ALLOWED_DIFF,
		// EXPECTED_ATTLEN + ALLOWED_DIFF);
		//calib.addConstraint(4, (-1) * ALLOWED_DIFF,
		//     (1) * ALLOWED_DIFF);
		//calib.addConstraint(5, EXPECTED_ATTLEN - ALLOWED_DIFF,
		//       EXPECTED_ATTLEN + ALLOWED_DIFF);
		//calib.addConstraint(6, (-1) * ALLOWED_DIFF,
		//    (1) * ALLOWED_DIFF);

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
					double MIPdirLeft = Double.parseDouble(lineValues[3]);
					double MIPdirErrLeft = Double.parseDouble(lineValues[4]);
					double MIPindirLeft = Double.parseDouble(lineValues[5]);
					double MIPindirErrLeft = Double.parseDouble(lineValues[6]);
					double MIPdirRight = Double.parseDouble(lineValues[7]);
					double MIPdirErrRight = Double.parseDouble(lineValues[8]);
					double MIPindirRight = Double.parseDouble(lineValues[9]);
					double MIPindirErrRight = Double.parseDouble(lineValues[10]);

					MIPValues.addEntry(sector, layer, component);
					MIPValues.setDoubleValue(MIPdirLeft,
							"mip_dir_L", sector, layer, component);
					MIPValues.setDoubleValue(MIPdirErrLeft,
							"mip_dir_L_err", sector, layer, component);
					MIPValues.setDoubleValue(MIPindirLeft,
							"mip_indir_L", sector, layer, component);
					MIPValues.setDoubleValue(MIPindirErrLeft,
							"mip_indir_L_err", sector, layer, component);
					MIPValues.setDoubleValue(MIPdirRight,
							"mip_dir_R", sector, layer, component);
					MIPValues.setDoubleValue(MIPdirErrRight,
							"mip_dir_R_err", sector, layer, component);
					MIPValues.setDoubleValue(MIPindirRight,
							"mip_indir_R", sector, layer, component);
					MIPValues.setDoubleValue(MIPindirErrRight,
							"mip_indir_R_err", sector, layer, component);

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

					MIPValues.addEntry(sector, layer, component);
					MIPValues.setDoubleValue(EXPECTED_MIPDIR[layer-1],
							"mip_dir_L", sector, layer, component);
					MIPValues.setDoubleValue(ALLOWED_DIFF,
							"mip_dir_L_err", sector, layer, component);
					MIPValues.setDoubleValue(EXPECTED_MIPINDIR,
							"mip_indir_L", sector, layer, component);
					MIPValues.setDoubleValue(ALLOWED_DIFF,
							"mip_indir_L_err", sector, layer, component);
					MIPValues.setDoubleValue(EXPECTED_MIPDIR[layer-1],
							"mip_dir_R", sector, layer, component);
					MIPValues.setDoubleValue(ALLOWED_DIFF,
							"mip_dir_R_err", sector, layer, component);
					MIPValues.setDoubleValue(EXPECTED_MIPINDIR,
							"mip_indir_R", sector, layer, component);
					MIPValues.setDoubleValue(ALLOWED_DIFF,
							"mip_indir_R_err", sector, layer, component);

				}
			}
		} else if (calDBSource == CAL_DB) {
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			MIPValues = dcp.readConstants("/calibration/cnd/Energy");
			dcp.disconnect();
		}
	}

	@Override
	public void resetEventListener() {

		// GM perform init processing
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				// create all the histograms
				//            int numBins = (int) (paddleLength(layer)*0.6);  // 1 bin per 2cm + 10% either side
				//            double min = paddleLength(layer) * -0.6;
				//            double max = paddleLength(layer) * 0.6;
				H2F histLdir
				= new H2F("adcLdir_vs_z",
						"adcLdir_vs_z",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);
				H2F histRdir
				= new H2F("adcRdir_vs_z",
						"adcRdir_vs_z",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);
				H2F histLindir
				= new H2F("adcLindir_vs_z",
						"adcLindir_vs_z",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);
				H2F histRindir
				= new H2F("adcRindir_vs_z",
						"adcRindir_vs_z",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);

				F1D funcLdir = new F1D("funcLdir", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);
				F1D funcRdir = new F1D("funcRdir", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);
				F1D funcLindir = new F1D("funcLindir", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);
				F1D funcRindir = new F1D("funcRindir", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);

				GraphErrors graphdirL = new GraphErrors("adcLdir_vs_z_graph");
				graphdirL.setName("adcLdir_vs_z_graph");
				graphdirL.setMarkerSize(MARKER_SIZE);
				graphdirL.setLineThickness(MARKER_LINE_WIDTH);

				GraphErrors graphdirR = new GraphErrors("adcRdir_vs_z_graph");
				graphdirR.setName("adcRdir_vs_z_graph");
				graphdirR.setMarkerSize(MARKER_SIZE);
				graphdirR.setLineThickness(MARKER_LINE_WIDTH);

				GraphErrors graphindirL = new GraphErrors("adcLindir_vs_z_graph");
				graphindirL.setName("adcLindir_vs_z_graph");
				graphindirL.setMarkerSize(MARKER_SIZE);
				graphindirL.setLineThickness(MARKER_LINE_WIDTH);

				GraphErrors graphindirR = new GraphErrors("adcRindir_vs_z_graph");
				graphindirR.setName("adcRindir_vs_z_graph");
				graphindirR.setMarkerSize(MARKER_SIZE);
				graphindirR.setLineThickness(MARKER_LINE_WIDTH);

				DataGroup dg = new DataGroup(2, 4);
				dg.addDataSet(histLdir, 0);
				dg.addDataSet(histRdir, 1);
				dg.addDataSet(histLindir, 2);
				dg.addDataSet(histRindir, 3);
				dg.addDataSet(graphdirL, 4);
				dg.addDataSet(funcLdir, 4);
				dg.addDataSet(graphdirR, 5);
				dg.addDataSet(funcRdir, 5);
				dg.addDataSet(graphindirL, 6);
				dg.addDataSet(funcLindir, 6);
				dg.addDataSet(graphindirR, 7);
				dg.addDataSet(funcRindir, 7);
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
				double paddleLength=0.0;
				double pathlength=0.0;
				double betaCND=0.0;
				double cutPIDpionVALUE=0.1;
				double cutPIDpion=0.0;

				int directHitPaddle = 0;
				int neighbourHitPaddle = 1;

				double time[] = {paddlePair.tdcToTime(paddlePair.TDCL), paddlePair.tdcToTime(paddlePair.TDCR)};

				// Only going to deal with direct hits!!
				if (time[0] < time[1]-leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)) {

					directHitPaddle = 0;
					neighbourHitPaddle = 1;

				} else if (time[1] < time[0]+leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)) {

					directHitPaddle = 1;
					neighbourHitPaddle = 0;
				}
				if (!(test)) {

					// hitPosition from tracking:
					hitPosition = paddlePair.zPosCND();

					pathInPaddle = paddlePair.T_LENGTH;

					paddleLength = paddlePair.paddleLength();

					pathlength= paddlePair.PATH_LENGTH;

					boolean include = false;
					if(Cosmics){


						for (int j= 0; j < paddlePairList.size(); j++) {
							//look for other hits in the same sector
							if ( Math.abs(paddlePairList.get(j).XPOSt+paddlePair.XPOSt) < 10 && 
									Math.abs(paddlePairList.get(j).YPOSt+paddlePair.YPOSt) < 10 && 
									Math.abs(paddlePairList.get(j).ZPOSt-paddlePair.ZPOSt) < 10
									) {
								include=true;
								//System.out.println(" X = " + paddlePair.XPOSt+" Y = " + paddlePair.YPOSt+" Z = " + paddlePair.ZPOSt);
								break;
								//System.out.println("layer "+layer+" layer 2 "+paddlePairList.get(j).getDescriptor().getLayer());
							}
						}

					}

					// cut sur le beta pion (marche pas trop)
					if(paddlePair.EVENT_START_TIME!=0.0 && pathlength!=0.0){
						betaCND=pathlength/(29.98*(paddlePair.TOF_TIME-paddlePair.EVENT_START_TIME));

						cutPIDpion=betaCND-paddlePair.BETA;
						//                    	 System.out.println();
						//                    	 System.out.println(" beta CND = " + betaCND+" beta CVT = " + paddlePair.BETA);
						//                    	 System.out.println("diff beta = " + Math.abs(cutPIDpion));
					}

					if(!Cosmics){
						// CND paddle thickness is 3.0cm
						// Normalise the ADC values as if they were verticle tracks traversing only 3.0cm of paddle thickness
						double normalisingADCFactor = 0.0;
						normalisingADCFactor = 3.0 / pathInPaddle;

						//                    dataGroups.getItem(sector,layer,component).getH2F("adcL_vs_z").fill( hitPosition, normalisingADCFactor*paddlePair.ADCL);
						//                    dataGroups.getItem(sector,layer,component).getH2F("adcR_vs_z").fill( hitPosition, normalisingADCFactor*paddlePair.ADCR);
						if (directHitPaddle == 0 && paddlePair.ZPOS!=0.0 && paddlePair.CHARGE==-1){ //&& paddlePair.EVENT_START_TIME!=0.0 && Math.abs(cutPIDpion)<cutPIDpionVALUE) {
							dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").fill(hitPosition, Math.log( normalisingADCFactor *paddlePair.ADCL));
							dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").fill(paddleLength-hitPosition, Math.log( normalisingADCFactor *paddlePair.ADCR));
							//                        System.out.println(" ");
							//                        System.out.println("z = " + hitPosition);
							//                        System.out.println("ADCR = " + paddlePair.ADCR);
							//                        System.out.println("ADCL = " + paddlePair.ADCL);
						} else if (directHitPaddle == 1 && paddlePair.ZPOS!=0.0  && paddlePair.CHARGE==-1){ // && paddlePair.EVENT_START_TIME!=0.0 && Math.abs(cutPIDpion)<cutPIDpionVALUE) {
							dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").fill(hitPosition, Math.log(normalisingADCFactor * paddlePair.ADCR));
							dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").fill(paddleLength-hitPosition, Math.log(normalisingADCFactor * paddlePair.ADCL));
						}

					}

					if(Cosmics){
						if (directHitPaddle == 0 ){//&& include ){ //&& paddlePair.EVENT_START_TIME!=0.0 && Math.abs(cutPIDpion)<cutPIDpionVALUE) {
							dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").fill(paddlePair.zPostCND(), Math.log( paddlePair.ADCL));
							dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").fill(paddleLength-paddlePair.zPostCND(), Math.log( paddlePair.ADCR));
							//                             System.out.println(" ");
							//                             System.out.println("z = " + hitPosition);
							//                             System.out.println("ADCR = " + paddlePair.ADCR);
							//                             System.out.println("ADCL = " + paddlePair.ADCL);
						} else if (directHitPaddle == 1  ){//&& include ){ // && paddlePair.EVENT_START_TIME!=0.0 && Math.abs(cutPIDpion)<cutPIDpionVALUE) {
							dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").fill(paddlePair.zPostCND(), Math.log( paddlePair.ADCR));
							dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").fill(paddleLength-paddlePair.zPostCND(), Math.log( paddlePair.ADCL));
						}
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

		System.out.println("IN ENERGY FIT for S" + sector + " L" + layer + " C" + component);
		//        System.out.println("minRange and maxRange = " + minRange + " " + maxRange);

		H2F histLdir = dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z");
		H2F histRdir = dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z");
		GraphErrors graphLdir = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcLdir_vs_z_graph");
		GraphErrors graphRdir = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcRdir_vs_z_graph");

		H2F histLindir = dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z");
		H2F histRindir = dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z");
		GraphErrors graphLindir = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcLindir_vs_z_graph");
		GraphErrors graphRindir = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcRindir_vs_z_graph");

		// fit function to the graph of means
		if (fitMethod == FIT_METHOD_SF) {
			ParallelSliceFitter psfLdir = new ParallelSliceFitter(histLdir);
			psfLdir.setFitMode(fitMode);
			psfLdir.setMinEvents(fitMinEvents);
			psfLdir.setBackgroundOrder(backgroundSF);
			psfLdir.setNthreads(1);
			//psfL.setShowProgress(false);
			setOutput(false);
			psfLdir.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfLdir.inspectFits();
			}
			graphLdir.copy(fixGraph(psfLdir.getMeanSlices(), "adcLdir_vs_z_graph"));


			ParallelSliceFitter psfRdir = new ParallelSliceFitter(histRdir);
			psfRdir.setFitMode(fitMode);
			psfRdir.setMinEvents(fitMinEvents);
			psfRdir.setBackgroundOrder(backgroundSF);
			psfRdir.setNthreads(1);
			//psfR.setShowProgress(false);
			setOutput(false);
			psfRdir.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfRdir.inspectFits();
				showSlices = false;
			}
			graphRdir.copy(fixGraph(psfRdir.getMeanSlices(), "adcRdir_vs_z_graph"));


			ParallelSliceFitter psfLindir = new ParallelSliceFitter(histLindir);
			psfLindir.setFitMode(fitMode);
			psfLindir.setMinEvents(fitMinEvents);
			psfLindir.setBackgroundOrder(backgroundSF);
			psfLindir.setNthreads(1);
			//psfL.setShowProgress(false);
			setOutput(false);
			psfLindir.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfLindir.inspectFits();
			}
			graphLindir.copy(fixGraph(psfLindir.getMeanSlices(), "adcLindir_vs_z_graph"));


			ParallelSliceFitter psfRindir = new ParallelSliceFitter(histRindir);
			psfRindir.setFitMode(fitMode);
			psfRindir.setMinEvents(fitMinEvents);
			psfRindir.setBackgroundOrder(backgroundSF);
			psfRindir.setNthreads(1);
			//psfR.setShowProgress(false);
			setOutput(false);
			psfRindir.fitSlicesX();
			setOutput(true);
			if (showSlices) {
				psfRindir.inspectFits();
				showSlices = false;
			}
			graphRindir.copy(fixGraph(psfRindir.getMeanSlices(), "adcRindir_vs_z_graph"));
		} else if (fitMethod == FIT_METHOD_MAX) {

			// ****
			// ****NEED TO EDIT THIS****
			// ****
			maxGraphError = 0.15;
			graphLdir.copy(maxGraph(histLdir, "adcLdir_vs_z_graph"));
			graphRdir.copy(maxGraph(histRdir, "adcRdir_vs_z_graph"));
			graphLindir.copy(maxGraph(histLindir, "adcLindir_vs_z_graph"));
			graphRindir.copy(maxGraph(histRindir, "adcRindir_vs_z_graph"));


		} else {
			//            graphL.copy(histL.getProfileX());
			//            graphR.copy(histR.getProfileX());
		}

		// ****
		// ****FIRST GRAPH
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
			if (graphLdir.getDataSize(0) >= 2) {
				for (int i = 0; i < graphLdir.getDataSize(0); i++) {
					if (graphLdir.getDataX(i) > 0.0) {
						lowLimit = graphLdir.getDataX(i);
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
			if (graphLdir.getDataSize(0) >= 2) {
				for (int i = (graphLdir.getDataSize(0) - 1); i > lowLimitBin; i--) {
					if (graphLdir.getDataX(i) > 0.0) {
						highLimit = graphLdir.getDataX(i);
						break;
					}
				}
			} else {
				highLimit = FIT_MAX;
			}
		}

		F1D funcLdir = dataGroups.getItem(sector, layer, component).getF1D("funcLdir");
		funcLdir.setRange(lowLimit, highLimit);

		//        if (sector==1 && layer ==1){
		//            System.out.println("lowLimit = " + lowLimit);
		//            System.out.println("highLimit = " + highLimit);
		//            graphL.getData(0, 0);
		//            graphL.getData(1, 1);
		//        }
		funcLdir.setParameter(0, 7.0);  //offset estimate
		funcLdir.setParameter(1, (1. / 150.));  //gradient estimate

		try {
			DataFitter.fit(funcLdir, graphLdir, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}

		// ****
		// ****SECOND GRAPH
		// ****
		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		} else {
			// Fit over filled data only:
			if (graphRdir.getDataSize(0) >= 2) {
				for (int i = 0; i < graphRdir.getDataSize(0); i++) {
					if (graphRdir.getDataX(i) > 0.0) {
						lowLimit = graphRdir.getDataX(i);
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
			if (graphRdir.getDataSize(0) >= 2) {
				for (int i = (graphRdir.getDataSize(0) - 1); i > lowLimitBin; i--) {
					if (graphRdir.getDataX(i) > 0.0) {
						highLimit = graphRdir.getDataX(i);
						break;
					}
				}
			} else {
				highLimit = FIT_MAX;
			}
		}

		F1D funcRdir = dataGroups.getItem(sector, layer, component).getF1D("funcRdir");
		funcRdir.setRange(lowLimit, highLimit);

		funcRdir.setParameter(0, 7.0);  //offset estimate
		funcRdir.setParameter(1, (1. / 150.));  //gradient estimate

		try {
			DataFitter.fit(funcRdir, graphRdir, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}

		// ****THIRD GRAPH
		// ****

		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		} else {
			// Fit over filled data only:
			if (graphLindir.getDataSize(0) >= 2) {
				for (int i = 0; i < graphLindir.getDataSize(0); i++) {
					if (graphLindir.getDataX(i) > 0.0) {
						lowLimit = graphLindir.getDataX(i);
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
			if (graphLindir.getDataSize(0) >= 2) {
				for (int i = (graphLindir.getDataSize(0) - 1); i > lowLimitBin; i--) {
					if (graphLindir.getDataX(i) > 0.0) {
						highLimit = graphLindir.getDataX(i);
						break;
					}
				}
			} else {
				highLimit = FIT_MAX;
			}
		}

		F1D funcLindir = dataGroups.getItem(sector, layer, component).getF1D("funcLindir");
		funcLindir.setRange(lowLimit, highLimit);

		//        if (sector==1 && layer ==1){
		//            System.out.println("lowLimit = " + lowLimit);
		//            System.out.println("highLimit = " + highLimit);
		//            graphL.getData(0, 0);
		//            graphL.getData(1, 1);
		//        }
		funcLindir.setParameter(0, 6.0);  //offset estimate
		funcLindir.setParameter(1, (1. / 150.));  //gradient estimate

		try {
			DataFitter.fit(funcLindir, graphLindir, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}

		// ****
		// ****FOURTH GRAPH
		// ****
		if (minRange != UNDEFINED_OVERRIDE) {
			// use custom values for fit
			lowLimit = minRange;
		} else {
			// Fit over filled data only:
			if (graphRindir.getDataSize(0) >= 2) {
				for (int i = 0; i < graphRindir.getDataSize(0); i++) {
					if (graphRindir.getDataX(i) > 0.0) {
						lowLimit = graphRindir.getDataX(i);
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
			if (graphRindir.getDataSize(0) >= 2) {
				for (int i = (graphRindir.getDataSize(0) - 1); i > lowLimitBin; i--) {
					if (graphRindir.getDataX(i) > 0.0) {
						highLimit = graphRindir.getDataX(i);
						break;
					}
				}
			} else {
				highLimit = FIT_MAX;
			}
		}

		F1D funcRindir = dataGroups.getItem(sector, layer, component).getF1D("funcRindir");
		funcRindir.setRange(lowLimit, highLimit);

		funcRindir.setParameter(0, 6.0);  //offset estimate
		funcRindir.setParameter(1, (1. / 150.));  //gradient estimate

		try {
			DataFitter.fit(funcRindir, graphRindir, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}
	}


	// a modifier
	public void adjustFit(int sector, int layer, int component,
			double minRange, double maxRange, int side) {

		//        System.out.println("IN adjustFit FIT for S" + sector + " L" + layer + " C" + component);


	}


	// a modifier
	public void customFit(int sector, int layer, int component) {



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
	public Double getMIPdirLeft(int sector, int layer, int component) {

		double MIPdirLeft = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPdirLeft = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(0);
			MIPdirLeft = Math.exp(intercept);
		}
		return MIPdirLeft;
	}

	public Double getMIPdirRight(int sector, int layer, int component) {

		double MIPdirRight = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_RIGHT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPdirRight = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").getParameter(0);
			MIPdirRight = Math.exp(intercept);
		}
		return MIPdirRight;
	}

	public Double getMIPindirLeft(int sector, int layer, int component) {

		double MIPindirLeft = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPindirLeft = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcLindir").getParameter(0);
			MIPindirLeft = Math.exp(intercept);
		}
		return MIPindirLeft;
	}

	public Double getMIPindirRight(int sector, int layer, int component) {

		double MIPindirRight = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_RIGHT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPindirRight = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcRindir").getParameter(0);
			MIPindirRight = Math.exp(intercept);
		}
		return MIPindirRight;
	}

	@Override
	public void saveRow(int sector, int layer, int component) {

		calib.setDoubleValue(getMIPdirLeft(sector, layer, component),
				"mip_dir_L", sector, layer, component);
		calib.setDoubleValue(ALLOWED_DIFF,
				"mip_dir_L_err", sector, layer, component);
		calib.setDoubleValue(getMIPdirRight(sector, layer, component),
				"mip_dir_R", sector, layer, component);
		calib.setDoubleValue(ALLOWED_DIFF,
				"mip_dir_R_err", sector, layer, component);
		calib.setDoubleValue(getMIPindirLeft(sector, layer, component),
				"mip_indir_L", sector, layer, component);
		calib.setDoubleValue(ALLOWED_DIFF,
				"mip_indir_L_err", sector, layer, component);
		calib.setDoubleValue(getMIPindirRight(sector, layer, component),
				"mip_indir_R", sector, layer, component);
		calib.setDoubleValue(ALLOWED_DIFF,
				"mip_indir_R_err", sector, layer, component);
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
		//direct
		dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").setTitle("ADCLdir vs Position - S" + sector + " L" + layer + " C" + component);
		dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").setTitle("ADCRdir vs Position - S" + sector + " L" + layer + " C" + component);
		if (test) {
			dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph").setTitleX("Calculated hit position (cm)");
		} else {
			dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
		}

		dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").setTitleY("Log(ADCLdir) (log(channels))");
		dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").setTitleY("Log(ADCRdir) (log(channels))");
		dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph").setTitle("Log(ADCLdir) vs Position");
		dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph").setTitle("Log(ADCRdir) vs Position");
		dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph").setTitleY("ADCdir slice peak(log(channels))");
		dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph").setTitleY("ADCdir slice peak (log(channels))");


		//indirect
		dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").setTitle("ADCLindir vs Position - S" + sector + " L" + layer + " C" + component);
		dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").setTitle("ADCRindir vs Position - S" + sector + " L" + layer + " C" + component);
		if (test) {
			dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph").setTitleX("Calculated hit position (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph").setTitleX("Calculated hit position (cm)");
		} else {
			dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
			dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
		}

		dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").setTitleY("Log(ADCLindir) (log(channels))");
		dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").setTitleY("Log(ADCRindir) (log(channels))");
		dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph").setTitle("Log(ADCLindir) vs Position");
		dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph").setTitle("Log(ADCRindir) vs Position");
		dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph").setTitleY("ADCindir slice peak(log(channels))");
		dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph").setTitleY("ADCindir slice peak (log(channels))");

	}

	@Override
	public void drawPlots(int sector, int layer, int component, EmbeddedCanvas canvas) {

		if (showPlotType == "MIPDIR_L") {
			H2F histLdir = dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z");
			histLdir.setTitle("");
			histLdir.setTitleX("");
			histLdir.setTitleY("");
			canvas.draw(histLdir);
		} else if (showPlotType == "MIPDIR_R") {
			H2F histRdir = dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z");
			histRdir.setTitle("");
			histRdir.setTitleX("");
			histRdir.setTitleY("");
			canvas.draw(histRdir);
		} else if (showPlotType == "MIPDIR_L_GRAPH") {
			GraphErrors graphLdir = dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph");
			if (graphLdir.getDataSize(0) != 0) {
				graphLdir.setTitle("");
				graphLdir.setTitleX("");
				graphLdir.setTitleY("");
				canvas.draw(graphLdir);
				canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("funcLdir"), "same");
			}
		} else if (showPlotType == "MIPDIR_R_GRAPH") {
			GraphErrors graphRdir = dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph");
			if (graphRdir.getDataSize(0) != 0) {
				graphRdir.setTitle("");
				graphRdir.setTitleX("");
				graphRdir.setTitleY("");
				canvas.draw(graphRdir);
				canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("funcRdir"), "same");
			}
		} else if (showPlotType == "MIPINDIR_L") {
			H2F histLindir = dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z");
			histLindir.setTitle("");
			histLindir.setTitleX("");
			histLindir.setTitleY("");
			canvas.draw(histLindir);
		} else if (showPlotType == "MIPINDIR_R") {
			H2F histRindir = dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z");
			histRindir.setTitle("");
			histRindir.setTitleX("");
			histRindir.setTitleY("");
			canvas.draw(histRindir);
		} else if (showPlotType == "MIPINDIR_L_GRAPH") {
			GraphErrors graphLindir = dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph");
			if (graphLindir.getDataSize(0) != 0) {
				graphLindir.setTitle("");
				graphLindir.setTitleX("");
				graphLindir.setTitleY("");
				canvas.draw(graphLindir);
				canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("funcLindir"), "same");
			}
		} else if (showPlotType == "MIPINDIR_R_GRAPH") {
			GraphErrors graphRindir = dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph");
			if (graphRindir.getDataSize(0) != 0) {
				graphRindir.setTitle("");
				graphRindir.setTitleX("");
				graphRindir.setTitleY("");
				canvas.draw(graphRindir);
				canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("funcRindir"), "same");
			}
		}
	}

	@Override
	public void showPlots(int sector, int layer) {

		showPlotType = "MIPDIR_L";
		stepName = "MIPdir - Left";
		super.showPlots(sector, layer);
		showPlotType = "MIPDIR_R";
		stepName = "MIPdir - Right";
		super.showPlots(sector, layer);
		showPlotType = "MIPDIR_L_GRAPH";
		stepName = "MIPdir Graph - Left";
		super.showPlots(sector, layer);
		showPlotType = "MIPDIR_R_GRAPH";
		stepName = "MIPdir Graph - Right";
		super.showPlots(sector, layer);

		showPlotType = "MIPINDIR_L";
		stepName = "MIPindir - Left";
		super.showPlots(sector, layer);
		showPlotType = "MIPINDIR_R";
		stepName = "MIPindir - Right";
		super.showPlots(sector, layer);
		showPlotType = "MIPINDIR_L_GRAPH";
		stepName = "MIPindir Graph - Left";
		super.showPlots(sector, layer);
		showPlotType = "MIPINDIR_R_GRAPH";
		stepName = "MIPindir Graph - Right";
		super.showPlots(sector, layer);

	}

	@Override
	public DataGroup getSummary() {

		double[] sectorNumber = new double[24];
		double[] zeroUncs = new double[24];        
		double[] mipdirL3 = new double[24];
		double[] mipdirR3 = new double[24];
		double[] mipdirL2 = new double[24];
		double[] mipdirR2 = new double[24];
		double[] mipdirL1 = new double[24];
		double[] mipdirR1 = new double[24];
		double[] mipindirL3 = new double[24];
		double[] mipindirR3 = new double[24];
		double[] mipindirL2 = new double[24];
		double[] mipindirR2 = new double[24];
		double[] mipindirL1 = new double[24];
		double[] mipindirR1 = new double[24];


		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				sectorNumber[sector - 1] = (double) sector;
				zeroUncs[sector - 1] = 0.0;

				if (layer == 3){
					mipdirL3[sector - 1] = getMIPdirLeft(sector, layer, component);
					mipdirR3[sector - 1] = getMIPdirRight(sector, layer, component);
					mipindirL3[sector - 1] = getMIPindirLeft(sector, layer, component);
					mipindirR3[sector - 1] = getMIPindirRight(sector, layer, component);
				} else if (layer == 2){
					mipdirL2[sector - 1] = getMIPdirLeft(sector, layer, component);
					mipdirR2[sector - 1] = getMIPdirRight(sector, layer, component);
					mipindirL2[sector - 1] = getMIPindirLeft(sector, layer, component);
					mipindirR2[sector - 1] = getMIPindirRight(sector, layer, component);
				} else if (layer == 1){
					mipdirL1[sector - 1] = getMIPdirLeft(sector, layer, component);
					mipdirR1[sector - 1] = getMIPdirRight(sector, layer, component);
					mipindirL1[sector - 1] = getMIPindirLeft(sector, layer, component);
					mipindirR1[sector - 1] = getMIPindirRight(sector, layer, component);
				}
			}
		}

		GraphErrors alldirL3 = new GraphErrors("alldirL3", sectorNumber,
				mipdirL3, zeroUncs, zeroUncs);

		alldirL3.setTitleX("Sector Number");
		alldirL3.setTitleY("MIP direct");
		alldirL3.setMarkerSize(MARKER_SIZE);
		alldirL3.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors alldirL2 = new GraphErrors("alldirL2", sectorNumber,
				mipdirL2, zeroUncs, zeroUncs);

		alldirL2.setTitleX("Sector Number");
		alldirL2.setTitleY("MIP direct");
		alldirL2.setMarkerSize(MARKER_SIZE);
		alldirL2.setLineThickness(MARKER_LINE_WIDTH); 

		GraphErrors alldirL1 = new GraphErrors("alldirL1", sectorNumber,
				mipdirL1, zeroUncs, zeroUncs);

		alldirL1.setTitleX("Sector Number");
		alldirL1.setTitleY("MIP direct");
		alldirL1.setMarkerSize(MARKER_SIZE);
		alldirL1.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allindirL3 = new GraphErrors("allindirL3", sectorNumber,
				mipindirL3, zeroUncs, zeroUncs);

		allindirL3.setTitleX("Sector Number");
		allindirL3.setTitleY("MIP indirect");
		allindirL3.setMarkerSize(MARKER_SIZE);
		allindirL3.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allindirL2 = new GraphErrors("allindirL2", sectorNumber,
				mipindirL2, zeroUncs, zeroUncs);

		allindirL2.setTitleX("Sector Number");
		allindirL2.setTitleY("MIP indirect");
		allindirL2.setMarkerSize(MARKER_SIZE);
		allindirL2.setLineThickness(MARKER_LINE_WIDTH); 

		GraphErrors allindirL1 = new GraphErrors("allindirL1", sectorNumber,
				mipindirL1, zeroUncs, zeroUncs);

		allindirL1.setTitleX("Sector Number");
		allindirL1.setTitleY("MIP indirect");
		allindirL1.setMarkerSize(MARKER_SIZE);
		allindirL1.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors alldirR3 = new GraphErrors("alldirR3", sectorNumber,
				mipdirR3, zeroUncs, zeroUncs);

		alldirR3.setTitleX("Sector Number");
		alldirR3.setTitleY("MIP direct");
		alldirR3.setMarkerSize(MARKER_SIZE);
		alldirR3.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors alldirR2 = new GraphErrors("alldirR2", sectorNumber,
				mipdirR2, zeroUncs, zeroUncs);

		alldirR2.setTitleX("Sector Number");
		alldirR2.setTitleY("MIP direct");
		alldirR2.setMarkerSize(MARKER_SIZE);
		alldirR2.setLineThickness(MARKER_LINE_WIDTH); 

		GraphErrors alldirR1 = new GraphErrors("alldirR1", sectorNumber,
				mipdirR1, zeroUncs, zeroUncs);

		alldirR1.setTitleX("Sector Number");
		alldirR1.setTitleY("MIP direct");
		alldirR1.setMarkerSize(MARKER_SIZE);
		alldirR1.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allindirR3 = new GraphErrors("allindirR3", sectorNumber,
				mipindirR3, zeroUncs, zeroUncs);

		allindirR3.setTitleX("Sector Number");
		allindirR3.setTitleY("MIP indirect");
		allindirR3.setMarkerSize(MARKER_SIZE);
		allindirR3.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allindirR2 = new GraphErrors("allindirR2", sectorNumber,
				mipindirR2, zeroUncs, zeroUncs);

		allindirR2.setTitleX("Sector Number");
		allindirR2.setTitleY("MIP indirect");
		allindirR2.setMarkerSize(MARKER_SIZE);
		allindirR2.setLineThickness(MARKER_LINE_WIDTH); 

		GraphErrors allindirR1 = new GraphErrors("allindirR1", sectorNumber,
				mipindirR1, zeroUncs, zeroUncs);

		allindirR1.setTitleX("Sector Number");
		allindirR1.setTitleY("MIP indirect");
		allindirR1.setMarkerSize(MARKER_SIZE);
		allindirR1.setLineThickness(MARKER_LINE_WIDTH);

		DataGroup dg = new DataGroup(2, 6);
		dg.addDataSet(alldirL3, 0);
		dg.addDataSet(alldirR3, 1);
		dg.addDataSet(alldirL2, 2);
		dg.addDataSet(alldirR2, 3);
		dg.addDataSet(alldirL1, 4);
		dg.addDataSet(alldirR1, 5);
		dg.addDataSet(allindirL3, 6);
		dg.addDataSet(allindirR3, 7);
		dg.addDataSet(allindirL2, 8);
		dg.addDataSet(allindirR2, 9);
		dg.addDataSet(allindirL1, 10);
		dg.addDataSet(allindirR1, 11);
		return dg;

	}

	@Override
	public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {

		//    	canvas.getPad(2).setAxisRange(TDC_MIN[layer], TDC_MAX[layer], -1.0, 1.0);
		//    	canvas.getPad(3).setAxisRange(TDC_MIN[layer], TDC_MAX[layer], -1.0, 1.0);
	}

}
