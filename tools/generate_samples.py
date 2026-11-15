#!/usr/bin/env python3
"""
Generates SYNTHETIC ISO 8583 sample messages for ParserFE.

All values are fictitious: the card numbers are the public industry test
numbers (Visa 4111 1111 1111 1111, Mastercard 5555 5555 5555 4444), and the
merchant, terminal, institution and EMV values are made up.

Usage: python3 tools/generate_samples.py   (writes the files into samples/)
"""
import os

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "samples")


# ---------------------------------------------------------------- helpers

def bcd(digits: str) -> bytes:
    """Packed BCD; left-padded with 0 to an even number of digits."""
    if len(digits) % 2:
        digits = "0" + digits
    return bytes.fromhex(digits)


def ebcdic(text: str) -> bytes:
    """ASCII -> EBCDIC (cp037) for upper-case letters, digits and spaces."""
    return text.encode("cp037")


def bitmap(fields) -> bytes:
    """Primary bitmap (fields 2..64) as 8 binary bytes."""
    value = 0
    for f in fields:
        value |= 1 << (64 - f)
    return value.to_bytes(8, "big")


def tlv(tag: str, value_hex: str) -> bytes:
    value = bytes.fromhex(value_hex)
    return bytes.fromhex(tag) + bytes([len(value)]) + value


def emv_data(aid: str, amount: str, currency: str, country: str, date: str) -> bytes:
    """Synthetic EMV chip data (field 55)."""
    return b"".join([
        tlv("82", "1980"),                 # Application Interchange Profile
        tlv("84", aid),                    # Dedicated File (AID)
        tlv("95", "0000000000"),           # Terminal Verification Results
        tlv("9A", date),                   # Transaction Date YYMMDD
        tlv("9C", "00"),                   # Transaction Type (purchase)
        tlv("5F2A", currency),             # Transaction Currency Code
        tlv("9F02", amount),               # Amount, Authorised
        tlv("9F03", "000000000000"),       # Amount, Other
        tlv("9F10", "06011203A00000"),     # Issuer Application Data
        tlv("9F1A", country),              # Terminal Country Code
        tlv("9F26", "1A2B3C4D5E6F7081"),   # Application Cryptogram
        tlv("9F27", "80"),                 # Cryptogram Information Data
        tlv("9F33", "E0F0C8"),             # Terminal Capabilities
        tlv("9F34", "1F0302"),             # CVM Results
        tlv("9F36", "0001"),               # Application Transaction Counter
        tlv("9F37", "12345678"),           # Unpredictable Number
    ])


def write(name: str, data: bytes):
    os.makedirs(OUT_DIR, exist_ok=True)
    hex_text = data.hex().upper()
    # 32 bytes per line, bytes separated by spaces (the parser ignores whitespace)
    lines = [" ".join(hex_text[i:i + 2] for i in range(j, min(j + 64, len(hex_text)), 2))
             for j in range(0, len(hex_text), 64)]
    with open(os.path.join(OUT_DIR, name + ".txt"), "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(OUT_DIR, name + ".bin"), "wb") as f:
        f.write(data)
    print(f"{name}: {len(data)} bytes")


# ------------------------------------------------------------------ VISA
# 4-byte length prefix + 22-byte VISA header + MTI (BCD) + bitmap + fields
# (numeric fields in BCD, text fields in EBCDIC)

def visa_0100() -> bytes:
    pan = "4111111111111111"
    emv = emv_data("A0000000031010", "000000002500", "0978", "0276", "251115")
    f55 = b"\x01" + len(emv).to_bytes(2, "big") + emv          # dataset 01

    fields = {
        2: bytes([len(pan)]) + bcd(pan),
        3: bcd("000000"),
        4: bcd("000000002500"),
        7: bcd("1115103045"),
        11: bcd("000123"),
        14: bcd("2812"),
        18: bcd("5411"),
        19: bcd("0276"),
        22: bcd("0510"),
        23: bcd("0001"),
        25: bcd("00"),
        32: bytes([6]) + bcd("123456"),
        37: ebcdic("531910000123"),
        41: ebcdic("TERM0001"),
        42: ebcdic("MERCHANT0000001"),
        43: ebcdic("DEMO GROCERY STORE       SAMPLE CITY  DE"),
        49: bcd("0978"),
        55: bytes([len(f55)]) + f55,
    }
    assert len(fields[43]) == 40
    body = bcd("0100") + bitmap(fields) + b"".join(fields[f] for f in sorted(fields))

    total = 22 + len(body)
    header = (bytes([0x16, 0x01, 0x02]) + total.to_bytes(2, "big")
              + bytes(3)        # destination station id
              + bytes(3)        # source station id
              + bytes(11))      # round-trip / flags / status / batch / reserved / user info
    assert len(header) == 22
    return total.to_bytes(2, "big") + b"\x00\x00" + header + body


# ------------------------------------------------------------ Mastercard
# 2-byte length prefix + MTI (ASCII) + binary bitmap + ASCII fields

def mc_0100() -> bytes:
    pan = "5555555555554444"
    emv = emv_data("A0000000041010", "000000004999", "0978", "0250", "251115")

    fields = {
        2: f"{len(pan):02d}{pan}".encode(),
        3: b"000000",
        4: b"000000004999",
        7: b"1115103045",
        11: b"000456",
        12: b"113045",
        13: b"1115",
        14: b"2809",
        18: b"5812",
        22: b"051",
        23: b"001",
        32: b"06654321",
        37: b"531910000456",
        41: b"TERM0002",
        42: b"MERCHANT0000002",
        43: ("SAMPLE CAFE".ljust(23) + "EXAMPLEVILLE".ljust(14) + "FRA").encode(),
        49: b"978",
        55: f"{len(emv):03d}".encode() + emv,
    }
    assert len(fields[43]) == 40
    body = b"0100" + bitmap(fields) + b"".join(fields[f] for f in sorted(fields))
    return len(body).to_bytes(2, "big") + body


# ------------------------------------------------------------------ Way4
# 2-byte length prefix + MTI (ASCII) + binary bitmap + BCD / ASCII fields

def way4_0100() -> bytes:
    pan = "4111111111111111"
    emv = emv_data("A0000000031010", "000000001500", "0978", "0040", "251115")
    f48 = b"002005DEMO1" + b"003008TESTDATA"                  # tag(3) + len(3) + value

    fields = {
        2: bcd(str(len(pan))) + bcd(pan),
        3: bcd("000000"),
        4: bcd("000000001500"),
        7: bcd("1115103045"),
        11: bcd("000789"),
        12: bcd("113045"),
        13: bcd("1115"),
        14: bcd("2706"),
        18: bcd("5999"),
        22: bcd("0051"),
        23: bcd("0001"),
        32: bcd("06") + bcd("777888"),
        37: b"531910000789",
        41: b"TERM0003",
        42: b"MERCHANT0000003",
        43: ("EXAMPLE BOOKSHOP".ljust(23) + "DEMO TOWN".ljust(14) + "AUT").encode(),
        48: bcd(f"{len(f48):04d}") + f48,
        49: bcd("0978"),
        55: bcd(f"{len(emv):04d}") + emv,
    }
    assert len(fields[43]) == 40
    body = b"0100" + bitmap(fields) + b"".join(fields[f] for f in sorted(fields))
    return len(body).to_bytes(2, "big") + body


if __name__ == "__main__":
    write("visa_0100", visa_0100())
    write("mc_0100", mc_0100())
    write("way4_0100", way4_0100())
