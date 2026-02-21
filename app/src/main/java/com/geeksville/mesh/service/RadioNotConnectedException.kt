
package com.geeksville.mesh.service

import android.os.RemoteException

open class RadioNotConnectedException(message: String = "Not connected to radio") : RemoteException(message)

class NoDeviceConfigException(message: String = "No radio settings received (is our app too old?)") :
    RadioNotConnectedException(message)

class BLEException(message: String) : RadioNotConnectedException(message)

class BLECharacteristicNotFoundException(message: String) : RadioNotConnectedException(message)

class BLEConnectionClosing(message: String = "BLE connection is closing") : RadioNotConnectedException(message)

