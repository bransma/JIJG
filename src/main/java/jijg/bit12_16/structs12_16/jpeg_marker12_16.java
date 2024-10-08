package jijg.bit12_16.structs12_16;

public class jpeg_marker12_16
{
    // used (primarily) for writing markers, after compression
    public jpeg_marker12_16 next; /* next in list, or NULL */
    public byte marker; /* marker code: JPEG_COM, or JPEG_APP0+n */
    public int original_length; /* # bytes of data in the file */
    public int data_length; /* # bytes of data saved at data[] */
    public byte data; /* the data contained in the marker */
    /* the marker length word is not counted in data_length or original_length */
}
