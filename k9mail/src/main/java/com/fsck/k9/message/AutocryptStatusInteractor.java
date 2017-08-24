package com.fsck.k9.message;


import java.io.InputStream;

import android.content.Intent;
import android.support.annotation.WorkerThread;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import timber.log.Timber;


public class AutocryptStatusInteractor {
    private static final AutocryptStatusInteractor INSTANCE = new AutocryptStatusInteractor();

    public static AutocryptStatusInteractor getInstance() {
        return INSTANCE;
    }


    @WorkerThread
    public RecipientAutocryptStatus retrieveCryptoProviderRecipientStatus(
            OpenPgpApi openPgpApi, String[] recipientAddresses) {
        Intent intent = new Intent(OpenPgpApi.ACTION_QUERY_AUTOCRYPT_STATUS);
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, recipientAddresses);

        Intent result = openPgpApi.executeApi(intent, (InputStream) null, null);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                boolean allKeysConfirmed = result.getBooleanExtra(OpenPgpApi.RESULT_KEYS_CONFIRMED, false);
                int autocryptStatus =
                        result.getIntExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_UNAVAILABLE);

                switch (autocryptStatus) {
                    case OpenPgpApi.AUTOCRYPT_STATUS_UNAVAILABLE:
                        return RecipientAutocryptStatus.UNAVAILABLE;
                    case OpenPgpApi.AUTOCRYPT_STATUS_DISCOURAGE:
                        if (allKeysConfirmed) {
                            return RecipientAutocryptStatus.DISCOURAGE_CONFIRMED;
                        } else {
                            return RecipientAutocryptStatus.DISCOURAGE_UNCONFIRMED;
                        }
                    case OpenPgpApi.AUTOCRYPT_STATUS_AVAILABLE:
                        if (allKeysConfirmed) {
                            return RecipientAutocryptStatus.AVAILABLE_CONFIRMED;
                        } else {
                            return RecipientAutocryptStatus.AVAILABLE_UNCONFIRMED;
                        }
                    case OpenPgpApi.AUTOCRYPT_STATUS_RECOMMEND:
                        if (allKeysConfirmed) {
                            return RecipientAutocryptStatus.RECOMMENDED_CONFIRMED;
                        } else {
                            return RecipientAutocryptStatus.RECOMMENDED_UNCONFIRMED;
                        }
                }
            case OpenPgpApi.RESULT_CODE_ERROR:
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                if (error != null) {
                    Timber.w("OpenPGP API Error #%s: %s", error.getErrorId(), error.getMessage());
                } else {
                    Timber.w("OpenPGP API Unknown Error");
                }
                return RecipientAutocryptStatus.ERROR;
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                // should never happen, so treat as error!
            default:
                return RecipientAutocryptStatus.ERROR;
        }
    }

    public enum RecipientAutocryptStatus {
        NO_RECIPIENTS,
        UNAVAILABLE,
        DISCOURAGE_UNCONFIRMED,
        DISCOURAGE_CONFIRMED,
        AVAILABLE_UNCONFIRMED,
        AVAILABLE_CONFIRMED,
        RECOMMENDED_UNCONFIRMED,
        RECOMMENDED_CONFIRMED,
        ERROR
    }
}
