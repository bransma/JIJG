package jijg.bit12_16;

public class jmorecfg12_16
{
    /*
     * Define BITS_IN_JSAMPLE as either
     *   8   for 8-bit sample values (the usual setting)
     *   12  for 12-bit sample values
     * Only 8 and 12 are legal data precisions for lossy JPEG according to the
     * JPEG standard, and the IJG code does not support anything else!
     * We do not support run-time selection of data precision, sorry.
     */

    public static int BITS_IN_JSAMPLE = 8; /* use 8 or 12 (or 16 for lossless) */
    public static int MAXJSAMPLE = 255;
    public static int CENTERJSAMPLE = 128;

    /*
     * Maximum number of components (color channels) allowed in JPEG image.
     * To meet the letter of the JPEG spec, set this to 255.  However, darn
     * few applications need more than 4 channels (maybe 5 for CMYK + alpha
     * mask).  We recommend 10 as a reasonable compromise; use 4 if you are
     * really short on memory.  (Each allowed component costs a hundred or so
     * bytes of storage, whether actually used in an image or not.)
     */

    public static final int MAX_COMPONENTS = 10; /* maximum number of image components */

    /* Arithmetic coding is unsupported for legal reasons.  Complaints to IBM. */

    /* Capability options common to encoder and decoder: */

    public static boolean DCT_ISLOW_SUPPORTED = true; /* slow but accurate integer algorithm */
    public static boolean DCT_IFAST_SUPPORTED = true; /* faster, less accurate integer method */
    public static boolean DCT_FLOAT_SUPPORTED = true; /* floating-point: accurate, fast on fast HW */

    /* Encoder capability options: */

    public static boolean C_ARITH_CODING_SUPPORTED = false; /* Arithmetic coding back end? */
    public static boolean C_MULTISCAN_FILES_SUPPORTED = true; /* Multiple-scan JPEG files? */
    public static boolean C_PROGRESSIVE_SUPPORTED = true; /* Progressive JPEG? (Requires MULTISCAN)*/
    public static boolean C_LOSSLESS_SUPPORTED = true; /* Lossless JPEG? */
    public static boolean ENTROPY_OPT_SUPPORTED = true; /* Optimization of entropy coding parms? */
    /* Note: if you selected 12-bit data precision, it is dangerous to turn off
     * ENTROPY_OPT_SUPPORTED.  The standard Huffman tables are only good for 8-bit
     * precision, so jcshuff.c normally uses entropy optimization to compute
     * usable tables for higher precision.  If you don't want to do optimization,
     * you'll have to supply different default Huffman tables.
     * The exact same statements apply for progressive and lossless JPEG:
     * the default tables don't work for progressive mode or lossless mode.
     * (This may get fixed, however.)
     */
    public static boolean INPUT_SMOOTHING_SUPPORTED = true; /* Input image smoothing option? */

    /* Decoder capability options: */

    public static boolean D_ARITH_CODING_SUPPORTED = false; /* Arithmetic coding back end? */
    public static boolean D_MULTISCAN_FILES_SUPPORTED = true; /* Multiple-scan JPEG files? */
    public static boolean D_PROGRESSIVE_SUPPORTED = true; /* Progressive JPEG? (Requires MULTISCAN)*/
    public static boolean D_LOSSLESS_SUPPORTED = true; /* Lossless JPEG? */
    public static boolean SAVE_MARKERS_SUPPORTED = true; /* jpeg_save_markers() needed? */
    public static boolean BLOCK_SMOOTHING_SUPPORTED = true; /* Block smoothing? (Progressive only) */
    public static boolean IDCT_SCALING_SUPPORTED = true; /* Output rescaling via IDCT? */
    public static boolean UPSAMPLE_SCALING_SUPPORTED = true; /* Output rescaling at upsample stage? */
    public static boolean UPSAMPLE_MERGING_SUPPORTED = true; /* Fast path for sloppy upsampling? */
    public static boolean QUANT_1PASS_SUPPORTED = true; /* 1-pass color quantization? */
    public static boolean QUANT_2PASS_SUPPORTED = true; /* 2-pass color quantization? */

    /* more capability options later, no doubt */
    public static boolean WITH_ARITHMETIC_PATCH = false;

    /*
     * Ordering of RGB data in scanlines passed to or from the application.
     * If your application wants to deal with data in the order B,G,R, just
     * change these macros.  You can also deal with formats such as R,G,B,X
     * (one extra byte per pixel) by changing RGB_PIXELSIZE.  Note that changing
     * the offsets will also change the order in which colormap data is organized.
     * RESTRICTIONS:
     * 1. The sample applications cjpeg,djpeg do NOT support modified RGB formats.
     * 2. These macros only affect RGB<=>YCbCr color conversion, so they are not
     *    useful if you are using JPEG color spaces other than YCbCr or grayscale.
     * 3. The color quantizer modules will not behave desirably if RGB_PIXELSIZE
     *    is not 3 (they don't understand about dummy color components!).  So you
     *    can't use color quantization if you change that value.
     */

    public static int RGB_RED = 0; /* Offset of Red in an RGB scanline element */
    public static int RGB_GREEN = 1; /* Offset of Green */
    public static int RGB_BLUE = 2; /* Offset of Blue */

    public static int RGB_PIXELSIZE = 3; /* JSAMPLEs per RGB scanline element */
}
