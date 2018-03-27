import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.process.ImageConverter;
import sun.awt.im.InputContext;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * Created by rudi on 07.05.2017.
 */


public class Main {

    //interface fields
    JFrame frame;
    ImagePlus imp;
    PanelObject canvas;
    JScrollPane scroll;
    Rectangle r;
    static int scrollValue;
    static JLabel jInfo;

    int roiType;
    boolean isStack;
    int sChannel;
    boolean cancel;
    double mmSensors=0.0;
    int sFrequnits;
    int nPhotodetectors=0;
    double ny=1;
    int sSize;
    int k;
    int i;
    int selecWidth;
    int selecHeight;
    int yMax;

    String title;
    Roi roi;

    double[] ESFLinea;
    double[][] ESFArray;
    double[][] LSFArray;
    double[][] Array;
    double[][] ArrayF;
    double[][] ESFArrayF;
    double[][] LSFArrayF;
    double[][] PosMax;
    double[]ESFVector;
    double[]LSFVector;
    double[]LSFDVector;
    double[]MTFVector;
    double[]SPPVector;
    double[]Max;

    int type = 0;
    boolean opButton=true;
    boolean genMTF = true,genEFS, genLSF, genSPP;

    ProfilePlot plotESF;
    Plot plotResult;

    public Main() {

        InterfaceConsole();
    }

    private void InterfaceConsole() {
        frame = new JFrame("MTF Calculator");
        frame.setSize(new Dimension(500, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton bOpenImage = new JButton("Open Image");
        JButton bGenerate = new JButton("Generate MTF");
        jInfo = new JLabel();

        frame.setMinimumSize(new Dimension(500, 600));
        frame.setLayout(new GridBagLayout());

        GridBagConstraints container = new GridBagConstraints();
        container.gridx = 0;
        container.gridy = 0;
        container.gridwidth = 1;
        container.gridheight = 1;
        container.anchor = GridBagConstraints.CENTER;
        container.insets = new Insets(10, 10, 10, 10);
        container.weightx = 1;
        container.weighty = 1;
       // container.fill = GridBagConstraints.BOTH;



        frame.add(bOpenImage, container);
        container.gridx = 1;
        frame.add(bGenerate, container);
        bGenerate.setVisible(false);
        container.gridx = 2;
        container.anchor = GridBagConstraints.LINE_END;
        container.fill = GridBagConstraints.EAST;
        frame.add(jInfo,container);
        frame.setBackground(Color.lightGray);
        frame.setResizable(true);

        container.gridx = 0;
        container.gridwidth = 3;
        container.gridy = 1;
        container.weightx = 8;
        container.weighty = 8;
        container.anchor = GridBagConstraints.CENTER;
        container.fill = GridBagConstraints.BOTH;
        scroll = new JScrollPane();


        bOpenImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                FileFilter imageFilter = new FileNameExtensionFilter(
                        "Image files", "bmp", "jpg", "jpeg", "png", "tif", "wbmp");

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.addChoosableFileFilter(imageFilter);
                fileChooser.setFileFilter(imageFilter);

                int open = fileChooser.showOpenDialog(frame);
                if (open == JFileChooser.APPROVE_OPTION) {
                    File inputFile = fileChooser.getSelectedFile();

                    try {
                        imp = new ImagePlus(inputFile.getAbsolutePath());


                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.out.println(ex.getMessage());
                    }
                    canvas = new PanelObject(imp);

                   scroll.setViewportView(canvas);


                    if(canvas == null)
                        frame.add(scroll,container);
                    if (canvas != null) {
                        frame.remove(scroll);
                        frame.add(scroll,container);
                        cancel = false;
                        bGenerate.setVisible(true);
                    }

                    System.out.println("Height: " + canvas.getHeight());
                    System.out.println("Width: " + canvas.getWidth());

                    title = inputFile.getPath();
                    if (imp == null) {
                        IJ.showMessage("Error", "Cannot read image!");
                        cancel = true;
                    }
                }
                frame.pack();
                frame.setVisible(true);
            }

        });

        bGenerate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (canvas == null) {
                    JOptionPane.showMessageDialog(frame, "First, chose a file to operate!", "Error", JOptionPane.INFORMATION_MESSAGE);
                } else {

                    try
                    {
                        setRectangle();
                    }
                    catch (NullPointerException ex)
                    {
                        JOptionPane.showMessageDialog(frame, "There are no ROI selected!","Warning",JOptionPane.INFORMATION_MESSAGE);
                    }

                    if (roi != null)
                    {
                        checkImage();
                        options();

                        generateESFArray( imp);
                        generateLSFArray( ESFArray);
                        calculateMax();

                        try {
                            ESFArrayF = alignArray(ESFArray);
                        }
                        catch (ArrayIndexOutOfBoundsException ex)
                        {
                            JOptionPane.showMessageDialog(frame, "There are no functional\n edge to calculate!",
                                    "Warning",JOptionPane.INFORMATION_MESSAGE);
                        }


                        if (!cancel) {
                            LSFArrayF = alignArray(LSFArray);
                            ESFVector = averageVector(ESFArrayF);
                            LSFVector = averageVector(LSFArrayF);

                            int aura = (LSFVector.length * 2);
                            LSFDVector = new double[aura];
                            int j = 0;

                            for (i = 0; i < (LSFDVector.length - 3); i++) {

                                if (i % 2 == 0) {
                                    LSFDVector[i] = LSFVector[j];
                                    j = j + 1;
                                } else {
                                    LSFDVector[i] = ((0.375 * LSFVector[(j - 1)]) + (0.75 * LSFVector[(j)]) - (0.125 * LSFVector[(j + 1)]));
                                }
                            }

                            LSFDVector[i] = ((LSFVector[j - 1] + LSFVector[j]) * 0.5);
                            LSFDVector[i + 1] = LSFVector[j];
                            LSFDVector[i + 2] = LSFVector[j];

                            int indexMax = 0;
                            double valorMax = LSFDVector[0];
                            for (int i = 0; i < LSFDVector.length; i++) {
                                if (valorMax < LSFDVector[i]) {
                                    indexMax = i;
                                    valorMax = LSFDVector[i];
                                }
                            }
                            i = indexMax;
                            LSFDVector[i - 1] = ((LSFDVector[i - 2] + LSFDVector[i]) * 0.5);

                            MTFVector = fftConversion(LSFDVector, "MTF");
                            Max = obtenerMax();
                            SPPVector = fftConversion(Max, "SPP");
                            ESFArrayF = null;

                            //genereating plots
                            if (genMTF)
                                generatePlot(MTFVector, "MTF");
                            if (genLSF)
                                generatePlot(LSFVector, "LSF");
                            if (genEFS)
                                generatePlot(ESFVector, "ESF");
                            if (genSPP)
                                generatePlot(SPPVector, "SPP");

                            cancel = false;
                            cleanImage();
                        }
                    }
                }
            }
        });

        frame.pack();
        frame.setVisible(true);
    }


private void checkImage()
{
    if (!cancel) {
        type = imp.getType();
        type = ImagePlus.COLOR_256;
        switch (type) {
            case ImagePlus.GRAY8: {
                isStack = false;
                yMax = 255;
            }
            break;
            case ImagePlus.GRAY16: {
                isStack = false;
                yMax = 65535;
            }
            break;
            case ImagePlus.GRAY32: {
                isStack = false;
                yMax = 2147483647;
            }
            break;
            case ImagePlus.COLOR_256: {
                isStack = true;
                yMax = 255;
            }
            break;
            case ImagePlus.COLOR_RGB: {
                isStack = true;
                yMax = 255;
            }
            break;
            default:
                yMax = 255;
        }


//        if (!cancel){
//            //Rectangular selection
//            if(canvas.rectField != null) {
//                imp.setRoi(rectField);
//               roi = imp.getRoi();
//                roiType = roi.getType();
//
//                if (!(roi.isLine() || roiType==Roi.RECTANGLE)) {
//                    IJ.showMessage("Error", "Line or rectangular selection required\nProcess canceled");
//                    cancel = true;
//
//                }
//            }
//        }
    }}


    private void cleanImage(){
        if(imp.getRoi()!= null)
        imp.killRoi();
    }

    private void generateStack(){

        if (isStack && sChannel!=3){
            ImageConverter ic = new ImageConverter(imp);
            ic.convertToRGBStack();
        }
    }


    private void options(){

        GenericDialog gd = new GenericDialog("MTF - Options",frame);

        //User can choose the units
        gd.addDialogListener(this::dialogItemChanged);
        String[]frequnits=new String[3];
        frequnits[0]="Absolute (lp/mm)";
        frequnits[1]="Relative (cycles/pixel)";
        frequnits[2]="Line widths per picture height (LW/PH)";
        gd.addChoice("Frequency units:",frequnits,frequnits[1]);


        //Input data
        gd.addNumericField("Sensor size (mm): ",mmSensors,0);
        gd.addNumericField("Number of photodetectors: ",nPhotodetectors,1);

        ((Component) gd.getNumericFields().get(0)).setEnabled(false);
        ((Component) gd.getNumericFields().get(1)).setEnabled(false);

        //The user can choose the sample width
        String[]sampleSize=new String[5];
        sampleSize[0]="32";
        sampleSize[1]="64";
        sampleSize[2]="128";
        sampleSize[3]="256";
        sampleSize[4]="512";
        gd.addChoice("Sample size (pixels):",sampleSize,sampleSize[0]);

//        //If is a greyscale image there is no options avaliable
//        if (!isStack){
//            gd.addMessage("This is a greyscale image, no options avaliable");
//        }
//
//        //If is a RGB image, user can choose each channel or the channels average to calculate MTF
//        else{
//            gd.addMessage("This is a three channel image, select an option");
//            String[]channel=new String[4];
//            channel[0]="Red Channel";
//            channel[1]="Green Channel";
//            channel[2]="Blue Channel";
//            channel[3]="Channels average";
//            gd.addChoice("Channel",channel,channel[3]);
//        }
        //Select which plots needed to display
//        gd.addCheckbox("Generate MTF plot",genMTF);
//        gd.addCheckbox("Generate LSF plot",genLSF);
//        gd.addCheckbox("Generate EFS plot",genEFS);
//        gd.addCheckbox("Generate SPP plot",genSPP);

        //Show general dialog

         gd.showDialog();

        //Ends the proccess
        if (gd.wasCanceled()){
            cancel=true;
            cleanImage();
            return;
        }
        gd.addDialogListener(this::dialogItemChanged);

        //Set the stat of the NumericText
        mmSensors = gd.getNextNumber();
        nPhotodetectors = (int)gd.getNextNumber();
        sFrequnits=gd.getNextChoiceIndex();

        //Frequency units
        if(sFrequnits==0){
            ny = (nPhotodetectors/mmSensors);
        }
        if(sFrequnits==1){
            ny = 1;
        }
        if(sFrequnits==2){
            ny = (nPhotodetectors*2);
        }

        //Save options
        sSize=gd.getNextChoiceIndex();
        String stringSize=sampleSize[sSize];
        sSize=Integer.parseInt(stringSize);

//        if (!isStack){
//            sChannel=0;
//        }
//        else{
//           sChannel=gd.getNextChoiceIndex();
//        }
//
//        if(isStack && sChannel!=3){
//            generateStack();
//            imp.setSlice(sChannel+1);
//        }

//        genMTF = gd.getNextBoolean();
//        genLSF = gd.getNextBoolean();
//        genEFS = gd.getNextBoolean();
//        genSPP = gd.getNextBoolean();

    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        sFrequnits=gd.getNextChoiceIndex();

        if(sFrequnits==1){
            ((Component) gd.getNumericFields().get(0)).setEnabled(false);
            ((Component) gd.getNumericFields().get(1)).setEnabled(false);
        }
        if(sFrequnits==0){
            ((Component) gd.getNumericFields().get(0)).setEnabled(true);
            ((Component) gd.getNumericFields().get(1)).setEnabled(true);
        }
        if(sFrequnits==2){
            ((Component) gd.getNumericFields().get(0)).setEnabled(false);
            ((Component) gd.getNumericFields().get(1)).setEnabled(true);
        }


        return true;
    }

private void setRectangle()
{
    r = canvas.getRectangle().getBounds();
        imp.setRoi(r);
        roi = imp.getRoi();
        selecWidth = r.width;
        selecHeight = r.height;
}
    //The grey values of the line selections are tipped in a Array
    private void generateESFArray( ImagePlus imp) {

       // setRectangle();


        if (sSize>=selecWidth){
            IJ.showMessage("Error","sample size is bigger than selection width\nProcess canceled");
            return;
        }

        int selectX=r.x;
        int selectY=r.y;
        int selectXFin=selectX+selecWidth;
        int selectYFin=selectY+selecHeight;
        ESFLinea=new double[selecWidth];
        ESFArray=new double[selecHeight][selecWidth];
        for(k=0;k<selecHeight;k++){
            //select line
            this.makeLine(selectX,k+selectY,selectXFin-1,k+selectYFin);

            //save values
            plotESF = new ProfilePlot(imp);
            ESFLinea=plotESF.getProfile();
            //Load Array ESF
            for(i=0;i<selecWidth;i++){
                ESFArray[k][i]=ESFLinea[i];
            }
        }

    }

     private void makeLine(int x1, int y1, int x2, int y2)
    {
        getImage().setRoi(new Line(x1, y1, x2, y2));
    }

     private ImagePlus getImage() {
        ImagePlus img = imp;
        if (img==null) {
            IJ.noImage();
        }
        return img;
    }


    private void generateLSFArray( double[][]ESFArray){

        LSFArray=new double[selecHeight][selecWidth];

        for(k=0;k<selecHeight;k++){
            for(i=0;i<selecWidth-1;i++){
                LSFArray[k][i]=ESFArray[k][i+1]-ESFArray[k][i];
            }
        }

    }


     private double[][] alignArray(double[][] Array){

        ArrayF = new double[selecHeight][sSize];
        int ini;
        int fin;

        //Create new array aligned
        for(k=0;k<selecHeight;k++){

            //Initial and end positions of k-line
            ini=(int)PosMax[k][2];
            fin=(int)PosMax[k][3];

            for(i=ini;i<fin;i++){
                ArrayF[k][i-ini]=Array[k][i];
            }
        }

        //final array - from 32 to 512 positions
        return ArrayF;
    }


    //Calculate maximum value and find 32 positions to align
    private void calculateMax(){

        PosMax=new double[selecHeight][4] ;
        int posMax;
        int halfSize;
        halfSize=sSize/2;
        for(k=0;k<selecHeight-1;k++){
            posMax=0;
            for(i=0;i<selecWidth-1;i++){
                if (LSFArray[k][posMax]<LSFArray[k][i] || LSFArray[k][posMax]==LSFArray[k][i]){
                    posMax=i;
                }
            }

            //The maximum value position on the line
            PosMax[k][0]=posMax;
            //The maximum value on the line
            PosMax[k][1]=LSFArray[k][posMax];
            //Starting and ending position to align maximum values
            PosMax[k][2]=PosMax[k][0]-halfSize;
            PosMax[k][3]=PosMax[k][0]+halfSize;
        }
    }


    public double[] averageVector(double[][] Array){

        double result;
        //int j;
        double[]Vector = new double[sSize];

        //Average of all linear ESF/LSF
        for(i=0;i<sSize;i++){
            result=0;
            //Average of all rows i-position
            for(k=0;k<selecHeight;k++){
                result=result+Array[k][i];
            }
            result=result/selecHeight;
            Vector[i]=result;
        }
        return Vector;
    }


    private int longPotDos(int length){
        int N;
        double log = Math.log(length)/Math.log(2);
        N=(int)Math.pow(2,(int)Math.floor(log));
        return N;
    }


    private double[] obtenerMax(){
        int N=longPotDos(selecHeight);
        Max=new double[N];
        for(k=0;k<N;k++){
            Max[k]=PosMax[k][1];
        }
        return Max;
    }


    void generatePlot(double[] Vector, String plot){

        double[]xValues;
        String ejeX="pixel";
        String ejeY="";
        String allTitle;

        //If MTF plot, calculate the scale of cycles per pixel for x-axis values
        xValues=calculateXValues(Vector,plot);

        //plot titles
        if (plot=="ESF"){
            ejeY="Grey Value";
        }

        if (plot=="LSF"){
            ejeY="Grey Value / pixel";
        }

        if (plot=="MTF"){
            ejeY="Modulation Factor";

            //Units
            if(sFrequnits==0){
                ejeX ="lp/mm";
            }
            if(sFrequnits==1){
                ejeX ="Cycles/Pixel";
            }
            if(sFrequnits==2){
                ejeX ="Line Width/Picture Height";
            }
        }

        if (plot=="SPP"){
            ejeY="SPP";
        }

        allTitle=plot + "_" + title;
        plotResult = new Plot(allTitle, ejeX, ejeY, xValues, Vector);

        //plot limits
        if (plot=="ESF"){
            plotResult.setLimits(1,Vector.length,0,yMax);
        }

        if (plot=="LSF"){
            plotResult.setLimits(1,Vector.length,0,yMax);
        }

        if (plot=="MTF"){
            plotResult.setLimits(0,ny,0,1);
        }

        if (plot=="SPP"){
            plotResult.setLimits(1,Vector.length,0,1);
        }

        plotResult.draw();
        plotResult.show();
    }


    public double[] calculateXValues(double[] Vector, String plot){

        int N=Vector.length;
        double[]xValues = new double[N];

        if(plot.equals("MTF")){
            xValues[0]=0;
            //Scale of values for x-axis
            for(i=1;i<N;i++){
                //xValues[i]=xValues[i-1]+(0.5/(N-1));
                xValues[i]=xValues[i-1]+(ny/(N-1));
            }
        }else{
            for(i=0;i<N;i++){
                xValues[i]=i+1;
            }
        }
        return xValues;
    }



    //data type conversion from complex to double, to implement fft
    private double[] fftConversion(double[] Vector, String plot){

        //Only half are necessary
        int N=Vector.length;
        int M=Vector.length/2;
        double divisor;
        Complex[] ArrayComplex = new Complex[N];
        Complex[] VectorFFTC = new Complex[M];
        double[]VectorFFTD=new double[M];


        for (i = 0; i < N; i++){
            //A double array is converted into a complex array ; imaginary part=0
            ArrayComplex[i] = new Complex(Vector[i], 0);
        }
        //FFT operation
        VectorFFTC = fft(ArrayComplex);


        if(plot.equals("SPP")){
            //Reject the first one
            for (i = 1; i < M; i++){
                //absolute value (module)
                VectorFFTD[i-1]=VectorFFTC[i].abs()/VectorFFTC[1].abs();
            }
        }else{
            for (i = 0; i < M; i++){
                //absolute value (module)
                VectorFFTD[i]=VectorFFTC[i].abs()/VectorFFTC[0].abs();
            }
        }

        //Normalize
        if(plot.equals("SPP")){
            divisor=findMaxSPP(VectorFFTD);
        }else{
            divisor=VectorFFTD[0];
        }

        for (i = 0; i < M; i++){
            VectorFFTD[i]=VectorFFTD[i]/divisor;
        }
        return VectorFFTD;
    }


     private double findMaxSPP(double[] Vector){
        double max = 0;
        for (int i=0; i<Vector.length; i++) {
            if (Vector[i]>max) max=Vector[i];
        }

        return max;
    }




     private Complex[] fft(Complex[] x) {

        int N = x.length;
        Complex[] y = new Complex[N];

        // base case
        if (N == 1) {
            y[0] = x[0];
            return y;
        }

        // radix 2 Cooley-Tukey FFT
        if (N % 2 != 0) throw new RuntimeException("N is not a power of 2");
        Complex[] even = new Complex[N/2];
        Complex[] odd  = new Complex[N/2];
        for (int k = 0; k < N/2; k++) even[k] = x[2*k];
        for (int k = 0; k < N/2; k++) odd[k]  = x[2*k + 1];

        Complex[] q = fft(even);
        Complex[] r = fft(odd);

        for (int k = 0; k < N/2; k++) {
            double kth = -2 * k * Math.PI / N;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = q[k].plus(wk.multiply(r[k]));
            y[k + N/2] = q[k].minus(wk.multiply(r[k]));
        }
        return y;
    }

    //Main method
    public static void main(String[] args) {

          Main main = new Main();
    }

    //class Complex
    public class Complex {
        private double real;   //the real part
        private double imaginary;   //the imaginary part

        // create a new object with the given real and imaginary parts
        public Complex(double re, double im) {
            this.real = re;
            this.imaginary = im;
        }

        // return a string representation of the invoking object
        public String toString()
        {
            if(imaginary > 0)
                return real + " + " + imaginary + "i";
            else
                return real + " " + imaginary + "i";
        }

        // return a new object whose value is (this + b)
        public Complex plus(Complex b) {
            Complex a = this;             // invoking object
            double re = a.real + b.real;
            double im = a.imaginary + b.imaginary;
            Complex equals = new Complex(re, im);
            return equals;
        }

        public Complex minus(Complex b) {
            Complex a = this;
            double re = a.real - b.real;
            double im = a.imaginary - b.imaginary;
            Complex equals = new Complex(re, im);
            return equals;
        }

        // return a new object whose value is (this * b)
        public Complex multiply(Complex b) {
            Complex a = this;
            double re = a.real * b.real - a.imaginary * b.imaginary;
            double im = a.real * b.imaginary + a.imaginary * b.real;
            Complex equals = new Complex(re, im);
            return equals;
        }

        // return |this|
        public double abs()
        {
            return Math.sqrt(real * real + imaginary * imaginary);
        }
    }
}

