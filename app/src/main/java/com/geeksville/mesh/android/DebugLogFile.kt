

package com.geeksville.mesh.android

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

class DebugLogFile(context: Context, name: String) {
    val stream = FileOutputStream(File(context.getExternalFilesDir(null), name), true)
    val file = PrintWriter(stream)

    fun close() {
        file.close()
    }

    fun log(s: String) {
        file.println(s) 
        file.flush() 
    }
}

class BinaryLogFile(context: Context, name: String) :
    FileOutputStream(File(context.getExternalFilesDir(null), name), true) {

}
