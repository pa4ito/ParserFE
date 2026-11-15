import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Util {
    static StringBuffer sb = new StringBuffer();

    static BitSet getBitmap(final byte[] buf, final int bitmapStart) throws Exception {
        final var bs = new BitSet();
        final int minlength = 8;
        int pos = 0;

        // Parse the bitmap (primary first)
        for (int i = bitmapStart; i < 8 + bitmapStart; i++) {
            int bit = 128; // 0x80
            for (int b = 0; b < 8; b++) {
                bs.set(pos++, (buf[i] & bit) != 0);
                bit >>= 1;
            }
        }

        // Check for secondary bitmap and parse if necessary
        if (bs.get(0)) {
            if (buf.length < minlength + 8) {
                throw new Exception("Insufficient length for secondary bitmap: " + buf.length);
            }

            for (int i = 8 + bitmapStart; i < 16 + bitmapStart; i++) {
                int bit = 128; // 0x80
                for (int b = 0; b < 8; b++) {
                    bs.set(pos++, (buf[i] & bit) != 0);
                    bit >>= 1;
                }
            }
        }

        return bs;
    }

    // -----------------------------

    public static String hexEncode(byte[] buffer, int start, int length) {
        final char[] HEX = "0123456789ABCDEF".toCharArray();

        if (buffer.length == 0) {
            return "";
        }
        int holder = 0;
        char[] chars = new char[length * 2];
        int pos = -1;
        for (int i = start; i < start + length; i++) {
            holder = (buffer[i] & 0xf0) >> 4;
            chars[++pos * 2] = HEX[holder];
            holder = buffer[i] & 0x0f;
            chars[(pos * 2) + 1] = HEX[holder];
        }
        return new String(chars);
    }

    // -----------------------------

    public static byte[] hexDecode(String hex) {
        // A null string returns an empty array
        if (hex == null || hex.length() == 0) {
            return new byte[0];
        } else if (hex.length() < 3) {
            return new byte[] { (byte) (Integer.parseInt(hex, 16) & 0xff) };
        }
        // Adjust accordingly for odd-length strings
        int count = hex.length();
        int nibble = 0;
        if (count % 2 != 0) {
            count++;
            nibble = 1;
        }
        byte[] buf = new byte[count / 2];
        char c = 0;
        int holder = 0;
        int pos = 0;
        for (int i = 0; i < buf.length; i++) {
            for (int z = 0; z < 2 && pos < hex.length(); z++) {
                c = hex.charAt(pos++);
                if (c >= 'A' && c <= 'F') {
                    c -= 55;
                } else if (c >= '0' && c <= '9') {
                    c -= 48;
                } else if (c >= 'a' && c <= 'f') {
                    c -= 87;
                }
                if (nibble == 0) {
                    holder = c << 4;
                } else {
                    holder |= c;
                    buf[i] = (byte) holder;
                }
                nibble = 1 - nibble;
            }
        }
        return buf;
    }

    // ------------------------------------

    // static int[] ASCII_EBCIDIC_TABLE = {
    // 0x00, 0x01, 0x02, 0x03, 0x37, 0x2D, 0x2E, 0x2F, 0x16, 0x05, 0x25, 0x0B, 0x0C,
    // 0x0D, 0x0E, 0x0F,
    // 0x10, 0x11, 0x12, 0x13, 0x3C, 0x3D, 0x32, 0x26, 0x18, 0x19, 0x3F, 0x27, 0x1C,
    // 0x1D, 0x1E, 0x1F,
    // 0x40, 0x5A, 0x7F, 0x7B, 0x5B, 0x6C, 0x50, 0x7D, 0x4D, 0x5D, 0x5C, 0x4E, 0x6B,
    // 0x60, 0x4B, 0x61,
    // 0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9, 0x7A, 0x5E, 0x4C,
    // 0x7E, 0x6E, 0x6F,
    // 0x7C, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xD1, 0xD2, 0xD3,
    // 0xD4, 0xD5, 0xD6,
    // 0xD7, 0xD8, 0xD9, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9, 0xAD, 0xE0,
    // 0xBD, 0x5F, 0x6D,
    // 0x79, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x91, 0x92, 0x93,
    // 0x94, 0x95, 0x96,
    // 0x97, 0x98, 0x99, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9, 0xC0, 0x4F,
    // 0xD0, 0xA1, 0x07,
    // 0x20, 0x21, 0x22, 0x23, 0x24, 0x15, 0x06, 0x17, 0x28, 0x29, 0x2A, 0x2B, 0x2C,
    // 0x09, 0x0A, 0x1B,
    // 0x30, 0x31, 0x1A, 0x33, 0x34, 0x35, 0x36, 0x08, 0x38, 0x39, 0x3A, 0x3B, 0x04,
    // 0x14, 0x3E, 0xE1,
    // 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x51, 0x52, 0x53, 0x54,
    // 0x55, 0x56, 0x57,
    // 0x58, 0x59, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x70, 0x71, 0x72,
    // 0x73, 0x74, 0x75,
    // 0x76, 0x77, 0x78, 0x80, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F, 0x90, 0x9A, 0x9B,
    // 0x9C, 0x9D, 0x9E,
    // 0x9F, 0xA0, 0xAA, 0xAB, 0xAC, 0x4A, 0xAE, 0xAF, 0xB0, 0xB1, 0xB2, 0xB3, 0xB4,
    // 0xB5, 0xB6, 0xB7,
    // 0xB8, 0xB9, 0xBA, 0xBB, 0xBC, 0x6A, 0xBE, 0xBF, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE,
    // 0xCF, 0xDA, 0xdB,
    // 0xDC, 0xDD, 0xDE, 0xDF, 0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF, 0xFA, 0xFB, 0xFC,
    // 0xFD, 0xFE, 0xFF
    // };

    /*
     * EBCDIC_ASCII_TABLE[74] 0x2E ====> 0x5B . ===> [
     * EBCDIC_ASCII_TABLE[79] 0x7C ====> 0x21 | ===> !
     * EBCDIC_ASCII_TABLE[90] 0x21 ====> 0x5D ! ===> ]
     * EBCDIC_ASCII_TABLE[121] 0x2E ====> 0x60 . ===> ~
     * EBCDIC_ASCII_TABLE[173] 0x5B ====> 0x2E [ ===> .
     */
    static int[] EBCDIC_ASCII_TABLE = {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
            0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
            0x2E, 0x2E, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x2E, 0x3F,
            0x20, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x2E, 0x5B, 0x2E, 0x3C, 0x28, 0x2B, 0x21,
            0x26, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x2E, 0x5D, 0x24, 0x2A, 0x29, 0x3B, 0x5E,
            0x2D, 0x2F, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x2E, 0x7C, 0x2C, 0x25, 0x5F, 0x3E, 0x3F,
            0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x60, 0x3A, 0x23, 0x40, 0x27, 0x3D, 0x22,
            0x2E, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x2E, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71,
            0x72, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x2E, 0x7E, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
            0x7A, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x2E, 0x2E, 0x2E, 0x2E, 0x5D, 0x2E, 0x2E,
            0x7B, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x7D, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51,
            0x52, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x5C, 0x2E, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
            0x5A, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E
    };

    static String convertEBDICtoASCII(byte[] rawArray, int offset, int length) {
        char[] convertedArray = new char[length];

        for (int index = 0; index < length; index++) {
            convertedArray[index] = (char) EBCDIC_ASCII_TABLE[rawArray[index + offset] & 0xff];
        }

        return new String(convertedArray);
    }

    // -----------------------------

    // Brand codes: VISA -> 1; MC -> 2; Way4 -> 3
    static int parseField55(int brand, int currentPos, byte[] buf, HashMap<Integer, Object> fields) {
        ArrayList<String> alTagValue = new ArrayList<>();
        int fieldLength = 0;
        int fieldEndingPos = 0;

        switch (brand) {
            case 1: // VISA has DataSet 01 at the beginning
                // Get field length
                fieldLength = getLength(1, currentPos, buf);
                currentPos++;

                // Get Dataset ID
                String dataSetId = parseBCD(1, currentPos, buf);
                currentPos++;

                // Get Dataset length
                fieldLength = getLength(2, currentPos, buf);
                currentPos += 2;

                break;

            case 2:
                // MC - Get field length
                fieldLength = Integer.parseInt(parseASCII(3, currentPos, buf));
                currentPos += 3;
                break;

            case 3: 
                // Way4 - Get field length
                fieldLength = Integer.parseInt(parseBCD(2, currentPos, buf));
                currentPos += 2;
                break;
        }

        fieldEndingPos = currentPos + fieldLength;

        while (currentPos < fieldEndingPos) {
            // Get Tag ID
            // String tagID = String.format("%02x", (buf[currentPos] & 0xff));
            String tagID = parseBCD(1, currentPos, buf);

            if (tagID.substring(1).equals("f")) {
                currentPos++;
                // tagID += String.format("%02x", (buf[currentPos] & 0xff));
                tagID += parseBCD(1, currentPos, buf);
            }

            currentPos++;

            // Get Tag ID Length
            int tagLength = 0;
            tagLength = (buf[currentPos] & 0xff);

            currentPos++;

            alTagValue.add(tagID + "=" + parseBCD(tagLength, currentPos, buf));

            currentPos += tagLength;
        }

        Collections.sort(alTagValue);
        fields.put(55, alTagValue);

        switch(brand) {
            case 1:
        // VISA has DataSet 01 at the beginning - xx01yyyy - xx(field length); yyyy(DataSet length)
            fieldLength += 4;
            break;

            case 2:
            // MC has DataSet 3 bytes for field length - 3x3y3z
            fieldLength += 3;
            break;

            default:
            // + 2 bytes for field length...
            fieldLength += 2;
            break;
        }
        
        return fieldLength;
    }

    // -----------------------------

    static int parseWay4_46_47_48(int filedID, int currentPos, byte[] buf, HashMap<Integer, Object> fields) {
        ArrayList<String> alTagValue = new ArrayList<>();

        // Get field length
        int fieldLength = Integer.parseInt(parseBCD(2, currentPos, buf));

        int fieldEndingPos = currentPos + fieldLength;

        currentPos += 2;

        while (currentPos < fieldEndingPos) {
            // Get Tag ID
            String tagID = parseASCII(3, currentPos, buf);
            currentPos += 3;

            // Get Tag ID Length
            int tagLength = Integer.parseInt(parseASCII(3, currentPos, buf));
            currentPos += 3;

            String tagValue = parseASCII(tagLength, currentPos, buf);

            if (tagID.equals("811"))
                tagValue = parseWay4_48_811(tagValue);

            alTagValue.add(tagID + "(" + tagLength + ")" + "=" + tagValue);
            currentPos += tagLength;
        }

        Collections.sort(alTagValue);
        fields.put(filedID, alTagValue);

        // + 2 bytes for field length...
        return fieldLength + 2;
    }

    // ------------------------------

    private static String parseWay4_48_811(String value) {
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        int length = value.length();

        sb.append("{");

        while (pos < length) {
            String tag = value.substring(pos, pos + 2);
            if (sb.length() > 1)
                sb.append(", ");
            sb.append(tag);
            pos += 2;
            int tagLength = Integer.parseInt(value.substring(pos, pos + 2));
            sb.append("(" + value.substring(pos, pos + 2) + ")");
            pos += 2;
            String tagValue = value.substring(pos, pos + tagLength);
            sb.append("=" + tagValue);
            pos += tagLength;
        }

        sb.append("}");

        return sb.toString();
    }

    // -----------------------------

    static int parseBerTlv(int fieldID, int currentPos, byte[] buf, HashMap<Integer, Object> fields) {
        // Get field length
        int fieldLength = Integer.parseInt(parseBCD(2, currentPos, buf));
        int fieldEndingPos = currentPos + fieldLength;

        currentPos += 2;

        Map<String, Object> hm = new LinkedHashMap<>();

        while (currentPos < fieldEndingPos) {
            Map<String, Object> hm1 = parse_BER_TLV(currentPos, buf);
            for (String key : hm1.keySet()) {
                hm.put(key, hm1.get(key));
            }

            // Adding 2 bytes for tagId and TagIdLength
            currentPos += 2 + Integer.parseInt(parseBCD(1, currentPos + 1, buf), 16);
        }

        fields.put(fieldID, hm);

        // + 2 bytes for field length...
        return fieldLength + 2;
    }

    // -----------------------------

    public static Map<String, Object> parse_BER_TLV(int currentPos, byte[] buf) {
        // Get Tag ID
        String tagID = parseBCD(1, currentPos, buf);
        boolean isConstructed = (buf[currentPos] & 0x20) != 0;

        currentPos++;

        if (tagID.equals("ff")) {
            tagID += parseBCD(1, currentPos, buf);
            currentPos++;
        }

        // Get Tag Length
        int tagLength = Integer.parseInt(parseBCD(1, currentPos, buf), 16);
        currentPos++;
        int endPos = currentPos + tagLength;

        Map<String, Object> hm = new LinkedHashMap<>();
        Map<String, Object> mhm = new LinkedHashMap<>();

        while (currentPos < endPos) {
            if (isConstructed) {
                // Constructed data object
                Map<String, Object> hm1 = parse_BER_TLV(currentPos, buf);
                for (String key : hm1.keySet()) {
                    hm.put(key, hm1.get(key));
                }

                // Adding 2 bytes for tagId and TagIdLength
                currentPos += 2 + Integer.parseInt(parseBCD(1, currentPos + 1, buf), 16);
            } else {
                // Primitive data object
                mhm.put(tagID, parseBCD(tagLength, currentPos, buf));
                currentPos = endPos;
            }
        }

        if (isConstructed)
            mhm.put(tagID, hm);

        return mhm;

    }

    // -----------------------------

    static String parseASCII(int fieldLength, int currentPos, byte[] buf) {
        sb.setLength(0);
        for (int i = currentPos; i < currentPos + fieldLength; i++) {
            sb.append(String.valueOf((char) (buf[i] & 0xff)));
        }
        return sb.toString();
    }

    // -----------------------------

    static String parseBCD(int fieldLength, int currentPos, byte[] buf) {
        sb.setLength(0);
        for (int i = currentPos; i < currentPos + fieldLength; i++) {
            sb.append(String.format("%02x", (buf[i] & 0xff)));
        }
        return sb.toString();
    }

    // -----------------------------

    static int getLength(int length, int currentPos, byte[] buf) {
        if (length == 2)
            return ((buf[currentPos] & 0xff) << 8) | (buf[currentPos + 1] & 0xff);

        return (buf[currentPos] & 0xff);
    }

}
