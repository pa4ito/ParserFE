import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

public class ISOMessage {
    // Brand codes: VISA -> 1; MC -> 2; Way4 -> 3
    private int brand;
    private int msgLength;
    private int msgRealLength;
    private int msgType;
    private BitSet bs;
    private HashMap<Integer, Object> fields = new HashMap<>();
    byte[] buf = null;

    ISOMessage(byte[] buffer) {
        buf = Arrays.copyOf(buffer, buffer.length);
    }

    // -----------------------------
    public void parseVisa() throws Exception {
        brand = 1;

        /*
         * Getting the length of the incoming message
         */
        msgLength = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
        // System.out.println("msgInLength: 0x"
        // + String.format("%02x", ((buf[0] & 0xff) << 8) | (buf[1] & 0xff))
        // + " -> "
        // + msgLength);

        // VISA - First 4 bytes are length
        if (msgLength < (buf.length - 4)) {
            throw new Exception("The incoming message is not complete!!!");
        }

        msgRealLength = msgLength - 22;
        // System.out.println("msgRealLength: " + msgRealLength);

        /* Getting a message type */
        msgType = Integer.parseInt(String.format("%02x", ((buf[26] & 0xff) << 8) | (buf[27] & 0xff)));
        // System.out.println("msgType: " + msgType);

        int pos;
        
        try {
            /* Getting bitmap */
            pos = 28;
            bs = Util.getBitmap(buf, pos);
            // System.out.println("-----");
            // System.out.println("bitmap: " + bs);
            // System.out.println("bs.length(): " + bs.length());

            fields = new ParserVisa(brand, buf, bs, pos, fields).parse();
        } catch (Exception e) {
            e.printStackTrace();
            this.printFields();
            System.exit(0);
        }
    }

    // -----------------------------

    public void parseMC() throws Exception {
        brand = 2;

        /*
         * Getting the length of the incoming message
         */
        msgLength = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
        // System.out.println("msgInLength: 0x"
        // + String.format("%02x", ((buf[0] & 0xff) << 8) | (buf[1] & 0xff))
        // + " -> "
        // + msgLength);

        // MC / Way4 - First 2 bytes are length
        if (msgLength < (buf.length - 2)) {
            throw new Exception("The incoming message is not complete!!!");
        }

        msgRealLength = msgLength - 2;
        // System.out.println("msgRealLength: " + msgRealLength);

        /* Getting a message type */
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i <= 5; i++) {
            // sb.append(String.format("%02x", (buf[i] & 0xff)));
            sb.append(String.valueOf((char) (buf[i] & 0xff)));
        }
        msgType = Integer.parseInt(sb.toString());
        // System.out.println("msgType: " + msgType);

        int pos;
        
        try {
            /* Getting bitmap */
            pos = 6;
            bs = Util.getBitmap(buf, pos);
            // System.out.println("-----");
            // System.out.println("bitmap: " + bs);
            // System.out.println("bs.length(): " + bs.length());

            fields = new ParserMC(brand, buf, bs, pos, fields).parse();
        } catch (Exception e) {
            e.printStackTrace();
            this.printFields();
            System.exit(0);
        }
    }

    // ----------------------------------

    public void parseWay4() throws Exception {
        brand = 3;

        /*
         * Getting the length of the incoming message
         */
        msgLength = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
        // System.out.println("msgInLength: 0x"
        // + String.format("%02x", ((buf[0] & 0xff) << 8) | (buf[1] & 0xff))
        // + " -> "
        // + msgLength);

        // MC / Way4 - First 2 bytes are length
        if (msgLength < (buf.length - 2)) {
            throw new Exception("The incoming message is not complete!!!");
        }

        msgRealLength = msgLength - 2;
        // System.out.println("msgRealLength: " + msgRealLength);

        /* Getting a message type */
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i <= 5; i++) {
            sb.append(String.valueOf((char) (buf[i] & 0xff)));
        }
        msgType = Integer.parseInt(sb.toString());
        // System.out.println("msgType: " + msgType);

        int pos;
        
        try {
            /* Getting bitmap */
            pos = 6;
            bs = Util.getBitmap(buf, pos);
            // System.out.println("-----");
            // System.out.println("bitmap: " + bs);
            // System.out.println("bs.length(): " + bs.length());

            fields = new ParserWay4(brand, buf, bs, pos, fields).parse();
        } catch (Exception e) {
            e.printStackTrace();
            this.printFields();
            System.exit(0);
        }
    }

    // ----------------------------------

    public void printMTI() {
        System.out.printf("MTI: %04d%n", msgType);
    }

    // ----------------------------------

    public void printMessageLength() {
        String msgLen = String.format("%02x", ((buf[0] & 0xff) << 8) | (buf[1] & 0xff));
        if (msgLen.length() % 2 != 0)
            msgLen = "0" + msgLen;
        System.out.println("Length: 0x" + msgLen + " -> " + msgLength);
    }

    // ----------------------------------

    public void printMessageRealLength() {
        System.out.println("RealLength: " + msgRealLength);
    }

    // ----------------------------------

    public void printFields() {
        // for (int field : fields.keySet()) {
        for (int i = 0; i < bs.length(); i++) {
            if (bs.get(i)) {
                if (i == 0)
                    System.out.printf("%s: %3d -> %s", "field", (i + 1), bs);
                else
                    System.out.printf("%s: %3d -> %s", "field", (i + 1), fields.get(i + 1));
                System.out.println();
            }
        }
    }

    // ----------------------------------

    public void printBitmap() {
        System.out.println("-----");
        // System.out.println("bs.length(): " + bs.length());
        // System.out.println("bitmap: " + bs);
        System.out.print("bitmap: {");
        for (int i = 0; i < bs.length(); i++) {
            if (bs.get(i)) {
                System.out.print((i + 1));
                if (i < (bs.length() - 1))
                    System.out.print(", ");
            }
        }
        System.out.println("}");
        // System.out.println();
        System.out.println("-----");
    }

    // ----------------------------------

    public void printBrand() {
        System.out.print("brand: ");
        switch (brand) {
            case 1:
                System.out.println("VISA");
                break;

            case 2:
                System.out.println("MasterCard");
                break;

            case 3:
                System.out.println("Way4");
                break;
        }
    }
}
