package fr.papam.cordova.nfc.emv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.util.Log;

import com.github.devnied.emvnfccard.model.EmvCard;
import fr.papam.cordova.nfc.emv.Provider;
import fr.papam.cordova.nfc.emv.EmvParser;


public class EmvNfcCordovaPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;
    private Activity activity = null;

    private PendingIntent pendingIntent = null;
    private final List<IntentFilter> intentFilters = new ArrayList<IntentFilter>();
    private final ArrayList<String[]> techLists = new ArrayList<String[]>();
    private Intent savedIntent = null;

    private static final String STATUS_NFC_OK = "NFC_OK";
    private static final String STATUS_NO_NFC = "NO_NFC";
    private static final String STATUS_NFC_DISABLED = "NFC_DISABLED";

    /**
     * Emv card
     */
    private EmvCard mReadCard;

    /**
     * Tag comm
     */
    private IsoDep mTagcomm;

    /**
     * Emv Card
     */
    private EmvCard mCard;

    /**
     * Last Ats
     */
    private byte[] lastAts;

    /**
     * IsoDep provider
     */
    private Provider mProvider = new Provider();

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (!getNfcStatus().equals(STATUS_NFC_OK)) {
            callbackContext.error(getNfcStatus());
            return true; // short circuit
        }

        this.callbackContext = callbackContext;
        createPendingIntent();

        boolean retValue = true;

        if (action.equals("scan")) {
            scan();
        } else if (action.equals("status")) {
            this.callbackContext.success(getNfcStatus());
        } else if (action.equals("stop")) {
            this.callbackContext.success(getNfcStatus());
        } else {
            retValue = false;
        }

        return retValue; // short circuit
    }

    /**
     * Start to scan credit Card
     */
    private void scan() {
        startNfc();
        if (!recycledIntent()) {
            waitForCreditCard();
        }
    }

    /**
     * Return the NFC status
     *
     * @return
     */
    private String getNfcStatus() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null) {
            return STATUS_NO_NFC;
        } else if (!nfcAdapter.isEnabled()) {
            return STATUS_NFC_DISABLED;
        } else {
            return STATUS_NFC_OK;
        }
    }

    /**
     * Create a pending intent
     */
    private void createPendingIntent() {
        if (pendingIntent == null) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        }
    }

    /**
     * Start NFC
     */
    private void startNfc() {
        createPendingIntent(); // onResume can call startNfc before execute

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null && !getActivity().isFinishing()) {
                    try {
                        nfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), getIntentFilters(), getTechLists());
                    } catch (IllegalStateException e) {
                        // issue 110 - user exits app with home button while nfc is initializing
                    }
                }
            }
        });
    }

    /**
     * Stop NFC
     */
    private void stopNfc() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null) {
                    try {
                        nfcAdapter.disableForegroundDispatch(getActivity());
                    } catch (IllegalStateException e) {
                        // issue 125 - user exits app with back button while nfc
                    }
                }
            }
        });
    }

    /**
     * Waiting for a credit card
     */
    void waitForCreditCard() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Intent intent = getIntent();
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                    parseCreditCard(intent);
                }
                setIntent(new Intent());
            }
        });
    }

    /**
     * Parse a credit card when available
     *
     * @param intent
     */
    private void parseCreditCard(Intent intent) {
        Tag mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        mTagcomm = IsoDep.get(mTag);
        if (mTagcomm != null) {
            try {
                mReadCard = null;
                // Open connection
                mTagcomm.connect();
                lastAts = getAts(mTagcomm);
                mProvider.setmTagCom(mTagcomm);
                EmvParser parser = new EmvParser(mProvider, true);
                mCard = parser.readEmvCard();

                this.callbackContext.success(this.toJSONObject(mCard)); // TODO
            } catch (IOException e) {
                this.callbackContext.error("Catch " + e.getMessage());
            } finally {
                // close tagcomm
                try {
                    if (mTagcomm.isConnected()) {
                        mTagcomm.close();
                    }
                } catch (IOException e) {
                    this.callbackContext.error("Catch close");
                }
                stopNfc();
            }

        } else {
            this.callbackContext.error("Error mTagcomm null");
        }
    }


    /**
     * Transform card result to JSON
     *
     * @param pCard
     * @return
     */
    private JSONObject toJSONObject(EmvCard pCard) {
        JSONObject scanCard = new JSONObject();
        try {
            if (pCard != null) {
                scanCard.put("holderFirstname", pCard.getHolderFirstname());
                scanCard.put("holderLastname", pCard.getHolderLastname());
                scanCard.put("cardType", pCard.getType());
                scanCard.put("cardNumber", pCard.getCardNumber());
                scanCard.put("expireDate", pCard.getExpireDate());
                scanCard.put("getLeftPinTry", pCard.getLeftPinTry());

            }
        } catch (JSONException e) {
            scanCard = null;
        }

        return scanCard;
    }


    private boolean recycledIntent() {

        int flags = getIntent().getFlags();
        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
            setIntent(new Intent());
            return true;
        }
        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        if (multitasking) {
            // nfc can't run in background
            stopNfc();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        startNfc();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
        waitForCreditCard();
    }

    /**
     * Get the pending intent
     *
     * @return
     */
    private PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    /**
     * Get the Intent filter
     *
     * @return
     */
    private IntentFilter[] getIntentFilters() {
        return intentFilters.toArray(new IntentFilter[intentFilters.size()]);
    }

    /**
     * Get the tech list
     *
     * @return
     */
    private String[][] getTechLists() {
        return techLists.toArray(new String[0][0]);
    }

    /**
     * Get ATS from isoDep
     *
     * @param pIso isodep
     * @return ATS byte array
     */
    private byte[] getAts(final IsoDep pIso) {
        byte[] ret = null;
        if (pIso.isConnected()) {
            // Extract ATS from NFC-A
            ret = pIso.getHistoricalBytes();
            if (ret == null) {
                // Extract ATS from NFC-B
                ret = pIso.getHiLayerResponse();
            }
        }
        return ret;
    }

    /**
     * Get the activity
     *
     * @return
     */
    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    /**
     * Get the activity intent
     *
     * @return
     */
    private Intent getIntent() {
        return getActivity().getIntent();
    }

    /**
     * Set the activity intent
     *
     * @param intent
     */
    private void setIntent(Intent intent) {
        getActivity().setIntent(intent);
    }
}



