
package fr.papam.cordova.nfc.emv;

import java.util.Arrays;

import com.github.devnied.emvnfccard.enums.SwEnum;

import fr.devnied.bitlib.BytesUtils;

/**
 * Method used to manipulate response from APDU command
 *
 * @author MILLAU Julien
 *
 */
public final class ResponseUtils {

    /**
     * Method used to check if the last command return SW1SW2 == 9000
     *
     * @param pByte
     *            response to the last command
     * @return true if the status is 9000 false otherwise
     */
    public static boolean isSucceed(final byte[] pByte) {
        return isEquals(pByte, SwEnum.SW_9000);
    }

    /**
     * Method used to check equality with the last command return SW1SW2 == pEnum
     *
     * @param pByte
     *            response to the last command
     * @param pEnum
     *            response to check
     * @return true if the response of the last command is equals to pEnum
     */
    public static boolean isEquals(final byte[] pByte, final SwEnum pEnum) {
        SwEnum val = SwEnum.getSW(pByte);
        return val != null && val == pEnum;
    }

    /**
     * Private constructor
     */
    private ResponseUtils() {
    }

}