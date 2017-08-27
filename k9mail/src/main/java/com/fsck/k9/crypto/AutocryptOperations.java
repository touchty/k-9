package com.fsck.k9.crypto;


import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeUtility;
import okio.ByteString;
import org.openintents.openpgp.AutocryptPeerUpdate;
import org.openintents.openpgp.util.OpenPgpApi;
import timber.log.Timber;


public class AutocryptOperations {
    private static final String AUTOCRYPT_HEADER = "Autocrypt";

    private static final String AUTOCRYPT_PARAM_TO = "addr";
    private static final String AUTOCRYPT_PARAM_KEY_DATA = "keydata";

    private static final String AUTOCRYPT_PARAM_TYPE = "type";
    private static final String AUTOCRYPT_TYPE_1 = "1";

    private static final String AUTOCRYPT_PARAM_PREFER_ENCRYPT = "prefer-encrypt";
    private static final String AUTOCRYPT_PREFER_ENCRYPT_MUTUAL = "mutual";


    public AutocryptOperations() {
    }


    public boolean addAutocryptPeerUpdateToIntentIfPresent(Message currentMessage, Intent intent) {
        AutocryptHeader autocryptHeader = getValidAutocryptHeader(currentMessage);
        if (autocryptHeader == null) {
            return false;
        }

        String messageFromAddress = currentMessage.getFrom()[0].getAddress();
        if (!autocryptHeader.addr.equalsIgnoreCase(messageFromAddress)) {
            return false;
        }

        Date messageDate = currentMessage.getSentDate();
        Date internalDate = currentMessage.getInternalDate();
        Date effectiveDate = messageDate.before(internalDate) ? messageDate : internalDate;

        AutocryptPeerUpdate data = AutocryptPeerUpdate.createAutocryptPeerUpdate(
                autocryptHeader.keyData, effectiveDate, autocryptHeader.isPreferEncryptMutual);
        intent.putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID, messageFromAddress);
        intent.putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_UPDATE, data);
        return true;
    }

    @Nullable
    @VisibleForTesting
    AutocryptHeader getValidAutocryptHeader(Message currentMessage) {
        String[] headers = currentMessage.getHeader(AUTOCRYPT_HEADER);
        ArrayList<AutocryptHeader> autocryptHeaders = parseAllAutocryptHeaders(headers);

        boolean isSingleValidHeader = autocryptHeaders.size() == 1;
        return isSingleValidHeader ? autocryptHeaders.get(0) : null;
    }

    @NonNull
    private ArrayList<AutocryptHeader> parseAllAutocryptHeaders(String[] headers) {
        ArrayList<AutocryptHeader> autocryptHeaders = new ArrayList<>();
        for (String header : headers) {
            AutocryptHeader autocryptHeader = parseAutocryptHeader(header);
            if (autocryptHeader != null) {
                autocryptHeaders.add(autocryptHeader);
            }
        }
        return autocryptHeaders;
    }

    @Nullable
    private AutocryptHeader parseAutocryptHeader(String headerValue) {
        Map<String,String> parameters = MimeUtility.getAllHeaderParameters(headerValue);

        String type = parameters.remove(AUTOCRYPT_PARAM_TYPE);
        if (type != null && !type.equals(AUTOCRYPT_TYPE_1)) {
            Timber.e("autocrypt: unsupported type parameter %s", type);
            return null;
        }

        String base64KeyData = parameters.remove(AUTOCRYPT_PARAM_KEY_DATA);
        if (base64KeyData == null) {
            Timber.e("autocrypt: missing key parameter");
            return null;
        }

        ByteString byteString = ByteString.decodeBase64(base64KeyData);
        if (byteString == null) {
            Timber.e("autocrypt: error parsing base64 data");
            return null;
        }

        String to = parameters.remove(AUTOCRYPT_PARAM_TO);
        if (to == null) {
            Timber.e("autocrypt: no to header!");
            return null;
        }

        boolean isPreferEncryptMutual = false;
        String preferEncrypt = parameters.remove(AUTOCRYPT_PARAM_PREFER_ENCRYPT);
        if (AUTOCRYPT_PREFER_ENCRYPT_MUTUAL.equalsIgnoreCase(preferEncrypt)) {
            isPreferEncryptMutual = true;
        }

        if (hasCriticalParameters(parameters)) {
            return null;
        }

        return new AutocryptHeader(parameters, to, byteString.toByteArray(), isPreferEncryptMutual);
    }

    private boolean hasCriticalParameters(Map<String, String> parameters) {
        for (String parameterName : parameters.keySet()) {
            if (parameterName != null && !parameterName.startsWith("_")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAutocryptHeader(Message currentMessage) {
        return currentMessage.getHeader(AUTOCRYPT_HEADER).length > 0;
    }

    @VisibleForTesting
    class AutocryptHeader {
        final byte[] keyData;
        final String addr;
        final Map<String,String> parameters;
        final boolean isPreferEncryptMutual;

        private AutocryptHeader(Map<String, String> parameters, String addr, byte[] keyData, boolean isPreferEncryptMutual) {
            this.parameters = parameters;
            this.addr = addr;
            this.keyData = keyData;
            this.isPreferEncryptMutual = isPreferEncryptMutual;
        }
    }
}
