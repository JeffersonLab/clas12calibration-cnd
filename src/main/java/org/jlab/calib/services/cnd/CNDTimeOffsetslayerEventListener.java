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

import org.jlab.detector.base.DetectorDescriptor;
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
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
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

public class CNDTimeOffsetslayerEventListener extends CNDCalibrationEngine {

    // hists
    public final int LAYER = 0;

    // indices for override values
    public final int LEFTRIGHT_OVERRIDE = 0;

    final double LEFT_RIGHT_RATIO = 0.15;
//	final double MAX_LEFTRIGHT = 10.0;

    final double MAX_LEFTRIGHT = 1.0;
    final double MAX_LEFTRIGHT_ERR = 0.5;

     int HIST_BINS = 250;
//    final double HIST_X_MIN = (-0.5*CNDCalibrationEngine.BEAM_BUCKET);
//    final double HIST_X_MAX = (0.5*CNDCalibrationEngine.BEAM_BUCKET);
//    final double HIST_X_MIN = (61.7*CNDCalibrationEngine.BEAM_BUCKET);
//    final double HIST_X_MAX = (62.7*CNDCalibrationEngine.BEAM_BUCKET);
   double HIST_X_MIN = 20;
   double HIST_X_MAX = 60;

    //Height threshold for leading and trailing edge calculations
    public double FIT_HEIGHT_THRESHOLD = 0.5;

    // Fit region around the centre (in percentage of the whole distribution's width) to search for the dip
    public double CENTRAL_REGION_TO_LOOK_FOR_DIP = 0.25;

    public CNDTimeOffsetslayerEventListener() {

        stepName = "Global TimeOffset";
        fileNamePrefix = "CND_CALIB_TIMEOFFSETS_LAYER";
        // get file name here so that each timer update overwrites it
        filename = nextFileName();

        calib = new CalibrationConstants(3,
                "time_offset_layer/F:time_offset_layer_err/F");
        calib.setName("/calibration/cnd/TimeOffsets_layer");
//		calib.setPrecision(3);
//
//        calib.addConstraint(3, -MAX_LEFTRIGHT, MAX_LEFTRIGHT);
//        calib.addConstraint(4, -MAX_LEFTRIGHT_ERR, MAX_LEFTRIGHT_ERR);

    }

//   @Override
//    public void populatePrevCalib() {
//            prevCalRead = true;
//    }        

//    @Override
//    public void populatePrevCalib() {
    @Override
    public void populatePrevCalib() {

        System.out.println("Populating " + stepName + " previous calibration values");
        if (calDBSource == CAL_FILE) {

            System.out.println("File: " + prevCalFilename);
            // read in the left right values from the text file			
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
                    double lr = Double.parseDouble(lineValues[3]);
                    double lrErr = Double.parseDouble(lineValues[3]);

                    layerValues.addEntry(sector, layer, component);
                    layerValues.setDoubleValue(lr,
                            "time_offset_layer", sector, layer, component);
                    layerValues.setDoubleValue(lrErr,
                            "time_offset_layer_err", sector, layer, component);

                    line = bufferedReader.readLine();
                }

                bufferedReader.close();
            } catch (FileNotFoundException ex) {
                System.out.println(
                        "Unable to open file '"
                        + prevCalFilename + "'");
                return;
            } catch (IOException ex) {
                System.out.println(
                        "Error reading file '"
                        + prevCalFilename + "'");
                ex.printStackTrace();
                return;
            }
        } else if (calDBSource == CAL_DEFAULT) {
            System.out.println("Default");
            for (int sector = 1; sector <= 24; sector++) {
                for (int layer = 1; layer <= 3; layer++) {
                    int component = 1;
                    layerValues.addEntry(sector, layer, component);
                    layerValues.setDoubleValue(0.0,
                            "time_offset_layer", sector, layer, component);
                    layerValues.setDoubleValue(0.0,
                            "time_offset_layer_err", sector, layer, component);

                }
            }
        } else if (calDBSource == CAL_DB) {
            System.out.println("Database Run No: " + prevCalRunNo);
            DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
            layerValues = dcp.readConstants("/calibration/cnd/TimeOffsets_layer");
            dcp.disconnect();
        }
        prevCalRead = true;
        System.out.println(stepName + " previous calibration values populated successfully");
    }

    public void resetEventListener() {

    	if(CNDCalibration.bintime!=0.0){
			HIST_BINS=CNDCalibration.bintime;
		}
		if(CNDCalibration.mintime!=0.0){
			HIST_X_MIN =CNDCalibration.mintime;
		}
		if(CNDCalibration.maxtimeLR!=0.0){
			HIST_X_MAX =CNDCalibration.maxtime;
		}

        // GM perform init processing
        for (int sector = 1; sector <= 24; sector++) {
            for (int layer = 1; layer <= 3; layer++) {
                int component = 1;

                // create all the histograms
                H1F hist = new H1F("time_offset_layer",
                        "Time offset layer  Sector " + sector + " Layer " + layer + " Component" + component,
                        HIST_BINS, HIST_X_MIN, HIST_X_MAX);

                // create all the functions
               // F1D func = new F1D("func", "[height]", HIST_X_MIN, HIST_X_MAX);
                F1D func = new F1D("func", "[amp]*gaus(x,[mean],[sigma])", HIST_X_MIN, HIST_X_MAX);
                func.setLineColor(FUNC_COLOUR);
                func.setLineWidth(FUNC_LINE_WIDTH);

                DataGroup dg = new DataGroup(1, 1);
                dg.addDataSet(hist, 0);
                dg.addDataSet(func, LAYER);

                dataGroups.add(dg, sector, layer, component);
                setPlotTitle(sector, layer, component);

                // initialize the constants array
                Double[] consts = {UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE};
                // time_offset_LR, time_offset_LR_err

                constants.add(consts, sector, layer, component);

            }
        }
    }

    @Override
    public void processEvent(DataEvent event) {
    	System.out.println("in process event");
        //DataProvider dp = new DataProvider();
        List<CNDPaddlePair> paddlePairList = DataProvider.getPaddlePairList(event);
        processPaddlePairList(paddlePairList);
        System.out.println("out process event");
    }

    @Override
    public void processPaddlePairList(List<CNDPaddlePair> paddlePairList) {
    	//System.out.println("in paddle pair list");
        for (CNDPaddlePair paddlePair : paddlePairList) {
            
            if (paddlePair.includeInCalib()){
            	//System.out.println("in paddle descriptor ");
                int sector = paddlePair.getDescriptor().getSector();
                int layer = paddlePair.getDescriptor().getLayer();
                int component = paddlePair.getDescriptor().getComponent();
//                System.out.println(sector+" "+layer+" "+component);
//                System.out.println("out paddle descriptor ");
                // Fill all histograms
                //System.out.println(sector +" "+ layer +" "+component +" "+"Layer offset "+paddlePair.layerOffset());
              
                //System.out.println("in histo ");
                if(paddlePair.layerOffset()!=0.0 && paddlePair.CHARGE==-1){
                dataGroups.getItem(sector, layer, component).getH1F("time_offset_layer").fill(paddlePair.layerOffset());
               // System.out.println("start time "+paddlePair.EVENT_START_TIME);
                }
                //System.out.println("out histo ");
            }
        }
       // System.out.println("out paddle pair list");
    }

    @Override
    public void fit(int sector, int layer, int component,
            double minRange, double maxRange) {
        
        System.out.println("S L C = " + sector + " " + layer + " " + component);
        H1F hist = dataGroups.getItem(sector, layer, component).getH1F("time_offset_layer");
       
        if (hist.getEntries() != 0) {  //Need non empty histograms for this method...

            double distributionLeftLeadingEdge = LeadingEdge(hist, FIT_HEIGHT_THRESHOLD);
            double distributionRightTrailingEdge = TrailingEdge(hist, FIT_HEIGHT_THRESHOLD);
//
            double halfWidthOfDistribution = (distributionRightTrailingEdge - distributionLeftLeadingEdge) / 2.;
            double centreOfDistribution = distributionLeftLeadingEdge + halfWidthOfDistribution;
//            double centralRegionWidth = CENTRAL_REGION_TO_LOOK_FOR_DIP * (distributionRightTrailingEdge - distributionLeftLeadingEdge);
//
            double fitMin = distributionLeftLeadingEdge;

            double fitMax = distributionRightTrailingEdge;
        
            F1D lrFunc = dataGroups.getItem(sector, layer, component).getF1D("func");
            lrFunc.setRange(fitMin, fitMax);
            lrFunc.setParameter(0, (hist.getBinContent(hist.getMaximumBin()))); 
           // lrFunc.setParLimits(0, (hist.getBinContent(hist.getMaximumBin()))*0.9, (hist.getBinContent(hist.getMaximumBin()))*1.1); 
            lrFunc.setParameter(1, centreOfDistribution);
            //lrFunc.setParLimits(1,centreOfDistribution*0.9, centreOfDistribution*1.1);
            lrFunc.setParameter(2, 2.*halfWidthOfDistribution);
            //lrFunc.setParLimits(2,2.*halfWidthOfDistribution*0.9, 2.*halfWidthOfDistribution*1.1);
            
            try {
                DataFitter.fit(lrFunc, hist, "RQ");
            } catch (Exception e) {
                System.out.println("Fit error with sector "+sector+" layer "+layer+" component "+component);
                e.printStackTrace();
            }
        }
        
    }

    @Override
    public void customFit(int sector, int layer, int component) {

        customFitCurrent(sector, layer, component);       
        //System.out.println("Left right value from file is "+leftRightAdjustment(sector,layer,paddle));
//        String[] fields = {"Override centroid:", "SPACE"};
//        CNDCustomFitPanel panel = new CNDCustomFitPanel(fields, sector, layer);
//
//        int result = JOptionPane.showConfirmDialog(null, panel,
//                "Adjust Fit / Override for paddle " + component, JOptionPane.OK_CANCEL_OPTION);
//        if (result == JOptionPane.OK_OPTION) {
//
//            double overrideValue = toDouble(panel.textFields[0].getText());
//
//            // save the override values
//            Double[] consts = constants.getItem(sector, layer, component);
//            consts[LEFTRIGHT_OVERRIDE] = overrideValue;
//
//            fit(sector, layer, component);
//
//            // update the table
//            saveRow(sector, layer, component);
//            calib.fireTableDataChanged();
//
//        }
    }
    
    public void customFitCurrent(int sector, int layer, int component) {

		H1F lrHist = dataGroups.getItem(sector, layer, component).getH1F("time_offset_layer");
		F1D lrFit = dataGroups.getItem(sector, layer, component).getF1D("func");

		String[] fields = {"(current=" + getFitMinInBinNumber(lrHist,lrFit) + " bins)",
				"(current=" + getFitMaxInBinNumber(lrHist,lrFit) + " bins)",
				"(current="+getFitHeightRelativeToMaxBinContent(lrHist,lrFit)+" as fraction of max)"};

		CNDCustomFitPanelTimeOffsetsLR panel = new CNDCustomFitPanelTimeOffsetsLR(sector, layer, component, fields);

		int result = JOptionPane
				.showConfirmDialog(null, panel, "Adjust fit for sector " + sector + " layer " + layer + " component " + component,
						JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			//Check for individual pair fit limit change
			int minRange = toInt(panel.textFields[0].getText());
			int maxRange = toInt(panel.textFields[1].getText());

			//convert from bins to x-axis value...            
			double minRangeInX = lrHist.getxAxis().getBinCenter(minRange);
			double maxRangeInX = lrHist.getxAxis().getBinCenter(maxRange);   

			//Check for new limits for the histogram, only when a lower OR an upper have been provided
			if (minRange != 0.0 || maxRange != 0.0) {

				//Use previous fit limits if required
				if (minRange == 0) {
					minRangeInX = lrFit.getMin();
				}
				if (maxRange == 0) {
					maxRangeInX = lrFit.getMax();
				}



				lrFit.setRange(minRangeInX, maxRangeInX);

				// update the table
				saveRow(sector, layer, component);
			}

			double layerHeightParameter = toDouble(panel.textFields[3].getText());
			if (layerHeightParameter != 0.0){

				fitCentralRegionNEWCustomFitHeight(lrHist, lrFit, layerHeightParameter);
				// update the table
				saveRow(sector, layer, component);              
			}

			//Now check for fit limit changes for all components in layers
			double[] allLayerHeightParameters = new double[3];

			allLayerHeightParameters[0] = toDouble(panel.textFields[6].getText());
			allLayerHeightParameters[1] = toDouble(panel.textFields[5].getText());
			allLayerHeightParameters[2] = toDouble(panel.textFields[4].getText());

			for (int l = 1; l <= 3; l++) {
				int l_index = l - 1;
				if (allLayerHeightParameters[l_index] != 0.0){
					for (int s = 1; s <= 24; s++) {

						H1F hist = dataGroups.getItem(s, l, component).getH1F("time_offset_layer");
						F1D fit = dataGroups.getItem(s, l, component).getF1D("func");

						fitCentralRegionNEWCustomFitHeight(hist, fit, allLayerHeightParameters[l_index]);
						// update the table
						saveRow(s, l, component);

					}
				}
			}

			calib.fireTableDataChanged();

		}

	}
    
    public void fitCentralRegionNEWCustomFitHeight(H1F hist, F1D fit, double fitHeight) {
        
        

        if (hist.getEntries() != 0) {  //Need non empty histograms for this method...

            double distributionLeftLeadingEdge = LeadingEdge(hist, fitHeight);
            double distributionRightTrailingEdge = TrailingEdge(hist, fitHeight);

            double halfWidthOfDistribution = (distributionRightTrailingEdge - distributionLeftLeadingEdge) / 2.;
            double centreOfDistribution = distributionLeftLeadingEdge + halfWidthOfDistribution;
            double centralRegionWidth = CENTRAL_REGION_TO_LOOK_FOR_DIP * (distributionRightTrailingEdge - distributionLeftLeadingEdge);            

            double fitMin = distributionLeftLeadingEdge;
            		
//            		FitLimitOnLeftSideOfDistribution(hist, 
//                    centreOfDistribution,
//                    centralRegionWidth,
//                    fitHeight);
            
            double fitMax = distributionRightTrailingEdge;
            		
//            		FitLimitOnRightSideOfDistribution(hist, 
//                    centreOfDistribution,
//                    centralRegionWidth,
//                    fitHeight);                    

            fit.setRange(fitMin, fitMax);
            fit.setParameter(0, (hist.getBinContent(hist.getMaximumBin()))); // height to draw line at
			//fit.setParLimits(0, (hist.getBinContent(hist.getMaximumBin()))*0.9, (hist.getBinContent(hist.getMaximumBin()))*1.1);
			fit.setParameter(1, centreOfDistribution); // height to draw line at
			//fit.setParLimits(1,centreOfDistribution*0.9, centreOfDistribution*1.1);
			fit.setParameter(2, 1.3*halfWidthOfDistribution); // height to draw line at

			try {
				DataFitter.fit(fit,hist, "RQ");
				System.out.println("in adjust fit ");
			} catch (Exception e) {

				e.printStackTrace();
			}
          
        }

    }    

    
    
    int getFitMinInBinNumber(H1F hist, F1D fit) {

        int fitMin = 0;
        try {
            double min = fit.getMin();
            fitMin = hist.getXaxis().getBin(min);
        } catch (NullPointerException e) {
            fitMin = -200;
        }
        return fitMin;
    }
    
    int getFitMaxInBinNumber(H1F hist, F1D fit) {

        int fitMin = 0;
        try {
            double min = fit.getMax();
            fitMin = hist.getXaxis().getBin(min);
        } catch (NullPointerException e) {
            fitMin = -200;
        }
        return fitMin;
    }
    
    
    

    public double getFitHeightRelativeToMaxBinContent(H1F hist, F1D fit) {

        double fitHeightRelativeToMaxBinContent = 0;

        try {
            
            double fitHeight = fit.getParameter(0);
            fitHeightRelativeToMaxBinContent = fitHeight/hist.getBinContent(hist.getMaximumBin());
            
        } catch (NullPointerException e) {
            fitHeightRelativeToMaxBinContent = -200;
        }
        return fitHeightRelativeToMaxBinContent;
    }
    

    public Double getCentroid(int sector, int layer, int paddle) {

        double leftRight = 0.0;
        double overrideVal = constants.getItem(sector, layer, paddle)[LEFTRIGHT_OVERRIDE];

        if (overrideVal != UNDEFINED_OVERRIDE) {
            leftRight = overrideVal;
        } else {

            //double min = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc").getMin(); 
            //double max = dataGroups.getItem(sector,layer,paddle).getF1D("edgeToEdgeFunc").getMax();
            //leftRight = (min+max)/2.0;
            leftRight = dataGroups.getItem(sector, layer, paddle).getH1F("time_offset_LR").getMean();

        }

        return leftRight;
    }
    
    public Double getCentreOfFit(int sector, int layer, int component) {

        double centreOfFit = dataGroups.getItem(sector, layer, component).getF1D("func").getParameter(1);

//        double fitMin = dataGroups.getItem(sector, layer, component).getF1D("func").getMin();
//        double fitMax = dataGroups.getItem(sector, layer, component).getF1D("func").getMax();
//        
//        double middleOfFit = fitMax - fitMin;
//        double halfLengthOfFit = middleOfFit / 2.;
//
//        centreOfFit = fitMin + halfLengthOfFit;
//        
//        // Subtract value in x of the centre bin of the distribution:
//        centreOfFit = centreOfFit - CNDPaddlePair.NS_PER_CH;      

        return centreOfFit;
    }
    
    public Double getWidthFit(int sector, int layer, int component) {

        

        
        
        double middleOfFit = dataGroups.getItem(sector, layer, component).getF1D("func").getParameter(2);

        return middleOfFit;
    }


    @Override
    public void saveRow(int sector, int layer, int component) {
    	 System.out.println("in save row");
        calib.setDoubleValue(getCentreOfFit(sector,layer,component),
                "time_offset_layer", sector, layer, component);

        calib.setDoubleValue(getWidthFit(sector, layer, component),
                "time_offset_layer_err", sector, layer, component);
        System.out.println("out save row");
    }

    @Override
    public boolean isGoodPaddle(int sector, int layer, int component) {

//        return (getCentroid(sector, layer, paddle) >= -MAX_LEFTRIGHT
//                && getCentroid(sector, layer, paddle) <= MAX_LEFTRIGHT);
        //***Need this test for both paddles in the component!***
        //System.out.println("in LR isGoodPaddle for S" + sector + " L" + layer + " C" + component);
        return (true);
        
    }

    @Override
    public boolean isGoodComponent(int sector, int layer, int component) {

        //***Need this test for both paddles in the component!***
       // System.out.println("in LR isGoodComponent for S" + sector + " L" + layer + " C" + component);
        return (true);

    }

    @Override
    public void setPlotTitle(int sector, int layer, int component) {
        // reset hist title as may have been set to null by show all 
        dataGroups.getItem(sector, layer, component).getH1F("time_offset_layer").setTitle("Time offset layer - S" + sector + " L" + layer + " C" + component);
        dataGroups.getItem(sector, layer, component).getH1F("time_offset_layer").setTitleX("layer offset (ns)");
        dataGroups.getItem(sector, layer, component).getH1F("time_offset_layer").setTitleX("Counts");

    }

    @Override
    public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

        H1F hist = dataGroups.getItem(sector, layer, paddle).getH1F("time_offset_layer");
        canvas.draw(hist);
//        canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("lrFunc"), "same");

    }

    @Override
    public DataGroup getSummary(int sector, int layer) {

        int layer_index = layer - 1;
        double[] paddleNumbers = new double[NUM_PADDLES[layer_index]];
        double[] paddleUncs = new double[NUM_PADDLES[layer_index]];
        double[] values = new double[NUM_PADDLES[layer_index]];
        double[] valueUncs = new double[NUM_PADDLES[layer_index]];

        for (int p = 1; p <= NUM_PADDLES[layer_index]; p++) {

            paddleNumbers[p - 1] = (double) p;
            paddleUncs[p - 1] = 0.0;
            values[p - 1] = getCentroid(sector, layer, p);
            valueUncs[p - 1] = 0.0;
        }

        GraphErrors summ = new GraphErrors("summ", paddleNumbers,
                values, paddleUncs, valueUncs);

        summ.setTitleX("Paddle Number");
        summ.setTitleY("Centroid");
        summ.setMarkerSize(MARKER_SIZE);
        summ.setLineThickness(MARKER_LINE_WIDTH);

        DataGroup dg = new DataGroup(1, 1);
        dg.addDataSet(summ, 0);
        return dg;

    }
    
    @Override
    public DataGroup getSummary() {

        double[] sectorNumber = new double[24];
        double[] zeroUncs = new double[24];        
        double[] offsetL3L = new double[24];
        double[] offsetL3R = new double[24];
        double[] offsetL2L = new double[24];
        double[] offsetL2R = new double[24];
        double[] offsetL1L = new double[24];
        double[] offsetL1R = new double[24];


        for (int sector = 1; sector <= 24; sector++) {
            for (int layer = 1; layer <= 3; layer++) {
                int component = 1;

                sectorNumber[sector - 1] = (double) sector;
                zeroUncs[sector - 1] = 0.0;

                if (layer == 3){
                    offsetL3L[sector - 1] = getCentreOfFit(sector, layer, component);
                    offsetL3R[sector - 1] = getCentreOfFit(sector, layer, component);
                } else if (layer == 2){
                    offsetL2L[sector - 1] = getCentreOfFit(sector, layer, component);
                    offsetL2R[sector - 1] = getCentreOfFit(sector, layer, component);
                } else if (layer == 1){
                    offsetL1L[sector - 1] = getCentreOfFit(sector, layer, component);
                    offsetL1R[sector - 1] = getCentreOfFit(sector, layer, component);
                }
            }
        }

        GraphErrors allL3L = new GraphErrors("allL3L", sectorNumber,
                offsetL3L, zeroUncs, zeroUncs);

        allL3L.setTitleX("Sector Number");
        allL3L.setTitleY("time_offset_LR (ns)");
        allL3L.setMarkerSize(MARKER_SIZE);
        allL3L.setLineThickness(MARKER_LINE_WIDTH);

        GraphErrors allL3R = new GraphErrors("allL3R", sectorNumber,
                offsetL3R, zeroUncs, zeroUncs);

        allL3R.setTitleX("Sector Number");
        allL3R.setTitleY("time_offset_LR (ns)");
        allL3R.setMarkerSize(MARKER_SIZE);
        allL3R.setLineThickness(MARKER_LINE_WIDTH);
        
        GraphErrors allL2L = new GraphErrors("allL2L", sectorNumber,
                offsetL2L, zeroUncs, zeroUncs);

        allL2L.setTitleX("Sector Number");
        allL2L.setTitleY("time_offset_LR (ns)");
        allL2L.setMarkerSize(MARKER_SIZE);
        allL2L.setLineThickness(MARKER_LINE_WIDTH);

        GraphErrors allL2R = new GraphErrors("allL2R", sectorNumber,
                offsetL2R, zeroUncs, zeroUncs);

        allL2R.setTitleX("Sector Number");
        allL2R.setTitleY("time_offset_LR (ns)");
        allL2R.setMarkerSize(MARKER_SIZE);
        allL2R.setLineThickness(MARKER_LINE_WIDTH);
        
        GraphErrors allL1L = new GraphErrors("allL1L", sectorNumber,
                offsetL1L, zeroUncs, zeroUncs);

        allL1L.setTitleX("Sector Number");
        allL1L.setTitleY("time_offset_LR (ns)");
        allL1L.setMarkerSize(MARKER_SIZE);
        allL1L.setLineThickness(MARKER_LINE_WIDTH);

        GraphErrors allL1R = new GraphErrors("allL1R", sectorNumber,
                offsetL1R, zeroUncs, zeroUncs);

        allL1R.setTitleX("Sector Number");
        allL1R.setTitleY("time_offset_LR (ns)");
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

    public double LeadingEdge(H1F hist, double fitThreshold) {

        //*************************************
        //****Now find leading edge point
        int leading_bin = 0;
        double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);
        int binOfMaxOfDistribution = hist.getMaximumBin();

        for (int i = binOfMaxOfDistribution; i > 0; i--) {
            //****Find leading edge bin with the threshold height
            if (hist.getBinContent(i) < (heightThreshold) && hist.getBinContent(i + 1) >= (heightThreshold)) {
                leading_bin = i;
                break;
            }
        }

        return hist.getAxis().getBinCenter(leading_bin);

    }

    public double TrailingEdge(H1F hist, double fitThreshold) {

        //*************************************
        //****Now find trailing edge point
        int trailing_bin = 0;
        double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);
        int n_bins = hist.getXaxis().getNBins();
        int binOfMaxOfDistribution = hist.getMaximumBin();

        for (int i = binOfMaxOfDistribution ; i < n_bins; i++) {
            //****Find leading edge middle height bin
            //This test excludes the binning issues (assume one bin dips only)
            if (hist.getBinContent(i - 1) >= (heightThreshold) && hist.getBinContent(i) < (heightThreshold)) {
                trailing_bin = i;
                break;
            }
        }

        return hist.getAxis().getBinCenter(trailing_bin);
    }

//            int leadingFitBin = 0;        
//        for (int i = firstBin; i < lastBin; i++) {
////            System.out.println("histo.getBinContent(i) = " + histo.getBinContent(i) + " for i = " + i);
//            //****Find leading edge middle height bin
//            if (histo.getBinContent(i - 1) > (heightThreshold) && histo.getBinContent(i) <= (heightThreshold)) {
//                leadingFitBin = i-1;
//                break;
//            }
//        }
    
    
//        for (int i = lastBin; i > firstBin; i--) {
////            System.out.println("histo.getBinContent(i) = " + histo.getBinContent(i) + " for i = " + i);            
//            //****Find leading edge middle height bin
//            //This test excludes the binning issues (assume one bin dips only)
//            if (histo.getBinContent(i - 1) < (heightThreshold) && histo.getBinContent(i) >= (heightThreshold)) {
//                trailing_bin = i;
//                break;
//            }
//        }


    
    public double FitLimitOnLeftSideOfDistribution(H1F hist, double distributionCentre, double widthOfCentralRegion, double fitThreshold) {

        //*************************************
        //****Get all required info from histogram

        int firstBin = hist.getxAxis().getBin(distributionCentre);
        int lastBin = hist.getxAxis().getBin((distributionCentre - widthOfCentralRegion));
        double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);
        int leadingFitBin = 0;
        
        for (int i = firstBin; i >= lastBin; i--) {
            //****Find leading edge middle height bin
            if (hist.getBinContent(i) < heightThreshold && hist.getBinContent(i-1) >= heightThreshold) {
                leadingFitBin = i;
                break;
            }
        }

        return hist.getAxis().getBinCenter(leadingFitBin);

    }

    public double FitLimitOnRightSideOfDistribution(H1F hist, double distributionCentre, double widthOfCentralRegion, double fitThreshold) {

        //*************************************
        //****Get all required info from histogram
        int trailing_bin = 0;        
        int firstBin = hist.getxAxis().getBin(distributionCentre);
        int lastBin = hist.getxAxis().getBin((distributionCentre + widthOfCentralRegion));        
        double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);

        for (int i = firstBin; i <= lastBin; i++) {  
            
            //****Find leading edge middle height bin
            //This test excludes the binning issues (assume one bin dips only)
            if (hist.getBinContent(i) < heightThreshold && hist.getBinContent(i+1) >= heightThreshold) {
                trailing_bin = i;
                break;
            }
        }
        
        return hist.getAxis().getBinCenter(trailing_bin);

    }
    

    


}
