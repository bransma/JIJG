package jijg.bit8.structs8;

public class jpeg_component_info8
{
    /* These values are fixed over the whole image. */
    /* For compression, they must be supplied by parameter setup; */
    /* for decompression, they are read from the SOF marker. */
    public int component_id; /* identifier for this component (0..255) */
    public int component_index; /* its index in SOF or cinfo->comp_info[] */
    public int h_samp_factor; /* horizontal sampling factor (1..4) */
    public int v_samp_factor; /* vertical sampling factor (1..4) */
    public int quant_tbl_no; /* quantization table selector (0..3) */
    /* These values may vary between scans. */
    /* For compression, they must be supplied by parameter setup; */
    /* for decompression, they are read from the SOS marker. */
    /* The decompressor output side may not use these variables. */
    public int dc_tbl_no; /* DC entropy table selector (0..3) */
    public int ac_tbl_no; /* AC entropy table selector (0..3) */

    /* Remaining fields should be treated as private by applications. */

    /* These values are computed during compression or decompression startup: */
    /* Component's size in data units.
     * Any dummy data units added to complete an MCU are not counted; therefore
     * these values do not depend on whether a scan is interleaved or not.
     */
    public int width_in_data_units;
    public int height_in_data_units;
    /* Size of a data unit in/output by the codec (in samples).  Always
     * data_unit for compression.  For decompression this is the size of the
     * output from one data_unit, reflecting any processing performed by the
     * codec.  For example, in the DCT-based codec, scaling may be applied
     * during the IDCT step.  Values of 1,2,4,8 are likely to be supported.
     * Note that different components may have different codec_data_unit sizes.
     */
    public int codec_data_unit;
    /* The downsampled dimensions are the component's actual, unpadded number
     * of samples at the main buffer (preprocessing/compression interface), thus
     * downsampled_width = ceil(image_width * Hi/Hmax)
     * and similarly for height.  For decompression, codec-based processing is
     * included (ie, IDCT scaling), so
     * downsampled_width = ceil(image_width * Hi/Hmax * codec_data_unit/data_unit)
     */
    public int downsampled_width; /* actual width in samples */
    public int downsampled_height; /* actual height in samples */
    /* This flag is used only for decompression.  In cases where some of the
     * components will be ignored (eg grayscale output from YCbCr image),
     * we can skip most computations for the unused components.
     */
    public boolean component_needed; /* do we need the value of this component? */

    /* These values are computed before starting a scan of the component. */
    /* The decompressor output side may not use these variables. */
    public int MCU_width; /* number of data units per MCU, horizontally */
    public int MCU_height; /* number of data units per MCU, vertically */
    public int MCU_data_units; /* MCU_width * MCU_height */
    public int MCU_sample_width; /* MCU width in samples, MCU_width*codec_data_unit */
    public int last_col_width; /* # of non-dummy data_units across in last MCU */
    public int last_row_height; /* # of non-dummy data_units down in last MCU */

    /* Saved quantization table for component; NULL if none yet saved.
     * See jdinput.c comments about the need for this information.
     * This field is currently used only for decompression.
     */
    public JQUANT_TBL8 quant_table;

    /* Private per-component storage for DCT or IDCT subsystem. */
    public int[] dct_table;
    public float[] dct_table_f;
}
