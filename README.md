# Android:Portrait Blur using DeeplabV3+ Semantic Image Segmentation 
A simple android app to implement Portrait mode using single sensor like in Pixel 2 (well not exactly exactly like Pixel 2's). This app allows you to either click image from your phone or select an image from storage and apply the blur.
I have 3 pre trained models of diffrent crop size (Default 1025 px). You can use any one of them but with increased crop size the processing time also increases (by a lot) so use any one of them as per your requirement.
<br>For using 1536 px: -<br>Copy '\InputSize 1536px\frozen_inference_graph.pb' and paste it in 'CameraBlur\app\src\main\assets\'. 
<br>Then change '\CameraBlur\app\src\main\java\com\anondev\gaurav\camerablur\DeeplabProcessor.java' line 28 from <pre> public final static int INPUT_SIZE = 1025;</pre> to <pre> public final static int INPUT_SIZE = 1536;</pre>  
