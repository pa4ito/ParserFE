import java.util.BitSet;
import java.util.HashMap;

public class ParserWay4 {
    // Brand codes: VISA -> 1; MC -> 2; Way4 -> 3
    int brand;
    byte[] buf;
    BitSet bs;
    int currentPos;
    HashMap<Integer, Object> fields;

    public ParserWay4(int brand, byte[] buf, BitSet bs, int currentPos, HashMap<Integer, Object> fields) {
        this.brand = brand;
        this.buf = buf;
        this.bs = bs;
        this.currentPos = currentPos;
        this.fields = fields;
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
                        fieldLength = Integer.parseInt(Util.parseBCD(1, currentPos, buf)) / 2;
                        currentPos++;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 3:
                        fieldLength = 3;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 4:
                    case 6:
                        fieldLength = 6;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 7:
                        fieldLength = 5;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 10:
                        fieldLength = 4;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 11:
                    case 12:
                        fieldLength = 3;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 13:
                    case 14:
                    case 16:
                    case 18:
                    case 19:
                    case 20:
                    case 22:
                    case 23:
                        fieldLength = 2;
                        String val = Util.parseBCD(fieldLength, currentPos, buf);
                        if (i + 1 > 18)
                            val = val.substring(1);
                        fields.put(i + 1, val);
                        break;

                    case 25:
                    case 26:
                        fieldLength = 1;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 28:
                        fieldLength = 9;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 32:
                    case 33:
                    case 35:
                        realLength = Integer.parseInt(Util.parseBCD(1, currentPos, buf));
                        fieldLength = (realLength / 2) + (realLength % 2);
                        currentPos++;
                        str = Util.parseBCD(fieldLength, currentPos, buf);
                        fields.put(i + 1, str.substring(str.length() - realLength));
                        break;

                    case 37:
                        fieldLength = 12;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 38:
                        fieldLength = 6;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 39:
                        fieldLength = 2;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 41:
                        fieldLength = 8;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 42:
                        fieldLength = 15;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 43:
                        fieldLength = 40;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 44:
                    case 45:
                        fieldLength = Integer.parseInt(Util.parseBCD(1, currentPos, buf));
                        currentPos++;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 46:
                    case 47:
                    case 48:
                        fieldLength = Util.parseWay4_46_47_48(i + 1, currentPos, buf, fields);
                        break;

                    case 49:
                    case 51:
                        fieldLength = 2;
                        realLength = 3;
                        str = Util.parseBCD(fieldLength, currentPos, buf);
                        fields.put(i + 1, str.substring(str.length() - realLength));
                        break;

                    case 52:
                    case 53:
                        fieldLength = 8;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 54:
                        fieldLength = Integer.parseInt(Util.parseBCD(2, currentPos, buf));
                        currentPos += 2;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 55:
                        fieldLength = Util.parseField55(brand, currentPos, buf, fields);
                        break;

                    case 70:
                        fieldLength = 2;
                        fields.put(i + 1,
                                Util.parseBCD(fieldLength, currentPos, buf).substring(1));
                        break;

                    case 90:
                        fieldLength = 21;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 91:
                        fieldLength = 1;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 95:
                        fieldLength = 21;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 100:
                        fieldLength = Integer.parseInt(Util.parseBCD(1, currentPos, buf));
                        currentPos++;
                        fields.put(i + 1, Util.parseBCD(fieldLength, currentPos, buf));
                        break;

                    case 101:
                    case 102:
                    case 103:
                    case 104:
                        fieldLength = Integer.parseInt(Util.parseBCD(1, currentPos, buf));
                        currentPos++;
                        fields.put(i + 1, Util.parseASCII(fieldLength, currentPos, buf));
                        break;

                    case 112:
                        fieldLength = Util.parseBerTlv(112, currentPos, buf, fields);
                        break;

                    case 122:
                        fieldLength = Integer.parseInt(Util.parseBCD(2, currentPos, buf));
                        currentPos += 2;
                        val = Util.parseASCII(fieldLength, currentPos, buf);
                        fields.put(i + 1, val);
                        break;
                }

                currentPos += fieldLength;
            }
        }

        return fields;
    }

    // -----------------------------

}
