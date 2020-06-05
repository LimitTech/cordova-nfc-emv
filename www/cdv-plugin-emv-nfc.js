/**
 * cdv-plugin-emv-nfc.js
 *
 */

/**
 * This class exposes EmvNfc's card scanning functionality to JavaScript.
 *
 * @constructor
 */
function EmvNfc() {
}


/**
 * Scan a credit card through NFC
 *
 * @param onSuccess Success callback
 * @param onFailure Error callback
 */
EmvNfc.prototype.scan = function (onSuccess, onFailure) {
    cordova.exec(onSuccess, onFailure, "EmvNfc", "scan", []);
};

/**
 * Get the current STATUS of NFC.
 *
 * NFC_OK => NFC is available and running
 * NO_NFC => NFC is not available on the device
 * NFC_DISABLED =>  NFC is disabled in android settings
 *
 * @param onSuccess Success callback
 * @param onFailure Error callback
 */
EmvNfc.prototype.getStatus = function (onSuccess, onFailure) {
    cordova.exec(onSuccess, onFailure, "EmvNfc", "status", []);
};

/**
 * Stop the NFC
 *
 * @param onSuccess Success callback
 * @param onFailure Error callback
 */
EmvNfc.prototype.stop = function (onSuccess, onFailure) {
    cordova.exec(onSuccess, onFailure, "EmvNfc", "stop", []);
};


/**
 * Plugin setup boilerplate.
 */
module.exports = new EmvNfc();
