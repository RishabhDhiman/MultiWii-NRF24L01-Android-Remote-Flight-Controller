package com.dhiman.droneremote

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dhiman.droneremote.usbserial.driver.Ch34xSerialDriver
import com.dhiman.droneremote.usbserial.driver.UsbSerialPort
import com.dhiman.droneremote.usbserial.driver.UsbSerialPort.*
import com.dhiman.droneremote.usbserial.util.SerialInputOutputManager
import com.dhiman.droneremote.usbserial.util.SerialInputOutputManager.Listener
import com.dhiman.droneremote.widgets.JoystickMovedListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs


class MainActivity : AppCompatActivity(), Listener {
    lateinit var actionString: String
    lateinit var mDevice: UsbSerialPort
    var throttle = 0
    var pitch = 1500
    var yaw = 1500
    var roll = 1500
    var aux1 = 0
    var aux2 = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        actionString = this.packageName + ".action.USB_PERMISSION"
        getDetail()
        yawSlider.setOnJoystickMovedListener(object : JoystickMovedListener {
            override fun onMoved(pan: Float, tilt: Float) {
                var mTilt = (tilt / 150) + 1
                mTilt *= 500
                yaw = mTilt.toInt() + 1000
            }

            override fun onReleased() {

            }

            override fun onReturnedToCenter() {

            }

        })
        rollSlider.setOnMovedListener(object : JoystickMovedListener {
            override fun onMoved(pan: Float, tilt: Float) {
                var mTilt = tilt + 1
                mTilt *= 500
                pitch = mTilt.toInt() + 1000
                var mPan = abs(pan - 1)
                mPan *= 500
                roll = mPan.toInt() + 1000
            }

            override fun onReleased() {
            }

            override fun onReturnedToCenter() {
            }
        })

        val handler = Handler(Looper.getMainLooper())
        val r: Runnable = object : Runnable {
            override fun run() {
                throttle = 1000 + throttleSlider.progress * 10
                aux1 = 1000 + mAux1.progress * 10
                aux2 = 1000 + mAux2.progress * 10
                val string =
                    "$throttle$yaw$pitch$roll$aux1$aux2"
                Log.e("Command", string)
                if (::mDevice.isInitialized && mDevice.isOpen) {
                    mDevice.write(string.toByteArray(), 5)
                }
                handler.postDelayed(this, 300)
            }
        }
        handler.postDelayed(r, 300)
    }

    fun getDetail() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = manager.deviceList
        val deviceIterator: Iterator<UsbDevice> = deviceList.values.iterator()
        while (deviceIterator.hasNext()) {
            val device = deviceIterator.next()
            val mPermissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(actionString),
                PendingIntent.FLAG_IMMUTABLE
            )
            val filter = IntentFilter(actionString)
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    unregisterReceiver(this)
                    if (!manager.hasPermission(device)) {
                        getDetail()
                    }
                    mDevice = Ch34xSerialDriver(device).ports[0]
                    mDevice.open(manager.openDevice(device))
                    mDevice.setParameters(115200, DATABITS_8, STOPBITS_1, PARITY_NONE)
                    SerialInputOutputManager(mDevice, this@MainActivity).start()
                }
            }, filter)
            manager.requestPermission(device, mPermissionIntent)
        }
    }

    override fun onNewData(data: ByteArray?) {
        val string = String(data!!).trim()
        if (string[0] == 'I') {
            runOnUiThread {
                status.setImageResource(R.drawable.red)
                ping.text = "Initialization Failed"
            }
        } else if (string[0] == 'S') {
            runOnUiThread {
                status.setImageResource(R.drawable.red)
                ping.text = "Sending Failed"
            }
        } else if (string[0] == 'A') {
            runOnUiThread {
                status.setImageResource(R.drawable.red)
                ping.text = "Acknowledgement Failed"
            }
        } else {
            runOnUiThread {
                if (ping.text.trim() != string) {
                    ping.text = String(data).trim() + " ms"
                    status.setImageResource(R.drawable.green)
                }
            }
        }
    }

    override fun onRunError(e: Exception?) {
    }
}