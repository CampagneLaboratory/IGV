package org.broad.igv.hic.tools;

import org.broad.igv.hic.AlignmentsParser;
import org.broad.igv.hic.data.Block;
import org.broad.igv.hic.data.ContactRecord;
import org.broad.igv.hic.data.Matrix;
import org.broad.igv.hic.data.MatrixZoomData;
import org.broad.igv.util.CompressionUtils;
import org.broad.tribble.util.LittleEndianOutputStream;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jrobinso
 * @date Aug 16, 2010
 */
public class Preprocessor {


    File outputFile;
    LittleEndianOutputStream fos;
    long bytesWritten = 0;

    long masterIndexPosition;
    Map<String, IndexEntry> matrixPositions = new LinkedHashMap();
    Map<String, Long> blockIndexPositions = new LinkedHashMap();
    Map<String, IndexEntry[]> blockIndexMap = new LinkedHashMap();


    public Preprocessor(File outputFile) {

        this.outputFile = outputFile;

    }


    public void preprocess(File inputFile, String genomeId) throws IOException {

        FileInputStream fis = null;

        try {
            fos = new LittleEndianOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));

            // Placeholder for master index position, replace later
            writeLong(0l);

            // Chromosome dictionary
            int nChrs = HiCTools.chromosomes.length;
            writeInt(nChrs);
            for (int i = 0; i < nChrs; i++) {
                writeString(HiCTools.chromosomes[i].getName());
                writeInt(HiCTools.chromosomes[i].getSize());
            }

            for (int c1 = 0; c1 < 7; c1++) {
                for (int c2 = c1; c2 < 7; c2++) {
                    fis = new FileInputStream(inputFile);
                    Matrix matrix = AlignmentsParser.readMatrix(fis, c1, c2);
                    System.out.println("writing matrix: " + matrix.getKey());
                    writeMatrix(matrix);
                    fis.close();
                }
            }
            masterIndexPosition = bytesWritten;
            writeMasterIndex();
        } finally {
            fos.close();
        }

        updateIndexPositions();
    }


    public void updateIndexPositions() throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(outputFile, "rw");

            // Master index -- first entry in file (change later)
            raf.getChannel().position(0);
            BufferedByteWriter buffer = new BufferedByteWriter();
            buffer.putLong(masterIndexPosition);
            raf.write(buffer.getBytes());

            // Block indeces
            for (String key : blockIndexPositions.keySet()) {
                long pos = blockIndexPositions.get(key);
                IndexEntry[] blockIndex = blockIndexMap.get(key);

                raf.getChannel().position(pos);

                // Write as little endian
                buffer = new BufferedByteWriter();
                for (int i = 0; i < blockIndex.length; i++) {
                    buffer.putInt(blockIndex[i].id);
                    buffer.putLong(blockIndex[i].position);
                    buffer.putInt(blockIndex[i].size);
                }
                raf.write(buffer.getBytes());
            }
        } finally {
            if (raf != null) raf.close();
        }
    }

    public void writeMasterIndex() throws IOException {

        BufferedByteWriter buffer = new BufferedByteWriter();
        buffer.putInt(matrixPositions.size());
        for (Map.Entry<String, IndexEntry> entry : matrixPositions.entrySet()) {
            buffer.putString(entry.getKey());
            buffer.putLong(entry.getValue().position);
            buffer.putInt(entry.getValue().size);
        }
        byte[] bytes = buffer.getBytes();

        writeInt(bytes.length);
        write(bytes);
    }

    public void writeMatrix(Matrix matrix) throws IOException {

        long position = bytesWritten;
        writeInt(matrix.chr1);
        writeInt(matrix.chr2);
        writeInt(matrix.zoomData.length);
        for (MatrixZoomData zd : matrix.zoomData) {
            writeZoomHeader(zd);
        }
        int size = (int) (bytesWritten - position);
        matrixPositions.put(matrix.getKey(), new IndexEntry(position, size));


        for (MatrixZoomData zd : matrix.zoomData) {
            IndexEntry[] blockIndex = writeZoomData(zd);
            blockIndexMap.put(getBlockKey(zd), blockIndex);
        }
    }

    private String getBlockKey(MatrixZoomData zd) {
        return zd.getChr1() + "_" + zd.getChr2() + "_" + zd.getZoom();
    }

    private void writeZoomHeader(MatrixZoomData zd) throws IOException {

        writeInt(zd.getZoom());
        writeInt(zd.getBinSize());
        writeInt(zd.getBlockSize());
        writeInt(zd.getBlockColumnCount());

        final Map<Integer, Block> blocks = zd.getBlocks();
        writeInt(blocks.size());
        blockIndexPositions.put(getBlockKey(zd), bytesWritten);

        // Placeholder for block index
        for (int i = 0; i < zd.getBlocks().size(); i++) {
            writeInt(0);
            writeLong(0l);
            writeInt(0);
        }

    }

    private IndexEntry[] writeZoomData(MatrixZoomData zd) throws IOException {

        final Map<Integer, Block> blocks = zd.getBlocks();

        IndexEntry[] indexEntries = new IndexEntry[blocks.size()];
        int i = 0;
        for (Map.Entry<Integer, Block> entry : blocks.entrySet()) {

            int blockNumber = entry.getKey().intValue();
            Block block = entry.getValue();

            long position = bytesWritten;
            writeContactRecords(block);
            int size = (int) (bytesWritten - position);

            indexEntries[i] = new IndexEntry(blockNumber, position, size);
            i++;
        }
        return indexEntries;

    }

    /**
     * Note -- compressed
     *
     * @param block
     * @throws IOException
     */
    private void writeContactRecords(Block block) throws IOException {

        final ContactRecord[] records = block.getContactRecords();
        final int len = records.length;

        BufferedByteWriter buffer = new BufferedByteWriter(len * 12);

        buffer.putInt(len);
        for (int i = 0; i < len; i++) {
            ContactRecord rec = records[i];
            buffer.putInt(rec.getX());
            buffer.putInt(rec.getY());
            buffer.putShort(rec.getCounts());
        }

        byte[] bytes = buffer.getBytes();
        byte[] compressedBytes = CompressionUtils.compress(bytes);
        write(compressedBytes);

    }


    private void writeInt(int v) throws IOException {
        fos.writeInt(v);
        bytesWritten += 4;
    }

    private void writeShort(short v) throws IOException {

        fos.writeShort(v);
        bytesWritten += 2;
    }

    public void writeLong(long v) throws IOException {
        fos.writeLong(v);
        bytesWritten += 8;
    }

    private void write(byte[] bytes) throws IOException {
        fos.write(bytes);
        bytesWritten += bytes.length;
    }

    private void writeString(String string) throws IOException {
        byte[] bytes = string.getBytes();
        write(bytes);
        fos.write((byte) 0);
        bytesWritten++;
    }


    static public class BufferedByteWriter {

        ByteArrayOutputStream buffer;
        LittleEndianOutputStream dos;

        public BufferedByteWriter() {
            this(8192);
        }


        public BufferedByteWriter(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("Buffer size <= 0");
            }
            buffer = new ByteArrayOutputStream(size);
            dos = new LittleEndianOutputStream(buffer);
        }

        public byte[] getBytes() {
            return buffer.toByteArray();
        }

        private void put(byte[] b) throws IOException {
            dos.write(b);
        }

        private void put(byte b) throws IOException {
            dos.write(b);
        }

        private void putShort(short v) throws IOException {

            dos.writeShort(v);
        }

        public void putInt(int v) throws IOException {
            dos.writeInt(v);
        }


        public void putLong(long v) throws IOException {
            dos.writeLong(v);
        }

        public void putString(String string) throws IOException {
            dos.writeString(string);
        }

    }


    public static class IndexEntry {
        int id;
        public long position;
        public int size;

        IndexEntry(int id, long position, int size) {
            this.id = id;
            this.position = position;
            this.size = size;
        }

        public IndexEntry(long position, int size) {
            this.position = position;
            this.size = size;
        }
    }


    // Example usage
    public static void main(String[] args) throws Exception {

        String inputFile = args[0];   // "test/data/test.summary.binned.sorted.txt";
        String outputFile = args[1];  //    "test/data/test.summary.binned.sorted.hic";

        Preprocessor writer = new Preprocessor(new File(outputFile));
        writer.preprocess(new File(inputFile), "dmel");

    }

}