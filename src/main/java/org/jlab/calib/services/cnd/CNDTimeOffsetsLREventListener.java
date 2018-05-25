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

public class CNDTimeOffsetsLREventListener extends CNDCalibrationEngine {


	
	// option (would be good to add to the graphical interface)
	public final boolean softTrigger= false;

	//mode 
	//0 is for no field, 1 for field on
	public int mode;
	
	// hists
	public final int LEFT_RIGHT = 0;

	// indices for override values
	public final int LEFTRIGHT_OVERRIDE = 0;

	final double LEFT_RIGHT_RATIO = 0.15;
	//	final double MAX_LEFTRIGHT = 10.0;

	final double MAX_LEFTRIGHT = 4.0;
	final double MAX_LEFTRIGHT_ERR = 0.5;

	// 1 bin per 2 TDC channel
	public static int HIST_BINS = 400;
	static double HIST_X_MIN = -1 *2* ((double) (HIST_BINS) * (CNDPaddlePair.NS_PER_CH));
	static double HIST_X_MAX = +1 *2* ((double) (HIST_BINS) * (CNDPaddlePair.NS_PER_CH));   
//	    static int HIST_BINS = 100;
//	    static double HIST_X_MIN = -8;
//	    static double HIST_X_MAX = 8;

	//Height threshold for leading and trailing edge calculations
	public double FIT_HEIGHT_THRESHOLD = 0.3;

	//Height threshold for depletion zone search
	public double FIT_THRESHOLD = 0.5;

	// Fit region around the centre (in percentage of the whole distribution's width) to search for the dip
	public double CENTRAL_REGION_TO_LOOK_FOR_DIP = 0.25;

	public CNDTimeOffsetsLREventListener() {

		stepName = "TimeOffsets_LR";
		fileNamePrefix = "CND_CALIB_TIMEOFFSETS_LR";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"time_offset_LR/F:time_offset_LR_err/F");
		calib.setName("/calibration/cnd/TimeOffsets_LR");
		calib.setPrecision(3);
	}
	
	public void setConstraints() {
		calib.addConstraint(3, -MAX_LEFTRIGHT, MAX_LEFTRIGHT);
		calib.addConstraint(4, -MAX_LEFTRIGHT_ERR, MAX_LEFTRIGHT_ERR);
	}


//	@Override
//	public void populatePrevCalib() {
//		prevCalRead = true;
//	}        

	//    @Override
	//    public void populatePrevCalib() {
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

					leftRightValues.addEntry(sector, layer, component);
					leftRightValues.setDoubleValue(lr,
							"time_offset_LR", sector, layer, component);
					System.out.println("lr: " + lr);
					leftRightValues.setDoubleValue(lrErr,
							"time_offset_LR_err", sector, layer, component);

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
					leftRightValues.addEntry(sector, layer, component);
					leftRightValues.setDoubleValue(0.0,
							"time_offset_LR", sector, layer, component);
					leftRightValues.setDoubleValue(0.0,
							"time_offset_LR_err", sector, layer, component);

				}
			}
		} else if (calDBSource == CAL_DB) {
			System.out.println("Database Run No: " + prevCalRunNo);
			DatabaseConstantProvider dcp = new DatabaseConstantProvider(prevCalRunNo, "default");
			leftRightValues = dcp.readConstants("/calibration/cnd/TimeOffsets_LR");
			dcp.disconnect();
		}
		prevCalRead = true;
		System.out.println(stepName + " previous calibration values populated successfully");
	}

	public void resetEventListener() {

		mode=CNDCalibration.modeLR;
		if(CNDCalibration.bintimeLR!=0.0){
			HIST_BINS=CNDCalibration.bintimeLR;
		}
		if(CNDCalibration.mintimeLR!=0.0){
			HIST_X_MIN =CNDCalibration.mintimeLR;
		}
		if(CNDCalibration.maxtimeLR!=0.0){
			HIST_X_MAX =CNDCalibration.maxtimeLR;
		}

		//Eh?
		//		// perform init processing
		//		for (int i=0; i<leftRightValues.getRowCount(); i++) {
		//			String line = new String();
		//			for (int j=0; j<leftRightValues.getColumnCount(); j++) {
		//				line = line+leftRightValues.getValueAt(i, j);
		//				if (j<leftRightValues.getColumnCount()-1) {
		//					line = line+" ";
		//				}
		//			}
		//			//System.out.println(line);
		//		}
		// GM perform init processing
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				// create all the histograms
				H1F hist = new H1F("time_offset_LR",
						"Left-right time diff Sector " + sector + " Layer " + layer + " Component" + component,
						HIST_BINS, HIST_X_MIN, HIST_X_MAX);

				// create all the functions
				F1D lrFunc = new F1D("lrFunc", "[height]", -8.0, 8.0);
				lrFunc.setLineColor(FUNC_COLOUR);
				lrFunc.setLineWidth(FUNC_LINE_WIDTH);

				DataGroup dg = new DataGroup(1, 1);
				dg.addDataSet(hist, LEFT_RIGHT);
				dg.addDataSet(lrFunc, LEFT_RIGHT);

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
						}
					}
				}


				if((softTrigger && include) || (!softTrigger)){

					// Fill all histograms
					dataGroups.getItem(sector, layer, component).getH1F("time_offset_LR").fill(paddlePair.rightLeftTimeDiff());
					//System.out.println(paddlePair.TRACK_ID);
//					paddlePair.show();
					//                dataGroups.getItem(sector, layer, component).getH1F("time_offset_LR").fill(paddlePair.rightLeftTimeDiff());
				}
			}
		}
	}

	@Override
	public void fit(int sector, int layer, int component,
			double minRange, double maxRange) {

		// System.out.println("S L C = " + sector + " " + layer + " " + component);

		H1F lrHist = dataGroups.getItem(sector, layer, component).getH1F("time_offset_LR");

		if (lrHist.getEntries() != 0) {  //Need non empty histograms for this method...

			double distributionLeftLeadingEdge = LeadingEdge(lrHist, FIT_HEIGHT_THRESHOLD);
			double distributionRightTrailingEdge = TrailingEdge(lrHist, FIT_HEIGHT_THRESHOLD);

			double halfWidthOfDistribution = (distributionRightTrailingEdge - distributionLeftLeadingEdge) / 2.;
			double centreOfDistribution = distributionLeftLeadingEdge + halfWidthOfDistribution;


			double centralRegionWidth = CENTRAL_REGION_TO_LOOK_FOR_DIP * (distributionRightTrailingEdge - distributionLeftLeadingEdge);

			//this has to be used if want to fit the uturn deep which DOESN't exist in real data
			
			double fitMin;
			double fitMax;
			if(mode==0){
			fitMin = FitLimitOnLeftSideOfDistribution(lrHist,
					centreOfDistribution,
					centralRegionWidth,
					FIT_THRESHOLD);

			fitMax = FitLimitOnRightSideOfDistribution(lrHist,
					centreOfDistribution,
					centralRegionWidth,
					FIT_THRESHOLD);
			}
			
			else{
			fitMin = FitDistributionFieldLeft(lrHist,
					centreOfDistribution,
					centralRegionWidth,
					FIT_THRESHOLD);
			fitMax = FitDistributionFieldRight(lrHist,
					centreOfDistribution,
					centralRegionWidth,
					FIT_THRESHOLD);
			}
			
			F1D lrFunc = dataGroups.getItem(sector, layer, component).getF1D("lrFunc");
			lrFunc.setRange(fitMin, fitMax);

			lrFunc.setParameter(0, (lrHist.getBinContent(lrHist.getMaximumBin()) * FIT_THRESHOLD)); // height to draw line at

		}

	}

	@Override
	public void customFit(int sector, int layer, int component) {

		customFitCurrent(sector, layer, component);

		//        //System.out.println("Left right value from file is "+leftRightAdjustment(sector,layer,paddle));
		//        String[] fields = {"Override centroid:", "SPACE"};
		//        TOFCustomFitPanel panel = new TOFCustomFitPanel(fields, sector, layer);
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

		H1F lrHist = dataGroups.getItem(sector, layer, component).getH1F("time_offset_LR");
		F1D lrFit = dataGroups.getItem(sector, layer, component).getF1D("lrFunc");

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

						H1F hist = dataGroups.getItem(s, l, component).getH1F("time_offset_LR");
						F1D fit = dataGroups.getItem(s, l, component).getF1D("lrFunc");

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

			double distributionLeftLeadingEdge = LeadingEdge(hist, FIT_HEIGHT_THRESHOLD);
			double distributionRightTrailingEdge = TrailingEdge(hist, FIT_HEIGHT_THRESHOLD);

			double halfWidthOfDistribution = (distributionRightTrailingEdge - distributionLeftLeadingEdge) / 2.;
			double centreOfDistribution = distributionLeftLeadingEdge + halfWidthOfDistribution;
			double centralRegionWidth = CENTRAL_REGION_TO_LOOK_FOR_DIP * (distributionRightTrailingEdge - distributionLeftLeadingEdge);            


			double fitMin;
			double fitMax;
			if(mode==0){
			fitMin = FitLimitOnLeftSideOfDistribution(hist,
					centreOfDistribution,
					centralRegionWidth,
					fitHeight);

			fitMax = FitLimitOnRightSideOfDistribution(hist,
					centreOfDistribution,
					centralRegionWidth,
					fitHeight);
			}
			
			else{
				//System.out.println("here");
				fitMin = FitDistributionFieldLeft(hist,
						centreOfDistribution,
						centralRegionWidth,
						fitHeight);
				fitMax = FitDistributionFieldRight(hist,
						centreOfDistribution,
						centralRegionWidth,
						fitHeight);
			}   
			
			fit.setRange(fitMin, fitMax);
			fit.setParameter(0, hist.getBinContent(hist.getMaximumBin()) * fitHeight);

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

		double centreOfFit = 0.0;

		double fitMin = dataGroups.getItem(sector, layer, component).getF1D("lrFunc").getMin();
		double fitMax = dataGroups.getItem(sector, layer, component).getF1D("lrFunc").getMax();

		double middleOfFit = fitMax - fitMin;
		double halfLengthOfFit = middleOfFit / 2.;

		centreOfFit = fitMin + halfLengthOfFit;

		// Subtract value in x of the centre bin of the distribution:
		centreOfFit = centreOfFit - CNDPaddlePair.NS_PER_CH;      

		return centreOfFit;
	}


	@Override
	public void saveRow(int sector, int layer, int component) {

		calib.setDoubleValue(getCentreOfFit(sector,layer,component),
				"time_offset_LR", sector, layer, component);

		calib.setDoubleValue(1.0,
				"time_offset_LR_err", sector, layer, component);

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
		//System.out.println("in LR isGoodComponent for S" + sector + " L" + layer + " C" + component);
		return (true);

	}

	@Override
	public void setPlotTitle(int sector, int layer, int component) {
		// reset hist title as may have been set to null by show all 
		dataGroups.getItem(sector, layer, component).getH1F("time_offset_LR").setTitle("Left-right time diff - S" + sector + " L" + layer + " C" + component);
		dataGroups.getItem(sector, layer, component).getH1F("time_offset_LR").setTitleX("(timeLeft-timeRight) (ns)");
		dataGroups.getItem(sector, layer, component).getH1F("time_offset_LR").setTitleY("Counts");

	}

	@Override
	public void drawPlots(int sector, int layer, int paddle, EmbeddedCanvas canvas) {

		H1F hist = dataGroups.getItem(sector, layer, paddle).getH1F("time_offset_LR");
		canvas.draw(hist);
		canvas.draw(dataGroups.getItem(sector,layer,paddle).getF1D("lrFunc"), "same");

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
		double meanOfDistribution = hist.getMean();
		int binOfMeanOfDistribution = hist.getAxis().getBin(meanOfDistribution);

		for (int i = 0; i < binOfMeanOfDistribution; i++) {
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
		double meanOfDistribution = hist.getMean();
		int binOfMeanOfDistribution = hist.getAxis().getBin(meanOfDistribution);

		for (int i = n_bins; i > binOfMeanOfDistribution; i--) {
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
	
	public double FitDistributionFieldLeft(H1F hist, double distributionCentre, double widthOfCentralRegion, double fitThreshold) {

		//*************************************
		//****Get all required info from histogram

		int firstBin = hist.getxAxis().getBin(distributionCentre);
		int lastBin = hist.getxAxis().getBin((distributionCentre - widthOfCentralRegion));
		double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);
		int leadingFitBin = 0;

		for (int i = firstBin; i >= lastBin; i--) {
			//****Find leading edge middle height bin
			if (hist.getBinContent(i) > heightThreshold && hist.getBinContent(i-2) <= heightThreshold) {
				leadingFitBin = i;
				break;
			}
		}

		return hist.getAxis().getBinCenter(leadingFitBin);

	}

	public double FitDistributionFieldRight(H1F hist, double distributionCentre, double widthOfCentralRegion, double fitThreshold) {

		//*************************************
		//****Get all required info from histogram
		int trailing_bin = 0;        
		int firstBin = hist.getxAxis().getBin(distributionCentre);
		int lastBin = hist.getxAxis().getBin((distributionCentre + widthOfCentralRegion));        
		double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);

		for (int i = firstBin; i <= lastBin; i++) {  

			//****Find leading edge middle height bin
			//This test excludes the binning issues (assume one bin dips only)
			if (hist.getBinContent(i) > heightThreshold && hist.getBinContent(i+2) <= heightThreshold) {
				trailing_bin = i;
				break;
			}
		}

		return hist.getAxis().getBinCenter(trailing_bin);

	}





}
