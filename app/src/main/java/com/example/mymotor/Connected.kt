package com.example.mymotor


import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings.Global.putString
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2


class ConnectedThread(private val mmSocket: BluetoothSocket, var messageRecieved:
    (String) -> Unit) : Thread() {

    private var mmInStream: InputStream = mmSocket.inputStream
    private var mmOutStream: OutputStream = mmSocket.outputStream
    private var mmBuffer: ByteArray = ByteArray(1024)


    override fun run() {
        Log.i("Bluetooth", "ConnectedThread is running, waiting for messages...")



        val buffer = ByteArray(1024)
        var numBytes: Int

        while (true) {
            try {
                numBytes = mmInStream.read(buffer)
                val receivedMessage = String(buffer, 0, numBytes)
                messageRecieved(receivedMessage)
            } catch (e: IOException) {
                break
            }
        }


    }


    fun sendCommand(command: String) {


        try {

            if(mmSocket.isConnected)
            {
                Log.i("Bluetooth", "Sending message: $command")
                val fullCommand = "$command\r"
                mmOutStream.write(fullCommand.toByteArray())
                mmOutStream.flush()
                Log.i("Bluetooth", "Message sent: $command")
            }
            else{
                Log.i("Bluetooth", "BluetoothSocket is not connected. Cannot send message.")

                //Toast.makeText(this, "Not connected to OBD device", Toast.LENGTH_SHORT).show()

            }

        } catch (e: IOException) {
            Log.e("Bluetooth", "Error occurred when sending data", e)


        }


    }

    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Could not close the connect socket", e)
        }
    }
}

class Connected {

}