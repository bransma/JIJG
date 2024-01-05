package edu.mayo.jpeg.bit8.structs8;

public abstract class jpeg_input_controller8
{
    /* State variables made visible to other modules */
    public boolean has_multiple_scans; /* True if file has multiple scans */
    public boolean eoi_reached; /* True when EOI has been consumed */
    //
    public boolean inheaders; /* true until first SOS is reached */

    public abstract void reset_input_controller(jpeg_decompress_struct8 cinfo);

    /*
     * Read JPEG markers before, between, or after compressed-data scans.
     * Change state as necessary when a new scan is reached.
     * Return value is JPEG_SUSPENDED, JPEG_REACHED_SOS, or JPEG_REACHED_EOI.
     *
     * The consume_input method pointer points either here or to the
     * coefficient controller's consume_data routine, depending on whether
     * we are reading a compressed data segment or inter-segment markers.
     */

    public abstract int consume_input(jpeg_decompress_struct8 cinfo);

    public abstract void initial_setup(jpeg_decompress_struct8 cinfo);

    public abstract void start_input_pass(jpeg_decompress_struct8 cinfo);

    public abstract void finish_input_pass(jpeg_decompress_struct8 cinfo);
}
