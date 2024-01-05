package edu.mayo.jpeg.bit12_16.structs12_16;

import edu.mayo.jpeg.bit12_16.jpegint12_16;
import edu.mayo.jpeg.bit12_16.jpeglib12_16;
import edu.mayo.jpeg.bit12_16.jpeglib12_16.J_CODEC_PROCESS;

import java.util.ArrayList;

public class jpeg_decompress_struct12_16
{
    /* Decompression processing parameters --- these fields must be set before
     * calling jpeg_start_decompress().  Note that jpeg_read_header() initializes
     * them to default values.
     */
    public boolean is_decompressor = true; /* So common code can tell which is which */
    public int global_state = jpegint12_16.DSTATE_START; /* For checking call sequence validity */

    /* Basic description of image --- filled in by jpeg_read_header(). */
    /* Application may inspect these values to decide how to process image. */

    public int image_width; /* nominal image width (from SOF marker) */
    public int image_height; /* nominal image height */
    public int num_components; /* # of color components in JPEG image */
    public jpeglib12_16.J_COLOR_SPACE jpeg_color_space; /* colorspace of JPEG image */

    /* Decompression processing parameters --- these fields must be set before
     * calling jpeg_start_decompress().  Note that jpeg_read_header() initializes
     * them to default values.
     */

    public jpeglib12_16.J_COLOR_SPACE out_color_space; /* colorspace for output */

    public int scale_num, scale_denom; /* fraction by which to scale image */

    public double output_gamma; /* image gamma wanted in output */

    public boolean buffered_image; /* TRUE=multiple output passes */
    public boolean raw_data_out; /* TRUE=downsampled data wanted */

    public jpeglib12_16.J_DCT_METHOD dct_method; /* IDCT algorithm selector */
    public boolean do_fancy_upsampling; /* TRUE=apply fancy upsampling */
    public boolean do_block_smoothing; /* TRUE=apply interblock smoothing */

    public boolean quantize_colors; /* TRUE=colormapped output wanted */
    /* the following are ignored if not quantize_colors: */
    public jpeglib12_16.J_DITHER_MODE dither_mode; /* type of color dithering to use */
    public boolean two_pass_quantize; /* TRUE=use two-pass color quantization */
    public int desired_number_of_colors; /* max # colors to use in created colormap */
    /* these are significant only in buffered-image mode: */
    public boolean enable_1pass_quant; /* enable future use of 1-pass quantizer */
    public boolean enable_external_quant;/* enable future use of external colormap */
    public boolean enable_2pass_quant; /* enable future use of 2-pass quantizer */

    /* Description of actual output image that will be returned to application.
     * These fields are computed by jpeg_start_decompress().
     * You can also use jpeg_calc_output_dimensions() to determine these values
     * in advance of calling jpeg_start_decompress().
     */

    public int output_width; /* scaled image width */
    public int output_height; /* scaled image height */
    public int out_color_components; /* # of color components in out_color_space */
    public int output_components; /* # of color components returned */
    /* output_components is 1 (a colormap index) when quantizing colors;
     * otherwise it equals out_color_components.
     */
    public int rec_outbuf_height; /* min recommended height of scanline buffer */
    /* If the buffer passed to jpeg_read_scanlines() is less than this many rows
     * high, space and time will be wasted due to unnecessary data copying.
     * Usually rec_outbuf_height will be 1 or 2, at most 4.
     */

    /* When quantizing colors, the output colormap is described by these fields.
     * The application can supply a colormap by setting colormap non-NULL before
     * calling jpeg_start_decompress; otherwise a colormap is created during
     * jpeg_start_decompress or jpeg_start_output.
     * The map has out_color_components rows and actual_number_of_colors columns.
     */
    public int actual_number_of_colors; /* number of entries in use */
    public short[][] colormap; /* The color map as a 2-D pixel array */

    /* State variables: these variables indicate the progress of decompression.
     * The application may examine these but must not modify them.
     */

    /* Row index of next scanline to be read from jpeg_read_scanlines().
     * Application may use this to control its processing loop, e.g.,
     * "while (output_scanline < output_height)".
     *
     * Java: made it into an array, because it's passed by reference
     */
    public int[] output_scanline; /* 0 .. output_height-1  */

    /* Current input scan number and number of iMCU rows completed in scan.
     * These indicate the progress of the decompressor input side.
     */
    public int input_scan_number; /* Number of SOS markers seen so far */
    public int input_iMCU_row; /* Number of iMCU rows completed */

    /* The "output scan number" is the notional scan being displayed by the
     * output side.  The decompressor will not allow output scan/row number
     * to get ahead of input scan/row, but it can fall arbitrarily far behind.
     */
    public int output_scan_number; /* Nominal scan number being displayed */
    public int output_iMCU_row; /* Number of iMCU rows read */

    /* Current progression status.  coef_bits[c][i] indicates the precision
     * with which component c's DCT coefficient i (in zigzag order) is known.
     * It is -1 when no data has yet been received, otherwise it is the point
     * transform (shift) value for the most recent scan of the coefficient
     * (thus, 0 at completion of the progression).
     * This pointer is NULL when reading a non-progressive file.
     */
    public int[][] coef_bits; /* -1 or current Al value for each coef */

    /* Internal JPEG parameters --- the application usually need not look at
     * these fields.  Note that the decompressor output side may not use
     * any parameters that can change between scans.
     */

    /* Quantization and Huffman tables are carried forward across input
     * datastreams when processing abbreviated JPEG datastreams.
     */

    public JQUANT_TBL12_16[] quant_tbl_ptrs = new JQUANT_TBL12_16[jpeglib12_16.NUM_QUANT_TBLS];
    /* ptrs to coefficient quantization tables, or null if not defined */

    public JHUFF_TBL12_16[] dc_huff_tbl_ptrs = new JHUFF_TBL12_16[jpeglib12_16.NUM_HUFF_TBLS];
    public JHUFF_TBL12_16[] ac_huff_tbl_ptrs = new JHUFF_TBL12_16[jpeglib12_16.NUM_HUFF_TBLS];
    /* ptrs to Huffman coding tables, or null if not defined */

    /* These parameters are never carried across datastreams, since they
     * are given in SOF/SOS markers or defined to be reset by SOI.
     */

    public int data_precision; /* bits of precision in image data */

    public jpeg_component_info12_16[] comp_info;
    /* comp_info[i] describes component that appears i'th in SOF */

    public boolean arith_code; /* TRUE=arithmetic coding, FALSE=Huffman */

    public byte[] arith_dc_L = new byte[jpeglib12_16.NUM_ARITH_TBLS]; /* L values for DC arith-coding tables */
    public byte[] arith_dc_U = new byte[jpeglib12_16.NUM_ARITH_TBLS]; /* U values for DC arith-coding tables */
    public byte[] arith_ac_K = new byte[jpeglib12_16.NUM_ARITH_TBLS]; /* Kx values for AC arith-coding tables */

    public int restart_interval; /* MCUs per restart interval, or 0 for no restart */

    /* These fields record data obtained from optional markers recognized by
     * the JPEG library.
     */
    public boolean is_lossless;
    public boolean saw_exif_marker; /* true if APP0 marker is found */
    public boolean saw_JFIF_marker; /* TRUE iff a JFIF APP0 marker was found */
    /* Data copied from JFIF marker; only valid if saw_JFIF_marker is TRUE: */
    public byte JFIF_major_version; /* JFIF version number */
    public byte JFIF_minor_version;
    public byte density_unit; /* JFIF code for pixel size units */
    public short X_density; /* Horizontal pixel density */
    public short Y_density; /* Vertical pixel density */
    public boolean saw_Adobe_marker; /* TRUE iff an Adobe APP14 marker was found */
    public byte Adobe_transform; /* Color transform code from Adobe marker */

    public boolean CCIR601_sampling; /* TRUE=first samples are cosited */

    /* Aside from the specific data retained from APPn markers known to the
     * library, the uninterpreted contents of any or all APPn and COM markers
     * can be saved in a list for examination by the application.
     */
    public ArrayList<jpeg_marker12_16> marker_list = new ArrayList<jpeg_marker12_16>(); /* jpeg_saved_marker_ptr: Head of list of saved markers */

    /* Remaining fields are known throughout decompressor, but generally
     * should not be touched by a surrounding application.
     */

    /*
     * These fields are computed during decompression startup
     */
    public int data_unit; /* size of data unit in samples */
    public J_CODEC_PROCESS process; /* decoding process of JPEG image */

    public int max_h_samp_factor; /* largest h_samp_factor */
    public int max_v_samp_factor; /* largest v_samp_factor */

    public int min_codec_data_unit; /* smallest codec_data_unit of any component */

    public int total_iMCU_rows; /* # of iMCU rows in image */
    /* The codec's input and output progress is measured in units of "iMCU"
     * (interleaved MCU) rows.  These are the same as MCU rows in fully
     * interleaved JPEG scans, but are used whether the scan is interleaved
     * or not.  We define an iMCU row as v_samp_factor data_unit rows of each
     * component.  Therefore, the codec output contains
     * v_samp_factor*codec_data_unit sample rows of a component per iMCU row.
     */

    public short[] sample_range_limit; /* table for fast range-limiting */
    // there are places in the C implementation where the pointers to tables is set, 
    // so Java needs an offset to keep track of the pointer arithmetic
    public int sample_range_limit_offset;

    /*
     * These fields are valid during any one scan.
     * They describe the components and MCUs actually appearing in the scan.
     * Note that the decompressor output side must not use these fields.
     */
    public int comps_in_scan; /* # of JPEG components in this scan */
    public jpeg_component_info12_16[] cur_comp_info = new jpeg_component_info12_16[jpeglib12_16.MAX_COMPS_IN_SCAN];
    /* *cur_comp_info[i] describes component that appears i'th in SOS */

    public int MCUs_per_row; /* # of MCUs across the image */
    public int MCU_rows_in_scan; /* # of MCU rows in the image */

    public int data_units_in_MCU; /* # of data _units per MCU */
    public int[] MCU_membership = new int[jpeglib12_16.D_MAX_DATA_UNITS_IN_MCU];
    /* MCU_membership[i] is index in cur_comp_info of component owning */
    /* i'th data unit in an MCU */

    public int Ss, Se, Ah, Al; /* progressive/lossless JPEG parms for scan */

    /* This field is shared between entropy decoder and marker parser.
     * It is either zero or the code of a JPEG marker that has been
     * read from the data source, but has not yet been processed.
     */
    public int unread_marker;

    /*
     * Links to decompression sub-objects (methods, private variables of modules)
     */
    public jpeg_decomp_master12_16 master; // not yet implemented and/or wired in
    public jpeg_d_main_controller12_16 main; // not yet implemented and/or wired in

    // These are the types of decompression routines;
    public jpeg_d_codec12_16 codec; // not yet implemented and/or wired in

    public jpeg_d_post_controller12_16 post; // not yet implemented and/or wired in
    public jpeg_input_controller12_16 inputctl;
    public jpeg_marker_reader12_16 marker;

    // This must be an abstract class, implemented by jdmerge and jdsample
    public jpeg_upsampler12_16 upsample; // not yet implemented and/or wired in

    public jpeg_color_deconverter12_16 cconvert; // not yet implemented and/or wired in
    public jpeg_color_quantizer12_16 cquantize; // not yet implemented and/or wired in

    public jpeg_progress_mgr12_16 progress; // not yet implemented and/or wired in (and seems to be null?)

    // wrapper for the input data
    public jpeg_source_mgr12_16 src;

    // memory manager, (methods are no-op's?) keep to have the code read like DCMTK/IJG
    public jpeg_memory_mgr12_16 mem;

    // error manager
    public jpeg_error_mgr12_16 err;
}
