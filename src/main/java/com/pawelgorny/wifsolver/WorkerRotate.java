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
            String foundAddress = len==52?LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey).toString()
                    :LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress()).toString();
            super.addResult(configuration.getWif() + " -> " + foundAddress);
            System.out.println(configuration.getWif() + " -> " + foundAddress);
            return;
        }catch (Exception e){
            System.out.println("Initial "+configuration.getWif()+" incorrect, starting rotation");
        }
        for (int c=0; c<len-4; c++){
            for (int z=0; z< Base58.ALPHABET.length ;z++) {
                stringBuilder.setCharAt(c, Base58.ALPHABET[z]);
                try {
                    ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), stringBuilder.toString()).getKey();
                    String foundAddress = len==52?LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey).toString()
                            :LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress()).toString();
                    if (configuration.getTargetAddress()!=null){
                        if(foundAddress.equals(configuration.getTargetAddress())) {
                            super.addResult(stringBuilder.toString() + " -> " + foundAddress);
                            System.out.println(stringBuilder.toString() + " -> " + foundAddress);
                            return;
                        }
                    }else{
                        super.addResult(stringBuilder.toString() + " -> " + foundAddress);
                        System.out.println(stringBuilder.toString() + " -> " + foundAddress);
                    }
                }catch (Exception e){
                    stringBuilder = new StringBuilder(configuration.getWif());
                }
            }
        }
    }
}
