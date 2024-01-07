package bransford.jpeg.bit8.structs8;

import bransford.jpeg.bit8.dct8.jidctflt8;
import bransford.jpeg.bit8.dct8.jidctfst8;
import bransford.jpeg.bit8.dct8.jidctint8;
import bransford.jpeg.bit8.dct8.jidctred8;
import bransford.jpeg.bit8.jpeglib8;
import bransford.jpeg.bit8.jutils8;

public abstract class idct_controller8
{
    public boolean PROVIDE_ISLOW_TABLES = false;

    public jidctflt8 idctflt = new jidctflt8();
    public jidctfst8 idctfst = new jidctfst8();
    public jidctint8 idctint = new jidctint8();
    public jidctred8 idctred = new jidctred8();

    /* The current scaled-IDCT routines require ISLOW-style multiplier tables,
     * so be sure to compile that code if either ISLOW or SCALING is requested.
     */
    public enum inverse_DCT_method_ptr
    {
        jpeg_idct_islow, jpeg_idct_ifast, jpeg_idct_float, jpeg_idct_4x4, jpeg_idct_2x2, jpeg_idct_1x1

    }

    public static int CONST_BITS = 14;
    public static int IFAST_SCALE_BITS = 2;

    public static short[] aanscales = new short[]{
            /* precomputed values scaled up by 14 bits */
            16384, 22725, 21407, 19266, 16384, 12873, 8867, 4520, 22725, 31521, 29692, 26722, 22725, 17855, 12299, 6270, 21407,
            29692, 27969, 25172, 21407, 16819, 11585, 5906, 19266, 26722, 25172, 22654, 19266, 15137, 10426, 5315, 16384, 22725,
            21407, 19266, 16384, 12873, 8867, 4520, 12873, 17855, 16819, 15137, 12873, 10114, 6967, 3552, 8867, 12299, 11585, 10426,
            8867, 6967, 4799, 2446, 4520, 6270, 5906, 5315, 4520, 3552, 2446, 1247};

    public static double[] aanscalefactor = new double[]{1.0, 1.387039845, 1.306562965, 1.175875602, 1.0, 0.785694958,
            0.541196100, 0.275899379};

    public jpeglib8.J_DCT_METHOD[] cur_method = new jpeglib8.J_DCT_METHOD[jpeglib8.MAX_COMPONENTS];

    public abstract void start_pass(jpeg_decompress_struct8 cinfo);

    public abstract void jinit_inverse_dct(jpeg_decompress_struct8 cinfo);

    //public abstract void inverse_DCT(jpeg_decompress_struct cinfo, jpeg_component_info compptr, JCOEFPTR coef_block, JSAMPARRAY output_buf, JDIMENSION output_col);
    // add method signatures for fdct methods, in addition to idct (e.g. jdct8.h line 84)
    public abstract void inverse_DCT(jpeg_decompress_struct8 cinfo, jpeg_component_info8 compptr, short[] coef_block,
                                     byte[][] output_buf, int output_buff_offset, int output_col, inverse_DCT_method_ptr method_ptr);

    public static int ONE = 1;
    public static int CONST_SCALE = ONE << CONST_BITS;

    /* Convert a positive real constant to an integer scaled by CONST_SCALE.
     * Caution: some C compilers fail to reduce "FIX(constant)" at compile time,
     * thus causing a lot of useless floating-point operations at run time.
     */

    public static int FIX(int x)
    {
        return ((int) ((x) * CONST_SCALE + 0.5));
    }

    /* Descale and correctly round an IJG_INT32 value that's scaled by N bits.
     * We assume RIGHT_SHIFT rounds towards minus infinity, so adding
     * the fudge factor is correct for either sign of X.
     */

    public static int DESCALE(int x, int n)
    {
        return jutils8.RIGHT_SHIFT((x) + (ONE << ((n) - 1)), n);
    }

    /* Multiply an IJG_INT32 variable by an IJG_INT32 constant to yield an IJG_INT32 result.
     * This macro is used only when the two inputs will actually be no more than
     * 16 bits wide, so that a 16x16->32 bit multiply can be used instead of a
     * full 32x32 multiply.  This provides a useful speedup on many machines.
     * Unfortunately there is no way to specify a 16x16->32 multiply portably
     * in C, but some C compilers will do the right thing if you provide the
     * correct combination of casts.
     */

    /* may work if 'int' is 32 bits */
    public static short MULTIPLY16C16(short var, short a)
    {
        return (short) (var * a);
    }

    //    /* known to work with Microsoft C 6.0 */
    //    public static short MULTIPLY16C16(short var, short a)  
    //    {
    //	return (short) (var * (long)a);
    //    }
    //
    //    /* default definition */
    //    public static short MULTIPLY16C16(int var, int a) 
    //    {
    //	return MULTIPLY16C16(var, a);  
    //    }

    /* Same except both inputs are variables. */

    /* may work if 'int' is 32 bits */
    public static short MULTIPLY16V16(short var1, short var2)
    {
        return MULTIPLY16C16(var1, var2);
    }

    //    /* default definition */
    //    public static short MULTIPLY16V16(short var1, short var2) {
    //	return MULTIPLY16C16(var1, var2);  
    //    }
}
