package edu.mayo.jpeg.bit8;

public final class jutils8
{
    public static final int[] jpeg_zigzag_order = new int[]{
            0, 1, 5, 6, 14, 15, 27, 28,
            2, 4, 7, 13, 16, 26, 29, 42,
            3, 8, 12, 17, 25, 30, 41, 43,
            9, 11, 18, 24, 31, 40, 44, 53,
            10, 19, 23, 32, 39, 45, 52, 54,
            20, 22, 33, 38, 46, 51, 55, 60,
            21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63};

    /*
     * jpeg_natural_order[i] is the natural-order position of the i'th element
     * of zigzag order.
     *
     * When reading corrupted data, the Huffman decoders could attempt
     * to reference an entry beyond the end of this array (if the decoded
     * zero run length reaches past the end of the block).  To prevent
     * wild stores without adding an inner-loop test, we put some extra
     * "63"s after the real entries.  This will cause the extra coefficient
     * to be stored in location 63 of the block, not somewhere random.
     * The worst case would be a run-length of 15, which means we need 16
     * fake entries.
     */

    public static final int[] jpeg_natural_order = new int[]{
            0, 1, 8, 16, 9, 2, 3, 10,
            17, 24, 32, 25, 18, 11, 4, 5,
            12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13, 6, 7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36,
            29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63,
            63, 63, 63, 63, 63, 63, 63, 63, /* extra entries for safety in decoder */
            63, 63, 63, 63, 63, 63, 63, 63
    };

    public static long jdiv_round_up(long a, long b)
        /* Compute a/b rounded up to next integer, ie, ceil(a/b) */
        /* Assumes a >= 0, b > 0 */
    {
        return (a + b - 1L) / b;
    }

    public static long jround_up(long a, long b)
        /* Compute a rounded up to next multiple of b, ie, ceil(a/b)*b */
        /* Assumes a >= 0, b > 0 */
    {
        a += b - 1L;
        return a - (a % b);
    }

    public static int RIGHT_SHIFT(int x, int shft)
    {
        return x >> shft;
    }

    /* Copy some rows of samples from one place to another.
     * num_rows rows are copied from input_array[source_row++]
     * to output_array[dest_row++]; these areas may overlap for duplication.
     * The source and destination arrays must be at least as wide as num_cols.
     */
    public static void jcopy_sample_rows(byte[][] input_array, int source_row, byte[][] output_array, int dest_row,
                                        int num_rows, int num_cols)
    {
        byte[] inptr, outptr;
        int row;
        int input_array_offset = source_row;
        int output_array_offset = dest_row;

        for (row = num_rows; row > 0; row--)
        {
            inptr = input_array[input_array_offset++];
            outptr = output_array[output_array_offset++];
            System.arraycopy(inptr, 0, outptr, 0, num_cols);
        }
    }
}
