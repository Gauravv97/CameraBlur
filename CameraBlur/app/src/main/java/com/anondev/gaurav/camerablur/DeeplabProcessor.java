package com.anondev.gaurav.camerablur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;


import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Output;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

public class DeeplabProcessor {

    private final static String MODEL_FILE = "file:///android_asset/frozen_inference_graph.pb";

    private final static String INPUT_NAME = "ImageTensor";
    private final static String OUTPUT_NAME = "SemanticPredictions";

    public final static int INPUT_SIZE = 1025;

    private static TensorFlowInferenceInterface sTFInterface = null;
    private static Context mContext;

    public synchronized static boolean initialize(Context context) {
        mContext=context;
        final File graphPath = new File(MODEL_FILE);

        FileInputStream graphStream;
        try {
            graphStream = new FileInputStream(graphPath);
        } catch (FileNotFoundException e) {
            Log.e("Input",e.getMessage());

            graphStream = null;
        }
/*

        if (graphStream == null) {
            return false;
        }
*/


        //sTFInterface = new TensorFlowInferenceInterface(graphStream);
        sTFInterface=new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
        if (sTFInterface == null) {
            Log.w("init model failed",
                    MODEL_FILE);

            return false;
        }

//        printGraph(sTFInterface.graph());
//        printOp(sTFInterface.graph(), "ImageTensor");

        if (graphStream != null) {
            try {
                graphStream.close();
            } catch (IOException e) {
            }
        }

        return true;
    }

    public synchronized static boolean isInitialized() {
        return (sTFInterface != null);
    }


    public synchronized static int[] GetBlurredImage(final Bitmap bitmap) {
        if (sTFInterface == null) {
            Log.e("tfmodel not init","");
            return null;
        }

        if (bitmap == null) {
            Log.e("bitmap null ","");
            return null;
        }

        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        Log.e("bitmap: "+w+"x"+h,"");

        if (w > INPUT_SIZE || h > INPUT_SIZE) {/*
            Log.w("invalid bitmap size: %d x %d [should be: %d x %d]",
                    w, h,
                    INPUT_SIZE, INPUT_SIZE);*/
            Log.e("bitmap: "+w+"x"+h,"");

            return null;
        }

        int[] mIntValues = new int[w * h];
        byte[] mFlatIntValues = new byte[w * h * 3];
        int[] mOutputs = new int[w * h];

        bitmap.getPixels(mIntValues, 0, w, 0, 0, w, h);
        for (int i = 0; i < mIntValues.length; ++i) {
            final int val = mIntValues[i];
            mFlatIntValues[i * 3 + 0] = (byte)((val >> 16) & 0xFF);
            mFlatIntValues[i * 3 + 1] = (byte)((val >> 8) & 0xFF);
            mFlatIntValues[i * 3 + 2] = (byte)(val & 0xFF);
        }

        final long start = System.currentTimeMillis();
        sTFInterface.feed(INPUT_NAME, mFlatIntValues, 1, h, w, 3 );
        sTFInterface.run(new String[] { OUTPUT_NAME }, true);
        sTFInterface.fetch(OUTPUT_NAME, mOutputs);
        double ar[]=new double[256];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if(mOutputs[y * w + x]!=0)
                    ar[mOutputs[y * w + x]]++;
            }
        }
        for (int y = h/2-10; y < h/2+10; y++) {
            for (int x = w/2-10; x < w/2+10; x++) {
                ar[mOutputs[y * w + x]]=ar[mOutputs[y * w + x]]*1.04;
            }
        }
        double max=0;
        int maxi=0;
        for (int i=1;i<256;i++){
            if(max<ar[i]) {
                max = ar[i];
                maxi=i;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                mOutputs[y * w + x]= (mOutputs[y * w + x]== maxi) ? 1:0;
            }
        }

        return  mOutputs;

    }

    /*public static void printOp(Graph graph, String opName) {
        if (graph == null
                || TextUtils.isEmpty(opName)) {
            return;
        }

        Operation op = graph.operation(opName);

        Logger.debug("op[%s]: %s",
                opName, op);
    }

    public static void printGraph(Graph graph) {
        if (graph == null) {
            return;
        }

        Iterator<Operation> operations = graph.operations();
        if (operations == null) {
            return;
        }

        Operation op;
        Output<?> output;
        int num;
        while (operations.hasNext()) {
            op = operations.next();

            Logger.debug("op: [%s]", op);
            num = op.numOutputs();

            for (int i = 0; i < num; i++) {
                output = op.output(i);

                Logger.debug("%s- [%d]: %s",
                        (i == num - 1) ? "`" : "|",
                        i, output);
            }
        }
    }*/
}

