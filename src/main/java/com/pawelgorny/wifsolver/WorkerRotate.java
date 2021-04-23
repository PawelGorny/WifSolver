package com.pawelgorny.wifsolver;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;

class WorkerRotate extends Worker {

    private final Configuration configuration;

    public WorkerRotate(Configuration configuration) {
        super(configuration);
        this.configuration=configuration;
    }

    @Override
    protected void run() {
        ECKey ecKey;
        StringBuilder stringBuilder = new StringBuilder(configuration.getWif());
        int len = configuration.getWif().length();
        try {
            ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), configuration.getWif()).getKey();
            String foundAddress = this.configuration.isCompressed() ? LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey).toString()
                    :LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress()).toString();
            super.addResult(configuration.getWif() + " -> " + foundAddress);
            System.out.println(configuration.getWif() + " -> " + foundAddress);
            return;
        }catch (Exception e){
            System.out.println("Initial "+configuration.getWif()+" incorrect, starting rotation");
        }
        for (int c = 0; c < len; c++) {
            for (int z=0; z< Base58.ALPHABET.length ;z++) {
                stringBuilder.setCharAt(c, Base58.ALPHABET[z]);
                test(stringBuilder.toString());
            }
        }
    }

    private void test(String suspect) {
        try {
            ECKey ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), suspect).getKey();
            String foundAddress = this.configuration.isCompressed() ? LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey).toString()
                    : LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress()).toString();
            if (configuration.getTargetAddress() != null) {
                if (foundAddress.equals(configuration.getTargetAddress())) {
                    super.addResult(suspect + " -> " + foundAddress);
                    System.out.println(suspect + " -> " + foundAddress);
                    return;
                }
            } else {
                super.addResult(suspect + " -> " + foundAddress);
                System.out.println(suspect + " -> " + foundAddress);
            }
        } catch (Exception e) {

        }
    }

}
