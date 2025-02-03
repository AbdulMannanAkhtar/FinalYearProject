package com.example.bluetooth_v2

import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings.Global.putString
import android.util.Log
import android.widget.TextView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2

class connected (private val handler: Handler){

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024)

        override fun run() {
            var numBytes: Int

            sendMessage("Hello, this is a test from ${Build.MODEL}")

            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1, mmBuffer
                )
                readMsg.sendToTarget()
            }
        }

        fun sendMessage(message: String) {


            try {

                //val message: String = "Hello, this is a test from ${Build.MODEL}"
                val bytes = message.toByteArray()

                mmOutStream.write(bytes)

                val testMessage = handler.obtainMessage(MESSAGE_WRITE, -1, -1, bytes)
                testMessage.sendToTarget()

            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
    }







