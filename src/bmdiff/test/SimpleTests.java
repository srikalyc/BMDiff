/**
 * Copyright 2014 Srikalyan Chandrashekar. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License. 
 * See accompanying LICENSE file.
 */
package bmdiff.test;

import bmdiff.BMDiffCodec;
import bmdiff.Utils;

/**
 * The input strings have been carefully chosen to stress most of the boundary 
 * conditions that could possibly make the implementation falter. If you are
 * implementing your own version then this test set would immensely help as it
 * helped uncover all the corner cases.
 * @author srikchan
 */
public class SimpleTests {

    private static final int blockSize = 8;
    private final String[] inputStrings = new String[] {
        "abcdefghijklmnopqrstuvwxijklmnopabcdefghqrstuvwx",
        "abcdefghijabcdefghij", 
        "aaaabcdeaabcde", 
        "aaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbb", 
        "aabaaaaaaaaabbbbbbbbbbbbbb", 
        "abcabcabcabcabcabc", 
        "               intended by the author                                     CHAPTER IV                                    SAMPOORNESHBABU",
        "aaaaaaaaaaaaaaaaINTENDED BY THE AUTHORaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaCHAPTER IVaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaSAMPOORNESHBABU", 
        "-------------+---------------------+--------------------------------------", 
        "+---------------------+--------------+---------------------+--------------",
        "abcdefghijklmnopq<12345",
        "Michael S. Hart <hart@pobox.com> abcdefg gfedcba <hart@pobox.com>hijklmnopqrstuv", 
        "Michael S. Hart <hart@pobox.com> abcdefg gfedcba <hart@pobox.com>hijklmnopqrstuv gefdbca <hart@pobox.com> zebra"
    };

    public static void main(String[] args) {
        SimpleTests test = new SimpleTests();
            test.test();
//        for (int i = 0;i < 10;i++)
//            test.test("/home/srikalyc/NetBeansProjects/big");
    }

    public void test() {
        BMDiffCodec bmdiff = new BMDiffCodec(blockSize);
        for (String inputString : inputStrings) {
            char[] enc = bmdiff.encode(inputString.toCharArray());
            char[] dec = bmdiff.decode();
            System.out.println(inputString);
            System.out.println(enc);
            System.out.println(dec);
            System.out.println("----------------------------------------------------");
            System.out.println(inputString.equals(new String(dec)) ? "PASS" : "FAIL");
            System.out.println("----------------------------------------------------");
        }
    }
    /**
     * FileName without extension.
     * @param fileName 
     */
    public void test(String fileName) {
        BMDiffCodec bmdiff = new BMDiffCodec(blockSize);
        char[] enc = bmdiff.encode(Utils.readFileInChars(fileName + ".txt"));
        Utils.writeFileInChars(enc, fileName + ".enc");
        Utils.writeFileInChars(bmdiff.decode(), fileName + ".dec");
        
    }
}
