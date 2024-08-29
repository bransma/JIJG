package jijg.bit12_16.structs12_16;

import jijg.bit12_16.jpeglib12_16;

public class d_coef_controller12_16
{
    public static final int SAVED_COEFS = 6; /* we save coef_bits[0..5] */

    /* These variables keep track of the current location of the input side. */
    /* cinfo.input_iMCU_row is also used for this. */
    public int MCU_ctr; /* counts MCUs processed in current row */
    public int MCU_vert_offset; /* counts MCU rows within iMCU row */
    public int MCU_rows_per_iMCU_row; /* number of such rows needed */

    /* The output side's location is represented by cinfo.output_iMCU_row. */

    /* In single-pass modes, it's sufficient to buffer just one MCU.
     * We allocate a workspace of D_MAX_DATA_UNITS_IN_MCU coefficient blocks,
     * and let the entropy decoder write into that workspace each time.
     * (On 80x86, the workspace is FAR even though it's not really very big;
     * this is to keep the module interfaces unchanged when a large coefficient
     * buffer is necessary.)
     * In multi-pass modes, this array points to the current MCU's blocks
     * within the virtual arrays; it is used only by the input side.
     */
    //JBLOCKROW MCU_buffer[D_MAX_DATA_UNITS_IN_MCU];
    // MCU_buffer is an array of length D_MAX_DATA_UNITS_IN_MCU OF block rows, hence is a short[] (-> JBLOCKROW) or short[jpeglib.D_MAX_DATA_UNITS_IN_MCU][jpeglib.DCTSIZE2]
    public short[][] MCU_buffer = new short[jpeglib12_16.D_MAX_DATA_UNITS_IN_MCU][jpeglib12_16.DCTSIZE2];

    //  #ifdef D_MULTISCAN_FILES_SUPPORTED
    /* In multi-pass modes, we need a virtual block array for each component. */
    public jvirt_barray_control12_16[] whole_image = new jvirt_barray_control12_16[jpeglib12_16.MAX_COMPONENTS];
    //  #endif

    //  #ifdef BLOCK_SMOOTHING_SUPPORTED
    /* When doing block smoothing, we latch coefficient Al values here */
    public int[] coef_bits_latch;
}
