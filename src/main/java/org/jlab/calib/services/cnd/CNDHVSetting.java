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
import org.jlab.groot.fitter.ParallelSliceFitter;
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
 * @author  Pierre Chatagnon
 */

// the trigger is implemented in this class, should be translated to data provoder or paddlePair

public class CNDHVSetting extends CNDCalibrationEngine {

	// option (would be good to add to the graphical interface)
	public final boolean softTrigger= false;

	//range of acceptable values for adc indirect peak
	private final double MINALLOWED = 0 ;
	private final double MAXALLOWED = 3000;

	//minimum number of event to strat fitting
	private final int MINEVENTFIT =1000; 

	// fit option
	private String fitOption="";
	private final double FIT_MIN = 200 ;
	private final double FIT_MAX = 9000;
	private final double fitThreshold = 0.5;
	//test vars
	public static int NUM_PROCESSED = 0;

	// hists
	public final int ADCL = 0;
	public final int ADCR = 1;

	// consts
	public final int LR_CENTROID = 0;
	public final int LR_ERROR = 1;
	public final int GEOMEAN_OVERRIDE = 2;
	public final int GEOMEAN_UNC_OVERRIDE = 3;
	public final int LOGRATIO_OVERRIDE = 4;
	public final int LOGRATIO_UNC_OVERRIDE = 5;	
	public final static double UNDEFINED_OVERRIDE = Double.NEGATIVE_INFINITY;

	// Gavin's calibration values
	private final int ADC_HIST_BINS = 200;
	private final int[] ADC_HIST_X_RANGE = {CNDPaddlePair.unallowedADCValue + 1, 10000};

	public H1F statHist;
	private String showPlotType = "ADCL";

	public CNDHVSetting() {

		stepName = "HVSetting";
		fileNamePrefix = "CND_HV_";
		// get file name here so that each timer update overwrites it
		filename = nextFileName();

		calib = new CalibrationConstants(3,
				"IndirectPeakValue/F");
		calib.setName("/calibration/cnd/HVSetting");
		calib.setPrecision(5);
	}

	@Override
	public void populatePrevCalib() {
		prevCalRead = true;
	}
	@Override
	public void resetEventListener() {

		// create histogram of stats per layer / sector
		statHist = new H1F("statHist","statHist", 30,0.0,30.0);
		statHist.setTitle("Total number of hits");
		statHist.getXaxis().setTitle("Sector");
		statHist.getYaxis().setTitle("Number of hits");

		// GM perform init processing
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				// create all the histograms
				TOFH1F adcLHist = new TOFH1F("adcL",
						"ADCL Sector " + sector + " Layer " + layer + " Component" + component,
						ADC_HIST_BINS, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1]);
				TOFH1F adcRHist = new TOFH1F("adcR",
						"ADCR Sector " + sector + " Layer " + layer + " Component" + component,
						ADC_HIST_BINS, ADC_HIST_X_RANGE[0], ADC_HIST_X_RANGE[1]);
				F1D PeakIndirectL = new F1D("PeakIndirectL", "[a]*landau(x,[m],[s])", 0, 10000);
				F1D PeakIndirectR = new F1D("PeakIndirectR", "[a]*landau(x,[m],[s])", 0, 10000);

				PeakIndirectL.setLineColor(2);
				PeakIndirectR.setLineColor(2);
				PeakIndirectL.setLineWidth(2);
				PeakIndirectR.setLineWidth(2);


				DataGroup dg = new DataGroup(2,1);
				dg.addDataSet(adcLHist, 0);
				dg.addDataSet(adcRHist, 1);	
				dg.addDataSet(PeakIndirectL, 0);
				dg.addDataSet(PeakIndirectR, 1);


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

		for (CNDPaddlePair paddlePair : paddlePairList) {



			NUM_PROCESSED++;

			//                        System.out.println("In Status process paddle pair list loop");

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

						//System.out.println(" layer "+layer+"  "+paddlePairList.get(j).getDescriptor().getLayer());
						include=true;
					}
				}
				//System.out.println("sector "+sector+" layer "+layer+" include "+include);
				//////////////// 
				// end of trigger
				////////////////

				///////////////
				// added software trigger : require a hit in at least two layers of a given sector
				///////////////

				//				for (int j= 0; j < paddlePairList.size(); j++) {
				//					for (int k= 0; j < paddlePairList.size(); k++) {
				//						if(paddlePairList.get(j).getDescriptor().getSector()==sector && 
				//								paddlePairList.get(k).getDescriptor().getSector()==sector){
				//
				//							int layerj=paddlePairList.get(j).getDescriptor().getLayer();
				//							int layerk=paddlePairList.get(k).getDescriptor().getLayer();
				//							
				//							if(layer+layerj+layerk==6){
				//								include=true;
				//								System.out.println("layer "+layer+" layerj "+layerj+" layerk "+layerk);
				//							}
				//							}
				//						}
				//					}
				//System.out.println("sector "+sector+" layer "+layer+" include "+include);
				//////////////// 
				// end of trigger
				////////////////
			}


			if((softTrigger && include) || (!softTrigger)){

				// only select the indirect adc (ask for the larger tdc)
				if (paddlePair.ADCL > CNDPaddlePair.unallowedADCValue && paddlePair.COMP==1){
					dataGroups.getItem(sector,layer,component).getH1F("adcL").fill(paddlePair.ADCL);
					//                            System.out.println("paddlePair.ADCL = " + paddlePair.ADCL);
				}
				if (paddlePair.ADCR > CNDPaddlePair.unallowedADCValue && paddlePair.COMP==2){
					dataGroups.getItem(sector,layer,component).getH1F("adcR").fill(paddlePair.ADCR);
					//                            System.out.println("paddlePair.ADCR = " + paddlePair.ADCR);                            
				}
			}    

		}
	}



	@Override
	public void fit(int sector, int layer, int component,
			double minRange, double maxRange){

		System.out.println("IN HV FIT for S" + sector + " L" + layer + " C" + component);

		// find the range for the fit
		double lowLimit = FIT_MIN;
		double highLimit = FIT_MAX;
		int lowLimitBin = 0;

		///////
		// LEFT 
		///////


		H1F histL = dataGroups.getItem(sector, layer, 1).getH1F("adcL");

		
		
//		if(minRange!=UNDEFINED_OVERRIDE && maxRange!=UNDEFINED_OVERRIDE){
//			lowLimit = minRange;
//			highLimit = maxRange;
//		}
		
		lowLimit=LeadingEdge(histL, fitThreshold);
		highLimit=TrailingEdge(histL, fitThreshold);
		
		F1D funcL = dataGroups.getItem(sector, layer, component).getF1D("PeakIndirectL");

		funcL.setRange(lowLimit, highLimit);

		System.out.println(" low limit L "+lowLimit+" high limit L "+highLimit);
		if(histL.getEntries()>MINEVENTFIT){
			//System.out.println("in fit L");
			int peakBin = histL.getMaximumBin(); // bin number of maximum (ie indirect adc peak)
			double peak = histL.getAxis().getBinCenter(peakBin); // value of the center of the bin

			funcL.setParameter(0, histL.getBinContent(peakBin)); //amplitude
			funcL.setParameter(1, peak);  //mean estimate
			funcL.setParameter(2, 1000.);  //width estimate



			// do the actual fit with a Landau function
			try {
				DataFitter.fit(funcL, histL, fitOption);
				System.out.println(" in fit ");
			} catch (Exception e) {
				System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + "left");
				e.printStackTrace();
			}

			//System.out.println("yeaaaah I'm there ");
		}

		///////
		// RIGHT
		///////


		H1F histR = dataGroups.getItem(sector, layer, 1).getH1F("adcR");  

		lowLimit=LeadingEdge(histR, fitThreshold);
		highLimit=TrailingEdge(histR, fitThreshold);
		
		F1D funcR = dataGroups.getItem(sector, layer, component).getF1D("PeakIndirectR");
		funcR.setRange(lowLimit, highLimit);

		//System.out.println(" out fit R");
		if(histR.getEntries()>MINEVENTFIT){
			//System.out.println("in fit R");
			int peakBin = histR.getMaximumBin(); // bin number of maximum (ie indirect adc peak)
			double peak = histR.getAxis().getBinCenter(peakBin); // value of the center of the bin

			funcR.setParameter(0, histR.getBinContent(peakBin)); //amplitude
			funcR.setParameter(1, peak);  //mean estimate
			funcR.setParameter(2, 1.);  //with estimate



			// do the actual fit with a Landau function
			try {
				DataFitter.fit(funcR, histR, fitOption);
			} catch (Exception e) {
				System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + "right");
				e.printStackTrace();
			}
		}
	}

	public void adjustFit(int sector, int layer, int component,
			double minRange, double maxRange) {

		//        System.out.println("IN adjustFit FIT for S" + sector + " L" + layer + " C" + component);

		F1D func = null;
		H1F graph = null;

		if (component == 1){
			func = dataGroups.getItem(sector, layer, 1).getF1D("PeakIndirectL");
			graph = dataGroups.getItem(sector, layer, 1).getH1F("adcL");
		} else if (component == 2){
			func = dataGroups.getItem(sector, layer, 1).getF1D("PeakIndirectR");
			graph =  dataGroups.getItem(sector, layer, 1).getH1F("adcR");
		}


		func.setRange(minRange, maxRange);

		try {
			DataFitter.fit(func, graph, fitOption);
		} catch (Exception e) {
			System.out.println("Fit error with sector " + sector + " layer " + layer + " component " + component);
			e.printStackTrace();
		}        

		// update the table
		saveRow(sector,layer,1);		
		calib.fireTableDataChanged();        

	}

	@Override
	public void customFit(int sector, int layer, int paddle){

		F1D funcL = dataGroups.getItem(sector, layer, 1).getF1D("PeakIndirectL");
		H1F graphL = dataGroups.getItem(sector, layer, 1).getH1F("adcL");
		F1D funcR = dataGroups.getItem(sector, layer, 1).getF1D("PeakIndirectR");
		H1F graphR = dataGroups.getItem(sector, layer, 1).getH1F("adcR");
		
		
		String[] fields = {"Min range for fit Left:", "Max range for fit Left:", "SPACE",
				"Min range for fit Right:", "Max range for fit Right:"};
		CNDCustomFitPanel panel = new CNDCustomFitPanel(fields,sector,layer);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRangeL = toInt(panel.textFields[0].getText());
			double maxRangeL = toInt(panel.textFields[1].getText());
			double minRangeR = toInt(panel.textFields[2].getText());
			double maxRangeR = toInt(panel.textFields[3].getText());

			if (minRangeL != 0.0 || maxRangeL != 0.0 || minRangeR != 0.0 || maxRangeR != 0.0) {

				//Use previous fit limits if required
				if (minRangeL == 0) {
					minRangeL = funcL.getMin();
				}
				if (maxRangeL == 0) {
					maxRangeL = funcL.getMax();
				}
				if (minRangeR == 0) {
					minRangeR = funcR.getMin();
				}
				if (maxRangeR == 0) {
					maxRangeR = funcR.getMax();
				}
			}
			
			System.out.println(minRangeL);
			System.out.println(maxRangeL);
			System.out.println(minRangeR);
			System.out.println(maxRangeR);

			// save the override values					
			adjustFit(sector, layer, 1, minRangeL, maxRangeL);
			adjustFit(sector, layer, 2, minRangeR, maxRangeR);
			// update the table
			saveRow(sector,layer,1);

			calib.fireTableDataChanged();

		} 

	}

	public double getComponentIndirectPeak(int sector, int layer, int component, int side) {

		double IndirectPeakADCValue=0;

		if(side==1){
			IndirectPeakADCValue=dataGroups.getItem(sector, layer, component).getF1D("PeakIndirectL").getParameter(1);
		}

		if(side==2){
			IndirectPeakADCValue=dataGroups.getItem(sector, layer, component).getF1D("PeakIndirectR").getParameter(1);
		}

		return IndirectPeakADCValue;

	}        


	@Override
	public void saveRow(int sector, int layer, int component) {
		// System.out.println("Now in CNDStatusEventListener.saveRow()");

		calib.setDoubleValue(getComponentIndirectPeak(sector, layer, component, 1),                        
				"IndirectPeakValue", sector, layer, 1);
		calib.setDoubleValue(getComponentIndirectPeak(sector, layer, component, 2),
				"IndirectPeakValue", sector, layer, 2);

	}

	@Override
	public void save() {

		System.out.println("from CNDCalibrationEngine.save() HV setting...");
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
					
					calib.addEntry(sector, layer, 1);
					calib.addEntry(sector, layer, 2);
					
					saveRow(sector,layer,1);
					
					//saveRow(sector, layer, component);

				}
			}				
		this.writeFile(filename);

	}

	@Override
	public DataGroup getSummary() {

		double[] sectorNumber = new double[24];
		double[] zeroUncs = new double[24];        
		double[] IndirectPeakL3L = new double[24];
		double[] IndirectPeakL3R = new double[24];
		double[] IndirectPeakL2L = new double[24];
		double[] IndirectPeakL2R = new double[24];
		double[] IndirectPeakL1L = new double[24];
		double[] IndirectPeakL1R = new double[24];

		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;

				sectorNumber[sector - 1] = (double) sector;
				zeroUncs[sector - 1] = 0.0;

				if (layer == 3){
					IndirectPeakL3L[sector - 1] = getComponentIndirectPeak(sector, layer, component, 1);
					IndirectPeakL3R[sector - 1] = getComponentIndirectPeak(sector, layer, component, 2);
				} else if (layer == 2){
					IndirectPeakL2L[sector - 1] = getComponentIndirectPeak(sector, layer, component, 1);
					IndirectPeakL2R[sector - 1] = getComponentIndirectPeak(sector, layer, component, 2);
				} else if (layer == 1){
					IndirectPeakL1L[sector - 1] = getComponentIndirectPeak(sector, layer, component, 1);
					IndirectPeakL1R[sector - 1] = getComponentIndirectPeak(sector, layer, component, 2);
				}

			}
		}

		GraphErrors allL3L = new GraphErrors("allL3L", sectorNumber,
				IndirectPeakL3L, zeroUncs, zeroUncs);

		allL3L.setTitleX("Sector Number");
		allL3L.setTitleY("IndirectPeak");
		allL3L.setMarkerSize(MARKER_SIZE);
		allL3L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL3R = new GraphErrors("allL3R", sectorNumber,
				IndirectPeakL3R, zeroUncs, zeroUncs);

		allL3R.setTitleX("Sector Number");
		allL3R.setTitleY("IndirectPeak");
		allL3R.setMarkerSize(MARKER_SIZE);
		allL3R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL2L = new GraphErrors("allL2L", sectorNumber,
				IndirectPeakL2L, zeroUncs, zeroUncs);

		allL2L.setTitleX("Sector Number");
		allL2L.setTitleY("IndirectPeak");
		allL2L.setMarkerSize(MARKER_SIZE);
		allL2L.setLineThickness(MARKER_LINE_WIDTH);     

		GraphErrors allL2R = new GraphErrors("allL2R", sectorNumber,
				IndirectPeakL2R, zeroUncs, zeroUncs);

		allL2R.setTitleX("Sector Number");
		allL2R.setTitleY("IndirectPeak");
		allL2R.setMarkerSize(MARKER_SIZE);
		allL2R.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1L = new GraphErrors("allL1L", sectorNumber,
				IndirectPeakL1L, zeroUncs, zeroUncs);

		allL1L.setTitleX("Sector Number");
		allL1L.setTitleY("IndirectPeak");
		allL1L.setMarkerSize(MARKER_SIZE);
		allL1L.setLineThickness(MARKER_LINE_WIDTH);

		GraphErrors allL1R = new GraphErrors("allL1R", sectorNumber,
				IndirectPeakL1R, zeroUncs, zeroUncs);

		allL1R.setTitleX("Sector Number");
		allL1R.setTitleY("IndirectPeak");
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
	public boolean isGoodComponent(int sector, int layer, int component) {	

		return true;

	}

	@Override
	public void setPlotTitle(int sector, int layer, int component) {
		dataGroups.getItem(sector,layer,component).getH1F("adcL").setTitle("ADCL - S"+sector+" L"+layer+" C"+1);
		dataGroups.getItem(sector,layer,component).getH1F("adcR").setTitle("ADCR - S"+sector+" L"+layer+" C"+2);               
		dataGroups.getItem(sector,layer,component).getH1F("adcL").setTitleX("ADC (channels)");
		dataGroups.getItem(sector,layer,component).getH1F("adcR").setTitleX("ADC (channels)");
		dataGroups.getItem(sector,layer,component).getH1F("adcL").setTitleY("Counts");
		dataGroups.getItem(sector,layer,component).getH1F("adcR").setTitleY("Counts");
	}

	@Override
	public void drawPlots(int sector, int layer, int component, EmbeddedCanvas canvas) {

		System.out.println("CNDHVSetting.drawPlots() for S L C "+sector+" "+layer+" "+component);

		if (showPlotType == "ADCL") {

			H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("adcL");
			canvas.draw(fitHist);

		} else if (showPlotType == "ADCR") {

			H1F fitHist = dataGroups.getItem(sector,layer,component).getH1F("adcR");
			canvas.draw(fitHist);                    
		}

	}

	@Override
	public void showPlots(int sector, int layer) {

		System.out.println("In showPlots()");

		showPlotType = "ADCL";
		stepName = "HV - ADCL";
		super.showPlots(sector, layer);
		showPlotType = "ADCR";
		stepName = "HV - ADCR";
		super.showPlots(sector, layer);                		              
	}


public double LeadingEdge(H1F hist, double fitThreshold) {

	//*************************************
	//****Now find leading edge point
	int leading_bin = 0;
	double heightThreshold = (hist.getBinContent(hist.getMaximumBin()) * fitThreshold);
	int binOfMaxOfDistribution = hist.getAxis().getBin(hist.getMaximumBin());

	for (int i = 0; i < binOfMaxOfDistribution; i++) {
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
	int binOfMaxOfDistribution = hist.getAxis().getBin(hist.getMaximumBin());

	for (int i = n_bins; i > binOfMaxOfDistribution; i--) {
		//****Find leading edge middle height bin
		//This test excludes the binning issues (assume one bin dips only)
		if (hist.getBinContent(i - 1) >= (heightThreshold) && hist.getBinContent(i) < (heightThreshold)) {
			trailing_bin = i;
			break;
		}
	}

	return hist.getAxis().getBinCenter(trailing_bin);
}


}
