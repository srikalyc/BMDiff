/**
 * Copyright 2012 Yahoo! Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License. 
 * See accompanying LICENSE file.
 */
package bmdiff;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Bentley and McIlroy "Data compression using long common strings"
 *
 * @author srikchan
 */
public class BMDiffCodec {

    public Map<Long, Integer> fingerPrintTable = new HashMap<>();
    private long[] cachedPows;// All calculated to base B
    private static final int B = 11;// Prime
    private static final String OPEN = "<".intern();
    private static final String DOUBLE_OPEN = "<<".intern();
    private static final String CLOSE = ">".intern();
    private static final String COMMA = ",".intern();
    private static final char OPEN_CHAR = '<';
    private static final char CLOSE_CHAR = '>';
    private static final char COMMA_CHAR = ',';
    private static final char PIPE_CHAR = '|';
    /**
     * When we encode a block of 'b' chars to <n,m> we do not put it directly to
     * the output because there may be scope to combine multiple blocks of 'b'
     * char encodings so we store it in mostRecentEnc and in the next iteration
     * either direct it to output or modify it.
     */
    private final EncData mostRecentEnc = new EncData(0, 0);
    private final EncData newEncData = new EncData(0, 0);
    private boolean mostRecentEncFlag = false;
    private static final int BUCKET_FACTOR = 1;
    private int blockLen = 10;// Block size

    public BMDiffCodec(int b) {
        this.blockLen = b;
    }
    public BMDiffCodec() {
    }

    public void setBlockLen(int blockLen) {
        this.blockLen = blockLen;
    }

    
    private void reinit(int capHint) {
        fingerPrintTable = new HashMap<>(capHint / BUCKET_FACTOR);
        cachedPows = new long[blockLen];// All calculated to base B
        cachedPows[0] = 1;
        for (int i = 1; i < blockLen; i++) {
            cachedPows[i] = cachedPows[i - 1] * B;
        }
    }
    private char[] encIpCharArray;
    private char[] encOpCharArray;
    private char[] decOpCharArray;
    private long fingerPrint ;
    
    
    
    private StringBuilder encOutBuff;
    /**
     * Encodes the string based on BMDiffCodec algorithm. We are reusing EncData
     * objects by using setters because creating object is costlier than mutating it. 
     *
     * @param chArray  uncompressed string
    */
    public char[] encode(char[] chArray) {
        encIpCharArray = chArray;
        reinit(encIpCharArray.length / blockLen);
        long startTime = System.nanoTime();
        fingerPrint = rollingHash(encIpCharArray, 0, blockLen);
        encOutBuff = new StringBuilder();
        int rawDataStartIndex = 0;
        int encCnt = 0;
        int collisionCounter = 0;
        Integer fpIndex = -1;// Index of the first block with a given fingerPrint.
        int mostRecentFpIndex = -1;
        int blockStartIndex = -1;
        int toBeRemovedCharIndex = -1;
        boolean seenOpen = false;

        for (int i = blockLen; i < encIpCharArray.length; i++) {
            // Update fp to include a[i] and exclude a[i-b]
            if (i % blockLen == 0) {
                store(i - blockLen);
            }
            toBeRemovedCharIndex = (i - blockLen);
            blockStartIndex = toBeRemovedCharIndex + 1;
            fingerPrint = (fingerPrint - encIpCharArray[toBeRemovedCharIndex] * cachedPows[(blockLen - 1)]) * B + encIpCharArray[i];
            if (--encCnt > 0) {
                continue;
            }
            if (encIpCharArray[i] == OPEN_CHAR) {
                seenOpen = true;
            }

            fpIndex = fingerPrintTable.get(fingerPrint);
            if (fpIndex != null) {// We can encode current block or club with earlier blocks.
                if (!checkSubStringMatch(fpIndex, blockStartIndex)) {// A collision
                    collisionCounter++;
                    writeEncBlock(mostRecentFpIndex);
                    continue;
                }
                newEncData.set(fingerPrint, blockLen, true, fpIndex);
                if (mostRecentEncFlag) {// Possible continuity of encoding <n,m>
                    if (mostRecentEnc.isFPMatch(newEncData)// Fingerprints match and also characters match.
                            && checkSubStringMatch(mostRecentFpIndex, fpIndex)) {
                        if (mostRecentEnc.isPerpetual() && mostRecentEnc.getEncLen() > blockLen) {// A set of blocks were combined into perpetual blocks earlier so flush it to avoid problems because now the block is not perpetual but repeating.
                            mostRecentEnc.appendToSB(mostRecentFpIndex);
                            mostRecentEnc.set(newEncData);
                            mostRecentEncFlag = true;
                        } else {// The earlier blocks is eligible to be combined with current block.
                            mostRecentEnc.addLength(blockLen);
                            mostRecentEnc.setPerpetual(false);
                        }
                    } else if (mostRecentEnc.isPerpetual() && mostRecentEnc.canPerpetuate(newEncData)) {// Fingerprints don't match but multiple blocks can be clubbed together.
                        mostRecentEnc.addLength(blockLen);// Default Enc is perpetual so no need to set perpetual to true here.
                    } else {
                        mostRecentEnc.appendToSB(mostRecentFpIndex);
                        mostRecentEnc.set(newEncData);
                        mostRecentEncFlag = true;
                        mostRecentFpIndex = mostRecentEnc.fpIndex;
                    }
                } else {
                    if (rawDataStartIndex >= 0 && rawDataStartIndex < blockStartIndex) {
                        if (seenOpen) {// Rare occurance so usage of substring method is ok.
                            for (int j = rawDataStartIndex;j < blockStartIndex;j++) {
                                encOutBuff.append(encIpCharArray[j]);
                                if (encIpCharArray[j] == OPEN_CHAR) {
                                    encOutBuff.append(OPEN_CHAR);
                                }
                            }
                            seenOpen = false;
                        } else {
                            encOutBuff.append(encIpCharArray, rawDataStartIndex, blockStartIndex - rawDataStartIndex);
                        }
                    }
                    mostRecentEnc.set(newEncData);
                    mostRecentEncFlag = true;
                    mostRecentFpIndex = mostRecentEnc.fpIndex;
                }
                rawDataStartIndex = i + 1;// Reset
                encCnt = blockLen;
            } else {
                writeEncBlock(mostRecentFpIndex);
            }
        }
        writeEncBlock(mostRecentFpIndex);
        if (rawDataStartIndex >= 0) {
            for (int j = rawDataStartIndex;j < encIpCharArray.length;j++) {
                encOutBuff.append(encIpCharArray[j]);
                if (encIpCharArray[j] == OPEN_CHAR) {
                    encOutBuff.append(OPEN_CHAR);
                }
            }
        }
        long timeTaken = (System.nanoTime() - startTime);
        System.out.println("Collisions count=" + collisionCounter);
        System.out.println("Encoding speed " + (encIpCharArray.length * 2 * 1000.0) / timeTaken  + "MB/sec");
        encOpCharArray = encOutBuff.toString().toCharArray();
        return encOpCharArray;
    }
/**
 * The most recent encoded block.
 * @param mostRecentFpIndex 
 */
    private void writeEncBlock(int mostRecentFpIndex) {
        if (mostRecentEncFlag) {
            mostRecentEnc.appendToSB(mostRecentFpIndex);
            mostRecentEncFlag = false;
        }
    }
/**
 * Char by Char match.
 * @param startIndex1
 * @param startIndex2
 * @return 
 */
    private boolean checkSubStringMatch(int startIndex1, int startIndex2) {
        for (int i = 0; i < blockLen; i++) {
            if (encIpCharArray[i + startIndex1] != encIpCharArray[i + startIndex2]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Decoding is straight forward except when runLength is greater than
     * decoderBuffer length.
     *
     * @return
     */
    public char[] decode() {
        long startTime = System.nanoTime();
        // The decoded output is atleast as big as encoded string, so give a good initial size.
        StringBuilder decodeBuffer = new StringBuilder(encOpCharArray.length);
        // Atleast 1 character will not be encoded.
        decodeBuffer.append(encOpCharArray, 0, 1);

        /**
         * If we see <n,m> then index of n is encIndexStart and is reset to -1
         * when enc block <n,m> is processed.
         */
        int encIndexStart = -1;
        int encCommaIndex = - 1;
        /**
         * Is the start of raw text(i.e unencoded input), we track it because
         * instead of writing one char at a time to decode output we write only
         * when we see start of an enc block <m,n> (OR) when stream ends.
         */
        int rawUnEncIndexStart = 1;
        for (int i = 1; i < encOpCharArray.length; i++) {
            if (encOpCharArray[i] == '<') {
                if (i < encOpCharArray.length- 1) {
                    if (encOpCharArray[i + 1] != '<') {
                        if (rawUnEncIndexStart < i) {
                            decodeBuffer.append(encOpCharArray, rawUnEncIndexStart, i - rawUnEncIndexStart);
                            rawUnEncIndexStart = Integer.MAX_VALUE;
                        }
                        encIndexStart = i + 1;
                        while (encOpCharArray[++i] != ',') {
                        }
                        encCommaIndex = i;
                            while (encOpCharArray[++i] != '>') {
                        }
                        int startIndex = Utils.strToInt(encOpCharArray, encIndexStart, encCommaIndex);
                        int runLen = 0;
                        boolean repeatition = false;
                        if (encOpCharArray[i - 1] != '|') {
                            runLen = Utils.strToInt(encOpCharArray, encCommaIndex + 1, i);
                        } else {
                            runLen = Utils.strToInt(encOpCharArray, encCommaIndex + 1, i - 1);
                            repeatition = true;
                        }
                        int endIndex = startIndex + runLen;
                        if (startIndex < decodeBuffer.length()) { // One could not have encoded something one has not seen yet.
                            if (repeatition) {
                                int rStartIndex = startIndex;
                                int rEndIndex = startIndex + blockLen;
                                if (startIndex + blockLen > decodeBuffer.length()) {
                                    rEndIndex = decodeBuffer.length();
                                }
                                int totalChunks = runLen / (rEndIndex - rStartIndex);
                                for (int k = 0; k < totalChunks; k++) {
                                    decodeBuffer.append(decodeBuffer, rStartIndex, rEndIndex);
                                }
                                decodeBuffer.append(decodeBuffer, rStartIndex, rStartIndex + runLen % (rEndIndex - rStartIndex));
                            } /**
                             * We cannot derive the repetition from decoded data
                             * but we need to generate from a small repetition
                             * in the decoded buffer. Ex: If i/p is
                             * aaaaaaaaaaaaaaaaaaaaa enc is aaa<0,18>
                             * Now when the decoder is processing <0,18> the
                             * decoder buffer only has aaa but we need string
                             * starting at 0 index and of length 18 so this
                             * means the string aaa has been repeated 6 times to
                             * get runLength 18.
                             */
                            else if (endIndex > decodeBuffer.length()) {// Still a repeatition but a different one.
                                int rStartIndex = startIndex;
                                int rEndIndex = decodeBuffer.length();
                                int totalChunks = runLen / (rEndIndex - rStartIndex);
                                for (int k = 0; k < totalChunks; k++) {
                                    decodeBuffer.append(decodeBuffer, rStartIndex, rEndIndex);
                                }
                                decodeBuffer.append(decodeBuffer, rStartIndex, rStartIndex + runLen % (rEndIndex - rStartIndex));
                            } else {// Simplest case
                                decodeBuffer.append(decodeBuffer, startIndex, endIndex);
                            }
                        }
                        rawUnEncIndexStart = i + 1;
                    } else {// There was << in input stream so flush raw data(if any).
                        if (rawUnEncIndexStart < encOpCharArray.length) {
                            decodeBuffer.append(encOpCharArray, rawUnEncIndexStart, i - rawUnEncIndexStart);
                        }
                        decodeBuffer.append(OPEN_CHAR);
                        i++;// As we also processed the next '<' character.
                        rawUnEncIndexStart = i + 1;// Adjust the pointer as we just flushed the raw data.
                    }
                }
            }// Otherwise this is just a raw character keep going.
        }
        if (rawUnEncIndexStart < encOpCharArray.length) {
            decodeBuffer.append(encOpCharArray, rawUnEncIndexStart, encOpCharArray.length- rawUnEncIndexStart);
        }
        System.out.println("Decoding speed " + (encOpCharArray.length * 2 * 1000.0) / (System.nanoTime() - startTime) + "MB/sec");
        decOpCharArray = decodeBuffer.toString().toCharArray();
        return decOpCharArray;
    }

    /**
     * See Rabin Karp's algorithm.
     *
     * @param a
     * @param startIndex
     * @param m
     * @param prevHash
     * @return
     */
    private long rollingHash(char[] encIpCharArray, int startIndex, int m) {
        long hash = 0;
        for (int i = 0; i < m; i++) {
            hash += encIpCharArray[startIndex + i] * cachedPows[((m - i) - 1)];
        }
        return hash;
    }

    /**
     * See Rabin Karp's algorithm.
     *
     * @param a
     * @param startIndex
     * @param m
     * @return
     */
    private void rollingHashUpdate(char[] encIpCharArray, int startIndex, int m) {
        fingerPrint = (fingerPrint - encIpCharArray[startIndex - 1] * cachedPows[(m - 1)]) * B + encIpCharArray[startIndex + m - 1];
    }

    /**
     * Store fingerprint (i.e hash of block of 'b' characters starting at index
     * i in the input stream.
     *
     * @param fp
     * @param i
     */
    private void store(int i) {
        if (!fingerPrintTable.containsKey(fingerPrint)) {
            fingerPrintTable.put(fingerPrint, i);
        }
        
    }

    /**
     * Caches the values for B^exponent
     *
     * @param exponent
     * @return
     */
//    @Tap
//    private long cachedPow(int exponent) {
//        return cachedPows[exponent];
//    }

    private class EncData {

        private long fingerPrint;// Rolling hash.
        private int runLength;// Multiples of 'b'
        boolean perpetual = true;// If true <n,m> is decoded as ip[n, n+m], if false the decoding is ip[n, n+b] repeat m/b times + ip[n,n%b]
        private int fpIndex;// The index from which upto block length of character produces this fingerPrint.
        
        public EncData(long fingerPrint, int runLength) {
            set(fingerPrint, runLength, true, -1);
        }

        public void set(long fingerPrint, int runLength, boolean perpetual, int i) {
            this.fingerPrint = fingerPrint;
            this.runLength = runLength;
            this.perpetual = perpetual;
            this.fpIndex = i;
        }
        public void set(EncData e) {
            this.fingerPrint = e.fingerPrint;
            this.runLength = e.runLength;
            this.perpetual = e.perpetual;
            this.fpIndex = e.fpIndex;
        }

        public boolean isFPMatch(EncData enc) {
            return  (this.fingerPrint == enc.fingerPrint);
        }

        /**
         * For Ex Let input be aaaabcdeaabcde, the encoding sequence is as
         * follows 1. aaaabcdeaabcde -> 2. aa<0,2>bcde<0,2><4,2><6,2> -> 3.
         * aa<0,2>bcde<0,2><4,4>
         *
         * In step 2 after bcde <0,2> cannot perpetuate to <4,6> but <4,2> can
         * perpetuate to <6,2> and hence <4,2> + <6,2> = <4,4>
         *
         * @param enc
         * @return
         */
        public boolean canPerpetuate(EncData enc) {
            return this.fpIndex + runLength == enc.fpIndex;
        }

        /**
         * When we add length amt to the current Encoded block we are actually encoding more
         * data but we are still carrying the fingerprint of the first block. i.e if the 
         * total size of the encoded block is 33 and the block size b = 11 then
         * the fingerprint of this is still the fingerprint of 1st b characters of the block.
         * @param amt 
         */
        public void addLength(int amt) {
            runLength += amt;
        }

        public long getFP() {
            return fingerPrint;
        }

        public long getEncLen() {
            return runLength;
        }

        public boolean isPerpetual() {
            return perpetual;
        }

        public void setPerpetual(boolean perpetual) {
            this.perpetual = perpetual;
        }
        

        @Override
        public String toString() {
            return OPEN + fpIndex + COMMA + runLength + CLOSE;
        }

        public void appendToSB(int mostRecentFpIndex) {
            encOutBuff.append(OPEN_CHAR);
            encOutBuff.append(mostRecentFpIndex);
            encOutBuff.append(COMMA_CHAR);
            encOutBuff.append(runLength);
            if (!perpetual) {// A small twist to B & M's original version
                encOutBuff.append(PIPE_CHAR);
            }
            encOutBuff.append(CLOSE_CHAR);

        }
    }

}
