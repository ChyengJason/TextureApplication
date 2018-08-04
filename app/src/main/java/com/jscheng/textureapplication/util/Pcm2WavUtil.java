package com.jscheng.textureapplication.util;

import android.media.AudioRecord;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Pcm2WavUtil {
    private int mSampleRate;
    //private int mChannels;
    private int minBufferSize;

    public Pcm2WavUtil(int mSampleRate, int mChannels, int mFormater) {
        this.mSampleRate = mSampleRate;
        //this.mChannels = mChannels;
        this.minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannels, mFormater);
    }

    public void pcm2wav(File inFile, File outFile) {
        FileInputStream pcmFileStream;
        FileOutputStream wavFileStream;

        byte[] buffer = new byte[minBufferSize];
        try {
            wavFileStream = new FileOutputStream(inFile);
            pcmFileStream = new FileInputStream(outFile);
            long pcmLen = pcmFileStream.getChannel().size();

            byte[] head = wavHeader(pcmLen, 1, mSampleRate, 16);
            wavFileStream.write(head);

            while(pcmFileStream.available() > 0) {
                int readSize = pcmFileStream.read(buffer);
                wavFileStream.write(buffer);
            }

            pcmFileStream.close();
            wavFileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param pcmLen pcm 数据长度
     * @param numChannels 声道设置, mono = 1, stereo = 2
     * @param sampleRate 采样频率
     * @param bitPerSample 单次数据长度, 例如 8bits
     * @return wav 头部信息
     */
    public static byte[] wavHeader(long pcmLen, int numChannels, int sampleRate, int bitPerSample) {
        byte[] header = new byte[44];
        // ChunkID, RIFF, 占 4bytes
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        // ChunkSize, pcmLen + 36, 占 4bytes
        long chunkSize = pcmLen + 36;
        header[4] = (byte) (chunkSize & 0xff);
        header[5] = (byte) ((chunkSize >> 8) & 0xff);
        header[6] = (byte) ((chunkSize >> 16) & 0xff);
        header[7] = (byte) ((chunkSize >> 24) & 0xff);
        // Format, WAVE, 占 4bytes
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // Subchunk1ID, 'fmt', 占 4bytes
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // Subchunk1Size, 16, 占 4bytes
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // AudioFormat, pcm = 1, 占 2bytes
        header[20] = 1;
        header[21] = 0;
        // NumChannels, mono = 1, stereo = 2, 占 2bytes
        header[22] = (byte) numChannels;
        header[23] = 0;
        // SampleRate, 占 4bytes
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        // ByteRate = SampleRate * NumChannels * BitsPerSample / 8, 占 4bytes
        long byteRate = sampleRate * numChannels * bitPerSample / 8;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // BlockAlign = NumChannels * BitsPerSample / 8, 占 2bytes
        header[32] = (byte) (numChannels * bitPerSample / 8);
        header[33] = 0;
        // BitsPerSample, 占 2bytes
        header[34] = (byte) bitPerSample;
        header[35] = 0;
        // Subhunk2ID, data, 占 4bytes
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        // Subchunk2Size, 占 4bytes
        header[40] = (byte) (pcmLen & 0xff);
        header[41] = (byte) ((pcmLen >> 8) & 0xff);
        header[42] = (byte) ((pcmLen >> 16) & 0xff);
        header[43] = (byte) ((pcmLen >> 24) & 0xff);

        return header;
    }
}
