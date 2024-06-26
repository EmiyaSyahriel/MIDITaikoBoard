package id.emiyasyahriel.taikoboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.Toast
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

class MainActivity : Activity() {
    private lateinit var midiManager : MidiManager
    private var devices = ArrayList<MidiDeviceInfo>()
    private var selectedDeviceIdx = -1
    private var activeDevice: MidiDevice? = null
    private var activePort: MidiInputPort? = null
    private var isFullScreen = false
    private lateinit var taiko : TaikoView
    private var timer = Timer("MIDI Queue")

    private data class Queue(val pos:Int, val up:Boolean)
    private val queue = arrayListOf<Queue>()

    private fun setDeviceName(str:String){
        actionBar?.subtitle = getString(R.string.title_bar_device_name_field, str)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taiko = TaikoView(this)
        setContentView(taiko)
        setDeviceName(getString(R.string.device_none))

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) displayUnsupportedAlert()

        midiManager = getSystemService(Context.MIDI_SERVICE) as MidiManager
        addConnectedDevices()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback { onBackPressedImpl() }
        }

        timer.scheduleAtFixedRate( timerTask {
            if(activePort != null){
                synchronized(queue){
                    if(queue.size <= 0) return@timerTask
                    val buffer = ByteArray(3 * queue.size)
                    var numBytes = 0
                    for(q in queue){
                        val flag = if(q.up) 0x80 else 0x90
                        buffer[numBytes++] = (flag).toByte()
                        buffer[numBytes++] = (50 + q.pos).toByte()
                        buffer[numBytes++] = (127).toByte()
                    }
                    // 5ms timestamp
                    val nPms = 1000000L
                    val timeStamp = System.nanoTime() + (5 * nPms)
                    activePort?.send(buffer, 0, numBytes, timeStamp)
                    queue.clear()
                }
            }
        }, 0, 5)
    }

    private fun addConnectedDevices(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val tDevice = midiManager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
            this.devices.addAll(tDevice)
            midiManager.registerDeviceCallback(
                MidiManager.TRANSPORT_MIDI_BYTE_STREAM,
                mainExecutor,
                MidiCallbacks(this)
            )
        }else @Suppress("DEPRECATION") {
            midiManager.registerDeviceCallback( MidiCallbacks(this), Handler() )
            devices.addAll(midiManager.devices)
        }
        Toast.makeText(this,
            getString(R.string.toast_midi_device_found_total, devices.size), Toast.LENGTH_SHORT).show()
    }

    inner class MidiCallbacks(private val context: Context) : MidiManager.DeviceCallback(){

        override fun onDeviceAdded(device: MidiDeviceInfo?) {
            super.onDeviceAdded(device)
            if( device!= null) devices.add(device)
            Toast.makeText(context, getString(R.string.toast_device_connected), Toast.LENGTH_SHORT).show()
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo?) {
            super.onDeviceRemoved(device)
            if( device!= null) devices.remove(device)
            Toast.makeText(context,
                getString(R.string.toast_device_disconnected), Toast.LENGTH_SHORT).show()
        }

        override fun onDeviceStatusChanged(status: MidiDeviceStatus?) {
            super.onDeviceStatusChanged(status)
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
                retval = true
            }
            R.id.select_device -> {
                showDeviceSelection()
                retval = true
            }
            R.id.change_color -> {
                retval = true
            }
            R.id.switch_fullscreen -> {
                setFullscreen(true)
                retval = true
            }
        }
        return retval || super.onOptionsItemSelected(item)
    }

    private fun setActiveMidiDevice(md:MidiDevice){
        activeDevice?.close()
        activeDevice = md
        val midiName = md.info.properties.getString(MidiDeviceInfo.PROPERTY_NAME,
            getString(R.string.midi_device_unknown_name))
        val midiManufacture = md.info.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER,
            getString(
                R.string.midi_device_unknown_manufacturer
            ))
        setDeviceName("$midiName - $midiManufacture")
        Toast.makeText(this,
            getString(R.string.toast_device_change_info, midiName), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        activePort?.flush()
        activePort?.close()
        activeDevice?.close()
        super.onDestroy()
    }

    private fun showDeviceSelection(){
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(R.string.title_set_midi_device)

        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        devices.forEach {
            val itemName = it.properties.getString(MidiDeviceInfo.PROPERTY_NAME,
                getString(R.string.midi_device_unknown_name))
            arrayAdapter.add(itemName)
        }

        dialog.setNegativeButton(android.R.string.cancel) { d, _ -> d.cancel() }
        dialog.setAdapter(arrayAdapter) { d, i ->
            selectedDeviceIdx = i
            midiManager.openDevice(
                devices[i],
                { setActiveMidiDevice(it) },
                Handler(Looper.getMainLooper()))
            d.dismiss()
        }

        dialog.show()
    }

    private fun showPortSelection(){
        if(activeDevice != null){
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle(getString(R.string.title_set_midi_device_port))
            val arrayAdapter = ArrayAdapter<Int>(this, android.R.layout.select_dialog_singlechoice)
            for(i in 0 until activeDevice!!.info.inputPortCount){ arrayAdapter.add(i) }
            dialog.setNegativeButton(android.R.string.cancel) { d, _ -> d.cancel() }
            dialog.setAdapter(arrayAdapter){d,i->
                activePort?.flush()
                activePort?.close()
                activePort = activeDevice?.openInputPort(i)!!
                d.dismiss()
            }
            dialog.show()
        }else{
            Toast.makeText(this, getString(R.string.alert_no_midi_device_active), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFullscreen(isTrue:Boolean = true){
        if(isTrue){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.systemBars())
                actionBar?.hide()
            }else{
                @Suppress("DEPRECATION") // This for lower than SDK R
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            }
            Toast.makeText(this, getString(R.string.toast_entering_fullscreen), Toast.LENGTH_SHORT).show()
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                window.insetsController?.show(WindowInsets.Type.systemBars())
                actionBar?.show()
            }else{
                @Suppress("DEPRECATION") // This for lower than SDK R
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
        isFullScreen = isTrue
    }

    private fun onBackPressedImpl() {
        if(isFullScreen){
            setFullscreen(false)
        }else{
            finish()
        }
    }

    // This should be fine
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        onBackPressedImpl()
    }

    fun onDown(pos: Int) {
        if(activePort != null){
            synchronized(queue){
                queue.add(Queue(pos, false))
            }
        }
    }

    fun onUp(pos: Int) {
        if(activePort != null) {
            synchronized(queue) {
                queue.add(Queue(pos, true))
            }
        }
    }
}