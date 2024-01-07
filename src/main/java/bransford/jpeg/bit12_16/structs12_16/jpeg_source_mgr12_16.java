package bransford.jpeg.bit12_16.structs12_16;

public class jpeg_source_mgr12_16
{
    public static final int ADVANCE_1BYTE = 1;
    public static final int ADVANCE_2BYTES = 2;

    public int next_input_byte; /* => next byte to read from buffer. In the C-world this was the actual byte value, but in Java is the index to that value*/
    public int bytes_in_buffer; /* # of bytes remaining in buffer */

    public byte[] byteBuffer;

    //public int indexToByteBuffer = 0;

    public jpeg_source_mgr12_16(byte[] src)
    {
        this.byteBuffer = src;
        this.next_input_byte = 0;
        this.bytes_in_buffer = src == null ? 0 : src.length;
    }

    public byte nextByte(int index)
    {
        if (bytes_in_buffer == 0)
        {
            return 0;
        }
        else
        {
            return byteBuffer[index];
        }
    }

    public void term_source(jpeg_decompress_struct12_16 cinfo)
    {
        // no-op
    }

    public void resync_to_restart(jpeg_decompress_struct12_16 cinfo, int desired)
    {
        // no-op
    }

    public boolean fill_input_buffer(jpeg_decompress_struct12_16 cinfo)
    {
        //no-op b/c using a full in-memory buffer
        return true;
    }

    public void init_source(jpeg_decompress_struct12_16 cinfo)
    {
        // handled by constructor
    }

    public int INPUT_BYTE()
    {
        return GETJOCTET(ADVANCE_1BYTE);
    }

    public int INPUT_2BYTES()
    {
        int V = GETJOCTET(ADVANCE_1BYTE) << 8;
        V += GETJOCTET(ADVANCE_1BYTE);
        return V;
    }

    public int GETJOCTET(int numToAdvance)
    {
        int joctet = byteBuffer[next_input_byte] & 0xFF;
        next_input_byte += numToAdvance;
        bytes_in_buffer -= numToAdvance;

        return joctet;
    }

    public void skip_input_data(int numberToSkip)
    {
        next_input_byte += numberToSkip;
        bytes_in_buffer -= numberToSkip;
    }
}
