package jijg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

/*
 * Here's the routine that will replace the standard error_exit method:
 */
public class Main
{
    public static void main(String[] args)
    {
        JIJGDecoder decoder = new JIJGDecoder();

        int bpp = Integer.parseInt(args[0]);
        String dcmFile = args[1];
        byte[] imageData = null;
        try
        {
            imageData = readCompressedData(dcmFile);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        String photometricInterpretation = args[2];
        String outputFile = args[3];
        HashMap<String, Integer> imageCharacteristics = new HashMap<>();

        byte[] decompressed = decoder.decompress(imageData, photometricInterpretation, bpp, imageCharacteristics);
        writeDecompressedData(decompressed, outputFile);

    }

    public static byte[] readCompressedData(String fileName) throws IOException
    {
        RandomAccessFile in = new RandomAccessFile(fileName, "r");
        byte[] rawData = new byte[(int) in.length()];
        in.readFully(rawData);
        in.close();
        return rawData;
    }

    public static void writeDecompressedData(byte[] decompressed, String outputFile)
    {
        try (FileOutputStream fos = new FileOutputStream(outputFile))
        {
            fos.write(decompressed);
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}
