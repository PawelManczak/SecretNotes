package com.example.keystorenotes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.keystorenotes.ui.theme.KeystoreNotesTheme
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeystoreNotesTheme {
                DataEncryptionScreen()
            }
        }
    }
}

@Composable
fun DataEncryptionScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val inputText = remember { mutableStateOf("") }
    val outputText = remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(value = inputText.value,
            onValueChange = { inputText.value = it },
            label = { Text("Enter text to encrypt") },
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        Button(onClick = {
            saveData(context, inputText.value)
        }) {
            Text("Save")
        }

        Button(onClick = {
            outputText.value = loadDecryptedData(context)
        }) {
            Text("Load")
        }

        OutlinedTextField(value = outputText.value,
            onValueChange = {},
            label = { Text("Decrypted text") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {

        }) {
            Text("inport")
        }

        Button(onClick = {

        }) {
            Text("export")
        }
    }
}

@Composable
fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            TextField(value = pin,
                onValueChange = { pin = it },
                label = { Text("Enter PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = {
                    onDismiss()
                }))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                onConfirm(pin)
            }) {
                Text("OK")
            }
        }
    }
}

private val cryptoManager = CryptoManager()

private fun saveData(context: Context, text: String) {
    val bytes = text.encodeToByteArray()
    val file = File(context.filesDir, "secret.txt")
    if(!file.exists()) {
        file.createNewFile()
    }
    val fos = FileOutputStream(file)

    cryptoManager.encryptWithKeyAndPin(
        data = bytes,
        outputStream = fos,
        pin = "1234"
    )
}

private fun loadDecryptedData(context: Context): String {
    val file = File(context.filesDir, "secret.txt")
    if(!file.exists()) {
        return ""
    }
    val fis = file.inputStream()

    val decryptedBytes = cryptoManager.decryptWithPin(
        inputStream = fis,
        pin = "1234")
    return decryptedBytes.toString(Charset.defaultCharset())
}