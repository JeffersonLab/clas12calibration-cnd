package org.jlab.calib.services.cnd;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.jlab.detector.calib.tasks.CalibrationEngine;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataEventType;
import org.jlab.utils.groups.IndexedList;

/**
* CND Calibration suite
* Based on the work of Louise Clark thanks!
*
* @author  Gavin Murdoch
*/

public class CNDCalibrationEngine extends CalibrationEngine {

	public final static int[] NUM_PADDLES = { 23, 62, 5 };
	public final static int 	NUM_LAYERS = 3;
	public final static String[] LAYER_NAME = { "FTOF1A", "FTOF1B", "FTOF2" };
	public final static double UNDEFINED_OVERRIDE = Double.NEGATIVE_INFINITY;

	// plot settings
	public final static int		FUNC_COLOUR = 2;
	public final static int		MARKER_SIZE = 3;
	public final static int		FUNC_LINE_WIDTH = 2;
	public final static int		MARKER_LINE_WIDTH = 1;

	// Run constants
	public final static double BEAM_BUCKET = 4.; // 2.0 for simulations, 2.004 for real data

	public IndexedList<Double[]> constants = new IndexedList<Double[]>(3);

	public CalibrationConstants calib;
	public IndexedList<DataGroup> dataGroups = new IndexedList<DataGroup>(3);

	public String stepName = "Unknown";
	public String fileNamePrefix = "Unknown";
	public String filename = "Unknown.txt";

	// configuration
	public int calDBSource = 0;
	public static final int CAL_DEFAULT = 0;
	public static final int CAL_FILE = 1;
	public static final int CAL_DB = 2;
	public String prevCalFilename;
	public int prevCalRunNo;
	public boolean prevCalRead = false;
	public boolean engineOn = true;
	
	public int fitMethod = 0; //  0=SLICES 1=MAX 2=PROFILE	
	public String fitMode = "L";
	public int fitMinEvents = 0;
	public double maxGraphError = 0.1;
	public double fitSliceMaxError = 0.3;
	public static int FULL_COUNTER = 0;
	public static int CENTRE_SECTION = 1;
	public static int LEFT_SECTION = 2;
	public static int RIGHT_SECTION = 3;
	public int counterSection = FULL_COUNTER;
	public double sectionWidth = 20.0;
	//public double[] dummyPoint = {0.0}; // dummy point for graph to prevent canvas throwing error when drawing dataGroup

	// Values from previous calibration
	// Need to be static as used by all engines
	public static CalibrationConstants statusValues;
	public static CalibrationConstants convValues;
	public static CalibrationConstants leftRightValues;
	public static CalibrationConstants veffValues;
	public static CalibrationConstants uturnTlossValues;
	public static CalibrationConstants attenuationValues;
	public static CalibrationConstants layerValues;
	public static CalibrationConstants rfOffsetValues;
        public static CalibrationConstants MIPValues;
    	public static CalibrationConstants HVValues;


	// Calculated counter status values
	public static IndexedList<Integer> adcLeftStatus = new IndexedList<Integer>(3);
	public static IndexedList<Integer> adcRightStatus = new IndexedList<Integer>(3);
	public static IndexedList<Integer> tdcLeftStatus = new IndexedList<Integer>(3);
	public static IndexedList<Integer> tdcRightStatus = new IndexedList<Integer>(3);

	public CNDCalibrationEngine() {
		// controlled by calibration step class
		CNDPaddlePair.cnd = "CND";
                statusValues = new CalibrationConstants(3,
				"status_L/I:status_R/I");
		convValues = new CalibrationConstants(3,
                                "slope_L/F:offset_L/F:slope_R/F:offset_R/F");
                leftRightValues = new CalibrationConstants(3,
				"time_offset_LR/F:time_offset_LR_err/F");
		veffValues = new CalibrationConstants(3,
				"veff_L/F:veff_L_err/F:veff_R/F:veff_R_err/F");
		uturnTlossValues = new CalibrationConstants(3,
				"uturn_tloss/F:uturn_tloss_err/F");
		attenuationValues = new CalibrationConstants(3,
				"attlen_L/F:attlen_L_err/F:attlen_R/F:attlen_R_err/F");
		layerValues = new CalibrationConstants(3,
				"time_offset_layer/F:time_offset_layer_err/F");
                MIPValues = new CalibrationConstants(3,
				"mip_dir_L/F:mip_dir_L_err/F:mip_indir_L/F:mip_indir_L_err/F:mip_dir_R/F:mip_dir_R_err/F:mip_indir_R/F:mip_indir_R_err/F");
//                HVValues = new CalibrationConstants(3,
//        				"Indirect_peak_left/F:Indirect_peak_right/F");
                HVValues = new CalibrationConstants(3,
        				"Indirect_peak/F");
//		timeWalkValues = new CalibrationConstants(3,
//				"tw0_left/F:tw1_left/F:tw0_right/F:tw1_right/F");
//		p2pValues =	new CalibrationConstants(3,
//						"paddle2paddle/F");
		rfOffsetValues = new CalibrationConstants(3,
				"time_offset_RF_L/F:time_offset_RF_R/F");
                
//    String[] dirs = {"/calibration/cnd/Status_LR",
//                     "/calibration/cnd/TDC_conv",
//                     "/calibration/cnd/TimeOffsets_LR",
//                     "/calibration/cnd/EffV",                     
//                     "/calibration/cnd/UturnTloss",
//                     "/calibration/cnd/Attenuation",
//                     "/calibration/cnd/TimeOffsets_layer",                
                

	}
	
	public void populatePrevCalib() {
		// overridden in calibration step classes
	}
	
	@Override
	public void dataEventAction(DataEvent event) {

		if (event.getType() == DataEventType.EVENT_START) {
			resetEventListener();
			processEvent(event);
		} else if (event.getType() == DataEventType.EVENT_ACCUMULATE) {
			processEvent(event);
		} else if (event.getType() == DataEventType.EVENT_STOP) {
			System.out.println("EVENT_STOP");
			analyze();
		}
	}

	public void processPaddlePairList(List<CNDPaddlePair> paddleList) {
		// overridden in calibration step classes
	}

	@Override
	public void timerUpdate() {
		//analyze();
	}

	public void processEvent(DataEvent event) {
		// overridden in calibration step classes

	}

	public void analyze() {

            
		System.out.println(stepName+" analyze");
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
				int component = 1;
                                    fit(sector, layer, component);
			}
		}
                System.out.println("from CNDCalibrationEngine.analyze()...");
		save();
//		saveCounterStatus();
		calib.fireTableDataChanged();
	}

	public void fit(int sector, int layer, int paddle) {
		// fit to default range
		fit(sector, layer, paddle, UNDEFINED_OVERRIDE, UNDEFINED_OVERRIDE);
	}

	public void fit(int sector, int layer, int paddle, double minRange,
			double maxRange) {
		// overridden in calibration step class
	}

	public void saveRow(int sector, int layer, int paddle) {
		// overridden in calibration step class
	}

//	public void saveCounterStatus() {
//
//                System.out.println("in CNDCalibrationEngine.saveCounterStatus()...");
//		for (int sector = 1; sector <= 24; sector++) {
//			for (int layer = 1; layer <= 3; layer++) {
//				int component = 1;
//
//                                int adcLStat = adcLeftStatus.getItem(sector, layer, component);
//                                int adcRStat = adcRightStatus.getItem(sector, layer, component);
//                                int tdcLStat = tdcLeftStatus.getItem(sector, layer, component);
//                                int tdcRStat = tdcRightStatus.getItem(sector, layer, component);
//                                int componentStatusLeft = 10;
//                                int componentStatusRight = 10;
//
//                                if (adcLStat == 0 && tdcLStat == 0) {
//                                    componentStatusLeft = 0;
//                                } else if (adcLStat == 1 && tdcLStat == 0) {
//                                    componentStatusLeft = 1;
//                                } else if (adcLStat == 0 && tdcLStat == 1) {
//                                    componentStatusLeft = 2;
//                                } else if (adcLStat == 1 && tdcLStat == 1) {
//                                    componentStatusLeft = 3;
//                                } else {
//                                    componentStatusLeft = 5;
//                                }
//                               
//                                if (adcRStat == 0 && tdcRStat == 0) {
//                                    componentStatusRight = 0;
//                                } else if (adcRStat == 1 && tdcRStat == 0) {
//                                    componentStatusRight = 1;
//                                } else if (adcRStat == 0 && tdcRStat == 1) {
//                                    componentStatusRight = 2;
//                                } else if (adcRStat == 1 && tdcRStat == 1) {
//                                    componentStatusRight = 3;
//                                } else {
//                                    componentStatusRight = 5;
//                                }                                
//
//                                System.out.println(
//                                        sector + " "
//                                        + layer + " "
//                                        + component + " "
//                                        + componentStatusLeft + " "
//                                        + componentStatusRight + " ");
//
//			}
//		}
//	}

	public void save() {

            System.out.println("from CNDCalibrationEngine.save()...");
		for (int sector = 1; sector <= 24; sector++) {
			for (int layer = 1; layer <= 3; layer++) {
                            int component = 1;

                            
                            calib.addEntry(sector, layer, component);
                           
                            saveRow(sector, layer, component);
                         
			}
		}
		//calib.save(filename);
		// current CalibrationConstants object does not write file in correct format
		// use local method for the moment
		
		//this.writeFile(filename);
		
	}

	public void writeFile(String filename) {

		try { 

			// Open the output file
			File outputFile = new File(filename);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);

			for (int i=0; i<calib.getRowCount(); i++) {
				String line = new String();
				for (int j=0; j<calib.getColumnCount(); j++) {
					line = line+calib.getValueAt(i, j);
					if (j<calib.getColumnCount()-1) {
						line = line+" ";
					}
				}
				outputBw.write(line);
				outputBw.newLine();
			}

			outputBw.close();
		}
		catch(IOException ex) {
			System.out.println(
					"Error reading file '" 
							+ filename + "'");                   
			// Or we could just do this: 
			ex.printStackTrace();
		}

	}
	
	public String nextFileName() {

		// Get the next file name
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String todayString = dateFormat.format(today);
		String filePrefix = fileNamePrefix + todayString;
		int newFileNum = 0;

		File dir = new File(".");
		File[] filesList = dir.listFiles();

		for (File file : filesList) {
			if (file.isFile()) {
				String fileName = file.getName();
				if (fileName.matches(filePrefix + "[.]\\d+[.]txt")) {
					String fileNumString = fileName.substring(
							fileName.indexOf('.') + 1,
							fileName.lastIndexOf('.'));
					int fileNum = Integer.parseInt(fileNumString);
					if (fileNum >= newFileNum)
						newFileNum = fileNum + 1;

				}
			}
		}

		return filePrefix + "." + newFileNum + ".txt";
	}

	public void customFit(int sector, int layer, int paddle) {
		// overridden in calibration step class
	}

	@Override
	public List<CalibrationConstants> getCalibrationConstants() {
		return Arrays.asList(calib);
	}

	@Override
	public IndexedList<DataGroup> getDataGroup() {
		return dataGroups;
	}
	
	public int getH1FEntries(H1F hist) {
		int n = 0;
		
		for (int i=0; i<hist.getxAxis().getNBins(); i++) {
			n = (int) (n+hist.getBinContent(i));
		}
		return n;
	}
	
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
	
	public GraphErrors fixGraph(GraphErrors graphIn, String graphName) {

		int n = graphIn.getDataSize(0);
		int m = 0;
		for (int i=0; i<n; i++) {
			if (graphIn.getDataEY(i) < fitSliceMaxError) {
				m++;
			}
		}		
		
		double[] x = new double[m];
		double[] xerr = new double[m];
		double[] y = new double[m];
		double[] yerr = new double[m];
		int j = 0;
		
		for (int i=0; i<n; i++) {
			if (graphIn.getDataEY(i) < fitSliceMaxError) {
				x[j] = graphIn.getDataX(i);
				xerr[j] = graphIn.getDataEX(i);
				y[j] = graphIn.getDataY(i);
				yerr[j] = graphIn.getDataEY(i);
				j++;
			}
		}
		
		GraphErrors fixGraph = new GraphErrors(graphName, x, y, xerr, yerr);
		fixGraph.setName(graphName);
		
		return fixGraph;
		
	}		

	public boolean isGoodPaddle(int sector, int layer, int paddle) {
		// Overridden in calibration step class
		return true;
	}
        
	public boolean isGoodComponent(int sector, int layer, int component) {
		// Overridden in calibration step class
		return true;
	}
        

	public DataGroup getSummary(int sector, int layer) {
		// Overridden in calibration step class
                System.out.println("+++++Now in CNDCalibrationEngine.getSummary()+++++");
		DataGroup dg = null;
		return dg;
	}
        
        // GM:
	public DataGroup getSummary() {
		// Overridden in calibration step class
                System.out.println("+++++Now in CNDCalibrationEngine.getSummary()+++++");
		DataGroup dg = null;
		return dg;
	}

	public double paddleLength(int layer) {

		double len = 0.0;

                if (layer == 1) {
                        len = 66.572;
                } else if (layer == 2) {
                        len = 70.000;
                } else if (layer == 3) {
                        len = 73.428;
                }

		return len;

	}
        

	public int paddleNumber(int sector, int layer, int component) {
		
		int p = 0;
		int[] paddleOffset = {0, 0, 23, 85};
		
		p = component + (sector-1)*90 + paddleOffset[layer]; 
		return p;
	}
	
        public int toInt(String stringVal) {

            int intVal;
            try {
                intVal = Integer.parseInt(stringVal);
            } catch (NumberFormatException e) {
                intVal = 0;
            }
            return intVal;
        }           
        
	public static double toDouble(String stringVal) {

		double doubleVal;
		try {
			doubleVal = Double.parseDouble(stringVal);
		} catch (NumberFormatException e) {
//			doubleVal = UNDEFINED_OVERRIDE;
			doubleVal = 0.0;
		}
		return doubleVal;
	}

	public void setPlotTitle(int sector, int layer, int paddle) {
		// Overridden in calibration step classes
	}

	public void drawPlots(int sector, int layer, int paddle,
			EmbeddedCanvas canvas) {
		// Overridden in calibration step classes
	}

	public void showPlots(int sector, int layer) {
            
                System.out.println("**In CNDCalibrationEngine.showPlots() going to drawPlots()**");

                int component = 1;
		int layer_index = layer - 1;
                
		EmbeddedCanvas[] fitCanvases;
		fitCanvases = new EmbeddedCanvas[3];
//		fitCanvases[0] = new EmbeddedCanvas();
//		fitCanvases[0].divide(6, 4);

		int canvasNum = 0;
		int padNum = 0;

                //A canvas for each layer
                //A pad for each sector
                
		for (int layerNum = 1; layerNum <= 3; layerNum++) {
                    
                    canvasNum = layerNum - 1;
                    
                    fitCanvases[canvasNum] = new EmbeddedCanvas();
                    fitCanvases[canvasNum].divide(6, 4);
                    
                    for (int sectorNum = 1; sectorNum <= 24; sectorNum++) {
                        
                        padNum = sectorNum - 1;

                        fitCanvases[canvasNum].cd(padNum);
                        fitCanvases[canvasNum].getPad(padNum).setTitle("Sector "+sectorNum);
                        fitCanvases[canvasNum].getPad(padNum).setOptStat(0);

//                        System.out.println("**In CNDCalibrationEngine.showPlots() going to drawPlots()**");
                        drawPlots(sectorNum, layerNum, component, fitCanvases[canvasNum]);
                        
                    }
                }


		JFrame frame = new JFrame("CND - " + stepName);
		frame.setSize(1000, 800);

		JTabbedPane pane = new JTabbedPane();
		for (int i = 0; i < 3; i++) {
			pane.add(
                                "Layer " + (i+1),
				fitCanvases[i]);
		}

		frame.add(pane);
		// frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

	}
	
	public void rescaleGraphs(EmbeddedCanvas canvas, int sector, int layer, int paddle) {
		// overridden in each step
	}

	public void setOutput(boolean outputOn) {
		if (outputOn) {
			System.setOut(CNDCalibration.oldStdout);
		}
		else {
			System.setOut(new java.io.PrintStream(
					new java.io.OutputStream() {
						public void write(int b){}
					}
					));
		}
	}

	public boolean hitInSection(CNDPaddlePair paddle) {
		// is the hit within the desired counter section?
		
//		System.out.println("hitInSection");
//		System.out.println("SLC "+paddle.getDescriptor().getSector()+" "+paddle.getDescriptor().getLayer()+" "+paddle.getDescriptor().getComponent());
//		System.out.println("paddleY "+paddle.paddleY());
//		System.out.println("sectionWidth "+sectionWidth);
//		System.out.println("counterSection "+counterSection);
//		System.out.println("paddleLength "+paddle.paddleLength());
//		System.out.println("old >= "+(paddle.paddleLength()/2.0 - 10.0 - sectionWidth));
//		System.out.println("old <= "+(paddle.paddleLength()/2.0 - 10.0));
//		System.out.println("left >= "+(-paddle.paddleLength()/2.0 + 10.0));
//		System.out.println("left <= "+(-paddle.paddleLength()/2.0 + 10.0 + sectionWidth));
//		System.out.println("right >= "+(paddle.paddleLength()/2.0 - 10.0 - sectionWidth));
//		System.out.println("right <= "+(paddle.paddleLength()/2.0 - 10.0));
		
		
		
		boolean hitInSection = true;
		// exclude ends of paddles in all cases
		if (paddle.paddleY() <= -paddle.paddleLength()/2.0 + 10.0 ||
			paddle.paddleY() >= paddle.paddleLength()/2.0 - 10.0) {
			hitInSection = false;
		}
		else if ( counterSection==FULL_COUNTER ||
			(counterSection==CENTRE_SECTION && Math.abs(paddle.paddleY()) <= sectionWidth/2.0) ||
			(counterSection==LEFT_SECTION && 
				paddle.paddleY() <= -paddle.paddleLength()/2.0 + 10.0 + sectionWidth) || 
			(counterSection==RIGHT_SECTION && 
			 	paddle.paddleY() >= paddle.paddleLength()/2.0 - 10.0 - sectionWidth)) {
			hitInSection = true;
		}
		else {
			hitInSection = false;
		}
		
//		System.out.println("hitInSection return "+hitInSection);
		return hitInSection;
	}
}
