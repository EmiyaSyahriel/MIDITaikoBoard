package id.emiyasyahriel.taikoboard

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.DialogCompat

class MainActivity : AppCompatActivity(), ITaikoViewReceiver {
    lateinit var midiManager : MidiManager
    var devices = ArrayList<MidiDeviceInfo>()
    var selectedDeviceIdx = -1
    var activeDevice: MidiDevice? = null
    var activePort: MidiInputPort? = null
    var isFullScreen = false
    lateinit var taiko : TaikoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taiko = TaikoView(this)
        taiko.addEventListener(this)
        setContentView(taiko)
        supportActionBar?.subtitle = "Device : NONE"

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) displayUnsupportedAlert()

        midiManager = getSystemService(Context.MIDI_SERVICE) as MidiManager
        midiManager.registerDeviceCallback( MidiCallbacks(this), Handler() )
        addConnectedDevices()
    }

    private fun addConnectedDevices(){
        midiManager.devices.forEach { devices.add(it) }
        Toast.makeText(this, "Found ${devices.size} MIDI devices.", Toast.LENGTH_SHORT).show()
    }

    inner class MidiCallbacks(context: Context) : MidiManager.DeviceCallback(){
        lateinit var context : Context
        init {
            this.context = context
        }
        override fun onDeviceAdded(device: MidiDeviceInfo?) {
            super.onDeviceAdded(device)
            if( device!= null) devices.add(device)
            Toast.makeText(context, "New MIDI device connected", Toast.LENGTH_SHORT).show()
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo?) {
            super.onDeviceRemoved(device)
            if( device!= null) devices.remove(device)
            Toast.makeText(context, "A MIDI device disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayUnsupportedAlert(){
        val alertDialog = AlertDialog.Builder(this)
        alertDialog
            .setMessage("This device doesn't support MIDI Output.")
            .setOnDismissListener { finish() }
            .setPositiveButton("CLOSE"){ _, _ -> finish() }
            .create().show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.mainbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval =false;
        when(item.itemId){
            R.id.select_port -> {
                showPortSelection()
            }
            R.id.select_device -> {
                showDeviceSelection()
            }
            R.id.change_color -> {

            }
            R.id.switch_fullscreen -> {
                setFullscreen(true)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setActiveMidiDevice(md:MidiDevice){
        activeDevice?.close()
        activeDevice = md
        val midiName = md.info.properties.getString(MidiDeviceInfo.PROPERTY_NAME, "Unknown MIDI Device")
        val midiManufacture = md.info.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER, "Unknown MIDI Manufacturer")
        supportActionBar?.subtitle = "Device : $midiName - $midiManufacture"
        Toast.makeText(this, "$midiName has opened.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        activePort?.flush()
        activePort?.close()
        activeDevice?.close()
        super.onDestroy()
    }

    private fun showDeviceSelection(){
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Select MIDI Device.")

        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        devices.forEach {
            val itemName = it.properties.getString(MidiDeviceInfo.PROPERTY_NAME, "Unknown MIDI Device.")
            arrayAdapter.add(itemName)
        }

        dialog.setNegativeButton("Cancel") { d, _ -> d.cancel() }
        dialog.setAdapter(arrayAdapter) { d, i ->
            val deviceName = arrayAdapter.getItem(i)
            selectedDeviceIdx = i
            midiManager.openDevice(
                devices[i],
                MidiManager.OnDeviceOpenedListener { setActiveMidiDevice(it) } ,
                Handler(Looper.getMainLooper()))
            d.dismiss()
        }

        dialog.show()
    }

    private fun showPortSelection(){
        if(activeDevice != null){
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Select MIDI Output Port.")
            val arrayAdapter = ArrayAdapter<Int>(this, android.R.layout.select_dialog_singlechoice)
            for(i in 0 until activeDevice!!.info.inputPortCount){ arrayAdapter.add(i) }
            dialog.setNegativeButton("Cancel") { d, _ -> d.cancel() }
            dialog.setAdapter(arrayAdapter){d,i->
                activePort?.flush()
                activePort?.close()
                activePort = activeDevice?.openInputPort(i)!!
                d.dismiss()
            }
            dialog.show()
        }else{
            Toast.makeText(this, "No MIDI device is active. Please select one MIDI device using the USB icon.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFullscreen(isTrue:Boolean = true){
        if(isTrue){
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            Toast.makeText(this, "Press back to return to normal view.", Toast.LENGTH_SHORT).show()
        }else{
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    override fun onBackPressed() {
        if(isFullScreen){
            setFullscreen(false)
        }else{
            super.onBackPressed()
        }
    }

    override fun onDown(pos: Int) {
        if(activePort != null){
            val buffer = ByteArray(3)
            var numBytes = 0;
            buffer[numBytes++] = (0x90).toByte()
            buffer[numBytes++] = (50 + pos).toByte()
            buffer[numBytes++] = (127).toByte()
            // 5ms timestamp
            val nPms = 1000000L;
            val timeStamp = System.nanoTime() + (5 * nPms)
            activePort?.send(buffer, 0, numBytes, timeStamp)
        }
    }

    override fun onUp(pos: Int) {
        if(activePort != null){
            val buffer = ByteArray(3)
            var numBytes = 0;
            buffer[numBytes++] = (0x80).toByte()
            buffer[numBytes++] = (50 + pos).toByte()
            buffer[numBytes++] = (127).toByte()
            // 5ms timestamp
            val nPms = 1000000L;
            val timeStamp = System.nanoTime() + (5 * nPms)
            activePort?.send(buffer, 0, numBytes, timeStamp)
        }
    }
}