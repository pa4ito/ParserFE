import java.util.BitSet;
import java.util.HashMap;

public class ParserVisa {
    // Brand codes: VISA -> 1; MC -> 2; Way4 -> 3
    int brand;
    byte[] buf;
    BitSet bs;
    int currentPos;
    HashMap<Integer, Object> fields;
    private static StringBuilder sb = new StringBuilder();
    private int fieldLength, fieldEndingPos, dataSetLength, tagLength, dataSetEndingPos;

    public ParserVisa(int brand, byte[] buf, BitSet bs, int currentPos, HashMap<Integer, Object> fields) {
        this.brand = brand;
        this.buf = buf;
        this.bs = bs;
        this.currentPos = currentPos;
        this.fields = fields;
    }

    // -----------------------------

    String parsingFieldBCD(int fieldLength) {
        sb.setLength(0);
        for (int i = currentPos; i < currentPos + fieldLength; i++) {
            sb.append(String.format("%02x", (buf[i] & 0xff)));
        }
        return sb.toString();
    }

    // -----------------------------

    int parsingFieldTLV(int field) {
        HashMap<String, Object> mapFieldID = new HashMap<>();

        // Get field length
        if ((field == 104) || (field == 116) || (field == 123) || (field == 125)) {
            fieldLength = (buf[currentPos] & 0xff);
            currentPos += 1;
        } else {
            fieldLength = ((buf[currentPos] & 0xff) << 8) | (buf[currentPos + 1] & 0xff);
            currentPos += 2;
        }

        fieldEndingPos = currentPos + fieldLength;

        while (currentPos < fieldEndingPos) {
            HashMap<String, String> mapDataSetId = new HashMap<>();

            // Get Dataset ID
            String dataSetId = String.format("%02x", (buf[currentPos] & 0xff));

            currentPos++;

            // Get Dataset ID Length
            dataSetLength = ((buf[currentPos] & 0xff) << 8) | (buf[currentPos + 1] & 0xff);

            currentPos += 2;

            dataSetEndingPos = currentPos + dataSetLength;

            while (currentPos < dataSetEndingPos) {
                // Get Tag ID
                String tagId = String.format("%02x", (buf[currentPos] & 0xff));

                currentPos++;

                // Get Tag Length
                tagLength = (buf[currentPos] & 0xff);

                currentPos++;

                switch (field) {
                    case 123:
                        switch (dataSetId) {
                            case "67":
                                mapDataSetId.put(tagId, parsingFieldBCD(tagLength));                                
                                break;
                            default:
                                mapDataSetId.put(tagId, Util.convertEBDICtoASCII(buf, currentPos, tagLength));
                        }
                        break;

                    default:
                        mapDataSetId.put(tagId, Util.convertEBDICtoASCII(buf, currentPos, tagLength));
                }

                currentPos += tagLength;
            }

            mapFieldID.put(dataSetId, mapDataSetId);

        }

        fields.put(field, mapFieldID);

        return 0;
    }

    // -----------------------------

    int parsingField55(int field) {
        HashMap<String, Object> mapFieldID = new HashMap<>();

        // Get field length
        fieldLength = (buf[currentPos] & 0xff);

        // System.out.println("(55)ICC - Related Data: ");
        // String tab = " ";
        // System.out.println(tab
        // + "fieldLength: 0x"
        // + String.format("%02x", fieldLength)
        // + " -> "
        // + fieldLength);

        currentPos++;

        fieldEndingPos = currentPos + fieldLength;

        while (currentPos < fieldEndingPos) {
            HashMap<String, String> mapDataSetId = new HashMap<>();

            // Get Dataset ID
            String dataSetId = String.format("%02x", (buf[currentPos] & 0xff));
            // System.out.println(tab + "Dataset ID:" + dataSetId);

            currentPos++;

            // Get Dataset ID Length
            dataSetLength = ((buf[currentPos] & 0xff) << 8) | (buf[currentPos + 1] & 0xff);
            // System.out.println(tab
            // + "Dataset Length: "
            // + String.format("%02x", dataSetLength)
            // + " -> "
            // + dataSetLength);

            currentPos += 2;

            dataSetEndingPos = currentPos + dataSetLength;

            while (currentPos < dataSetEndingPos) {
                // Get Tag ID
                String tagId = String.format("%02x", (buf[currentPos] & 0xff));
                // System.out.print(tab
                // + tab
                // + "Tag ID: "
                // + tagId);

                if (tagId.substring(1).equals("f")) {
                    currentPos++;
                    tagId += String.format("%02x", (buf[currentPos] & 0xff));
                }

                currentPos++;

                // Get Tag Length
                tagLength = (buf[currentPos] & 0xff);
                // System.out.println(tab
                // + tab
                // + "Tag Length: 0x"
                // + String.format("%02x", tagLength)
                // + " -> "
                // + tagLength);

                currentPos++;

                mapDataSetId.put(tagId, parsingFieldBCD(tagLength));

                currentPos += tagLength;
            }

            mapFieldID.put(dataSetId, mapDataSetId);

        }

        fields.put(field, mapFieldID);

        return 0;
    }

    // -----------------------------

    int parsingFieldBitmap(int field) {
        BitSet bitmap = new BitSet();
        int bitMapLength = 8;

        if (field == 63)
            bitMapLength = 3;

        HashMap<String, String> mapDataSetId = null;

        fieldLength = (buf[currentPos] & 0xff);

        currentPos++;

        fieldEndingPos = currentPos + fieldLength;

        try {
            /* Getting bitmap */
            int pos = 0;
            for (int i = 0; i < bitMapLength; i++) {
                int bit = 128; // 0x80
                for (int b = 0; b < 8; b++) {
                    bitmap.set(pos++, (buf[i + currentPos] & bit) != 0);
                    bit >>= 1;
                }
            }
            // System.out.println("-----");
            // System.out.println(tab + "bitmap: " + bitmap);
            // System.out.println(tab + "bitmap.length(): " + bitmap.length());
            // System.out.println("-----");

            currentPos += bitMapLength;

            switch (field) {
                case 62:
                    mapDataSetId = parsingField62(bitmap);
                    break;

                case 63:
                    mapDataSetId = parsingField63(bitmap);
                    break;

                case 126:
                    mapDataSetId = parsingField126(bitmap);
                    break;
            }

            if (currentPos < fieldEndingPos)
                currentPos = fieldEndingPos;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        fields.put(field, mapDataSetId);

        return 0;
    }

    // -----------------------------

    HashMap<String, String> parsingField62(BitSet bitmap) {
        HashMap<String, String> mapDataSetId = new HashMap<>();
        int subFieldLength;

        int bit = 128; // 0x80

        for (int i = 0; i < bitmap.length(); i++) {
            if (currentPos < fieldEndingPos) {
                if (bitmap.get(i)) {
                    switch (i + 1) {
                        case 1:
                            subFieldLength = 1;
                            mapDataSetId.put(String.valueOf(i + 1), String.valueOf((char) (buf[currentPos] & bit)));
                            currentPos += subFieldLength;
                            break;

                        case 2:
                            subFieldLength = 8;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 3:
                            subFieldLength = 4;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append((char) (buf[j] & 0xff));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 4:
                            subFieldLength = 1;
                            mapDataSetId.put(String.valueOf(i + 1), String.valueOf((char) (buf[currentPos] & bit)));
                            currentPos += subFieldLength;
                            break;

                        case 5:
                            subFieldLength = 1;
                            mapDataSetId.put(String.valueOf(i + 1), String.format("%02x", (buf[currentPos] & 0xff)));
                            currentPos += subFieldLength;
                            break;

                        case 21:
                            subFieldLength = 4;
                            mapDataSetId.put(String.valueOf(i + 1),
                                    Util.convertEBDICtoASCII(buf, currentPos, subFieldLength));
                            currentPos += subFieldLength;
                            break;

                        case 22:
                            subFieldLength = 6;
                            mapDataSetId.put(String.valueOf(i + 1),
                                    Util.convertEBDICtoASCII(buf, currentPos, subFieldLength));
                            currentPos += subFieldLength;
                            break;
                    }
                }
            }
        }

        return mapDataSetId;
    }

    // -----------------------------

    HashMap<String, String> parsingField63(BitSet bitmap) {
        HashMap<String, String> mapDataSetId = new HashMap<>();
        int subFieldLength;

        for (int i = 0; i < bitmap.length(); i++) {
            if (currentPos < fieldEndingPos) {
                if (bitmap.get(i)) {
                    switch (i + 1) {
                        case 1:
                            subFieldLength = 2;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 2:
                            subFieldLength = 2;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 3:
                            subFieldLength = 2;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 4:
                            subFieldLength = 2;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 19:
                            subFieldLength = 3;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;
                    }
                }
            }
        }

        return mapDataSetId;
    }

    // -----------------------------

    HashMap<String, String> parsingField126(BitSet bitmap) {
        HashMap<String, String> mapDataSetId = new HashMap<>();
        int subFieldLength;

        for (int i = 0; i < bitmap.length(); i++) {
            if (currentPos < fieldEndingPos) {
                if (bitmap.get(i)) {
                    switch (i + 1) {
                        case 5:
                            subFieldLength = 8;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 6:
                        case 7:
                            subFieldLength = 17;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 8:
                        case 9:
                            subFieldLength = 20;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;

                        case 13:
                        case 20:
                            subFieldLength = 1;
                            sb.setLength(0);

                            for (int j = currentPos; j < currentPos + subFieldLength; j++) {
                                sb.append(String.format("%02x", (buf[j] & 0xff)));
                            }

                            mapDataSetId.put(String.valueOf(i + 1), sb.toString());
                            currentPos += subFieldLength;
                            break;
                    }
                }
            }
        }

        return mapDataSetId;
    }

    // -----------------------------

    public HashMap<Integer, Object> parse() {
        int bitmapLength = bs.length() < 65 ? 8 : 16;
        int fieldLength = 0;
        int realLength = 0;

        String str;

        currentPos += bitmapLength;
        for (int i = 0; i < bs.length(); i++) {
            if (bs.get(i)) {
                switch (i + 1) {
                    case 2:
                        fieldLength = (buf[currentPos++] & 0xff) / 2;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 3:
                        fieldLength = 3;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 4:
                    case 6:
                        fieldLength = 6;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 7:
                        fieldLength = 5;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 10:
                        fieldLength = 4;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 11:
                        fieldLength = 3;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 12:
                        fieldLength = 12;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 13:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 14:
                    case 15:
                    case 18:
                    case 19:
                    case 20:
                    case 22:
                    case 23:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 25:
                    case 26:
                        fieldLength = 1;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 28:
                        fieldLength = 9;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 32:
                    case 33:
                        realLength = (buf[currentPos] & 0xff);
                        fieldLength = (realLength / 2) + (realLength % 2);
                        currentPos++;
                        str = parsingFieldBCD(fieldLength);
                        fields.put(i + 1, str.substring(str.length() - realLength));
                        break;

                    case 34:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 35:
                        realLength = (buf[currentPos] & 0xff);
                        fieldLength = (realLength / 2) + (realLength % 2);
                        currentPos++;
                        sb.setLength(0);

                        for (int j = currentPos; j < currentPos + fieldLength; j++) {
                            sb.append(String.format("%02x", (buf[j] & 0xff)));
                        }

                        sb.replace(sb.indexOf("d"), sb.indexOf("d") + 1, "=");
                        str = sb.toString();
                        fields.put(i + 1, str.substring(str.length() - realLength));
                        break;

                    case 37:
                        fieldLength = 12;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 38:
                        fieldLength = 6;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 39:
                        fieldLength = 2;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 41:
                        fieldLength = 8;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 42:
                        fieldLength = 15;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 43:
                        fieldLength = 40;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 44:
                    case 45:
                    case 46:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 48:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 49:
                    case 51:
                        fieldLength = 2;
                        realLength = 3;
                        str = parsingFieldBCD(fieldLength);
                        fields.put(i + 1, str.substring(str.length() - realLength));
                        break;

                    case 52:
                    case 53:
                        fieldLength = 8;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 54:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 55:
                        //fieldLength = parsingField55(i + 1);
                        fieldLength = Util.parseField55(brand, currentPos, buf, fields);
                        break;

                    case 56:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 59:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 60:
                    case 61:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 62:
                    case 63:
                        fieldLength = parsingFieldBitmap(i + 1);
                        break;

                    case 68:
                    case 70:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 73:
                        fieldLength = 3;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 90:
                        fieldLength = 21;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 91:
                        fieldLength = 1;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 92:
                        fieldLength = 2;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 95:
                        fieldLength = 21;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 99:
                    case 100:
                        fieldLength = (buf[currentPos] & 0xff) / 2;
                        currentPos++;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 101:
                    case 102:
                    case 103:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 104:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 105:
                        fieldLength = 16;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 110:
                    case 111:
                    case 114:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 115:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 116:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 117:
                    case 118:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 120:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 121:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, Util.convertEBDICtoASCII(buf, currentPos, fieldLength));
                        break;

                    case 123:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 125:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                    case 126:
                        fieldLength = parsingFieldBitmap(i + 1);
                        break;

                    case 127:
                    case 130:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 131:
                    case 132:
                        fieldLength = 5;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 134:
                    case 135:
                    case 136:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 137:
                    case 138:
                    case 139:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 140:
                    case 142:
                    case 143:
                        fieldLength = (buf[currentPos] & 0xff);
                        currentPos++;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 144:
                    case 145:
                    case 146:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 147:
                        fieldLength = 6;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 148:
                    case 149:
                    case 152:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;
                }

                currentPos += fieldLength;
            }
        }

        return fields;
    }

    // -----------------------------
}
