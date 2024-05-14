package com.example.keystorenotes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset

class MainActivity : ComponentActivity() {

    lateinit var context: Context
    private var exportText: String = ""
    private var exportPin: String = ""
    private var importText: String = ""
    private var importStream: InputStream? = null
    private var fileSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            KeystoreNotesTheme {
                context = LocalContext.current
                DataEncryptionScreen()
            }
        }
    }

    @Composable
    fun DataEncryptionScreen() {

        val context = LocalContext.current
        val focusManager = LocalFocusManager.current

        var inputText by remember { mutableStateOf("") }
        var outputText by remember { mutableStateOf("") }

        val focusRequester = remember { FocusRequester() }

        var showSavePinDialog by remember { mutableStateOf(false) }
        var showLoadPinDialog by remember { mutableStateOf(false) }
        var showExportPinDialog by remember { mutableStateOf(false) }
        var showImportPinDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Enter text to encrypt") },
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Button(onClick = {
                showSavePinDialog = true
            }) {
                Text("Save")
            }

            Button(onClick = {
                showLoadPinDialog = true
            }) {
                Text("Load")
            }

            OutlinedTextField(
                value = outputText,
                onValueChange = {},
                label = { Text("Decrypted text") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = {
                importDataFromFile()
                showImportPinDialog = true

            }) {
                Text("import")
            }

            Button(onClick = {
                showExportPinDialog = true
            }) {
                Text("export")
            }
        }

        if (showSavePinDialog) {
            PinDialog(onDismiss = { showSavePinDialog = false }, onConfirm = { pin ->
                saveData(context, inputText, pin)
                showSavePinDialog = false
            })
        }
        if (showLoadPinDialog) {
            PinDialog(onDismiss = { showLoadPinDialog = false }, onConfirm = { pin ->
                try {
                    outputText = loadDecryptedData(context, pin)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Wystąpił błąd podczas odczytu pliku, spróbuj jeszcze raz",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showLoadPinDialog = false
            })
        }
        if (showImportPinDialog) {
            PinDialog(onDismiss = { showImportPinDialog = false }, onConfirm = { pin ->
                try {
                    inputText = cryptoManager.decryptWithPin(importStream!!, pin).decodeToString()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Wystąpił błąd podczas importu pliku, spróbuj jeszcze raz",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                importStream!!.close()
                showImportPinDialog = false
                fileSelected = false
            })
        }
        if (showExportPinDialog) {
            PinDialog(onDismiss = { showExportPinDialog = false }, onConfirm = { pin ->
                saveDataToFile(inputText, pin)
                showExportPinDialog = false
            })

        }
    }

    @Composable
    fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
        var pin by remember { mutableStateOf("") }
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                    .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Enter PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(onDone = {
                        onDismiss()
                    })
                )
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

    private fun saveData(context: Context, text: String, pin: String) {
        val bytes = text.encodeToByteArray()
        val file = File(context.filesDir, "secret.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        val fos = FileOutputStream(file)

        cryptoManager.encryptWithKeyAndPin(
            data = bytes, outputStream = fos, pin = pin
        )
    }

    private fun loadDecryptedData(context: Context, pin: String): String {
        val file = File(context.filesDir, "secret.txt")
        if (!file.exists()) {
            return ""
        }
        val fis = file.inputStream()

        val decryptedBytes = cryptoManager.decryptWithPin(
            inputStream = fis, pin = pin
        )
        return decryptedBytes.toString(Charset.defaultCharset())
    }

    private val exportFile = registerForActivityResult(
        CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                cryptoManager.encryptWithKeyAndPin(
                    exportText.toByteArray(), outputStream!!, exportPin
                )
                exportText = ""
                exportPin = ""
                Toast.makeText(this, "Plik zapisany", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Wystąpił błąd podczas zapisu pliku", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private val importFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedFileUri: Uri? = result.data?.data
                selectedFileUri?.let { uri ->
                    try {
                        importStream = contentResolver.openInputStream(uri)
                        fileSelected = true
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Nie można odnaleźć pliku", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this, "Wystąpił błąd podczas odczytu pliku", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    fun saveDataToFile(text: String, pin: String) {
        exportText = text
        exportPin = pin
        exportFile.launch("exported_text.txt")
    }

    fun importDataFromFile() {
        importFile.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        })
    }


}
