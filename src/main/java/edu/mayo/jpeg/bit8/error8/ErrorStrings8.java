package edu.mayo.jpeg.bit8.error8;

public class ErrorStrings8
{
    public static final String JERR_ARITH_NOTIMPL = "Sorry, there are legal restrictions on arithmetic coding";
    public static final String JERR_BAD_ALIGN_TYPE = "ALIGN_TYPE is wrong, please fix";
    public static final String JERR_BAD_ALLOC_CHUNK = "MAX_ALLOC_CHUNK is wrong, please fix";
    public static final String JERR_BAD_BUFFER_MODE = "Bogus buffer control mode";
    public static final String JERR_BAD_COMPONENT_ID = "Invalid component ID %d in SOS";
    public static final String JERR_BAD_DCT_COEF = "DCT coefficient out of range";
    public static final String JERR_BAD_DCTSIZE = "IDCT output block size %d not supported";
    public static final String JERR_BAD_DIFF = "spatial difference out of range";
    public static final String JERR_BAD_HUFF_TABLE = "Bogus Huffman table definition";
    public static final String JERR_BAD_IN_COLORSPACE = "Bogus input colorspace";
    public static final String JERR_BAD_J_COLORSPACE = "Bogus JPEG colorspace";
    public static final String JERR_BAD_LENGTH = "Bogus marker length";
    public static final String JERR_BAD_LIB_VERSION = "Wrong JPEG library version: library is %d, caller expects %d";
    public static final String JERR_BAD_LOSSLESS = "Invalid lossless parameters Ss=%d Se=%d Ah=%d Al=%d";
    public static final String JERR_BAD_LOSSLESS_SCRIPT = "Invalid lossless parameters at scan script entry %d";
    public static final String JERR_BAD_MCU_SIZE = "Sampling factors too large for interleaved scan";
    public static final String JERR_BAD_POOL_ID = "Invalid memory pool code %d";
    public static final String JERR_BAD_PRECISION = "Unsupported JPEG data precision %d";
    public static final String JERR_BAD_PROGRESSION = "Invalid progressive parameters Ss=%d Se=%d Ah=%d Al=%d";
    public static final String JERR_BAD_PROG_SCRIPT = "Invalid progressive parameters at scan script entry %d";
    public static final String JERR_BAD_RESTART = "Invalid restart interval: %d, must be an integer multiple of the number of MCUs in an MCU_row (%d;";
    public static final String JERR_BAD_SAMPLING = "Bogus sampling factors";
    public static final String JERR_BAD_SCAN_SCRIPT = "Invalid scan script at entry %d";
    public static final String JERR_BAD_STATE = "Improper call to JPEG library in state %d";
    public static final String JERR_BAD_STRUCT_SIZE = "JPEG parameter struct mismatch: library thinks size is %u, caller expects %u";
    public static final String JERR_BAD_VIRTUAL_ACCESS = "Bogus virtual array access";
    public static final String JERR_BUFFER_SIZE = "Buffer passed to JPEG library is too small";
    public static final String JERR_CANT_SUSPEND = "Suspension not allowed here";
    public static final String JERR_CANT_TRANSCODE = "Cannot transcode to/from lossless JPEG datastreams";
    public static final String JERR_CCIR601_NOTIMPL = "CCIR601 sampling not implemented yet";
    public static final String JERR_COMPONENT_COUNT = "Too many color components: %d, max %d";
    public static final String JERR_CONVERSION_NOTIMPL = "Unsupported color conversion request";
    public static final String JERR_DAC_INDEX = "Bogus DAC index %d";
    public static final String JERR_DAC_VALUE = "Bogus DAC value 0x%x";
    public static final String JERR_DHT_INDEX = "Bogus DHT index %d";
    public static final String JERR_DQT_INDEX = "Bogus DQT index %d";
    public static final String JERR_EMPTY_IMAGE = "Empty JPEG image (DNL not supported;";
    public static final String JERR_EMS_READ = "Read from EMS failed";
    public static final String JERR_EMS_WRITE = "Write to EMS failed";
    public static final String JERR_EOI_EXPECTED = "Didn't expect more than one scan";
    public static final String JERR_FILE_READ = "Input file read error";
    public static final String JERR_FILE_WRITE = "Output file write error --- out of disk space?";
    public static final String JERR_FRACT_SAMPLE_NOTIMPL = "Fractional sampling not implemented yet";
    public static final String JERR_HUFF_CLEN_OVERFLOW = "Huffman code size table overflow";
    public static final String JERR_HUFF_MISSING_CODE = "Missing Huffman code table entry";
    public static final String JERR_IMAGE_TOO_BIG = "Maximum supported image dimension is %u pixels";
    public static final String JERR_INPUT_EMPTY = "Empty input file";
    public static final String JERR_INPUT_EOF = "Premature end of input file";
    public static final String JERR_MISMATCHED_QUANT_TABLE = "Cannot transcode due to multiple use of quantization table %d";
    public static final String JERR_MISSING_DATA = "Scan script does not transmit all data";
    public static final String JERR_MODE_CHANGE = "Invalid color quantization mode change";
    public static final String JERR_NOTIMPL = "Not implemented yet";
    public static final String JERR_NOT_COMPILED = "Requested feature was omitted at compile time";
    public static final String JERR_NO_ARITH_TABLE = "Arithmetic table 0x%02x was not defined";
    public static final String JERR_NO_BACKING_STORE = "Backing store not supported";
    public static final String JERR_NO_HUFF_TABLE = "Huffman table 0x%02x was not defined";
    public static final String JERR_NO_IMAGE = "JPEG datastream contains no image";
    public static final String JERR_NO_LOSSLESS_SCRIPT = "Lossless encoding was requested but no scan script was supplied";
    public static final String JERR_NO_QUANT_TABLE = "Quantization table 0x%02x was not defined";
    public static final String JERR_NO_SOI = "Not a JPEG file: starts with 0x%02x 0x%02x";
    public static final String JERR_OUT_OF_MEMORY = "Insufficient memory (case %d;";
    public static final String JERR_QUANT_COMPONENTS = "Cannot quantize more than %d color components";
    public static final String JERR_QUANT_FEW_COLORS = "Cannot quantize to fewer than %d colors";
    public static final String JERR_QUANT_MANY_COLORS = "Cannot quantize to more than %d colors";
    public static final String JERR_SOF_DUPLICATE = "Invalid JPEG file structure: two SOF markers";
    public static final String JERR_SOF_NO_SOS = "Invalid JPEG file structure: missing SOS marker";
    public static final String JERR_SOF_UNSUPPORTED = "Unsupported JPEG process: SOF type 0x%02x";
    public static final String JERR_SOI_DUPLICATE = "Invalid JPEG file structure: two SOI markers";
    public static final String JERR_SOS_NO_SOF = "Invalid JPEG file structure: SOS before SOF";
    public static final String JERR_TFILE_CREATE = "Failed to create temporary file %s";
    public static final String JERR_TFILE_READ = "Read failed on temporary file";
    public static final String JERR_TFILE_SEEK = "Seek failed on temporary file";
    public static final String JERR_TFILE_WRITE = "Write failed on temporary file --- out of disk space?";
    public static final String JERR_TOO_LITTLE_DATA = "Application transferred too few scanlines";
    public static final String JERR_UNKNOWN_MARKER = "Unsupported marker type 0x%02x";
    public static final String JERR_VIRTUAL_BUG = "Virtual array controller messed up";
    public static final String JERR_WIDTH_OVERFLOW = "Image too wide for this implementation";
    public static final String JERR_XMS_READ = "Read from XMS failed";
    public static final String JERR_XMS_WRITE = "Write to XMS failed";
    public static final String JMSG_COPYRIGHT = "JCOPYRIGHT";
    public static final String JMSG_VERSION = "JVERSION";
    public static final String JTRC_16BIT_TABLES = "Caution: quantization tables are too coarse for baseline JPEG";
    public static final String JTRC_ADOBE = "Adobe APP14 marker: version %d, flags 0x%04x 0x%04x, transform %d";
    public static final String JTRC_APP0 = "Unknown APP0 marker (not JFIF;, length %u";
    public static final String JTRC_APP14 = "Unknown APP14 marker (not Adobe;, length %u";
    public static final String JTRC_DAC = "Define Arithmetic Table 0x%02x: 0x%02x";
    public static final String JTRC_DHT = "Define Huffman Table 0x%02x";
    public static final String JTRC_DQT = "Define Quantization Table %d  precision %d";
    public static final String JTRC_DRI = "Define Restart Interval %u";
    public static final String JTRC_EMS_CLOSE = "Freed EMS handle %u";
    public static final String JTRC_EMS_OPEN = "Obtained EMS handle %u";
    public static final String JTRC_EOI = "End Of Image";
    public static final String JTRC_HUFFBITS = "        %3d %3d %3d %3d %3d %3d %3d %3d";
    public static final String JTRC_JFIF = "JFIF APP0 marker: version %d.%02d, density %dx%d  %d";
    public static final String JTRC_JFIF_BADTHUMBNAILSIZE = "Warning: thumbnail image size does not match data length %u";
    public static final String JTRC_JFIF_EXTENSION = "JFIF extension marker: type 0x%02x, length %u";
    public static final String JTRC_JFIF_THUMBNAIL = "    with %d x %d thumbnail image";
    public static final String JTRC_MISC_MARKER = "Miscellaneous marker 0x%02x, length %u";
    public static final String JTRC_PARMLESS_MARKER = "Unexpected marker 0x%02x";
    public static final String JTRC_QUANTVALS = "        %4u %4u %4u %4u %4u %4u %4u %4u";
    public static final String JTRC_QUANT_3_NCOLORS = "Quantizing to %d = %d*%d*%d colors";
    public static final String JTRC_QUANT_NCOLORS = "Quantizing to %d colors";
    public static final String JTRC_QUANT_SELECTED = "Selected %d colors for quantization";
    public static final String JTRC_RECOVERY_ACTION = "At marker 0x%02x, recovery action %d";
    public static final String JTRC_RST = "RST%d";
    public static final String JTRC_SMOOTH_NOTIMPL = "Smoothing not supported with nonstandard sampling ratios";
    public static final String JTRC_SOF = "Start Of Frame 0x%02x: width=%u, height=%u, components=%d";
    public static final String JTRC_SOF_COMPONENT = "    Component %d: %dhx%dv q=%d";
    public static final String JTRC_SOI = "Start of Image";
    public static final String JTRC_SOS = "Start Of Scan: %d components";
    public static final String JTRC_SOS_COMPONENT = "    Component %d: dc=%d ac=%d";
    public static final String JTRC_SOS_PARAMS = "  Ss=%d, Se=%d, Ah=%d, Al=%d";
    public static final String JTRC_TFILE_CLOSE = "Closed temporary file %s";
    public static final String JTRC_TFILE_OPEN = "Opened temporary file %s";
    public static final String JTRC_THUMB_JPEG = "JFIF extension marker: JPEG-compressed thumbnail image, length %u";
    public static final String JTRC_THUMB_PALETTE = "JFIF extension marker: palette thumbnail image, length %u";
    public static final String JTRC_THUMB_RGB = "JFIF extension marker: RGB thumbnail image, length %u";
    public static final String JTRC_UNKNOWN_LOSSLESS_IDS = "Unrecognized component IDs %d %d %d, assuming RGB";
    public static final String JTRC_UNKNOWN_LOSSY_IDS = "Unrecognized component IDs %d %d %d, assuming YCbCr";
    public static final String JTRC_XMS_CLOSE = "Freed XMS handle %u";
    public static final String JTRC_XMS_OPEN = "Obtained XMS handle %u";
    public static final String JWRN_ADOBE_XFORM = "Unknown Adobe color transform code %d";
    public static final String JWRN_ARITH_BAD_CODE = "Corrupt JPEG data: bad arithmetic code";
    public static final String JWRN_BOGUS_PROGRESSION = "Inconsistent progression sequence for component %d coefficient %d";
    public static final String JWRN_EXTRANEOUS_DATA = "Corrupt JPEG data: %u extraneous bytes before marker 0x%02x";
    public static final String JWRN_HIT_MARKER = "Corrupt JPEG data: premature end of data segment";
    public static final String JWRN_HUFF_BAD_CODE = "Corrupt JPEG data: bad Huffman code";
    public static final String JWRN_JFIF_MAJOR = "Warning: unknown JFIF revision number %d.%02d";
    public static final String JWRN_JPEG_EOF = "Premature end of JPEG file";
    public static final String JWRN_MUST_DOWNSCALE = "Must downscale data from %d bits to %d";
    public static final String JWRN_MUST_RESYNC = "Corrupt JPEG data: found marker 0x%02x instead of RST%d";
    public static final String JWRN_NOT_SEQUENTIAL = "Invalid SOS parameters for sequential JPEG";
    public static final String JWRN_TOO_MUCH_DATA = "Application transferred too many scanlines";
}
