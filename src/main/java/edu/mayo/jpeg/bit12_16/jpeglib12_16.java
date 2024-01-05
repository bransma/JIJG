package edu.mayo.jpeg.bit12_16;

public class jpeglib12_16
{
    public static final int JPEG_REACHED_SOS = 1; /* Reached start of new scan */
    public static final int JPEG_REACHED_EOI = 2; /* Reached end of image */
    public static final int JPEG_ROW_COMPLETED = 3; /* Completed one iMCU row */
    public static final int JPEG_SCAN_COMPLETED = 4; /* Completed last iMCU row of a scan */
    /* Return value is one of: */
    public static final int JPEG_SUSPENDED = 0; /* Suspended due to lack of input data */
    public static final int JPEG_HEADER_OK = 1; /* Found valid image datastream */
    public static final int JPEG_HEADER_TABLES_ONLY = 2; /* Found valid table-specs-only datastream */
    //
    public static final int JPEG_MAX_DIMENSION = 65500;
    public static final int MAX_COMPONENTS = 10;
    public static final int _8_BIT = 8;
    public static final int _12_BIT = 12;
    public static final int _16_BIT = 16;

    /* Various constants determining the sizes of things.
     * All of these are specified by the JPEG standard, so don't change them
     * if you want to be compatible.
     */

    public static final int DCTSIZE = 8; /* The basic DCT block is 8x8 samples */
    public static final int DCTSIZE2 = 64; /* DCTSIZE squared; # of elements in a block */
    public static final int NUM_QUANT_TBLS = 4; /* Quantization tables are numbered 0..3 */
    public static final int NUM_HUFF_TBLS = 4; /* Huffman tables are numbered 0..3 */
    public static final int NUM_ARITH_TBLS = 16; /* Arith-coding tables are numbered 0..15 */
    public static int MAX_COMPS_IN_SCAN = 4; /* JPEG limit on # of components in one scan */
    public static int MAX_SAMP_FACTOR = 4; /* JPEG limit on sampling factors */
    /* Unfortunately, some bozo at Adobe saw no reason to be bound by the standard;
     * the PostScript DCT filter can emit files with many more than 10 data units
     * per MCU.
     * If you happen to run across such a file, you can up D_MAX_DATA_UNITS_IN_MCU
     * to handle it.  We even let you do this from the jconfig.h file.  However,
     * we strongly discourage changing C_MAX_DATA_UNITS_IN_MCU; just because Adobe
     * sometimes emits noncompliant files doesn't mean you should too.
     */

    public static final int C_MAX_DATA_UNITS_IN_MCU = 10; /* compressor's limit on data units/MCU */
    public static final int D_MAX_DATA_UNITS_IN_MCU = 10; /* decompressor's limit on data units/MCU */

    /* These marker codes are exported since applications and data source modules
     * are likely to want to use them.
     */
    public static final int JPEG_RST0 = 0xD0;    /* RST0 marker code */
    public static final int JPEG_EOI = 0xD9;    /* EOI marker code */
    public static final int JPEG_APP0 = 0xE0;    /* APP0 marker code */
    public static final int JPEG_COM = 0xFE;    /* COM marker code */


    /* Memory manager object.
     * Allocates "small" objects (a few K total), "large" objects (tens of K),
     * and "really big" objects (virtual arrays with backing store if needed).
     * The memory manager does not allow individual objects to be freed; rather,
     * each created object is assigned to a pool, and whole pools can be freed
     * at once.  This is faster and more convenient than remembering exactly what
     * to free, especially where malloc()/free() are not too speedy.
     * NB: alloc routines never return NULL.  They exit to error_exit if not
     * successful.
     */
    public static final int JPOOL_PERMANENT = 0; /* lasts until master record is destroyed */
    public static final int JPOOL_IMAGE = 1; /* lasts until done with image/datastream */
    public static final int JPOOL_NUMPOOLS = 2;


    /* Representation of a DCT frequency coefficient.
     * This should be a signed value of at least 16 bits; "short" is usually OK.
     * Again, we allocate large arrays of these, but you can change to int
     * if you have memory to burn and "short" is really slow.
     */

    // JSAMPLE

    //typedef byte or short JSAMPLE 8 or 12/16 bits
    //typedef JSAMPLE FAR *JSAMPROW;  /* ptr to one image row of pixel samples. */
    //typedef JSAMPROW *JSAMPARRAY;   /* ptr to some rows (a 2-D sample array) */
    //typedef JSAMPARRAY *JSAMPIMAGE; /* a 3-D sample array: top index is color */
    // translation:
    // byte *JSAMPROW --> JSAMPROW = array of bytes or a byte[]
    // byte[] *JSAMPARRAY --> array of byte[] or a byte[][]
    // byte[][] *JSAMPIMAGE --> array of byte[][] or a byte[][][]

    //byte[] or short[] 8 or 12/16 bits JSAMPROW /* ptr to one image row of pixel samples. */
    //byte[][] or short[][] JSAMPARRAY;   /* ptr to some rows (a 2-D sample array) */
    //byte[][][] or short[][][] JSAMPIMAGE; /* a 3-D sample array: top index is color */

    // JBLOCKS

    //typedef short JCOEF
    //typedef JCOEF JBLOCK[DCTSIZE2]; /* one block of coefficients */
    //typedef JBLOCK FAR *JBLOCKROW;  /* pointer to one row of coefficient blocks */
    //typedef JBLOCKROW *JBLOCKARRAY;         /* a 2-D array of coefficient blocks */
    //typedef JBLOCKARRAY *JBLOCKIMAGE;       /* a 3-D array of coefficient blocks */
    // translation:
    // short JBLOCK[DCTSIZE2] --> JBLOCK is a short array of size DCTSIZE2, e.g, replace JBLOCK with short[DCTSIZE2]
    // short[] *JBLOCKROW --> JBLOCKROW is an array of short[][DCTSIZE2]
    // short[][] *JBLOCKARRAY --> JBLOCKARRAY is an array of short[][] or a short[][][DCTSIZE2]
    // short[][][] *JBLOCKIMAGE --> JBLOCKIMAGE is an 2-d array of short[DCTSIZE2][][] or a short[][][DCTSIZE2]

    //typedef short JCOEF
    //short *JCOEFPTR; an array of JCOEFS (short) or a short[] /* useful in a couple of places */

    /* Representation of a spatial difference value.
     * This should be a signed value of at least 16 bits; int is usually OK.
     */

    //typedef int JDIFF;
    //int[] JDIFFROW; /* pointer to one row of difference values */
    //int[][] JDIFFARRAY; /* ptr to some rows (a 2-D diff array) */
    //int[][][] JDIFFIMAGE; /* a 3-D diff array: top index is color */

    public enum J_CODEC_PROCESS
    {
        JPROC_SEQUENTIAL, /* baseline/extended sequential DCT */
        JPROC_PROGRESSIVE, /* progressive DCT */
        JPROC_LOSSLESS /* lossless (sequential) */
    }

    /* Known color spaces. */

    public enum J_COLOR_SPACE
    {
        JCS_UNKNOWN, /* error/unspecified */
        JCS_GRAYSCALE, /* monochrome */
        JCS_RGB, /* red/green/blue */
        JCS_YCbCr, /* Y/Cb/Cr (also known as YUV) */
        JCS_CMYK, /* C/M/Y/K */
        JCS_YCCK /* Y/Cb/Cr/K */
    }

    /* DCT/IDCT algorithm options. */

    public enum J_DCT_METHOD
    {
        JDCT_ISLOW, /* slow but accurate integer algorithm */
        JDCT_IFAST, /* faster, less accurate integer method */
        JDCT_FLOAT, /* floating-point: accurate, fast on fast HW */
        UNINITIALIZED /* -1 in the C-code */
    }

    public static final J_DCT_METHOD JDCT_DEFAULT = J_DCT_METHOD.JDCT_ISLOW;
    public static final J_DCT_METHOD JDCT_FASTEST = J_DCT_METHOD.JDCT_IFAST;

    public enum J_DITHER_MODE
    {
        JDITHER_NONE, /* no dithering */
        JDITHER_ORDERED, /* simple ordered dither */
        JDITHER_FS /* Floyd-Steinberg error diffusion dither */
    }

    // in jpeglib.h the following structs are defined, which are separate classes in edu.mayo.jpeg.codec.structs
    // JQUANT_TBL
    // JHUFF_TBL
    // jpeg_component_info
    // jpeg_scan_info
    // jpeg_marker_struct
    // jpeg_compress_struct
    // jpeg_decompress_struct
    // jpeg_error_manager
    // jpeg_destination_mgr
    // jpeg_source_mgr
    // jvirt_sarry_contol
    // jvirt_barray_control
    // jpeg_memory_mgr
    // 
    // There are then a whole slew of method signatures, akin to those that would be placed in an interface in Java
}
