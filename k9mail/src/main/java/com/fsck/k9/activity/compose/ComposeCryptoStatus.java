package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;

import com.fsck.k9.activity.compose.RecipientMvpView.CryptoSpecialModeDisplayType;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoProviderState;
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatus;
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatusType;
import com.fsck.k9.view.RecipientSelectView.Recipient;

/** This is an immutable object which contains all relevant metadata entered
 * during e-mail composition to apply cryptographic operations before sending
 * or saving as draft.
 */
public class ComposeCryptoStatus {


    private CryptoProviderState cryptoProviderState;
    private Long signingKeyId;
    private Long selfEncryptKeyId;
    private String[] recipientAddresses;
    private boolean enablePgpInline;
    private CryptoMode cryptoMode;
    private RecipientAutocryptStatus recipientAutocryptStatus;


    public Long getSigningKeyId() {
        return signingKeyId;
    }

    CryptoStatusDisplayType getCryptoStatusDisplayType() {
        switch (cryptoProviderState) {
            case UNCONFIGURED:
                return CryptoStatusDisplayType.UNCONFIGURED;
            case UNINITIALIZED:
                return CryptoStatusDisplayType.UNINITIALIZED;
            case LOST_CONNECTION:
            case ERROR:
                return CryptoStatusDisplayType.ERROR;
            case OK:
                // provider status is ok -> return value is based on cryptoMode
                break;
            default:
                throw new AssertionError("all CryptoProviderStates must be handled!");
        }

        if (recipientAutocryptStatus == null) {
            throw new IllegalStateException("Display type must be obtained from provider!");
        }

        RecipientAutocryptStatusType recipientAutocryptStatusType = recipientAutocryptStatus.type;

        if (recipientAutocryptStatusType == RecipientAutocryptStatusType.ERROR) {
            return CryptoStatusDisplayType.ERROR;
        }

        switch (cryptoMode) {
            case CHOICE_ENABLED:
                if (recipientAutocryptStatusType.canEncrypt()) {
                    if (recipientAutocryptStatusType.isConfirmed()) {
                        return CryptoStatusDisplayType.CHOICE_ENABLED_TRUSTED;
                    } else {
                        return CryptoStatusDisplayType.CHOICE_ENABLED_UNTRUSTED;
                    }
                } else {
                    return CryptoStatusDisplayType.CHOICE_ENABLED_ERROR;
                }
            case CHOICE_DISABLED:
                if (recipientAutocryptStatusType.canEncrypt()) {
                    if (recipientAutocryptStatusType.isConfirmed()) {
                        return CryptoStatusDisplayType.CHOICE_DISABLED_TRUSTED;
                    } else {
                        return CryptoStatusDisplayType.CHOICE_DISABLED_UNTRUSTED;
                    }
                } else {
                    return CryptoStatusDisplayType.CHOICE_DISABLED_UNAVAILABLE;
                }
            case NO_CHOICE:
                if (recipientAutocryptStatusType == RecipientAutocryptStatusType.NO_RECIPIENTS) {
                    return CryptoStatusDisplayType.NO_CHOICE_EMPTY;
                } else if (recipientAutocryptStatusType.canEncrypt() && recipientAutocryptStatusType.isMutual()) { // TODO check own "mutual" status
                    if (recipientAutocryptStatusType.isConfirmed()) {
                        return CryptoStatusDisplayType.NO_CHOICE_MUTUAL_TRUSTED;
                    } else {
                        return CryptoStatusDisplayType.NO_CHOICE_MUTUAL;
                    }
                } else if (recipientAutocryptStatusType.canEncrypt()) {
                    return CryptoStatusDisplayType.NO_CHOICE_AVAILABLE;
                }
                return CryptoStatusDisplayType.NO_CHOICE_UNAVAILABLE;
            case SIGN_ONLY:
                return CryptoStatusDisplayType.SIGN_ONLY;
            default:
                throw new AssertionError("all CryptoModes must be handled!");
        }
    }

    CryptoSpecialModeDisplayType getCryptoSpecialModeDisplayType() {
        if (cryptoProviderState != CryptoProviderState.OK) {
            return CryptoSpecialModeDisplayType.NONE;
        }

        if (isSignOnly() && isPgpInlineModeEnabled()) {
            return CryptoSpecialModeDisplayType.SIGN_ONLY_PGP_INLINE;
        }

        if (isSignOnly()) {
            return CryptoSpecialModeDisplayType.SIGN_ONLY;
        }

        if (isEncryptionEnabled() && isPgpInlineModeEnabled()) {
            return CryptoSpecialModeDisplayType.PGP_INLINE;
        }

        return CryptoSpecialModeDisplayType.NONE;
    }

    public boolean shouldUsePgpMessageBuilder() {
        return cryptoProviderState != CryptoProviderState.UNCONFIGURED && (isEncryptionEnabled() || isSignOnly());
    }

    public boolean isEncryptionEnabled() {
        return cryptoMode == CryptoMode.CHOICE_ENABLED ||
                canEncryptAndIsMutual() && cryptoMode != CryptoMode.CHOICE_DISABLED;
    }

    boolean isSignOnly() {
        return cryptoMode == CryptoMode.SIGN_ONLY;
    }

    public boolean isSigningEnabled() {
        return cryptoMode == CryptoMode.SIGN_ONLY || isEncryptionEnabled();
    }

    public boolean isPgpInlineModeEnabled() {
        return enablePgpInline;
    }

    public boolean isProviderStateOk() {
        return cryptoProviderState == CryptoProviderState.OK;
    }

    boolean canEncrypt() {
        return recipientAutocryptStatus != null && recipientAutocryptStatus.type.canEncrypt();
    }

    public String[] getRecipientAddresses() {
        return recipientAddresses;
    }

    public boolean hasRecipients() {
        return recipientAddresses.length > 0;
    }

    boolean canEncryptAndIsMutual() {
        return canEncrypt() && recipientAutocryptStatus.type.isMutual();
    }

    boolean isEncryptionEnabledError() {
        return isEncryptionEnabled() && !canEncrypt();
    }

    boolean hasAutocryptPendingIntent() {
        return recipientAutocryptStatus.hasPendingIntent();
    }

    PendingIntent getAutocryptPendingIntent() {
        return recipientAutocryptStatus.intent;
    }

    public static class ComposeCryptoStatusBuilder {

        private CryptoProviderState cryptoProviderState;
        private CryptoMode cryptoMode;
        private Long signingKeyId;
        private Long selfEncryptKeyId;
        private List<Recipient> recipients;
        private Boolean enablePgpInline;

        public ComposeCryptoStatusBuilder setCryptoProviderState(CryptoProviderState cryptoProviderState) {
            this.cryptoProviderState = cryptoProviderState;
            return this;
        }

        public ComposeCryptoStatusBuilder setCryptoMode(CryptoMode cryptoMode) {
            this.cryptoMode = cryptoMode;
            return this;
        }

        public ComposeCryptoStatusBuilder setSigningKeyId(Long signingKeyId) {
            this.signingKeyId = signingKeyId;
            return this;
        }

        public ComposeCryptoStatusBuilder setSelfEncryptId(Long selfEncryptKeyId) {
            this.selfEncryptKeyId = selfEncryptKeyId;
            return this;
        }

        public ComposeCryptoStatusBuilder setRecipients(List<Recipient> recipients) {
            this.recipients = recipients;
            return this;
        }

        public ComposeCryptoStatusBuilder setEnablePgpInline(boolean cryptoEnableCompat) {
            this.enablePgpInline = cryptoEnableCompat;
            return this;
        }

        public ComposeCryptoStatus build() {
            if (cryptoProviderState == null) {
                throw new AssertionError("cryptoProviderState must be set!");
            }
            if (cryptoMode == null) {
                throw new AssertionError("crypto mode must be set!");
            }
            if (recipients == null) {
                throw new AssertionError("recipients must be set!");
            }
            if (enablePgpInline == null) {
                throw new AssertionError("enablePgpInline must be set!");
            }

            ArrayList<String> recipientAddresses = new ArrayList<>();
            for (Recipient recipient : recipients) {
                recipientAddresses.add(recipient.address.getAddress());
            }

            ComposeCryptoStatus result = new ComposeCryptoStatus();
            result.cryptoProviderState = cryptoProviderState;
            result.cryptoMode = cryptoMode;
            result.recipientAddresses = recipientAddresses.toArray(new String[0]);
            result.signingKeyId = signingKeyId;
            result.selfEncryptKeyId = selfEncryptKeyId;
            result.enablePgpInline = enablePgpInline;
            return result;
        }
    }

    ComposeCryptoStatus withRecipientAutocryptStatus(RecipientAutocryptStatus recipientAutocryptStatusType) {
        ComposeCryptoStatus result = new ComposeCryptoStatus();
        result.cryptoProviderState = cryptoProviderState;
        result.cryptoMode = cryptoMode;
        result.recipientAddresses = recipientAddresses;
        result.signingKeyId = signingKeyId;
        result.selfEncryptKeyId = selfEncryptKeyId;
        result.enablePgpInline = enablePgpInline;
        result.recipientAutocryptStatus = recipientAutocryptStatusType;
        return result;
    }

    public enum SendErrorState {
        PROVIDER_ERROR,
        ENABLED_ERROR
    }

    public SendErrorState getSendErrorStateOrNull() {
        if (cryptoProviderState != CryptoProviderState.OK) {
            // TODO: be more specific about this error
            return SendErrorState.PROVIDER_ERROR;
        }

        if (isEncryptionEnabledError()) {
            return SendErrorState.ENABLED_ERROR;
        }

        return null;
    }

    enum AttachErrorState {
        IS_INLINE
    }

    AttachErrorState getAttachErrorStateOrNull() {
        if (cryptoProviderState == CryptoProviderState.UNCONFIGURED) {
            return null;
        }

        if (enablePgpInline) {
            return AttachErrorState.IS_INLINE;
        }

        return null;
    }

}
