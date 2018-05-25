package org.jlab.calib.services.cnd;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import static org.jlab.calib.services.cnd.CNDCalibrationEngine.CAL_DB;
import static org.jlab.calib.services.cnd.CNDCalibrationEngine.CAL_DEFAULT;
import static org.jlab.calib.services.cnd.CNDCalibrationEngine.CAL_FILE;
import static org.jlab.calib.services.cnd.CNDCalibrationEngine.convValues;

import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.utils.groups.IndexedList;

/**
 * CND Calibration suite
 * Based on the work of Louise Clark thanks!
 *
 * @author  Gavin Murdoch
 */

public class CNDStatusEventListener extends CNDCalibrationEngine {

	//2D option (would be good to add to graphical interface)

	public static boolean View1D = true;
	public static boolean View2D = false;


	public final boolean softTrigger= false;

	//test vars
	public static int NUM_PROCESSED = 0;

	// hists
	//	public final int GEOMEAN = 0;
	//	public final int LOGRATIO = 1;
	public final int ADCL = 0;
	public final int ADCR = 1;
	public final int TDCL = 2;
	public final int TDCR = 3;



	// consts
	public final int LR_CENTROID = 0;
	public final int LR_ERROR = 1;
	public final int GEOMEAN_OVERRIDE = 2;
	public final int GEOMEAN_UNC_OVERRIDE = 3;
	public final int LOGRATIO_OVERRIDE = 4;
	public final int LOGRATIO_UNC_OVERRIDE = 5;	

	// calibration values
	private final double[]		GM_HIST_MAX = {4000.0,8000.0,4000.0};
	private final int[]			GM_HIST_BINS = {160, 320, 160};
	private final double 		LR_THRESHOLD_FRACTION = 0.2;
	private final int			GM_REBIN_THRESHOLD = 50000;

	// Gavin's calibration values
	public static int ADC_HIST_BINS = 60;
	//private final int[] ADC_HIST_X_RANGE = {200, 5000};
	public static double[] ADC_HIST_X_RANGE = {200, 9000};
	public static int TDC_HIST_BINS = 100;
	//private final int[] TDC_HIST_X_RANGE = {CNDPaddlePair.unallowedTDCValues[0] + 1, CNDPaddlePair.unallowedTDCValues[1] - 1};        
	//private final int[] TDC_HIST_X_RANGE = {-360, -340};  
	//private final int[] TDC_HIST_X_RANGE = {-340, -330}; 
	//private final int[] TDC_HIST_X_RANGE = {-1000, 1000}; 
	public static double[] TDC_HIST_X_RANGE = {40, 65}; 

	//        public double TDC_HIST_X_MIN = CNDPaddlePair.unallowedTDCValues[0] + 1;
	//        public double TDC_HIST_X_MAX = CNDPaddlePair.unallowedTDCValues[1] - 1.;

	public final int		EXPECTED_STATUS = 0;
	public final double[]	ALPHA = {13.4, 4.7, 8.6};
	public final double[]	MAX_VOLTAGE = {2500.0, 2000.0, 2500.0};
	public final double		MAX_DELTA_V = 250.0;
	public final int		MIN_STATS = 100;

	public String hvSetPrefix = "FTOFHVSET";

	public H1F statHistL1;
	public H1F statHistL2;
	public H1F statHistL3;

	private String showPlotType = "ADCL";

	public CNDStatusEventListener() {

		stepName = "Status";
		fileNamePrefix = "CND_CALIB_STATUS_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"status_L/I:status_R/I");
		calib.setName("/calibration/cnd/Status_LR");

		//******Set to 0?
		//                calib.setPrecision(1); // record calibration constants to 3 dp


		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {

				//Only good status is zero for both.  Dealing with ints.
				calib.addConstraint(3, 0.0,0.99);
				calib.addConstraint(4, 0.0,0.99);
			}
		}                

		// initialize the counter status
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;
				adcLeftStatus.add(10, sector, layer, component);
				adcRightStatus.add(10, sector, layer, component);
				tdcLeftStatus.add(10, sector, layer, component);
				tdcRightStatus.add(10, sector, layer, component);
			}
		}
	}

	@Override
	public void populatePrevCalib() {
		prevCalRead = true;
	}

	//    @Override
	//    public void populatePrevCalib() {
	//    public void populatePrevCalib2() {
	//        
	//        System.out.println("!!!!!IN populatePrevCalib2 FOR STATUS!!!!!");
	//        
	//        
	//
	//        if (calDBSource==CAL_FILE) {
	//
	//            // read in the values from the text file            
	//            String line = null;
	//            try { 
	//
	//                // Open the file
	//                FileReader fileReader = 
	//                        new FileReader(prevCalFilename);
	//
	//                // Always wrap FileReader in BufferedReader
	//                BufferedReader bufferedReader = 
	//                        new BufferedReader(fileReader);            
	//
	//                line = bufferedReader.readLine();
	//
	//                while (line != null) {
	//
	//                    String[] lineValues;
	//                    lineValues = line.split(" ");
	//
	//                    int sector = Integer.parseInt(lineValues[0]);
	//                    int layer = Integer.parseInt(lineValues[1]);
	//                    int component = Integer.parseInt(lineValues[2]);
	//                    int statusLeft = Integer.parseInt(lineValues[3]);
	//                    int statusRight = Integer.parseInt(lineValues[4]);
	//
	//                    statusValues.addEntry(sector, layer, component);
	//                    statusValues.setIntValue(statusLeft,
	//                            "status_L", sector, layer, component);                    
	//                    statusValues.setIntValue(statusRight,
	//                            "status_R", sector, layer, component);                    
	//
	//                    line = bufferedReader.readLine();
	//                }
	//
	//                bufferedReader.close();            
	//            }
	//            catch(FileNotFoundException ex) {
	//                ex.printStackTrace();
	//                System.out.println(
	//                        "Unable to open file '" + 
	//                                prevCalFilename + "'");                
	//            }
	//            catch(IOException ex) {
	//                System.out.println(
	//                        "Error reading file '" 
	//                                + prevCalFilename + "'");                   
	//                ex.printStackTrace();
	//            }            
	//        }
	//        else if (calDBSource==CAL_DEFAULT) {
	//            
	//            System.out.println("Default");
	//
	//            for (int sector = 1; sector <= 24; sector++) {
	//                for (int layer = 1; layer <= 3; layer++) {
	//                    int component = 1;
	//                            
	//                    statusValues.addEntry(sector, layer, component);
	//                    statusValues.setIntValue(EXPECTED_STATUS,
	//                            "slope_L", sector, layer, component);
	//                    statusValues.setIntValue(EXPECTED_STATUS,
	//                            "offset_L", sector, layer, component);                    
	//
	//                }
	//                
	//                
	//
	////			for (int sector = 1; sector <= 6; sector++) {
	////				for (int layer = 1; layer <= 3; layer++) {
	////					int layer_index = layer - 1;
	////					for (int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++) {
	////						leftRightValues.addEntry(sector, layer, paddle);
	//
	////						
	////					}
	////				}
	//}                 
	//                
	//            }            
	//        }
	//        else if (calDBSource==CAL_DB) {
	//            DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
	//            statusValues = dcp.readConstants("/calibration/cnd/TDC_conv");   
	//            dcp.disconnect();
	//        }
	//    }



	@Override
	public void resetEventListener() {

		if(CNDCalibration.statusmode==1){
			View1D = false;
			View2D = true;
		}

		if(CNDCalibration.binADCstatus!=0.0){
			ADC_HIST_BINS=CNDCalibration.binADCstatus;
		}

		if(CNDCalibration.bintimestatus!=0.0){
			TDC_HIST_BINS=CNDCalibration.bintimestatus;
		}

		if(CNDCalibration.maxADCstatus!=0.0){
			ADC_HIST_X_RANGE[1]=CNDCalibration.maxADCstatus;
		}

		if(CNDCalibration.minADCstatus!=0.0){
			ADC_HIST_X_RANGE[0]=CNDCalibration.minADCstatus;
		}


		if(CNDCalibration.maxtimestatus!=0.0){
			TDC_HIST_X_RANGE[1]=CNDCalibration.maxtimestatus;
		}

		if(CNDCalibration.mintimestatus!=0.0){
			TDC_HIST_X_RANGE[0]=CNDCalibration.mintimestatus;
		}


		// create histogram of stats per layer / sector
		statHistL1 = new H1F("statHist","statHist", 48,0.0,48.0);
		statHistL1.setTitle("Number of hits per sector for layer 1");
		statHistL1.getXaxis().setTitle("Sector");
		statHistL1.getYaxis().setTitle("Number of hits");

		statHistL2 = new H1F("statHist","statHist", 48,0.0,48.0);
		statHistL2.setTitle("Number of hits per sector for layer 2");
		statHistL2.getXaxis().setTitle("Sector");
		statHistL2.getYaxis().setTitle("Number of hits");

		statHistL3 = new H1F("statHist","statHist", 48,0.0,48.0);
		statHistL3.setTitle("Number of hits per sector for layer 3");
		statHistL3.getXaxis().setTitle("Sector");
		statHistL3.getYaxis().setTitle("Number of hits");

		// GM perform init processing
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				// create all the histograms
				TOFH1F adcLHist = new TOFH1F("adcL",
						"ADCL Sector " + sector + " Layer " + layer + " Component" + component,
						200, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1]);
				TOFH1F adcRHist = new TOFH1F("adcR",
						"ADCR Sector " + sector + " Layer " + layer + " Component" + component,
						200, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1]);
				TOFH1F tdcLHist = new TOFH1F("tdcL",
						"TDCL Sector " + sector + " Layer " + layer + " Component" + component,
						TDC_HIST_BINS, TDC_HIST_X_RANGE[0], TDC_HIST_X_RANGE[1]);
				TOFH1F tdcRHist = new TOFH1F("tdcR",
						"TDCR Sector " + sector + " Layer " + layer + " Component" + component,
						TDC_HIST_BINS, TDC_HIST_X_RANGE[0], TDC_HIST_X_RANGE[1]);

				H2F histL
				= new H2F("tdcLvstdcR",
						"Sector " + sector + " Layer " + layer + " Component" + component,
						TDC_HIST_BINS, TDC_HIST_X_RANGE[0], TDC_HIST_X_RANGE[1],
						TDC_HIST_BINS, TDC_HIST_X_RANGE[0], TDC_HIST_X_RANGE[1]);

				H2F histR
				= new H2F("adcLvsadcR",
						"Sector " + sector + " Layer " + layer + " Component" + component,
						ADC_HIST_BINS, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1],
						ADC_HIST_BINS, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1]);

				H2F histL2
				= new H2F("adcvstdcL",
						"ADCL Sector " + sector + " Layer " + layer + " Component" + component,
						TDC_HIST_BINS, TDC_HIST_X_RANGE[0], TDC_HIST_X_RANGE[1],
						ADC_HIST_BINS, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1]);

				H2F histR2
				= new H2F("adcvstdcR",
						"ADCR Sector " + sector + " Layer " + layer + " Component" + component,
						TDC_HIST_BINS, TDC_HIST_X_RANGE[0], TDC_HIST_X_RANGE[1],
						ADC_HIST_BINS, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1]);
				//                          


				DataGroup dg = new DataGroup(2,2);

				if(View1D){

					dg.addDataSet(adcLHist, 0);
					dg.addDataSet(adcRHist, ADCR);
					dg.addDataSet(tdcLHist, TDCL);
					dg.addDataSet(tdcRHist, TDCR);
				}

				else if (View2D){ 

					dg.addDataSet(histL, 0);
					dg.addDataSet(histR, 1); 
					dg.addDataSet(histL2, 2);
					dg.addDataSet(histR2, 3);   
				}

				//                            



				dataGroups.add(dg, sector, layer, component);
				setPlotTitle(sector, layer, component);

				//System.out.println("I'm there ");
				// initialize the constants array
				Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};

				// status_L, status_R ?

				constants.add(consts, sector, layer, component);

			}
		}
	}

	@Override
	public void processEvent(DataEvent event) {

		//DataProvider dp = new DataProvider();
		List<CNDPaddlePair> paddleList = DataProvider.getPaddlePairList(event);
		processPaddlePairList(paddleList);

	}

	public void processPaddlePairList(List<CNDPaddlePair> paddlePairList) {

		//		System.out.println("HV Process paddle list");

		//System.out.println("enter processPaddlelist");

		for (CNDPaddlePair paddlePair : paddlePairList) {


			NUM_PROCESSED++;

			//                        System.out.println("In Status process paddle pair list loop");

			int sector = paddlePair.getDescriptor().getSector();
			int layer = paddlePair.getDescriptor().getLayer();
			int component = paddlePair.getDescriptor().getComponent();

			int directHitPaddle = 0;
			int neighbourHitPaddle = 1;

			double tdc[] = {paddlePair.TDCL, paddlePair.TDCR};
			double time[] = {paddlePair.tdcToTime(paddlePair.TDCL), paddlePair.tdcToTime(paddlePair.TDCR)};

			// Only going to deal with direct hits!!
			if (time[0] < time[1]){

				directHitPaddle = 0;
				neighbourHitPaddle = 1;

			} else if (time[1] < time[0]){

				directHitPaddle = 1;
				neighbourHitPaddle = 0;
			}

			if(layer==1){
				if (directHitPaddle == 0){
					statHistL1.fill((sector-1)*2);
				}
				else if (directHitPaddle == 1){
					statHistL1.fill(((sector-1)*2)+1);
				}
			}
			else if(layer==2){
				if (directHitPaddle == 0){
					statHistL2.fill((sector-1)*2);
				}
				else if (directHitPaddle == 1){
					statHistL2.fill(((sector-1)*2)+1);
				}
			}
			else if(layer==3){
				if (directHitPaddle == 0){
					statHistL3.fill((sector-1)*2);
				}
				else if (directHitPaddle == 1){
					statHistL3.fill(((sector-1)*2)+1);
				}
			}
			//System.out.println("HV paddle "+sector+" "+layer+" "+component );


			//                                        if (sector == 10 && layer == 3 ) {
			//                                            System.out.println("\nSTATUS TAB");
			//                                            System.out.println("ADC L "+paddlePair.ADCL);
			//                                            System.out.println("ADC R "+paddlePair.ADCR);
			//                                            System.out.println("TDC L "+paddlePair.TDCL);
			//                                            System.out.println("TDC R "+paddlePair.TDCR);
			//                                        }                          

			//System.out.println("HV paddle "+sector+layer+component+" geoMean "+paddle.geometricMean());

			//			if (paddle.isValidGeoMean() && paddle.geometricMean() > EXPECTED_STATUS[layer-1] * 0.25) {
			///&& paddle.trackFound()) {
			//				dataGroups.getItem(sector,layer,component).getH1F("geomean").fill(paddle.geometricMean());
			//				statHist.fill(((layer-1)*10)+sector);
			//			}

			//			if (paddle.isValidLogRatio()) {
			//				dataGroups.getItem(sector,layer,component).getH1F("logratio").fill(paddle.logRatio());
			//			}

			// Fill all histograms

			//        private final double[] ADC_HIST_X_RANGE = {CNDPaddlePair.unallowedADCValue + 1, 6000.};
			//        private final int TDC_HIST_BINS = 640;
			//        private final double[] TDC_HIST_X_RANGE = {CNDPaddlePair.unallowedTDCValues[0] + 1, CNDPaddlePair.unallowedTDCValues[1] - 1.};   

			//                        System.out.println("Now filling status");

			boolean include=false;
			if(softTrigger){
				///////////////
				// added software trigger : require a hit in at least two layers of a given sector
				///////////////



				for (int j= 0; j < paddlePairList.size(); j++) {
					//look for other hits in the same sector
					if (paddlePairList.get(j).getDescriptor().getSector()==sector &&

							(	(layer==1 && paddlePairList.get(j).getDescriptor().getLayer()==2) 	
									|| 
									(layer==2 && (paddlePairList.get(j).getDescriptor().getLayer()==3 || paddlePairList.get(j).getDescriptor().getLayer()==1))
									||
									(layer==3 && paddlePairList.get(j).getDescriptor().getLayer()==2)	) ) {
						include=true;
						//System.out.println("layer "+layer+" layer 2 "+paddlePairList.get(j).getDescriptor().getLayer());
					}
				}

				//								for (int j= 0; j < paddlePairList.size(); j++) {
				//									for (int k= 0; j < paddlePairList.size(); k++) {
				//										if(paddlePairList.get(j).getDescriptor().getSector()==sector && 
				//												paddlePairList.get(k).getDescriptor().getSector()==sector){
				//				
				//											int layerj=paddlePairList.get(j).getDescriptor().getLayer();
				//											int layerk=paddlePairList.get(k).getDescriptor().getLayer();
				//											
				//											if(
				//													(layer==1 && layerj==2 && layerk==3) ||
				//													(layer==1 && layerj==3 && layerk==2) ||
				//													(layer==2 && layerj==1 && layerk==3) ||
				//													(layer==2 && layerj==3 && layerk==1) ||
				//													(layer==3 && layerj==1 && layerk==2) ||
				//													(layer==3 && layerj==2 && layerk==1) 
				//													){
				//												include=true;
				//												System.out.println("layer "+layer+" layerj "+layerj+" layerk "+layerk);
				//											}
				//											}
				//										}
				//									}
				//System.out.println("sector "+sector+" layer "+layer+" include "+include);
				//////////////// 
				// end of trigger
				////////////////
			}
			//normal Status code
			if((softTrigger && include) || (!softTrigger)){
				if (View1D){
					//					if (paddlePair.ADCL > CNDPaddlePair.unallowedADCValue && paddlePair.TDCL > CNDPaddlePair.unallowedTDCValues[0]
					//							&& paddlePair.TDCL < CNDPaddlePair.unallowedTDCValues[1]){
					//						dataGroups.getItem(sector,layer,component).getH1F("adcL").fill(paddlePair.ADCL);
					//						dataGroups.getItem(sector,layer,component).getH1F("tdcL").fill(paddlePair.TDCL);
					//						//                            System.out.println("paddlePair.ADCL = " + paddlePair.ADCL);
					//					}
					//					if (paddlePair.ADCR > CNDPaddlePair.unallowedADCValue && paddlePair.TDCR > CNDPaddlePair.unallowedTDCValues[0]
					//							&& paddlePair.TDCR < CNDPaddlePair.unallowedTDCValues[1]){
					//						dataGroups.getItem(sector,layer,component).getH1F("adcR").fill(paddlePair.ADCR);
					//						dataGroups.getItem(sector,layer,component).getH1F("tdcR").fill(paddlePair.TDCR);
					//						//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            
					//					}
					if (paddlePair.ADCL > CNDPaddlePair.unallowedADCValue && paddlePair.TDCL > CNDPaddlePair.unallowedTDCValues[0]
							&& paddlePair.TDCL < CNDPaddlePair.unallowedTDCValues[1]){
						dataGroups.getItem(sector,layer,component).getH1F("adcL").fill(paddlePair.ADCL);
						dataGroups.getItem(sector,layer,component).getH1F("tdcL").fill(paddlePair.TDCL*CNDPaddlePair.NS_PER_CH-(paddlePair.EVENT_START_TIME));
						//System.out.println("paddlePair.ADCL = " + paddlePair.EVENT_START_TIME);
					}
					if (paddlePair.ADCR > CNDPaddlePair.unallowedADCValue && paddlePair.TDCR > CNDPaddlePair.unallowedTDCValues[0]
							&& paddlePair.TDCR < CNDPaddlePair.unallowedTDCValues[1]){
						dataGroups.getItem(sector,layer,component).getH1F("adcR").fill(paddlePair.ADCR);
						dataGroups.getItem(sector,layer,component).getH1F("tdcR").fill(paddlePair.TDCR*CNDPaddlePair.NS_PER_CH-(paddlePair.EVENT_START_TIME));
						//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            
					}


					//for no cuts
					//					
					//						dataGroups.getItem(sector,layer,component).getH1F("adcL").fill(paddlePair.ADCL);
					//						dataGroups.getItem(sector,layer,component).getH1F("tdcL").fill(paddlePair.TDCL);
					//						//                            System.out.println("paddlePair.ADCL = " + paddlePair.ADCL);
					//					
					//					
					//						
					//						dataGroups.getItem(sector,layer,component).getH1F("adcR").fill(paddlePair.ADCR);
					//						dataGroups.getItem(sector,layer,component).getH1F("tdcR").fill(paddlePair.TDCR);
					//						//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            


				}

				//
				//			
				else if (View2D){          
					// 2D distributions

					//paddlePair.show();
					if(paddlePair.EVENT_START_TIME!=0.0){
						//System.out.println(paddlePair.EVENT_START_TIME);
						if (paddlePair.ADCL > CNDPaddlePair.unallowedADCValue && paddlePair.TDCL > CNDPaddlePair.unallowedTDCValues[0] + 1
						&& paddlePair.TDCL < CNDPaddlePair.unallowedTDCValues[1] - 1){
							dataGroups.getItem(sector,layer,component).getH2F("tdcLvstdcR").fill(paddlePair.TDCL*CNDPaddlePair.NS_PER_CH-(paddlePair.EVENT_START_TIME),paddlePair.TDCR*CNDPaddlePair.NS_PER_CH-(paddlePair.EVENT_START_TIME));
							// System.out.println(paddlePair.TDCL*CNDPaddlePair.NS_PER_CH-(paddlePair.EVENT_START_TIME));
						}
						if (paddlePair.ADCR > CNDPaddlePair.unallowedADCValue && paddlePair.TDCR > CNDPaddlePair.unallowedTDCValues[0] + 1
						&& paddlePair.TDCR < CNDPaddlePair.unallowedTDCValues[1] - 1){
							dataGroups.getItem(sector,layer,component).getH2F("adcLvsadcR").fill(paddlePair.ADCL,paddlePair.ADCR);
							//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            
						}
						if (paddlePair.ADCL > CNDPaddlePair.unallowedADCValue && paddlePair.TDCL > CNDPaddlePair.unallowedTDCValues[0] + 1
						&& paddlePair.TDCL < CNDPaddlePair.unallowedTDCValues[1] - 1){
							dataGroups.getItem(sector,layer,component).getH2F("adcvstdcL").fill(paddlePair.TDCL*CNDPaddlePair.NS_PER_CH-(paddlePair.EVENT_START_TIME),paddlePair.ADCL);
							//                            System.out.println("paddlePair.ADCL = " + paddlePair.ADCL);
						}
						if (paddlePair.ADCR > CNDPaddlePair.unallowedADCValue && paddlePair.TDCR > CNDPaddlePair.unallowedTDCValues[0] + 1
						&& paddlePair.TDCR < CNDPaddlePair.unallowedTDCValues[1] - 1){
							dataGroups.getItem(sector,layer,component).getH2F("adcvstdcR").fill(paddlePair.TDCR*CNDPaddlePair.NS_PER_CH-(paddlePair.EVENT_START_TIME),paddlePair.ADCR);
							//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            
						}
					}
					//										if (paddlePair.ADCL > CNDPaddlePair.unallowedADCValue && paddlePair.TDCL > CNDPaddlePair.unallowedTDCValues[0] + 1
					//										&& paddlePair.TDCL < CNDPaddlePair.unallowedTDCValues[1] - 1){
					//											dataGroups.getItem(sector,layer,component).getH2F("tdcLvstdcR").fill(paddlePair.TDCL,paddlePair.TDCR);
					//											//                            System.out.println("paddlePair.ADCL = " + paddlePair.ADCL);
					//										}
					//										if (paddlePair.ADCR > CNDPaddlePair.unallowedADCValue && paddlePair.TDCR > CNDPaddlePair.unallowedTDCValues[0] + 1
					//										&& paddlePair.TDCR < CNDPaddlePair.unallowedTDCValues[1] - 1){
					//											dataGroups.getItem(sector,layer,component).getH2F("adcLvsadcR").fill(paddlePair.ADCL,paddlePair.ADCR);
					//											//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            
					//										}
					//										if (paddlePair.ADCL > CNDPaddlePair.unallowedADCValue && paddlePair.TDCL > CNDPaddlePair.unallowedTDCValues[0] + 1
					//										&& paddlePair.TDCL < CNDPaddlePair.unallowedTDCValues[1] - 1){
					//											dataGroups.getItem(sector,layer,component).getH2F("adcvstdcL").fill(paddlePair.TDCL,paddlePair.ADCL);
					//											//                            System.out.println("paddlePair.ADCL = " + paddlePair.ADCL);
					//										}
					//										if (paddlePair.ADCR > CNDPaddlePair.unallowedADCValue && paddlePair.TDCR > CNDPaddlePair.unallowedTDCValues[0] + 1
					//										&& paddlePair.TDCR < CNDPaddlePair.unallowedTDCValues[1] - 1){
					//											dataGroups.getItem(sector,layer,component).getH2F("adcvstdcR").fill(paddlePair.TDCR,paddlePair.ADCR);
					//											//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            
					//										}

				}
			}
		}



		// Tests:
		//                        if (sector == 1 && layer == 2){
		////                            dataGroups.getItem(sector,layer,component).getH1F("adcL").fill(paddlePair.ADCL);
		//                            dataGroups.getItem(sector,layer,component).getH1F("adcR").fill(paddlePair.ADCR);
		//                            dataGroups.getItem(sector,layer,component).getH1F("tdcL").fill(paddlePair.TDCL);
		////                            dataGroups.getItem(sector,layer,component).getH1F("tdcR").fill(paddlePair.TDCR);
		//                        } else if (sector == 3 && layer == 3){
		////                            dataGroups.getItem(sector,layer,component).getH1F("adcL").fill(paddlePair.ADCL);
		//                            dataGroups.getItem(sector,layer,component).getH1F("adcR").fill(paddlePair.ADCR);
		////                            dataGroups.getItem(sector,layer,component).getH1F("tdcL").fill(paddlePair.TDCL);
		//                            dataGroups.getItem(sector,layer,component).getH1F("tdcR").fill(paddlePair.TDCR);                            
		//                        } else {
		//                            dataGroups.getItem(sector,layer,component).getH1F("adcL").fill(paddlePair.ADCL);
		//                            dataGroups.getItem(sector,layer,component).getH1F("adcR").fill(paddlePair.ADCR);
		//                            dataGroups.getItem(sector,layer,component).getH1F("tdcL").fill(paddlePair.TDCL);
		//                            dataGroups.getItem(sector,layer,component).getH1F("tdcR").fill(paddlePair.TDCR);
		//                        }



		//System.out.println("leave processPaddlelist");
	}

	@Override
	public void fit(int sector, int layer, int paddle,
			double minRange, double maxRange){

		//        System.out.println("Nothing to fit for Status");

	}

	//	public void fitLogRatio(int sector, int layer, int paddle,
	//			double minRange, double maxRange){
	//
	//		H1F h = dataGroups.getItem(sector,layer,paddle).getH1F("logratio");
	//
	//		// calculate the mean value using portion of the histogram where counts are > 0.2 * max counts
	//		
	//		double sum =0.0;
	//		double sumWeight =0.0;
	//		double sumSquare =0.0;
	//		int maxBin = h.getMaximumBin();
	//		double maxCounts = h.getBinContent(maxBin);
	//		int nBins = h.getAxis().getNBins();
	//		int lowThresholdBin = 0;
	//		int highThresholdBin = nBins-1;
	//		
	//		// find the bin left of max bin where counts drop to 0.2 * max
	//		for (int i=maxBin; i>0; i--) {
	//
	//			if (h.getBinContent(i) < LR_THRESHOLD_FRACTION*maxCounts) {
	//				lowThresholdBin = i;
	//				break;
	//			}
	//		}
	//
	//		// find the bin right of max bin where counts drop to 0.2 * max
	//		for (int i=maxBin; i<nBins; i++) {
	//
	//			if (h.getBinContent(i) < LR_THRESHOLD_FRACTION*maxCounts) {
	//				highThresholdBin = i;
	//				break;
	//			}
	//		}
	//
	//		// include the values in the sum if we're within the thresholds
	//		for (int i=lowThresholdBin; i<=highThresholdBin; i++) {
	//
	//			double value=h.getBinContent(i);
	//			double middle=h.getAxis().getBinCenter(i);
	//
	//			sum+=value;			
	//			sumWeight+=value*middle;
	//			sumSquare+=value*middle*middle;
	//		}			
	//
	//		double logRatioMean = 0.0;
	//		double logRatioError = 0.0;
	//
	//		if (sum>0) {
	//			logRatioMean=sumWeight/sum;
	//			logRatioError=(1/Math.sqrt(sum))*Math.sqrt((sumSquare/sum)-(logRatioMean*logRatioMean));
	//		}
	//		else {
	//			logRatioMean=0.0;
	//			logRatioError=0.0;
	//		}
	//		
	//		// store the function showing the width over which mean is calculated
	////		F1D lrFunc = dataGroups.getItem(sector,layer,paddle).getF1D("lrFunc");
	////		lrFunc.setRange(h.getAxis().getBinCenter(lowThresholdBin), h.getAxis().getBinCenter(highThresholdBin));
	////
	////		lrFunc.setParameter(0, LR_THRESHOLD_FRACTION*maxCounts); // height to draw line at
	//
	//		// put the constants in the list
	//		Double[] consts = constants.getItem(sector, layer, paddle);
	//		consts[LR_CENTROID] = logRatioMean;
	//		consts[LR_ERROR] = logRatioError;
	//
	//	}

	@Override
	public void customFit(int sector, int layer, int paddle){

		//            System.out.println("Nothing to fit for Status");

		JOptionPane.showMessageDialog(new JPanel(),
				"Nothing to fit for this tab...");                    

		//			for (int p=minP; p<=maxP; p++) {
		//				// save the override values
		//				Double[] consts = constants.getItem(sector, layer, p);
		//				consts[GEOMEAN_OVERRIDE] = overrideGM;
		//				consts[GEOMEAN_UNC_OVERRIDE] = overrideGMUnc;
		//				consts[LOGRATIO_OVERRIDE] = overrideLR;
		//				consts[LOGRATIO_UNC_OVERRIDE] = overrideLRUnc;
		//	
		//				// update the table
		//				saveRow(sector,layer,p);
		//			}
		//			calib.fireTableDataChanged();

	}

	public int getComponentStatus(int sector, int layer, int component, int side) {

		//• Output of this function
		//• 0 - fully functioning
		//• 1 - no ADC
		//• 2 - no TDC
		//• 3 - no ADC, no TDC (PMT is dead)
		//• 5 - any other issue        

		int side_index = side - 1;

		String[] histNames = {
				"adcL",
				"adcR",
				"tdcL",
		"tdcR"};

		int componentStatus = 0;  //Assume good at first
		//       

		if(View1D){
			//remove to use 2D distributions
			H1F adcHist;
			H1F tdcHist; 
			adcHist = dataGroups.getItem(sector,layer,component).getH1F(histNames[0+side_index]); 
			tdcHist = dataGroups.getItem(sector,layer,component).getH1F(histNames[2+side_index]);

			if ( adcHist.getEntries() <100 && tdcHist.getEntries() > 100 ) componentStatus = 1;
			if ( adcHist.getEntries() >100 && tdcHist.getEntries() <100 ) componentStatus = 2;
			if ( adcHist.getEntries() < 100 && tdcHist.getEntries() <100 ) componentStatus = 3;
		}


		return componentStatus;

	}        


	@Override
	public void saveRow(int sector, int layer, int component) {
		// System.out.println("Now in CNDStatusEventListener.saveRow()");

		calib.setIntValue(getComponentStatus(sector, layer, component, 1),                        
				"status_L", sector, layer, component);
		calib.setIntValue(getComponentStatus(sector, layer, component, 2),
				"status_R", sector, layer, component);

	}
	//	
	//	@Override
	//	public DataGroup getSummary(int sector, int layer) {
	//
	//		double[] componentNumbers = new double[1];
	//		double[] componentUncs = new double[1];
	//		double[] status = new double[1];
	//		double[] statusUncs = new double[1];       
	//		
	//		for (int c = 1; c <= 1; c++) {
	//
	//                    System.out.println("In loop");
	//			componentNumbers[c - 1] = (double) c;
	//			componentUncs[c - 1] = 0.0;
	//                        //****Need to display both left are right!
	//			status[c - 1] = (double) getComponentStatus(sector, layer, c, 1);
	//			statusUncs[c - 1] = 0.0;
	//		}
	//
	//                GraphErrors summ = new GraphErrors("summ", componentNumbers,
	//                    status, componentUncs, statusUncs);
	//                
	//		summ.setTitleX("Component Number");
	//		summ.setTitleY("Status");
	//		summ.setMarkerSize(CNDCalibrationEngine.MARKER_SIZE);
	//		summ.setLineThickness(CNDCalibrationEngine.MARKER_LINE_WIDTH);
	//
	//		DataGroup dg = new DataGroup(1,1);
	//		dg.addDataSet(summ, 0);
	//		return dg;
	//		
	//	}

	@Override
	public DataGroup getSummary() {

		double[] sectorNumber = new double[24];
		double[] zeroUncs = new double[24];        
		double[] statusL3L = new double[24];
		double[] statusL3R = new double[24];
		double[] statusL2L = new double[24];
		double[] statusL2R = new double[24];
		double[] statusL1L = new double[24];
		double[] statusL1R = new double[24];

		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				sectorNumber[sector - 1] = (double) sector;
				zeroUncs[sector - 1] = 0.0;

				if (layer == 3){
					statusL3L[sector - 1] = getComponentStatus(sector, layer, component, 1);
					statusL3R[sector - 1] = getComponentStatus(sector, layer, component, 2);
				} else if (layer == 2){
					statusL2L[sector - 1] = getComponentStatus(sector, layer, component, 1);
					statusL2R[sector - 1] = getComponentStatus(sector, layer, component, 2);
				} else if (layer == 1){
					statusL1L[sector - 1] = getComponentStatus(sector, layer, component, 1);
					statusL1R[sector - 1] = getComponentStatus(sector, layer, component, 2);
				}

				//                if ( layer == 3 ){
				//                    System.out.println("\n S" + sector + " L" + layer + " C" + component);
				//                    System.out.println(statusL3L[sector - 1] + "\t" + statusL3R[sector - 1]);
				//                    System.out.println(statusL2L[sector - 1] + "\t" + statusL2R[sector - 1]);
				//                    System.out.println(statusL1L[sector - 1] + "\t" + statusL1R[sector - 1]);
				//                }

			}
		}

		GraphErrors allL3L = new GraphErrors("allL3L", sectorNumber,
				statusL3L, zeroUncs, zeroUncs);

		allL3L.setTitleX("Sector Number");
		allL3L.setTitleY("Status");
		allL3L.setMarkerSize(MARKER_SIZE);
		allL3L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL3R = new GraphErrors("allL3R", sectorNumber,
				statusL3R, zeroUncs, zeroUncs);

		allL3R.setTitleX("Sector Number");
		allL3R.setTitleY("Status");
		allL3R.setMarkerSize(MARKER_SIZE);
		allL3R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL2L = new GraphErrors("allL2L", sectorNumber,
				statusL2L, zeroUncs, zeroUncs);

		allL2L.setTitleX("Sector Number");
		allL2L.setTitleY("Status");
		allL2L.setMarkerSize(MARKER_SIZE);
		allL2L.setLineThickness(MARKER_LINE_WIDTH);     

		GraphErrors allL2R = new GraphErrors("allL2R", sectorNumber,
				statusL2R, zeroUncs, zeroUncs);

		allL2R.setTitleX("Sector Number");
		allL2R.setTitleY("Status");
		allL2R.setMarkerSize(MARKER_SIZE);
		allL2R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1L = new GraphErrors("allL1L", sectorNumber,
				statusL1L, zeroUncs, zeroUncs);

		allL1L.setTitleX("Sector Number");
		allL1L.setTitleY("Status");
		allL1L.setMarkerSize(MARKER_SIZE);
		allL1L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1R = new GraphErrors("allL1R", sectorNumber,
				statusL1R, zeroUncs, zeroUncs);

		allL1R.setTitleX("Sector Number");
		allL1R.setTitleY("Status");
		allL1R.setMarkerSize(MARKER_SIZE);
		allL1R.setLineThickness(MARKER_LINE_WIDTH);    

		//		DataGroup dg = new DataGroup(2, 3);
		//		dg.addDataSet(allL3L, 0);
		//		dg.addDataSet(allL3R, 1);
		//		dg.addDataSet(allL2L, 2);
		//		dg.addDataSet(allL2R, 3);
		//		dg.addDataSet(allL1L, 4);    
		//		dg.addDataSet(allL1R, 5);

		DataGroup dg = new DataGroup(1, 3);
		dg.addDataSet(statHistL1, 0);
		dg.addDataSet(statHistL2, 1);
		dg.addDataSet(statHistL3, 2);


		return dg;

	}


	@Override
	public boolean isGoodComponent(int sector, int layer, int component) {

		//***Need this test for both paddles in the component!***
		//System.out.println("in Status isGoodComponent for S"+sector+" L"+layer+" C"+component);
		return (getComponentStatus(sector,layer,component,1) == EXPECTED_STATUS
				&& getComponentStatus(sector,layer,component,2) == EXPECTED_STATUS);

	}

	@Override
	public void setPlotTitle(int sector, int layer, int component) {
		// reset hist title as may have been set to null by show all 
		if (View1D){
			dataGroups.getItem(sector,layer,component).getH1F("adcL").setTitle("ADCL - S"+sector+" L"+layer+" C"+1);
			dataGroups.getItem(sector,layer,component).getH1F("adcR").setTitle("ADCR - S"+sector+" L"+layer+" C"+2);
			dataGroups.getItem(sector,layer,component).getH1F("tdcL").setTitle("TDCL - S"+sector+" L"+layer+" C"+1);
			dataGroups.getItem(sector,layer,component).getH1F("tdcR").setTitle("TDCR - S"+sector+" L"+layer+" C"+2);                
			dataGroups.getItem(sector,layer,component).getH1F("adcL").setTitleX("ADC (channels)");
			dataGroups.getItem(sector,layer,component).getH1F("adcR").setTitleX("ADC (channels)");
			dataGroups.getItem(sector,layer,component).getH1F("tdcL").setTitleX("TDC (channels)");
			dataGroups.getItem(sector,layer,component).getH1F("tdcR").setTitleX("TDC (channels)");
			dataGroups.getItem(sector,layer,component).getH1F("adcL").setTitleY("Counts");
			dataGroups.getItem(sector,layer,component).getH1F("adcR").setTitleY("Counts");
			dataGroups.getItem(sector,layer,component).getH1F("tdcL").setTitleY("Counts");
			dataGroups.getItem(sector,layer,component).getH1F("tdcR").setTitleY("Counts");
		}
		else if (View2D){
			dataGroups.getItem(sector,layer,component).getH2F("tdcLvstdcR").setTitle("TDCR VS TDCL - S"+sector+" L"+layer+" C"+1);
			dataGroups.getItem(sector,layer,component).getH2F("adcLvsadcR").setTitle("ADCR VS ADCL- S"+sector+" L"+layer+" C"+2);
			dataGroups.getItem(sector,layer,component).getH2F("tdcLvstdcR").setTitleX("TDC");
			dataGroups.getItem(sector,layer,component).getH2F("adcLvsadcR").setTitleX("ADC");
			dataGroups.getItem(sector,layer,component).getH2F("tdcLvstdcR").setTitleY("TDC");
			dataGroups.getItem(sector,layer,component).getH2F("adcLvsadcR").setTitleY("ADC");

			dataGroups.getItem(sector,layer,component).getH2F("adcvstdcL").setTitle("ADCL TDCL - S"+sector+" L"+layer+" C"+1);
			dataGroups.getItem(sector,layer,component).getH2F("adcvstdcR").setTitle("ADCR TDCR- S"+sector+" L"+layer+" C"+2);
			dataGroups.getItem(sector,layer,component).getH2F("adcvstdcL").setTitleY("ADC (channels)");
			dataGroups.getItem(sector,layer,component).getH2F("adcvstdcR").setTitleY("ADC (channels)");
			dataGroups.getItem(sector,layer,component).getH2F("adcvstdcL").setTitleX("TDC");
			dataGroups.getItem(sector,layer,component).getH2F("adcvstdcR").setTitleX("TDC");
		}
	}

	@Override
	public void drawPlots(int sector, int layer, int component, EmbeddedCanvas canvas) {

		System.out.println("CNDStatusEventListener.drawPlots() for S L C "+sector+" "+layer+" "+component);

		if(View1D){
			if (showPlotType == "ADCL") {

				H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("adcL");
				canvas.draw(fitHist);

			} else if (showPlotType == "ADCR") {

				H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("adcR");
				canvas.draw(fitHist);                    

			} else if (showPlotType == "TDCL") {

				H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("tdcL");
				canvas.draw(fitHist);                    

			} else if (showPlotType == "TDCR") {

				H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("tdcR");
				canvas.draw(fitHist);                    

			}
		}
		else if (View2D){
			if (showPlotType == "ADCL") {

				H2F fitHist = dataGroups.getItem(sector,layer,component).getH2F("tdcLvstdcR");
				canvas.draw(fitHist);

			} else if (showPlotType == "TDCL") {

				H2F fitHist1 = dataGroups.getItem(sector,layer,component).getH2F("adcLvsadcR");
				canvas.draw(fitHist1);                    

			} else if (showPlotType == "ADCR") {

				H2F fitHist = dataGroups.getItem(sector,layer,component).getH2F("adcvstdcL");
				canvas.draw(fitHist);                    

			} else if (showPlotType == "TDCR") {

				H2F fitHist = dataGroups.getItem(sector,layer,component).getH2F("adcvstdcR");
				canvas.draw(fitHist);                    

			}
		}

	}

	@Override
	public void showPlots(int sector, int layer) {

		System.out.println("In showPlots()");

		showPlotType = "ADCL";
		stepName = "Status - ADCL";
		super.showPlots(sector, layer);
		showPlotType = "ADCR";
		stepName = "Status - ADCR";
		super.showPlots(sector, layer);                
		showPlotType = "TDCL";
		stepName = "Status - TDCL";
		super.showPlots(sector, layer);
		showPlotType = "TDCR";
		stepName = "Status - TDCR";
		super.showPlots(sector, layer);                

	}

}
