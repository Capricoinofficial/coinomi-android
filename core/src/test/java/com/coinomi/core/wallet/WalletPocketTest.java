package com.coinomi.core.wallet;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.DogecoinTest;
import com.coinomi.core.network.AddressStatus;
import com.coinomi.core.network.BlockchainConnection;
import com.coinomi.core.network.ServerClient;
import com.coinomi.core.network.interfaces.TransactionEventListener;
import com.coinomi.core.protos.Protos;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicHierarchy;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Giannis Dzegoutanis
 */
public class WalletPocketTest {
    static final List<String> MNEMONIC = ImmutableList.of("citizen", "fever", "scale", "nurse", "brief", "round", "ski", "fiction", "car", "fitness", "pluck", "act");
    DeterministicSeed seed = new DeterministicSeed(MNEMONIC, "", 0);
    private DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
    CoinType type = DogecoinTest.get();
    DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);
    DeterministicKey rootKey = hierarchy.get(type.getBip44Path(0), false, true);
    private WalletPocket pocket;

    @Before
    public void setup() {
        BriefLogFormatter.init();

        pocket = new WalletPocket(rootKey, type);
    }

    @Test
    public void watchingAddresses() {
        List<Address> watchingAddresses = pocket.getAddressesToWatch();
        assertEquals(40, watchingAddresses.size()); // 20 + 20 lookahead size
        for (int i = 0; i < addresses.length; i++) {
            assertEquals(addresses[i], watchingAddresses.get(i).toString());
        }
    }

    @Test
    public void fillTransactions() throws AddressFormatException, JSONException {
        final HashMap<Address, AddressStatus> statuses = getDummyStatuses();
        final HashMap<Address, List<ServerClient.UnspentTx>> utxs = getDummyUTXs();
        final HashMap<Sha256Hash, byte[]> rawTxs = getDummyRawTXs();

        pocket.onConnection(new BlockchainConnection() {
            @Override
            public void subscribeToAddresses(CoinType coin, List<Address> addresses, TransactionEventListener listener) {
                for (Address a : addresses) {
                    AddressStatus status = statuses.get(a);
                    if (status == null) {
                        status = new AddressStatus(a, null);
                    }
                    listener.onAddressStatusUpdate(status);
                }
            }

            @Override
            public void getUnspentTx(CoinType coinType, AddressStatus status, TransactionEventListener listener) {
                List<ServerClient.UnspentTx> utx = utxs.get(status.getAddress());
                if (status == null) {
                    utx = ImmutableList.of();
                }
                listener.onUnspentTransactionUpdate(status, utx);
            }

            @Override
            public void getTx(CoinType coinType, AddressStatus status, ServerClient.UnspentTx utx, TransactionEventListener listener) {
                listener.onTransactionUpdate(utx, rawTxs.get(utx.getTxHash()));
            }

            @Override
            public void ping() {

            }
        });

        // Issued keys
        assertEquals(18, pocket.keys.getIssuedExternalKeys());
        assertEquals(9, pocket.keys.getIssuedInternalKeys());

        // No addresses left to subscribe
        List<Address> addressesToWatch = pocket.getAddressesToWatch();
        assertEquals(0, addressesToWatch.size());

        // 18 external issued + 20 lookahead +  9 external issued + 20 lookahead
        assertEquals(67, pocket.addressesStatus.size());
        assertEquals(67, pocket.addressesSubscribed.size());

        pocket.getReceiveAddress();
        assertEquals(19, pocket.keys.getIssuedExternalKeys());
        assertEquals(68, pocket.addressesStatus.size());
        assertEquals(68, pocket.addressesSubscribed.size());

        // TODO added more tests to insure it uses the "holes" in the keychain
    }


    @Test
    public void serializeUnencryptedNormal() throws UnreadableWalletException {
        serializeUnencrypted("");
    }

    public void serializeUnencrypted(String expectedSerialization) throws UnreadableWalletException {
        Protos.WalletPocket walletPocketProto = pocket.toProtobuf();

        assertEquals(expectedSerialization, walletPocketProto.toString());
    }


    HashMap<Address, AddressStatus> getDummyStatuses() throws AddressFormatException {
        HashMap<Address, AddressStatus> status = new HashMap<Address, AddressStatus>(40);

        for (int i = 0; i < addresses.length; i++) {
            Address address = new Address(type, addresses[i]);
            status.put(address, new AddressStatus(address, statuses[i]));
        }

        return status;
    }

    private HashMap<Address,List<ServerClient.UnspentTx>> getDummyUTXs() throws AddressFormatException, JSONException {
        HashMap<Address, List<ServerClient.UnspentTx>> utxs = new HashMap<Address, List<ServerClient.UnspentTx>>(40);

        for (int i = 0; i < statuses.length; i++) {
            utxs.put(new Address(type, addresses[i]), ServerClient.UnspentTx.fromArray(new JSONArray(unspent[i])));
        }

        return utxs;
    }

    private HashMap<Sha256Hash, byte[]> getDummyRawTXs() throws AddressFormatException, JSONException {
        HashMap<Sha256Hash, byte[]> rawTxs = new HashMap<Sha256Hash, byte[]>();

        for (int i = 0; i < txs.length; i++) {
            String[] txEntry = txs[i];
            rawTxs.put(new Sha256Hash(txs[i][0]), Utils.HEX.decode(txs[i][1]));
        }

        return rawTxs;
    }

    String[] addresses = {
            "nYFeqwDM3Wgoa6uSpxHoXrTHKaEH2DzJt7",
            "nfgMcGAFcXqHnzJkGPhxAYK9nmDPmfFipP",
            "ns1bSy9bbS5iW7f156iLvCXqzDx5KbZBkK",
            "nV6qdoT3y4cVfTYUYWShhf1kraVDEUQLWk",
            "nsRxBPxUwPnNH9gP13hhGHYxBGfYFuDmvw",
            "nc6f6D7g2ZGri3etd1qvoVaZ2ws8nuSwe6",
            "nYr9JVhDGdvpmgeM6qHiKSKpzTKwkKiZy7",
            "nXbPMkrHsm2376JHV5fExjJV5ir5FLwqB3",
            "nbNnS9CpedCzUD5h83MMP1ViDZVz8ogZ87",
            "nXuh974fJejhcBZhYqedyGJ2mFHi6q6Xby",
            "ns3onfhQNfoUtxKJoRPG1TmgRSdHBvc7Ae",
            "naJzuBsmcN2JRQ1F2bUuuMWqvqEapYPcV4",
            "nkQUsdUivWTMb5AQSs4KA2NLFzsHAdGnF1",
            "nYZ8UnuSw6CVzgafX9SjLer2Vne83o49Vu",
            "nVALmju3ZTrG3ZnoKMdDWc5h7mw5ZpGUXi",
            "nksLoC68mXgHEbmKvLRt3uy1T99r3kFJnx",
            "nnoCjmceF5sEaSUVjyJGZ82Lt5JPtxfSTW",
            "nYtgLvqnUGfH1bsdtT89pbHnwhyTmMDUUZ",
            "nVy9vTMs3Fd3LeWZfSufAVqcymiqNA7Ej4",
            "nUZMEXha6DqVCCLXUavbpNQT9EA8x7zsKF",
            "ncCd8qUxmuMQmyxfWHfZwXmuj1U5Fd8bFh",
            "nX719JSGwSTABddKFk5i6VSGsANSVq3Quy",
            "ns8g4haVJpS9hq5jCErmgpyaMqrmkaZttx",
            "nUWeegDyag2FFxQaEbYSw5QZv5TnYU1rTC",
            "ncAM9qkZZQ6ncH5eTTq5x2GM1ziwvtn671",
            "npSJBfhVeJvo6gvwkU4ZxnrJBfRzjzwTwx",
            "nUgx4NJqjHcka1HNg3MxHTVxKRWj1meDk5",
            "nqpUfJnKXuZoRegF7GUzzFfjEuCFSqdwZa",
            "nmXS4wzdadXs73bvHJgQr3kwCyYxMbAXoQ",
            "nn4XWZRsNjPsZMa9eU3p7pSSTmowXGMxsw",
            "ndfhy13fYFbVFhjyDcsH6kRAGnkHxuSNgV",
            "nUWF48YWZqprfKPtB9TemaCQjXME7ufxNs",
            "nX8z9Hsa47TeEyFCAAe48azBAZdL5JzsNo",
            "niWd7u3bkSnJSomA2LFBqiyXFkrgv6G1WJ",
            "nnSg5ePzYyx7BEWXdcbSWDWHd3dJUwiLh1",
            "nhbXcTtx5BzSMvhpyEoCKHcfss8zGoLP5K",
            "nipWJ97boZtn26Fg8SHWMz9PH8TgDS2bem",
            "nrFHEkMkk47uKAvp6aT5z2aBg1LshrfpBz",
            "ndNYuT9kEFMdWcNnXXndzjcvmMbGmtoufm",
            "nmGojMLim2J33e9SJo72y93AkqEZbHMn9V"
    };

    String[] statuses = {
            "fe7c109d8bd90551a406cf0b3499117db04bc9c4f48e1df27ac1cf3ddcb3d464",
            "8a53babd831c6c3a857e20190e884efe75a005bdd7cd273c4f27ab1b8ec81c2d",
            "86bc2f0cf0112fd59c9aadfe5c887062c21d7a873db260dff68dcfe4417fe212",
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            null,
            null,
            null,
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            null,
            null,
            null,
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            null,
            null,
            null,
            null,
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            null,
            null,
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            null,
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            null,
            null,
            null,
            "64a575b5605671831185ca715e8197f0455733e721a6c6c5b8add31bd6eabbe9",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
    };


    String[] unspent = {
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 0, \"value\": 500000000, \"height\": 160267}, {\"tx_hash\": \"dcb7d6ee9b97a66ddb1342ff4836b9f426ac71103dd48702b212c0930899a364\", \"tx_pos\": 0, \"value\": 500000000, \"height\": 160267}]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 1, \"value\": 1000000000, \"height\": 160267}, {\"tx_hash\": \"c8b259c80b1e2b33f554ec529962a1b7ba6b515f59c26d654dd467c8b375e900\", \"tx_pos\": 0, \"value\": 500000000, \"height\": 160267}]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 2, \"value\": 500000000, \"height\": 160267}, {\"tx_hash\": \"defadb60324f3b4bca36d4d6fdf8c0492e4c0df01143e54c24360a49f998affe\", \"tx_pos\": 0, \"value\": 1000000000, \"height\": 160267}]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 3, \"value\": 500000000, \"height\": 160267}]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 4, \"value\": 1000000000, \"height\": 160267}]",
            "[]",
            "[]",
            "[]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 11, \"value\": 500000000, \"height\": 160267}]",
            "[]",
            "[]",
            "[]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 12, \"value\": 1000000000, \"height\": 160267}]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 13, \"value\": 500000000, \"height\": 160267}]",
            "[]",
            "[]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 6, \"value\": 500000000, \"height\": 160267}]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 7, \"value\": 1000000000, \"height\": 160267}]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 8, \"value\": 500000000, \"height\": 160267}]",
            "[]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 9, \"value\": 500000000, \"height\": 160267}]",
            "[]",
            "[]",
            "[]",
            "[{\"tx_hash\": \"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a\", \"tx_pos\": 10, \"value\": 1000000000, \"height\": 160267}]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[]",
            "[]"
    };

    String[][] txs = {
        {"c75e4a439d9b33da4883ea0c88f814cb11f4b47ed5fae6d186b26d70676d651a", "0100000001b8778dff640ccb144346d9db48201639b2707a0cc59e19672d2dd76cc6d1a5a6010000006b48304502210098d2e5b8a6c72442430bc09f2f4bcb56612c5b9e5eee821d65b412d099bb723402204f7f008ac052e5d7be0ab5b0c85ea5e627d725a521bd9e9b193d1fdf81c317a0012102d26e423c9da9ff4a7bf6b756b2dafb75cca34fbd34f64c4c3b77c37179c5bba2ffffffff0e0065cd1d000000001976a9142c899bd73eaf6d15cff18849d532b3f6aee9c15188ac00ca9a3b000000001976a9147dfe8027ca67427a7cdcf4b8941ff2e09b481cf788ac0065cd1d000000001976a914fa4b5721ee7d401138763e19c71016b590bea5a988ac0065cd1d000000001976a91409f67ed5b19b08e2ac079a53c8d0670fbc46ca7c88ac00ca9a3b000000001976a914fee6a115d97a992520d2b76af1c1122a1dbaf26e88ac00f633bce60000001976a914937258e3a8c463ec07e78ce62326c488170ad25e88ac0065cd1d000000001976a91457d7700f6c1e512e36e4c9595b019e925984a2d688ac00ca9a3b000000001976a9141feeaf3572242f852a2fc9546dd1cba448af3bb288ac0065cd1d000000001976a914fba21afab015ee0be4e60440986252477dfd2bc788ac0065cd1d000000001976a9145769439ee82c8afc1879fd1115f5c4fc3773db0888ac00ca9a3b000000001976a914be1f3c1a050211e6f17677592a990660373dd12388ac0065cd1d000000001976a9144ecb3bc3f974a6214ff9adb2a767486ce8872a4688ac00ca9a3b000000001976a914b1d689e2ed6df912adb3b6f0e11e096c0d22deeb88ac0065cd1d000000001976a914338a405ab4102a2ef1392d9f9aaf5bdb0227a46788ac00000000"},
        {"dcb7d6ee9b97a66ddb1342ff4836b9f426ac71103dd48702b212c0930899a364", "01000000011a656d67706db286d1e6fad57eb4f411cb14f8880cea8348da339b9d434a5ec7050000006a47304402201d69fddb269b53aa742ff6437a45adb4ca5c59f666c9b4eabc4a0c7a6e6f4c0f022015a747b7a6d9371a4020f5b396dcd094b0f36af3fc82e95091da856181912dfa012102c9a8d5b2f768afe30ee772d185e7a61f751be05649a79508b38a2be8824adec3ffffffff020065cd1d000000001976a9142c899bd73eaf6d15cff18849d532b3f6aee9c15188ac00b07098e60000001976a9141630d812e219e6bcbe494eb96f7a7900c216ad5d88ac00000000"},
        {"c8b259c80b1e2b33f554ec529962a1b7ba6b515f59c26d654dd467c8b375e900", "010000000164a3990893c012b20287d43d1071ac26f4b93648ff4213db6da6979beed6b7dc010000006b48304502210086ac11d4a8146b4176a72059960690c72a9776468cd671fd07c064b51f24961d02205bcf008d6995014f3cfd79100ee9beab5688c88cca15c5cea38b769563785d900121036530415a7b3b9c5976f26a63a57d119ab39491762121723c773399a2531a1bd7ffffffff020065cd1d000000001976a9147dfe8027ca67427a7cdcf4b8941ff2e09b481cf788ac006aad74e60000001976a914e5616848352c328c9f61b167eb1b0fde39b5cb6788ac00000000"},
        {"defadb60324f3b4bca36d4d6fdf8c0492e4c0df01143e54c24360a49f998affe", "010000000141c217dfea3a1d8d6a06e9d3daf75b292581f652256d73a7891e5dc9c7ee3cca000000006a47304402205cce451228f98fece9645052546b82c2b2d425a4889b03999001fababfc7f4690220583b2189faef07d6b0191c788301cfab1b3f47ffe2c403d632b92c6dde27e14f012102d26e423c9da9ff4a7bf6b756b2dafb75cca34fbd34f64c4c3b77c37179c5bba2ffffffff0100ca9a3b000000001976a914fa4b5721ee7d401138763e19c71016b590bea5a988ac00000000"}
    };




}