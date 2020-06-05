package fr.papam.cordova.nfc.emv;

import java.io.IOException;

import android.nfc.tech.IsoDep;
import android.util.Log;

import com.github.devnied.emvnfccard.enums.SwEnum;
import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.github.devnied.emvnfccard.utils.TlvUtil;



/**
 * Provider used to communicate with EMV card
 *
 * @author Millau Julien
 */

public class Provider implements IProvider {

    /**
     * Logger
     */
    private StringBuffer log = new StringBuffer();

    /**
     * Tag comm
     */
    private IsoDep mTagCom;

    @Override
    public byte[] transceive(final byte[] pCommand) throws CommunicationException {


        byte[] response = null;
        try {
            // send command to emv card
            response = mTagCom.transceive(pCommand);
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage());
        }



        return response;
    }

    /**
     * Setter for the field mTagCom
     *
     * @param mTagCom the mTagCom to set
     */
    public void setmTagCom(final IsoDep mTagCom) {
        this.mTagCom = mTagCom;
    }

    /**
     * Method used to get the field log
     *
     * @return the log
     */
    public StringBuffer getLog() {
        return log;
    }

}