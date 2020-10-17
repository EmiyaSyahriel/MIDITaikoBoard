package id.emiyasyahriel.taikoboard

import android.graphics.Color

object AppState {
    var keyCount = 4

    // 0
    var whiteColor = Color.BLUE
    // 1
    var magentaColor = Color.MAGENTA
    // 2
    var yellowColor = Color.YELLOW

    var colors = arrayListOf(
        arrayListOf(2),
        arrayListOf(0,0),
        arrayListOf(0,2,0),
        arrayListOf(0,1,1,0),
        arrayListOf(0,1,2,1,0),
        arrayListOf(0,1,0,0,1,0),
        arrayListOf(0,1,0,2,0,1,0),
        arrayListOf(0,1,1,0,0,1,1,0),
        arrayListOf(0,1,1,0,2,0,1,1,0),
    )
}