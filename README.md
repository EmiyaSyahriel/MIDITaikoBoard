# MIDITaikoBoard
A simple MIDI-based Taiko controller for osu! in combination with MIDI to keyboard converter app

**Warning**: This app still suffers multi-touch delay

## Requirements
- Android device with OS version 6.0 or later and MIDI output capability
- USB Data Cable (that can really transfer data.)
- A MIDI signal to Keyboard converter PC app (or on some Linux Distro, configuration)

# How it works
This app sends a send a MIDI Signal when there is a touch event, the signal depends on which 
button you tap and which event the device receive. The signal is always the same for every session

# How to use
- Plug your phone to your PC
- Open the Device USB connection type selector
(it usually appear as dialog during connection, but sometimes you have to find it in notification area)
- Select MIDI Output
- Open this App
- Choose the PC as the output device by tapping the USB Icon at the action bar
and then choosing "Android MIDI USB Peripheral"
- Choose the Input port number by tapping the Up/Down Arrow icon at the action bar
and then choosing whichever you like (this affects your setting at the converter app)
- On PC, open your MIDI-to-Keyboard converter. Set it up, then start it. 
Now every MIDI signal received from MIDITaikoBoard will be treated as keyboard event in your PC depending your converter setup
- Now fire up your game, and then setup your keymap. Then you can enjoy your game using this app.

# License
GNU GPL v3, See LICENSE
