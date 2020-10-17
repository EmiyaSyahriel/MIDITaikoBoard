package id.emiyasyahriel.taikoboard

interface ITaikoViewReceiver {
    fun onDown(pos:Int)
    fun onUp(pos:Int)
}