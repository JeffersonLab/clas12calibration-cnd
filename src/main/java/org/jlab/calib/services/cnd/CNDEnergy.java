package org.jlab.calib.services.cnd;


import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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



public class CNDEnergy extends CNDCalibrationEngine{





	private static boolean test = false;  //Test working for raw ADC and TDC values

	// hists

	
	
	public final int MIPDIR_L = 0;
	public final int MIPDIR_R = 1;
	public final int MIPDIR_L_GRAPH = 2;
	public final int MIPDIR_R_GRAPH = 3;
	public final int MIPINDIR_L = 4;
	public final int MIPINDIR_R = 5;
	

	public final int OVERRIDE_LEFT = 0;
	public final int OVERRIDE_RIGHT = 0;

	public final double[] EXPECTED_MIPDIR = {1200., 1400., 1800.};
	public final double EXPECTED_MIPINDIR = 500.;
	public final double ALLOWED_DIFF = 100.;



	//    private String fitOption = "RNQ";
	private String fitOption = "RQ";
	private String showPlotType = "EFFV_L";
	int backgroundSF = 0;
	int fitMethod = 0;
	public String fitMode = "RQ";
	boolean showSlices = false;
	private int FIT_METHOD_SF = 1;
	private int FIT_METHOD_MAX = 0;
	public int fitMinEvents = 20;
	//public final double MAX_CHI = 10;
	
	public double fitSliceMaxError = 0.3;
	


	public int HIST_X_BINS = 100;
	public double HIST_X_MIN = 0;
	public double HIST_X_MAX = 80;
	
	
	
	public int HIST_Y_BINS = 100;
	public double HIST_Y_MIN = 0.15;
	public double HIST_Y_MAX = 2.5;

	//logmean parameters
	public int HIST_M_BIN = 100;
	public double HIST_M_MIN = 500;
	public double StartFitGeoMean = 1000;
	public double HIST_M_MAX = 3000;
	
	//        public int HIST_Y_BINS = 300;
	//        public double HIST_Y_MIN = 2.4;
	//        public double HIST_Y_MAX = 8.4;
	private final double FIT_MIN = HIST_X_MIN;
	private final double FIT_MAX = HIST_X_MAX;
	private final double[] TDC_MIN = {0.0, 25000.0, 24000.0, 25000.0};
	private final double[] TDC_MAX = {0.0, 28000.0, 27000.0, 28000.0};

	public CNDEnergy() {

		stepName = "Attenuation & ADC-to-Energy Conversion";
		fileNamePrefix = "CND_CALIB_Energy_";
		fileNamePrefix2 = "CND_CALIB_ATTENUATION_"; // --PN
		// get file name here so that each timer update overwrites it
		filename = nextFileName();
		filename2 = nextFileName2(); // --PN
		calib = new CalibrationConstants(3,
				"attlen_L/F:attlen_L_err/F:attlen_R/F:attlen_R_err/F:mip_dir_L/F:mip_dir_L_err/F:mip_indir_L/F:mip_indir_L_err/F:mip_dir_R/F:mip_dir_R_err/F:mip_indir_R/F:mip_indir_R_err/F");
		calib.setName("/calibration/cnd/Energy");
		calib.setPrecision(5);
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
		
		
		

		//logmean parameters
		
		
		if(CNDCalibration.modee!=0){
			fitMethod = CNDCalibration.modee;
		}
		
		if(CNDCalibration.binXaxise1!=0){
			HIST_X_BINS=CNDCalibration.binXaxise1;
		}
		if(CNDCalibration.binXaxise!=0){
			HIST_M_BIN=CNDCalibration.binXaxise;
		}
		if(CNDCalibration.minXaxise!=0){
			HIST_M_MIN=CNDCalibration.minXaxise;
		}
		if(CNDCalibration.maxXaxise!=0){
			HIST_M_MAX=CNDCalibration.maxXaxise;
		}
		if(CNDCalibration.binYaxise!=0){
			HIST_Y_BINS=CNDCalibration.binYaxise;
		}
		if(CNDCalibration.minYaxise!=0){
			HIST_Y_MIN=CNDCalibration.minYaxise;
		}
		if(CNDCalibration.maxYaxise!=0){
			HIST_Y_MAX=CNDCalibration.maxYaxise;
		}

		// GM perform init processing
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				
				// create all the histograms
				//            int numBins = (int) (paddleLength(layer)*0.6);  // 1 bin per 2cm + 10% either side
				//            double min = paddleLength(layer) * -0.6;
				//            double max = paddleLength(layer) * 0.6;
				H2F histLdir
				= new H2F("logratioL",
						"logratioL",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);
				H2F histRdir
				= new H2F("logratioR",
						"logratioR",
						HIST_X_BINS, HIST_X_MIN, HIST_X_MAX,
						HIST_Y_BINS, HIST_Y_MIN, HIST_Y_MAX);
				H1F logmeanL
				= new H1F("logmeanL",
						"logmeanL",
						HIST_M_BIN, HIST_M_MIN, HIST_M_MAX);
				H1F logmeanR
				= new H1F("logmeanR",
						"logmeanR",
						HIST_M_BIN, HIST_M_MIN, HIST_M_MAX);

				
				
				F1D funcLdir = new F1D("funcLdir", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);
				F1D funcRdir = new F1D("funcRdir", "[a]+[b]*x", HIST_X_MIN, HIST_X_MAX);
				
				funcLdir.setLineColor(FUNC_COLOUR);
				funcRdir.setLineColor(FUNC_COLOUR);
				
				F1D meanL = new F1D("meanL", "[amp]*landau(x,[mean],[sigma])+[cst]", HIST_M_MIN, HIST_M_MAX);
				F1D meanR = new F1D("meanR", "[amp]*landau(x,[mean],[sigma])+[cst]", HIST_M_MIN, HIST_M_MAX);
				
				meanL.setLineColor(FUNC_COLOUR);
				meanR.setLineColor(FUNC_COLOUR);

				
				GraphErrors graphdirL = new GraphErrors("adcLdir_vs_z_graph");
				graphdirL.setName("adcLdir_vs_z_graph");
				graphdirL.setMarkerSize(MARKER_SIZE);
				graphdirL.setLineThickness(MARKER_LINE_WIDTH);

				GraphErrors graphdirR = new GraphErrors("adcRdir_vs_z_graph");
				graphdirR.setName("adcRdir_vs_z_graph");
				graphdirR.setMarkerSize(MARKER_SIZE);
				graphdirR.setLineThickness(MARKER_LINE_WIDTH);

				
				
				DataGroup dg = new DataGroup(2, 3);
				dg.addDataSet(histLdir, 0);
				dg.addDataSet(histRdir, 1);
				
				dg.addDataSet(logmeanL, 4);
				dg.addDataSet(logmeanR, 5);
				
				dg.addDataSet(graphdirL, 2);
				dg.addDataSet(funcLdir, 2);
				
				dg.addDataSet(graphdirR, 3);
				dg.addDataSet(funcRdir, 3);
				
				dg.addDataSet(meanL, 4);
				dg.addDataSet(meanR, 5);

				
				
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


				// hitPosition from tracking:
				hitPosition = paddlePair.zPosCND();

				pathInPaddle = paddlePair.T_LENGTH;

				paddleLength = paddlePair.paddleLength();

				pathlength= paddlePair.PATH_LENGTH;

				// CND paddle thickness is 3.0cm
				// Normalise the ADC values as if they were verticle tracks traversing only 3.0cm of paddle thickness
				double normalisingADCFactor = 0.0;
				normalisingADCFactor = 3.0 / pathInPaddle;
				if(Math.abs((time[0]-time[1]+leftRightValues.getDoubleValue("time_offset_LR", sector, layer, component)))>1.5){
				if (paddlePair.COMP==1 && paddlePair.ZPOS!=0.0){ 
					dataGroups.getItem(sector, layer, component).getH2F("logratioL").fill(hitPosition, Math.log( (float)paddlePair.ADCL/paddlePair.ADCR));
					
					if(paddlePair.CHARGE==-1){
					dataGroups.getItem(sector, layer, component).getH1F("logmeanL").fill(Math.sqrt( (float)paddlePair.ADCR*paddlePair.ADCL*normalisingADCFactor*normalisingADCFactor));
					}

				} else if (paddlePair.COMP==2 && paddlePair.ZPOS!=0.0){ 
					dataGroups.getItem(sector, layer, component).getH2F("logratioR").fill(hitPosition, Math.log( (float)paddlePair.ADCR/paddlePair.ADCL));
					
					if(paddlePair.CHARGE==-1){
					dataGroups.getItem(sector, layer, component).getH1F("logmeanR").fill(Math.sqrt( (float) paddlePair.ADCR*paddlePair.ADCL*normalisingADCFactor*normalisingADCFactor));
					}
				}
				
				}
			}
		}
	}

	@Override
		public GraphErrors maxGraph(H2F hist, String graphName) {
		
		ArrayList<H1F> slices = hist.getSlicesX();
		int nBins = hist.getXAxis().getNBins();
		double[] sliceMax = new double[nBins];
		double[] maxErrs = new double[nBins];
		double[] xVals = new double[nBins];
		double[] xErrs = new double[nBins];
		
		for (int i=0; i<nBins; i++) {
			
//			System.out.println("getH1FEntries "+getH1FEntries(slices.get(i)));
//			System.out.println("H1F getEntries "+slices.get(i).getEntries());
			
			if (getH1FEntries(slices.get(i)) > fitMinEvents) {

				int maxBin = slices.get(i).getMaximumBin();
				sliceMax[i] = slices.get(i).getxAxis().getBinCenter(maxBin);
				maxErrs[i] = slices.get(i).getRMS();
				//maxErrs[i] = maxGraphError;

				xVals[i] = hist.getXAxis().getBinCenter(i);
				xErrs[i] = hist.getXAxis().getBinWidth(i)/2.0;
                                
//                                System.out.println("sliceMax = " + sliceMax[i]);
//                                System.out.println("maxErr = " + maxErrs[i]);
//                                System.out.println("xVal = " + xVals[i]);
//                                System.out.println("xErr = " + xErrs[i]);
			}
		}

		GraphErrors maxGraph = new GraphErrors(graphName, xVals, sliceMax, xErrs, maxErrs);
		maxGraph.setName(graphName);
		
		return maxGraph;
		
	}
		
		@Override
	public GraphErrors fixGraph(GraphErrors graphIn, String graphName) {

		int n = graphIn.getDataSize(0);
		int m = 0;
		for (int i=0; i<n; i++) {
			if (graphIn.getDataEY(i) < fitSliceMaxError ) {
				m++;
			}
		}		
		
		double[] x = new double[m];
		double[] xerr = new double[m];
		double[] y = new double[m];
		double[] yerr = new double[m];
		int j = 0;
		
		//System.out.println();
		for (int i=0; i<n; i++) {
			if (graphIn.getDataEY(i) < fitSliceMaxError) {
				x[j] = graphIn.getDataX(i);
				xerr[j] = graphIn.getDataEX(i);
				y[j] = graphIn.getDataY(i);
				yerr[j] = graphIn.getDataEY(i);
				//System.out.println("x " + x[j] + " xerr" + xerr[j] + " y " + y[j]+ " yerr "+yerr[j]);
				
				j++;
			}
		}
		
		GraphErrors fixGraph = new GraphErrors(graphName, x, y, xerr, yerr);
		fixGraph.setName(graphName);
		
		return fixGraph;
		
	}		
	
	@Override
	public void fit(int sector, int layer, int component,
			double minRange, double maxRange) {

				System.out.println("IN ENERGY FIT for S" + sector + " L" + layer + " C" + component);
				//        System.out.println();
		
				H2F histLdir = dataGroups.getItem(sector, layer, component).getH2F("logratioL");
				H2F histRdir = dataGroups.getItem(sector, layer, component).getH2F("logratioR");
				GraphErrors graphLdir = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcLdir_vs_z_graph");
				GraphErrors graphRdir = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcRdir_vs_z_graph");
				
				int fitMinEventsL = histLdir.getEntries()/HIST_X_BINS;
				int fitMinEventsR = histRdir.getEntries()/HIST_X_BINS;
		
				if(CNDCalibration.minevente!=0){
					fitMinEventsL= (int) CNDCalibration.minevente;
					fitMinEventsR=(int) CNDCalibration.minevente;
				}
		
				// fit function to the graph of means
				if (fitMethod == FIT_METHOD_SF) {
					ParallelSliceFitter psfLdir = new ParallelSliceFitter(histLdir);
					psfLdir.setFitMode(fitMode);
					psfLdir.setMinEvents(fitMinEventsL);
					psfLdir.setBackgroundOrder(backgroundSF);
					psfLdir.setNthreads(1);
					//psfLdir.setMaxChiSquare(MAX_CHI);
					//psfL.setShowProgress(false);
					setOutput(false);
					psfLdir.fitSlicesX();
					setOutput(true);
					if ( showSlices) {
						psfLdir.inspectFits();
					}
					graphLdir.copy(fixGraph(psfLdir.getMeanSlices(), "adcLdir_vs_z_graph"));
		
		
					ParallelSliceFitter psfRdir = new ParallelSliceFitter(histRdir);
					psfRdir.setFitMode(fitMode);
					psfRdir.setMinEvents(fitMinEventsR);
					psfRdir.setBackgroundOrder(backgroundSF);
					//psfRdir.setMaxChiSquare(MAX_CHI);
					psfRdir.setNthreads(1);
					//psfR.setShowProgress(false);
					setOutput(false);
					psfRdir.fitSlicesX();
					setOutput(true);
					if ( showSlices) {
						psfRdir.inspectFits();
						showSlices = false;
					}
					graphRdir.copy(fixGraph(psfRdir.getMeanSlices(), "adcRdir_vs_z_graph"));
		
		
					
				} else if (fitMethod == FIT_METHOD_MAX) {
		
					// ****
					// ****NEED TO EDIT THIS****
					// ****
					maxGraphError = 0.15;
					graphLdir.copy(maxGraph(histLdir, "adcLdir_vs_z_graph"));
					graphRdir.copy(maxGraph(histRdir, "adcRdir_vs_z_graph"));
					
				} else {
					            graphLdir.copy(histLdir.getProfileX());
					            graphRdir.copy(histRdir.getProfileX());
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
		

				if(lowLimit<20.)lowLimit=20.;
				if(highLimit>60.)highLimit=60.+2.5*(layer-1);
				
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
				// ****Second graph
				// ****
				// find the range for the fit
				 lowLimit = FIT_MIN;
				 highLimit = FIT_MAX;
				 lowLimitBin = 0;
		
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
		
				if(lowLimit<20.)lowLimit=20.;
				if(highLimit>60.)highLimit=60.+2.5*(layer-1);

				F1D funcRdir = dataGroups.getItem(sector, layer, component).getF1D("funcRdir");
				funcRdir.setRange(lowLimit, highLimit);
		
				//        if (sector==1 && layer ==1){
				//            System.out.println("lowLimit = " + lowLimit);
				//            System.out.println("highLimit = " + highLimit);
				//            graphL.getData(0, 0);
				//            graphL.getData(1, 1);
				//        }
				funcRdir.setParameter(0, 7.0);  //offset estimate
				funcRdir.setParameter(1, (1. / 150.));  //gradient estimate
		
				try {
					DataFitter.fit(funcRdir, graphRdir, fitOption);
				} catch (Exception e) {
					System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
					e.printStackTrace();
				}
				
				
				fitGeoMean(sector, layer, component, minRange, maxRange);
				
				
		

	}

	public void fitGeoMean(int sector, int layer, int component,
			double minRange, double maxRange){
		
		
		///
		//FIT L
		///

		F1D meanL = dataGroups.getItem(sector, layer, component).getF1D("meanL");
		H1F logmeanL = dataGroups.getItem(sector, layer, component).getH1F("logmeanL");

		double maxChannel = logmeanL.getAxis().getBinCenter(logmeanL.getAxis().getNBins()-1);
		double startChannelForFit = 0.0;
		double endChannelForFit = 0.0;
		if (minRange==UNDEFINED_OVERRIDE) {
			// default value
			startChannelForFit = StartFitGeoMean;
		}
		else {
			// custom value
			startChannelForFit = StartFitGeoMean;
		}
		if (maxRange==UNDEFINED_OVERRIDE) {
			//default value
			endChannelForFit = maxChannel;
		}
		else {
			// custom value
			endChannelForFit = maxRange;
		}

		// find the maximum bin after the start channel for the fit
		int startBinForFit = logmeanL.getxAxis().getBin(startChannelForFit);
		int endBinForFit = logmeanL.getxAxis().getBin(endChannelForFit);

		double maxCounts = 0;
		int maxBin = 0;
		for (int i=startBinForFit; i<=endBinForFit; i++) {
			if (logmeanL.getBinContent(i) > maxCounts) {
				maxBin = i;
				maxCounts = logmeanL.getBinContent(i);
			};
		}

		double maxPos = logmeanL.getAxis().getBinCenter(maxBin);

		// adjust the range now that the max has been found
		// unless it''s been set to custom value
		//        if (minRange == 0.0) {
		//            startChannelForFit = maxPos*0.5;
		//        }
		if (maxRange == UNDEFINED_OVERRIDE) {
			endChannelForFit = maxPos+HIST_M_MAX*0.4;
			if (endChannelForFit > 0.9*HIST_M_MAX) {
				endChannelForFit = 0.9*HIST_M_MAX;
			}    
		}

		meanL.setRange(startChannelForFit, endChannelForFit);

		meanL.setParameter(0, 1.7*maxCounts);  //1.7 being the inverse of the amplitude of a amplitude 1 Landau fct at x=mean
		//meanL.setParLimits(0, maxCounts, maxCounts*2);
		meanL.setParameter(1, maxPos);
		meanL.setParLimits(1, maxPos*0.9, maxPos*1.1);
		meanL.setParameter(2, 100.0);
		meanL.setParameter(3, logmeanL.getBinContent(logmeanL.getxAxis().getBin(startChannelForFit)));
		System.out.println("param.3 "+logmeanL.getBinContent(logmeanL.getxAxis().getBin(startChannelForFit)));
		//meanL.setParLimits(2, 0.0,400.0);
		
		System.out.println(maxCounts+ " "+maxPos);
	
		
		
		try {					
			DataFitter.fit(meanL, logmeanL, fitOption);
			System.out.println("width "+meanL.getParameter(2));
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}
		
		System.out.println(meanL.getParameter(0));
		System.out.println(meanL.getParameter(1));

		///
		// FIT R
		///
		
		
		F1D meanR = dataGroups.getItem(sector, layer, component).getF1D("meanR");
		H1F logmeanR = dataGroups.getItem(sector, layer, component).getH1F("logmeanR");

		

		 maxChannel = logmeanR.getAxis().getBinCenter(logmeanR.getAxis().getNBins()-1);
		 startChannelForFit = 0.0;
		 endChannelForFit = 0.0;
		if (minRange==UNDEFINED_OVERRIDE) {
			// default value
			startChannelForFit = StartFitGeoMean;
		}
		else {
			// custom value
			startChannelForFit = StartFitGeoMean;
		}
		if (maxRange==UNDEFINED_OVERRIDE) {
			//default value
			endChannelForFit = maxChannel;
		}
		else {
			// custom value
			endChannelForFit = maxRange;
		}

		// find the maximum bin after the start channel for the fit
		 startBinForFit = logmeanR.getxAxis().getBin(startChannelForFit);
		 endBinForFit = logmeanR.getxAxis().getBin(endChannelForFit);

		 maxCounts = 0;
		 maxBin = 0;
		for (int i=startBinForFit; i<=endBinForFit; i++) {
			if (logmeanR.getBinContent(i) > maxCounts) {
				maxBin = i;
				maxCounts = logmeanR.getBinContent(i);
			};
		}

		 maxPos = logmeanR.getAxis().getBinCenter(maxBin);

		// adjust the range now that the max has been found
		// unless it''s been set to custom value
		//        if (minRange == 0.0) {
		//            startChannelForFit = maxPos*0.5;
		//        }
		if (maxRange == UNDEFINED_OVERRIDE) {
			endChannelForFit = maxPos+HIST_M_MAX*0.4;
			if (endChannelForFit > 0.9*HIST_M_MAX) {
				endChannelForFit = 0.9*HIST_M_MAX;
			}    
		}

		meanR.setRange(startChannelForFit, endChannelForFit);

		meanR.setParameter(0, 1.7*maxCounts);
		//meanR.setParLimits(0, 1.6*maxCounts, maxCounts*2);
		meanR.setParameter(1, maxPos);
		meanR.setParLimits(1, maxPos*0.9, maxPos*1.1);
		meanR.setParameter(2, 100.0);
		meanR.setParameter(3, logmeanR.getBinContent(logmeanR.getxAxis().getBin(startChannelForFit)));
		//meanR.setParLimits(2, 0.0,400.0);
		
//		System.out.println();
//		System.out.println(meanR.getParameter(1));
		
		
		try {
			DataFitter.fit(meanR, logmeanR, fitOption);
			//System.out.println(meanR.getParameter(1));
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}
		
	}
	

	// a modifier
	public void adjustFitlogratio(int sector, int layer, int component,
			double minRange, double maxRange, int side) {

		        System.out.println("IN adjustFit FIT for S" + sector + " L" + layer + " C" + component);

		F1D func = null;
		GraphErrors graph = null;

		
		if (side == 1){
			func = dataGroups.getItem(sector, layer, component).getF1D("funcLdir");
			
			graph = (GraphErrors) (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcLdir_vs_z_graph");
			
			
		} else if (side == 2){
			func = dataGroups.getItem(sector, layer, component).getF1D("funcRdir");
			
			graph = (GraphErrors) dataGroups.getItem(sector, layer, component).getData("adcRdir_vs_z_graph");
		
			
		}
	

		System.out.println("minrange " + minRange + "manrange " + maxRange );
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
	
	
	public void adjustFitmeangeo(int sector, int layer, int component,
			double minRange, double maxRange, int side) {

		//        System.out.println("IN adjustFit FIT for S" + sector + " L" + layer + " C" + component);

		F1D func = null;
		H1F mean  = null;

		if (side == 1){
			func = dataGroups.getItem(sector, layer, component).getF1D("meanL");
			mean =  dataGroups.getItem(sector, layer, component).getH1F("logmeanL");
		} else if (side == 2){
			func = dataGroups.getItem(sector, layer, component).getF1D("meanR");
			mean =  dataGroups.getItem(sector, layer, component).getH1F("logmeanR");
		}
		
		
		
		double startChannelForFit = minRange;
		double endChannelForFit = maxRange;

		// find the maximum bin after the start channel for the fit
		int startBinForFit = mean.getxAxis().getBin(startChannelForFit);
		int endBinForFit = mean.getxAxis().getBin(endChannelForFit);

		double maxCounts = 0;
		int maxBin = 0;
		for (int i=startBinForFit; i<=endBinForFit; i++) {
			if (mean.getBinContent(i) > maxCounts) {
				maxBin = i;
				maxCounts = mean.getBinContent(i);
			};
		}

		double maxPos = mean.getAxis().getBinCenter(maxBin);
		
		func.setParameter(0, 1.7*maxCounts);  //1.7 being the inverse of the amplitude of a amplitude 1 Landau fct at x=mean
		//meanL.setParLimits(0, maxCounts, maxCounts*2);
		func.setParameter(1, maxPos);
		func.setParLimits(1, maxPos*0.9, maxPos*1.1);
		func.setParameter(2, 100.);
		
		System.out.println("range " + minRange + " " + maxRange );
		System.out.println("max pos " + maxPos );
		func.setRange(minRange, maxRange);

		System.out.println("parameters " + func.getParameter(0) +" "+ func.getParameter(1)+" "+ func.getParameter(2));
		try {
			DataFitter.fit(func, mean, fitOption);
			System.out.println("parameters " + func.getParameter(0) +" "+ func.getParameter(1)+" "+ func.getParameter(2));

		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}        

		// update the table
		saveRow(sector,layer,component);		
		calib.fireTableDataChanged();  
	}


	
	public void customFit(int sector, int layer, int component) {

		F1D funcLdir = dataGroups.getItem(sector, layer, component).getF1D("funcLdir");   
		F1D funcRdir = dataGroups.getItem(sector, layer, component).getF1D("funcRdir"); 
		F1D meanL = dataGroups.getItem(sector, layer, component).getF1D("meanL");
		F1D meanR = dataGroups.getItem(sector, layer, component).getF1D("meanR");
		

		String[] fields = {""+funcLdir.getMin(),
				""+funcLdir.getMax(),
				""+funcRdir.getMin(),
				""+funcRdir.getMax(),
				""+meanL.getMin(),
				""+meanL.getMax(),
				""+meanR.getMin(),
				""+meanR.getMax()};

		CNDCustomFitPanelFor4Graphs panel = new CNDCustomFitPanelFor4Graphs(fields);
		

		int result = JOptionPane
				.showConfirmDialog(null, panel, "Adjust fit for S" + sector + " L" + layer + " C" + component,
						JOptionPane.OK_CANCEL_OPTION);

		
		//limit fit logratio
		if (result == JOptionPane.OK_OPTION) {

			double[] fitLimitsLeft = new double[2];
			double[] fitLimitsRight = new double[2];

			// System.out.println("taille text field " + panel.textFields.length);
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
							fitLimitsLeft[p] = funcLdir.getMin();
						} else {
							fitLimitsLeft[p] = funcLdir.getMax();                            
						}
					}
				}
				adjustFitlogratio(sector, layer, component, fitLimitsLeft[0], fitLimitsLeft[1], 1);
			}    

			//Check for new limits for the RIGHT histogram, only when a lower OR an upper have been provided
			if (fitLimitsRight[0] != 0.0 || fitLimitsRight[1] != 0.0) {

				//Use previous fit limits if required
				for (int p = 0; p < 2; p++) {
					if (fitLimitsRight[p] == 0.0) {
						if (p % 2 == 0) {
							fitLimitsRight[p] = funcRdir.getMin();
						} else {
							fitLimitsRight[p] = funcRdir.getMax();                            
						}
					}
				}
				adjustFitlogratio(sector, layer, component, fitLimitsRight[0], fitLimitsRight[1], 2);
			}            

			

		
		
		//limit meanGeo


			double[] fitLimitsLeft1 = new double[2];
			double[] fitLimitsRight1 = new double[2];

			
				fitLimitsLeft1[0] = toDouble(panel.textFields[4].getText());
				 System.out.println(panel.textFields[4].getText());
				fitLimitsLeft1[1] = toDouble(panel.textFields[5].getText());
				 System.out.println(panel.textFields[5].getText());
				fitLimitsRight1[0] = toDouble(panel.textFields[6].getText());
				 System.out.println(panel.textFields[6].getText());
				fitLimitsRight1[1] = toDouble(panel.textFields[7].getText());
				 System.out.println(panel.textFields[7].getText());


			//Check for new limits for the LEFT histogram, only when a lower OR an upper have been provided
			if (fitLimitsLeft1[0] != 0.0 || fitLimitsLeft1[1] != 0.0) {

				//Use previous fit limits if required
				for (int p = 0; p < 2; p++) {
					if (fitLimitsLeft1[p] == 0.0) {
						if (p % 2 == 0) {
							fitLimitsLeft1[p] = meanL.getMin();
						} else {
							fitLimitsLeft1[p] = meanL.getMax();                            
						}
					}
				}
				System.out.println("Reach here");
				adjustFitmeangeo(sector, layer, component, fitLimitsLeft1[0], fitLimitsLeft1[1], 1);
			}    

			//Check for new limits for the RIGHT histogram, only when a lower OR an upper have been provided
			if (fitLimitsRight1[0] != 0.0 || fitLimitsRight1[1] != 0.0) {

				//Use previous fit limits if required
				for (int p = 0; p < 2; p++) {
					if (fitLimitsRight1[p] == 0.0) {
						if (p % 2 == 0) {
							fitLimitsRight1[p] = meanR.getMin();
						} else {
							fitLimitsRight1[p] = meanR.getMax();                            
						}
					}
				}
				adjustFitmeangeo(sector, layer, component, fitLimitsRight1[0], fitLimitsRight1[1], 2);
			}            

			

		}
		
		
		

	}


	
	
	
	public Double getMIPdirLeft(int sector, int layer, int component) {

		double MIPdirLeft = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPdirLeft = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(0);
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(1);
			double attlength= -2/gradient;
			double mean = dataGroups.getItem(sector, layer, component).getF1D("meanL").getParameter(1);
			double L = 0.0;
			
			 if (layer == 1) {
	                L = 66.572;
	            } else if (layer == 2) {
	                L = 70.000;
	            } else if (layer == 3) {
	                L = 73.428;
	            }
			
//			System.out.println("attlength "+attlength);
//			System.out.println("ratio MIP "+Math.exp(intercept-(L/attlength)));
			MIPdirLeft = Math.sqrt(Math.exp(intercept-(L/attlength))*mean*mean*Math.exp(L/attlength));
		}
		return MIPdirLeft;
	}

	public Double getMIPdirLeftErr(int sector, int layer, int component) {

		double MIPdirLeftErr;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPdirLeftErr = overrideVal;
		} else {
			// System.out.println();
			// System.out.println(sector+ " "+layer);
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(0);
			double Errintercept = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").parameter(0).error();
			//System.out.println("error intercept "+Errintercept);
			double gradient = Math.abs(dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(1));
			double Errgradient = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").parameter(1).error();
			double attlength= 2/gradient;
			double mean = dataGroups.getItem(sector, layer, component).getF1D("meanL").getParameter(1);
			double Errmean = dataGroups.getItem(sector, layer, component).getF1D("meanL").parameter(1).error();
			//System.out.println("error mean "+Errmean);
			
			double L = 0.0;
			
			 if (layer == 1) {
	                L = 66.572;
	            } else if (layer == 2) {
	                L = 70.000;
	            } else if (layer == 3) {
	                L = 73.428;
	            }
			
			
			double MddMi=Math.sqrt(Math.exp(intercept-(L/attlength)));
			//System.out.println("MddMi "+MddMi);
			double ErrMddMi=MddMi*(Errintercept+(Errgradient*L/gradient));
			//System.out.println("ErrMddMi "+ErrMddMi);
			double MdMi=mean*mean*Math.exp(L/attlength);
			//System.out.println("MdMi "+MdMi);
			double ErrMdMi=MdMi*((Errgradient*L/gradient)+(2*Errmean/mean));
			//System.out.println("ErrMdMi "+ErrMdMi);		 
			
			MIPdirLeftErr = ((Math.sqrt(MddMi/MdMi)*ErrMdMi)+(Math.sqrt(MdMi/MddMi)*ErrMddMi))/2;
			//System.out.println("MIPdirLeftErr 1 "+(Math.sqrt(MddMi/MdMi)*ErrMdMi));
			//System.out.println("MIPdirLeftErr 2 "+(Math.sqrt(MdMi/MddMi)*ErrMddMi));
			//System.out.println("MIPdirLeftErr "+MIPdirLeftErr);
			//System.out.println();
		}
		return MIPdirLeftErr;
	}
	
	public Double getMIPindirLeft(int sector, int layer, int component) {

		double MIPindirLeft = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPindirLeft = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(0);
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(1);
			double attlength= -2/gradient;
			double mean = dataGroups.getItem(sector, layer, component).getF1D("meanL").getParameter(1);
			double L = 0.0;
			
			 if (layer == 1) {
	                L = 66.572;
	            } else if (layer == 2) {
	                L = 70.000;
	            } else if (layer == 3) {
	                L = 73.428;
	            }
			
//			System.out.println("attlength "+attlength);
//			System.out.println("ratio MIP "+Math.exp(intercept-(L/attlength)));
			MIPindirLeft = Math.sqrt((1/Math.exp(intercept-(L/attlength)))*mean*mean*Math.exp(L/attlength));
		}
		return MIPindirLeft;
	}
	
	public Double getMIPdirRight(int sector, int layer, int component) {

		double MIPdirRight = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_RIGHT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPdirRight = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").getParameter(0);
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").getParameter(1);
			double attlength= -2/gradient;
			double mean = dataGroups.getItem(sector, layer, component).getF1D("meanR").getParameter(1);
			double L = 0.0;
			
			 if (layer == 1) {
	                L = 66.572;
	            } else if (layer == 2) {
	                L = 70.000;
	            } else if (layer == 3) {
	                L = 73.428;
	            }
			
//			System.out.println("attlength "+attlength);
//			System.out.println("ratio MIP "+Math.exp(intercept-(L/attlength)));
			MIPdirRight = Math.sqrt(Math.exp(intercept-(L/attlength))*mean*mean*Math.exp(L/attlength));
		}
		return MIPdirRight;
	}

	

	public Double getMIPindirRight(int sector, int layer, int component) {

		double MIPindirRight = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_RIGHT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			MIPindirRight = overrideVal;
		} else {
			double intercept = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").getParameter(0);
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").getParameter(1);
			double attlength= -2/gradient;
			double mean = dataGroups.getItem(sector, layer, component).getF1D("meanR").getParameter(1);
			double L = 0.0;
			
			 if (layer == 1) {
	                L = 66.572;
	            } else if (layer == 2) {
	                L = 70.000;
	            } else if (layer == 3) {
	                L = 73.428;
	            }
			
//			System.out.println("attlength "+attlength);
//			System.out.println("ratio MIP "+Math.exp(intercept-(L/attlength)));
			MIPindirRight = Math.sqrt((1/Math.exp(intercept-(L/attlength)))*mean*mean*Math.exp(L/attlength));
		}
		return MIPindirRight;
	}

	public Double getAttLenLeft(int sector, int layer, int component) {

		double attLenLeft = 0.0;
		double overrideVal = constants.getItem(sector, layer, component)[OVERRIDE_LEFT];

		if (overrideVal != UNDEFINED_OVERRIDE) {
			attLenLeft = overrideVal;
		} else {
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(1);
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
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").getParameter(1);
			double gradienterror = dataGroups.getItem(sector, layer, component).getF1D("funcLdir").parameter(1).error();
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
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").getParameter(1);
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
			double gradient = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").getParameter(1);
			double gradienterror = dataGroups.getItem(sector, layer, component).getF1D("funcRdir").parameter(1).error();
			attLenRight = -2. / gradient;
			attLenRighterror = gradienterror* attLenRight* attLenRight/2.;
		}
		return attLenRighterror;
	}
	
	@Override
	public void saveRow(int sector, int layer, int component) {

		calib.setDoubleValue(getMIPdirLeft(sector, layer, component),
				"mip_dir_L", sector, layer, component);
		calib.setDoubleValue(getMIPdirLeftErr(sector,layer,component),
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
		calib.setDoubleValue(getAttLenLeft(sector, layer, component),
				"attlen_L", sector, layer, component);
		calib.setDoubleValue(getAttLenLeftError(sector, layer, component),
				"attlen_L_err", sector, layer, component);
		calib.setDoubleValue(getAttLenRight(sector, layer, component),
				"attlen_R", sector, layer, component);
		calib.setDoubleValue(getAttLenRightError(sector, layer, component),
				"attlen_R_err", sector, layer, component);
	}

	@Override //split table contents into the two files --PN
	public void writeFile(String filename) {

		//indexes for what is to be written out in lines of the file --PN
		int[] writeCols = {1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3};
		
		try { 

			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			for (int i=0; i<calib.getRowCount(); i++) {
				String line = new String();
				for (int j=0; j<calib.getColumnCount(); j++) {
					if (writeCols[j] == 1 || writeCols[j] == 3) {
						line = line+calib.getValueAt(i, j);
						if (j<calib.getColumnCount()-1) {
							line = line+" ";
						}
					}
				}
				outputBw.write(line);
				outputBw.newLine();
			}

			outputBw.close();
			
			// Open the second output file --PN
			File outputFile2 = new File(filename2);
			FileWriter outputFw2 = new FileWriter(outputFile2.getAbsoluteFile());
			BufferedWriter outputBw2 = new BufferedWriter(outputFw2);

			for (int i=0; i<calib.getRowCount(); i++) {
				String line = new String();
				for (int j=0; j<calib.getColumnCount(); j++) {
					if (writeCols[j] == 1 || writeCols[j] == 2) {
						line = line+calib.getValueAt(i, j);
						if (j<calib.getColumnCount()-1) {
							line = line+" ";
						}
					}
				}
				outputBw2.write(line);
				outputBw2.newLine();
			}

			outputBw2.close();
			filename2 = nextFileName2(); //increment filename2 after writing (filename incremented by button execution in CNDCalibration.java).
			
		}
		catch(IOException ex) {
			System.out.println(
					"Error reading file '" 
							+ filename + "'");                   
			// Or we could just do this: 
			ex.printStackTrace();
		}

	}
	
	@Override
	public boolean isGoodPaddle(int sector, int layer, int paddle) {

		return (true);

	}

	@Override
	public boolean isGoodComponent(int sector, int layer, int component) {

		return (true);

	}

	@Override
	public void setPlotTitle(int sector, int layer, int component) {

		// reset hist title as may have been set to null by show all 
		//direct
		dataGroups.getItem(sector, layer, component).getH2F("logratioL").setTitle("Logratio vs Pos - S" + sector + " L" + layer + " C" + 1);
		dataGroups.getItem(sector, layer, component).getH2F("logratioR").setTitle("Logratio vs Pos - S" + sector + " L" + layer + " C" + 2);
//		
//			dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").setTitleX("Local hit position from tracking (cm)");
//			dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").setTitleX("Local hit position from tracking (cm)");
//			dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
//			dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
//		
//
//		dataGroups.getItem(sector, layer, component).getH2F("adcLdir_vs_z").setTitleY("Log(ADCLdir) (log(channels))");
//		dataGroups.getItem(sector, layer, component).getH2F("adcRdir_vs_z").setTitleY("Log(ADCRdir) (log(channels))");
//		dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph").setTitle("Log(ADCLdir) vs Position");
//		dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph").setTitle("Log(ADCRdir) vs Position");
//		dataGroups.getItem(sector, layer, component).getGraph("adcLdir_vs_z_graph").setTitleY("ADCdir slice peak(log(channels))");
//		dataGroups.getItem(sector, layer, component).getGraph("adcRdir_vs_z_graph").setTitleY("ADCdir slice peak (log(channels))");
//
//
//		//indirect
//		dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").setTitle("ADCLindir vs Position - S" + sector + " L" + layer + " C" + component);
//		dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").setTitle("ADCRindir vs Position - S" + sector + " L" + layer + " C" + component);
//		
//			dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").setTitleX("Local hit position from tracking (cm)");
//			dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").setTitleX("Local hit position from tracking (cm)");
//			dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
//			dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph").setTitleX("Local hit position from tracking (cm)");
//		
//
//		dataGroups.getItem(sector, layer, component).getH2F("adcLindir_vs_z").setTitleY("Log(ADCLindir) (log(channels))");
//		dataGroups.getItem(sector, layer, component).getH2F("adcRindir_vs_z").setTitleY("Log(ADCRindir) (log(channels))");
//		dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph").setTitle("Log(ADCLindir) vs Position");
//		dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph").setTitle("Log(ADCRindir) vs Position");
//		dataGroups.getItem(sector, layer, component).getGraph("adcLindir_vs_z_graph").setTitleY("ADCindir slice peak(log(channels))");
//		dataGroups.getItem(sector, layer, component).getGraph("adcRindir_vs_z_graph").setTitleY("ADCindir slice peak (log(channels))");

	}

	@Override
	public void drawPlots(int sector, int layer, int component, EmbeddedCanvas canvas) {

		if (showPlotType == "MIPDIR_L") {
			H2F histLdir = dataGroups.getItem(sector, layer, component).getH2F("logratioL");
			histLdir.setTitle("");
			histLdir.setTitleX("");
			histLdir.setTitleY("");
			canvas.draw(histLdir);
		} else if (showPlotType == "MIPDIR_R") {
			H2F histRdir = dataGroups.getItem(sector, layer, component).getH2F("logratioR");
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
			H1F histLindir = dataGroups.getItem(sector, layer, component).getH1F("logmeanL");
			histLindir.setTitle("");
			histLindir.setTitleX("");
			histLindir.setTitleY("");
			canvas.draw(histLindir);
			canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("meanL"), "same");
		} else if (showPlotType == "MIPINDIR_R") {
			H1F histRindir = dataGroups.getItem(sector, layer, component).getH1F("logmeanR");
			histRindir.setTitle("");
			histRindir.setTitleX("");
			histRindir.setTitleY("");
			canvas.draw(histRindir);
			canvas.draw(dataGroups.getItem(sector, layer, component).getF1D("meanR"), "same");
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



