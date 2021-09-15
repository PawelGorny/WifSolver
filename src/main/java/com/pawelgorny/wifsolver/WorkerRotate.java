package com.pawelgorny.wifsolver;

import org.bitcoinj.core.*;

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
        mainLoop:
        for (int c = 0; c < len; c++) {
            for (int z=0; z< Base58.ALPHABET.length ;z++) {
                stringBuilder.setCharAt(c, Base58.ALPHABET[z]);
                if (test(stringBuilder.toString())) {
                    break mainLoop;
                }
            }
            stringBuilder.replace(0, len, configuration.getWif());
        }
    }

    private boolean test(String suspect) {
        try {
            ECKey ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), suspect).getKey();
            Address foundAddress = this.configuration.isCompressed() ? LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey)
                    : LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress());
            if (configuration.getTargetAddress() != null) {
                if (foundAddress.equals(configuration.getAddress())) {
                    super.addResult(suspect + " -> " + foundAddress);
                    System.out.println("Expected address found:");
                    System.out.println(suspect + " -> " + foundAddress);
                    return true;
                }
            } else {
                super.addResult(suspect + " -> " + foundAddress);
                System.out.println(suspect + " -> " + foundAddress);
            }
        } catch (Exception e) {

        }
        return false;
    }

}
