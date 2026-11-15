import java.util.BitSet;
import java.util.HashMap;

public class ParserMC {
    // Brand codes: VISA -> 1; MC -> 2; Way4 -> 3
    int brand;
    byte[] buf;
    BitSet bs;
    int currentPos;
    HashMap<Integer, Object> fields;
    private static StringBuilder sb = new StringBuilder();
    private int fieldLength, fieldEndingPos, seIdLength, tagLength, seIdEndingPos, dataSetLength, dataSetEndingPos;

    public ParserMC(int brand, byte[] buf, BitSet bs, int currentPos, HashMap<Integer, Object> fields) {
        this.brand = brand;
        this.buf = buf;
        this.bs = bs;
        this.currentPos = currentPos;
        this.fields = fields;
    }

    // -----------------------------

    String parsingFieldASCII(int fieldLength) {
        sb.setLength(0);
        for (int i = currentPos; i < currentPos + fieldLength; i++) {
            sb.append(String.valueOf((char) (buf[i] & 0xff)));
        }
        return sb.toString();
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

    int parsingField48_MC() {
        int length = 3;
        fieldEndingPos = currentPos + length + Integer.parseInt(parsingFieldASCII(length));
        // System.out.println("currentPos: " + currentPos);
        // System.out.println("fieldEndingPos: " + fieldEndingPos);

        currentPos += length;
        // System.out.println("currentPos: " + currentPos);

        HashMap<String, Object> map_DE = new HashMap<>();

        // System.out.println("TCC: " + parsingFieldASCII(1));
        map_DE.put("TCC", parsingFieldASCII(1));
        currentPos++;

        // System.out.println("map_DE: " + map_DE);
        // System.out.println("currentPos: " + currentPos);

        while (currentPos < fieldEndingPos) {
            // Get SubElement ID
            String seId = parsingFieldASCII(2);
            // System.out.println("seId: " + seId);
            currentPos += 2;

            // Get SubElement ID Length
            seIdLength = Integer.parseInt(parsingFieldASCII(2));
            // System.out.println("seIdLength: " + seIdLength);
            int seIdEndingPos = currentPos + seIdLength;
            // System.out.println("seIdEndingPos: " + seIdEndingPos);
            currentPos += 2;

            switch (seId) {
                case "10":
                case "11":
                case "12":
                case "13":
                case "14":
                case "15":
                case "16":
                case "17":
                case "20":
                case "23":
                case "26":
                case "28":
                case "29":
                case "30":
                case "32":
                case "34":
                case "35":
                case "38":
                case "39":
                case "43":
                case "44":
                case "45":
                case "46":
                case "47":
                case "49":
                case "51":
                case "52":
                case "53":
                case "56":
                case "57":
                case "58":
                case "61":
                case "63":
                case "64":
                case "65":
                case "71":
                case "72":
                case "74":
                case "76":
                case "77":
                case "78":
                case "79":
                case "80":
                case "82":
                case "83":
                case "84":
                case "85":
                case "86":
                case "87":
                case "88":
                case "89":
                case "90":
                case "91":
                case "92":
                case "93":
                case "94":
                case "95":
                case "96":
                case "97":
                case "98":
                case "99":
                    // System.out.println("seId: " + setId + ": " + parsingFieldASCII(seIdLength));
                    map_DE.put(seId, parsingFieldASCII(seIdLength));
                    currentPos += seIdLength;
                    break;

                case "05":
                case "09":
                case "18":
                case "21":
                case "22":
                case "24":
                case "25":
                case "27":
                case "33":
                case "36":
                case "37":
                case "40":
                case "41":
                case "42":
                case "48":
                case "50":
                case "55":
                case "60":
                case "62":
                case "66":
                case "67":
                case "68":
                case "75":
                    HashMap<String, String> map_DE_SE_SF = new HashMap<>();
                    while (currentPos < seIdEndingPos) {

                        // Get SubElement ID
                        String sesfId = parsingFieldASCII(2);
                        // System.out.println("seId: 33 : sesfId : " + sesfId);
                        currentPos += 2;

                        // Get SubElement.SubField ID Length
                        int sesfIdLength = Integer.parseInt(parsingFieldASCII(2));
                        // System.out.println("seId: 33 : sesfId : " + sesfId + " : len : " +
                        // sesfIdLength);
                        currentPos += 2;

                        // System.out.println("seId: 33 : sesfId : " + sesfId + " : val : " +
                        // parsingFieldASCII(sesfIdLength));

                        map_DE_SE_SF.put(sesfId, parsingFieldASCII(sesfIdLength));

                        currentPos += sesfIdLength;
                    }
                    map_DE.put(seId, map_DE_SE_SF);
                    break;

            }
        }

        fields.put(48, map_DE);

        return 0;
    }

    // -----------------------------

    int parsingField55() {
        HashMap<String, Object> mapFieldID = new HashMap<>();

        // System.out.println("currentPos: " + currentPos);
        // String tab = " ";

        // Get field length
        int length = 3;
        fieldLength = Integer.parseInt(parsingFieldASCII(length));
        currentPos += length;

        // System.out.println("(55)ICC - Related Data: ");
        // System.out.println("fieldLength: " + fieldLength);

        fieldEndingPos = currentPos + fieldLength;

        while (currentPos < fieldEndingPos) {
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

            mapFieldID.put(tagId, parsingFieldBCD(tagLength));

            currentPos += tagLength;
        }

        fields.put(55, mapFieldID);

        return 0;
    }

    // -----------------------------

    int parsingField61() {
        int length = 3;
        fieldLength = Integer.parseInt(parsingFieldASCII(length));
        // System.out.println("currentPos: " + currentPos);
        // System.out.println("fieldEndingPos: " + fieldEndingPos);
        currentPos += length;
        // System.out.println("currentPos: " + currentPos);

        HashMap<String, Object> map_DE = new HashMap<>();

        String de61_Value = parsingFieldASCII(fieldLength);

        for (int i = 0; i < fieldLength; i++) {
            switch (i + 1) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                    if (de61_Value.length() >= (i + 1))
                        map_DE.put(String.valueOf(i + 1), de61_Value.substring(i, i + 1));
                    break;

                case 12:
                    if (de61_Value.length() >= (i + 2))
                        map_DE.put(String.valueOf(i + 1), de61_Value.substring(i, i + 2));

                    break;

                case 14:
                    if (de61_Value.length() >= (i + 3))
                        map_DE.put(String.valueOf(i + 1), de61_Value.substring(i, i + 3));

                    break;

                case 17:
                    if (de61_Value.length() >= (i + 9))
                        map_DE.put(String.valueOf(i + 1), de61_Value.substring(i, i + 10));
                    break;
            }
        }

        fields.put(61, map_DE);

        currentPos += fieldLength;

        return 0;
    }

    // -----------------------------

    int parsingField63() {
        int length = 3;
        fieldLength = Integer.parseInt(parsingFieldASCII(length));
        // System.out.println("currentPos: " + currentPos);
        // System.out.println("fieldEndingPos: " + fieldEndingPos);
        currentPos += length;
        // System.out.println("currentPos: " + currentPos);

        HashMap<String, Object> map_DE = new HashMap<>();

        String de61_Value = parsingFieldASCII(fieldLength);

        // SubField 1
        if (de61_Value.length() >= 3)
            map_DE.put("1", de61_Value.substring(0, 3));

        // SubField 2
        if (de61_Value.length() >= 12)
            map_DE.put("2", de61_Value.substring(3, 12));
        else if (de61_Value.length() >= 3)
            map_DE.put("2", de61_Value.substring(3));

        fields.put(63, map_DE);

        currentPos += fieldLength;

        return 0;
    }

    // -----------------------------

    int parsingField90() {
        // System.out.println("currentPos: " + currentPos);
        fieldLength = 42;

        HashMap<String, Object> map_DE = new HashMap<>();

        String de90_Value = parsingFieldASCII(fieldLength);
        // System.out.println("de90_Value: " + de90_Value);

        for (int i = 0; i < fieldLength; i++) {
            switch (i + 1) {
                case 1:
                    if (de90_Value.length() >= (i + 4))
                        map_DE.put("1", de90_Value.substring(i, i + 4));
                    break;

                case 5:
                    if (de90_Value.length() >= (i + 6))
                        map_DE.put("2", de90_Value.substring(i, i + 6));
                    break;

                case 11:
                    if (de90_Value.length() >= (i + 10))
                        map_DE.put("3", de90_Value.substring(i, i + 10));
                    break;

                case 21:
                    if (de90_Value.length() >= (i + 11))
                        map_DE.put("4", de90_Value.substring(i, i + 11));

                    break;

                case 32:
                    if (de90_Value.length() >= (i + 11))
                        map_DE.put("5", de90_Value.substring(i, i + 11));

                    break;
            }
        }

        fields.put(90, map_DE);

        currentPos += fieldLength;

        return 0;
    }

    // -----------------------------

    int parsingFieldTLV(int field) {
        int length = 3;
        fieldEndingPos = currentPos + length + Integer.parseInt(parsingFieldASCII(length));
        // System.out.println("currentPos: " + currentPos);
        // System.out.println("fieldEndingPos: " + fieldEndingPos);

        currentPos += length;
        // System.out.println("currentPos: " + currentPos);

        HashMap<String, Object> map_DE = new HashMap<>();

        // System.out.println("currentPos: " + currentPos);
        while (currentPos < fieldEndingPos) {
            // Get SubElement ID
            String seId = parsingFieldASCII(3);
            // System.out.println("seId: " + seId);
            currentPos += 3;

            // Get SubElement ID Length
            seIdLength = Integer.parseInt(parsingFieldASCII(3));
            // System.out.println("seIdLength: " + seIdLength);
            int seIdEndingPos = currentPos + seIdLength;
            // System.out.println("seIdEndingPos: " + seIdEndingPos);
            currentPos += 3;

            switch (seId) {
                case "001":
                    if (field == 106) {
                        HashMap<String, String> map_DE_SE_SF = new HashMap<>();
                        while (currentPos < seIdEndingPos) {

                            // Get SubElement ID
                            String sesfId = parsingFieldASCII(3);
                            // System.out.println("seId: 33 : sesfId : " + sesfId);
                            currentPos += 3;

                            // Get SubElement.SubField ID Length
                            int sesfIdLength = Integer.parseInt(parsingFieldASCII(3));
                            // System.out.println("seId: 33 : sesfId : " + sesfId + " : len : " +
                            // sesfIdLength);
                            currentPos += 3;

                            // System.out.println("seId: 33 : sesfId : " + sesfId + " : val : " +
                            // parsingFieldASCII(sesfIdLength));

                            map_DE_SE_SF.put(sesfId, parsingFieldASCII(sesfIdLength));

                            currentPos += sesfIdLength;
                        }
                        map_DE.put(seId, map_DE_SE_SF);
                    } else {
                        map_DE.put(seId, parsingFieldASCII(seIdLength));
                        currentPos += seIdLength;
                    }
                    break;

                case "002":
                case "003":
                    if (field == 108) {
                        HashMap<String, String> map_DE_SE_SF = new HashMap<>();
                        while (currentPos < seIdEndingPos) {

                            // Get SubElement ID
                            String sesfId = parsingFieldASCII(3);
                            // System.out.println("seId: 33 : sesfId : " + sesfId);
                            currentPos += 3;

                            // Get SubElement.SubField ID Length
                            int sesfIdLength = Integer.parseInt(parsingFieldASCII(3));
                            // System.out.println("seId: 33 : sesfId : " + sesfId + " : len : " +
                            // sesfIdLength);
                            currentPos += 3;

                            // System.out.println("seId: 33 : sesfId : " + sesfId + " : val : " +
                            // parsingFieldASCII(sesfIdLength));

                            map_DE_SE_SF.put(sesfId, parsingFieldASCII(sesfIdLength));

                            currentPos += sesfIdLength;
                        }
                        map_DE.put(seId, map_DE_SE_SF);
                    } else {
                        map_DE.put(seId, parsingFieldASCII(seIdLength));
                        currentPos += seIdLength;
                    }
                    break;

                case "004":
                case "005":
                    HashMap<String, String> map_DE_SE_SF = new HashMap<>();
                    while (currentPos < seIdEndingPos) {

                        // Get SubElement ID
                        String sesfId = parsingFieldASCII(3);
                        // System.out.println("seId: 33 : sesfId : " + sesfId);
                        currentPos += 3;

                        // Get SubElement.SubField ID Length
                        int sesfIdLength = Integer.parseInt(parsingFieldASCII(3));
                        // System.out.println("seId: 33 : sesfId : " + sesfId + " : len : " +
                        // sesfIdLength);
                        currentPos += 3;

                        // System.out.println("seId: 33 : sesfId : " + sesfId + " : val : " +
                        // parsingFieldASCII(sesfIdLength));

                        map_DE_SE_SF.put(sesfId, parsingFieldASCII(sesfIdLength));

                        currentPos += sesfIdLength;
                    }
                    map_DE.put(seId, map_DE_SE_SF);
                    break;
            }
        }

        fields.put(field, map_DE);

        return 0;
    }

    // -----------------------------

    public HashMap<Integer, Object> parse() {
        int bitmapLength = bs.length() < 65 ? 8 : 16;
        int fieldLength = 0;
        int length = 0;
        int realLength = 0;

        String str;

        currentPos += bitmapLength;
        for (int i = 0; i < bs.length(); i++) {
            if (bs.get(i)) {
                switch (i + 1) {
                    case 2:
                        length = 2;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 3:
                        fieldLength = 6;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 4:
                    case 5:
                    case 6:
                        fieldLength = 12;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 7:
                        fieldLength = 10;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 8:
                    case 9:
                    case 10:
                        fieldLength = 8;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 11:
                    case 12:
                        fieldLength = 6;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                        fieldLength = 4;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                        fieldLength = 3;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 25:
                    case 26:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 27:
                        fieldLength = 1;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 28:
                    case 29:
                    case 30:
                    case 31:
                        fieldLength = 9;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 32:
                    case 33:
                    case 34:
                    case 35:
                        length = 2;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 36:
                        length = 3;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 37:
                        fieldLength = 12;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 38:
                        fieldLength = 6;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 39:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 40:
                        fieldLength = 3;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 41:
                        fieldLength = 8;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 42:
                        fieldLength = 15;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 43:
                        fieldLength = 40;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 44:
                    case 45:
                        length = 2;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    // ToDo...
                    // Way4 dialect
                    case 46:
                    case 47:
                        break;

                    case 48:
                        int tcc = (buf[currentPos + 3] & 0xff);
                        // System.out.println("tcc: " + tcc);

                        if (((tcc >= 65) & (tcc <= 90))
                                || (tcc == 32)) {
                            // MC 8583 dialect - TCC is between "A" and "Z" OR is space
                            fieldLength = parsingField48_MC();
                        } else {
                            // ToDo...
                            // Way4 8583 dialect
                        }
                        break;

                    case 49:
                    case 50:
                    case 51:
                        fieldLength = 3;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 52:
                        fieldLength = 8;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 53:
                        fieldLength = 16;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 54:
                        length = 3;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 55:
                        //fieldLength = parsingField55();
                        fieldLength = Util.parseField55(brand, currentPos, buf, fields);
                        break;

                    case 57:
                    case 60:
                        length = 3;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 61:
                        fieldLength = parsingField61();
                        break;

                    case 62:
                        length = 3;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 63:
                        fieldLength = parsingField63();
                        break;

                    case 64:
                        fieldLength = 8;
                        fields.put(i + 1, parsingFieldBCD(fieldLength));
                        break;

                    case 66:
                        fieldLength = 1;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 67:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 68:
                    case 69:
                    case 70:
                        fieldLength = 3;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 71:
                    case 72:
                        fieldLength = 4;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 73:
                        fieldLength = 6;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                        fieldLength = 10;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 82:
                    case 83:
                    case 84:
                    case 85:
                        fieldLength = 12;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 86:
                    case 87:
                    case 88:
                    case 89:
                        fieldLength = 16;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 90:
                        fieldLength = parsingField90();
                        break;

                    case 91:
                        fieldLength = 1;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 92:
                    case 93:
                        fieldLength = 2;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 94:
                        fieldLength = 7;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 95:
                        fieldLength = 42;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 96:
                        fieldLength = 8;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 97:
                        fieldLength = 17;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 98:
                        fieldLength = 25;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 99:
                    case 100:
                        length = 11;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 101:
                        length = 17;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 102:
                    case 103:
                        length = 28;
                        fieldLength = Integer.parseInt(parsingFieldASCII(length));
                        currentPos += length;
                        fields.put(i + 1, parsingFieldASCII(fieldLength));
                        break;

                    case 104:
                    case 105:
                    case 106:
                    case 108:
                        fieldLength = parsingFieldTLV(i + 1);
                        break;

                }

                currentPos += fieldLength;
            }
        }

        return fields;
    }
}
