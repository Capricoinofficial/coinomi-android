package com.coinomi.core.coins;

import com.coinomi.core.coins.families.PeerFamily;

/**
 * @author John L. Jegutanis
 */
public class CapricoinMain extends PeerFamily {
    private CapricoinMain() {
        id = "capricoin.main";

        addressHeader = 28;
        p2shHeader = 35;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 100;
        dumpedPrivateKeyHeader = 156;

        name = "Capricoin";
        symbol = "CPC";
        uriScheme = "capricoin";
        bip44Index = 10;
        unitExponent = 8;
        feeValue = value(1000); // 0.00001 CPC
        minNonDust = value(1);
        softDustLimit = value(10000); // 0.0001 CPC
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT;
        signedMessageHeader = toBytes("Capricoin Signed Message:\n");
    }

    private static CapricoinMain instance = new CapricoinMain();
    public static synchronized CoinType get() {
        return instance;
    }
}
