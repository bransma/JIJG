package bransford.jpeg.bit8.structs8;

import bransford.jpeg.bit8.jpeglib8;

public class d_coef_controller8
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
    public short[][] MCU_buffer = new short[jpeglib8.D_MAX_DATA_UNITS_IN_MCU][jpeglib8.DCTSIZE2];

    //  #ifdef D_MULTISCAN_FILES_SUPPORTED
    /* In multi-pass modes, we need a virtual block array for each component. */
    public jvirt_barray_control8[] whole_image = new jvirt_barray_control8[jpeglib8.MAX_COMPONENTS];
    //  #endif

    //  #ifdef BLOCK_SMOOTHING_SUPPORTED
    /* When doing block smoothing, we latch coefficient Al values here */
    public int[] coef_bits_latch;
}
