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
package bmdiff;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Very simple file IO util.
 * @author srikchan
 */
public class Utils {

    public static String readFile(String fileName) {
        try {
            return new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static char[] readFileInChars(String fileName) {
        try {
            String s = new String(Files.readAllBytes(Paths.get(fileName)));
            return s.toCharArray();
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    public static void writeFile(String s, String fileName) {
        try {
            try (PrintWriter out = new PrintWriter(fileName)) {
                out.print(s);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    public static void writeFileInChars(char[] s, String fileName) {
        try {
            try (PrintWriter out = new PrintWriter(fileName)) {
                out.print(s);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
/**
 * Will not work for negative or strings preceded with sign.
 * @param chArr
 * @param start
 * @param end
 * @return 
 */
    public static int strToInt(char[] chArr, int start, int end) {
        int i = start;
        int num = 0;
        while (i < end) {
            num *= 10;
            num += chArr[i++] - '0';
        }
        return num;
    }    
}
