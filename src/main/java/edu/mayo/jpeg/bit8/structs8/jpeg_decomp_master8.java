package edu.mayo.jpeg.bit8.structs8;

public abstract class jpeg_decomp_master8
{
    public boolean is_dummy_pass;
    public int pass_number;
    public boolean using_merged_upsample; /* true if using merged upsample/cconvert */

    /* Saved references to initialized quantizer modules,
     * in case we need to switch modes.
     */
    public jpeg_color_quantizer8 quantizer_1pass;
    public jpeg_color_quantizer8 quantizer_2pass;

    public abstract void prepare_for_output_pass(jpeg_decompress_struct8 cinfo);

    public abstract void finish_output_pass(jpeg_decompress_struct8 cinfo);

    public abstract void jpeg_new_colormap(jpeg_decompress_struct8 cinfo);
}
