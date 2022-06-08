package com.pawelgorny.wifsolver;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

import javax.mail.Session;
import java.util.Map;

public class Configuration {
    final static char UNKNOWN_CHAR='_';
    final static String COMMENT_CHAR="#";
    final static int COMPRESSED_WIF_LENGTH = 52;

    private final static NetworkParameters NETWORK_PARAMETERS = MainNetParams.get();
    private final static int STATUS_PERIOD = 60 * 1000; //1 minute
    private final static int CHECKSUM_CHARS = 5;
    private final static int CHECKSUM_CHARS_COMPRESSED = 6;

    private Boolean isP2SH;
    private final String targetAddress;
    private final String wif;
    private final String wifStatus;
    private final WORK work;
    private final Map<Integer, char[]> guess;
    private Address address;
    private byte[] addressHash;
    private boolean compressed;

    private Integer forceThreads = null;

    private EmailConfiguration emailConfiguration = null;

    public Configuration(String targetAddress, String wif, String wifStatus, WORK work, Map<Integer, char[]> guess) {
        this.targetAddress = targetAddress;
        isP2SH = false;
        if (targetAddress != null) {
            if (targetAddress.startsWith("3")){
                compressed = true;
                isP2SH = true;
            }else {
                this.address = LegacyAddress.fromBase58(NETWORK_PARAMETERS, getTargetAddress());
                this.addressHash = address.getHash();
            }
        }
        this.wif = wif;
        this.compressed = wif.length() == COMPRESSED_WIF_LENGTH || (WORK.END.equals(work) && (wif.startsWith("L") || wif.startsWith("K")));
        this.wifStatus = wifStatus;
        this.work = work;
        this.guess = guess.isEmpty()?null:guess;
    }

    public static int getChecksumChars() {
        return getChecksumChars(false);
    }
    public static int getChecksumChars(boolean compressed) {
        return compressed?CHECKSUM_CHARS_COMPRESSED:CHECKSUM_CHARS;
    }

    public static NetworkParameters getNetworkParameters() {
        return NETWORK_PARAMETERS;
    }

    public static int getStatusPeriod() {
        return STATUS_PERIOD;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public Address getAddress() {
        return address;
    }

    public byte[] getAddressHash() {
        return addressHash;
    }

    public String getWif() {
        return wif;
    }

    public WORK getWork() {
        return work;
    }

    public Map<Integer, char[]> getGuess() {
        return guess;
    }

    public String getWifStatus() {
        return wifStatus;
    }

    public void setEmailConfiguration(String emailFrom,  Session mailSession) {
        this.emailConfiguration = new EmailConfiguration(emailFrom, mailSession);
    }

    public EmailConfiguration getEmailConfiguration() {
        return emailConfiguration;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public Integer getForceThreads() {
        return forceThreads;
    }

    public void setForceThreads(Integer forceThreads) {
        this.forceThreads = forceThreads;
    }

    public Boolean getIsP2SH() {
        return isP2SH;
    }

    class EmailConfiguration{
        private final Session mailSession;
        private final String emailTo;
        private final String emailFrom;

        public EmailConfiguration(String emailFrom,  Session mailSession) {
            this.emailFrom = emailFrom;
            this.emailTo = emailFrom;
            this.mailSession = mailSession;
        }

        public Session getMailSession() {
            return mailSession;
        }

        public String getEmailTo() {
            return emailTo;
        }

        public String getEmailFrom() {
            return emailFrom;
        }
    }
}
