

// ChatGPT prompts
//
// 1. Best way to implement grid like Wordle in Kotlin
// 2. Android app development logic and structure
// 3. How to check if the row is full or not full
// 4. How do display pop up window
// 5. How to make it jump to next cell when user inputs letter in cell



package com.example.wordleclone

import android.os.Bundle                        // Nodrošina iespēju pārsūtīt datus starp dažādām Android aktivitātēm
import android.text.Editable                    // Nodrošina teksta rediģēšanas funkcionalitāti
import android.text.TextWatcher                 // Ļauj uztverēt un reaģēt uz teksta ievades izmaiņām
import android.view.Gravity                     // Nodrošina konstantes elementu izkārtošanai ekrānā
import android.view.View                        // Nodrošina piekļuvi UI elementa pamatfunkcijām
import android.widget.EditText                  // Teksta ievades lauks
import android.widget.GridLayout                // Tīkla izkārtojums (grid layout), kas ļauj izkārtot elementus tīklveida struktūrā
import androidx.appcompat.app.AppCompatActivity // Bāzes klase Android aktivitātei ar atbalstu bibliotēkas funkcijām
import androidx.core.content.ContextCompat      // Nodrošina metodes resursu un krāsu izmantošanai
import android.text.InputFilter                 // Ļauj definēt ierobežojumus ievadītajam tekstam
import android.text.InputType                   // Ļauj definēt ievades tipu, piemēram, skaitļiem vai teksta lielajiem burtiem
import java.io.IOException                      // Kļūdu apstrāde, kas rodas darbībās ar ievadi/izvadi
import android.util.Log                         // Izmanto, lai veiktu žurnālēšanu attīstības procesā
import android.widget.Toast                     // Īss ziņojums, kas parādās uz ekrāna
import android.app.AlertDialog                  // Dialogu logs, kas parāda brīdinājumus vai informāciju lietotājam
import java.util.Locale                         // Klase, kas nodrošina atbalstu dažādām lokalizācijām un valodām

class MainActivity : AppCompatActivity() {
    // Privātie mainīgie priekš GridLayout un EditText elementiem
    private lateinit var gridLayout: GridLayout
    private val rows = 6                                // Rindu skaits
    private val columns = 5                             // Kolonnu skaits
    private val totalCells = rows * columns             // Kopējais šūnu skaits
    private val editTexts = mutableListOf<EditText>()   // Teksta ievades lauki
    private var currentActiveRow = 0                    // Pašreiz aktīvā rinda
    private lateinit var wordList: List<String>         // Vārdu saraksts
    private lateinit var currentWord: String            // Pašreizējais vārds

    // Funkcija, kas tiek izsaukta, kad aktivitāte tiek izveidota
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)                           // Iestata sākotnējo skatu
        wordList = readWordsFromAssets()                                 // Ielasīt vārdu sarakstu no failiem
        gridLayout = findViewById(R.id.gridLayout)                       // Atrod GridLayout elementu
        currentWord = wordList.random()                                  // Izvēlēties gadījuma vārdu no saraksta
        Log.d("WordleClone", "Chosen word: $currentWord")       // Logot izvēlēto vārdu

        // Definē katras šūnas izmēru pikseļos
        val sizeInPixels = resources.getDimensionPixelSize(R.dimen.cell_size)

        // Inicializēt režģi ar kvadrātveida EditText elementiem
        for (i in 0 until totalCells) {
            val editText = EditText(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = sizeInPixels
                    height = sizeInPixels
                    setMargins(2, 2, 2, 2)
                }
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(context, R.drawable.cell_background)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                filters = arrayOf(InputFilter.LengthFilter(1))
                isEnabled = i / columns == currentActiveRow // Tā lai lietotājs varētu piekļūt tikai pirmajai rindai un nebūtu spējīgs neko rakstīt nākamājā līdz nav iesniedzis savu minējumu
            }

            // Teksta izmaiņu novērošana
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s != null && s.length == 1) {
                        val nextEditTextIndex = editTexts.indexOf(editText) + 1
                        if (nextEditTextIndex < editTexts.size && nextEditTextIndex / columns == currentActiveRow) {
                            editTexts[nextEditTextIndex].requestFocus()
                        }
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            editTexts.add(editText)
            gridLayout.addView(editText)
        }
    }

    // Funkcija, kas tiek izsaukta, kad tiek nospiesta Ievadīt (Enter) taustiņš
    @Suppress("UNUSED_PARAMETER")
    fun onEnterClick(view: View) {
        // Pārbaudīt, vai pašreizējā rinda ir pilnībā aizpildīta
        val start = currentActiveRow * columns
        val end = start + columns
        val currentRowWord = StringBuilder()
        var isRowComplete = true

        for (i in start until end) {
            val letter = editTexts[i].text.toString()
            if (letter.isEmpty()) {
                isRowComplete = false
                break
            }
            currentRowWord.append(letter)
        }

        val guessedWord = currentRowWord.toString()                 // Pārveidot StringBuilder par String

        if (!isRowComplete) {
            showMessage("Not enough letters")                       // Parādīt ziņojumu, ja nav pietiekami daudz burtu
            return
        }

        if (wordList.contains(guessedWord.lowercase(Locale.ROOT))) {
            colorCells(guessedWord, currentWord)
        }

        if (!wordList.contains(guessedWord.lowercase(Locale.ROOT))) {
            showMessage("Not in word list")                         // Parādīt ziņojumu, ja minētais vārds nav sarakstā
            return
        }

        if (guessedWord.equals(currentWord, ignoreCase = true)) {
            showWinDialog()                                         // Uzvaras dialoga parādīšana
            for (editText in editTexts) {
                editText.isEnabled = false                          // Atspējot visus ievades laukus
            }
        } else {
            if (currentActiveRow == rows - 1) {
                showLossDialog()                                    // Zaudējuma dialoga parādīšana
            } else {
                advanceToNextRow()                                  // Sagatavoties nākamajam mēģinājumam
            }
        }
    }

    // Funkcija, kas parāda dialogu pēc zaudējuma
    private fun showLossDialog() {
        val dialogBuilder = AlertDialog.Builder(this)                       // Izveido dialoga veidotāju
        dialogBuilder.setMessage("You lost! The correct word was '$currentWord'.") // Uzstāda ziņojumu par zaudējumu un pareizo vārdu
            .setPositiveButton("Play again") { _, _ ->
                startNewGame()                                                     // Ja tiek nospiesta pogas "Spēlēt vēlreiz", sākt jaunu spēli
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()                                                           // Ja tiek nospiesta pogas "Iziet", beigt lietotnes darbību
            }
        dialogBuilder.create().show()                                              // Izveidot un parādīt dialogu
    }

    // Funkcija, kas krāso šūnas atkarībā no minējuma rezultātiem
    private fun colorCells(guessedWord: String, currentWord: String) {
        val correctPositions = BooleanArray(currentWord.length) { false }
        val presentPositions = BooleanArray(guessedWord.length) { false }

        // Seko burtu biežumam aktuālajā vārdā
        val letterFrequency = currentWord.groupingBy { it }.eachCount().toMutableMap()

        // Pirmā iterācija lai atzīmētu pareizos burtus
        for (i in guessedWord.indices) {
            val char = guessedWord[i].lowercaseChar()
            if (char == currentWord[i].lowercaseChar()) {
                correctPositions[i] = true
                // Samazina burtu skaitu frekvencē
                letterFrequency[char] = letterFrequency[char]!! - 1
                editTexts[currentActiveRow * columns + i].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.colorCorrect)
                )
            }
        }

        // Otrā iterācija lai atzīmētu klātesošos burtus
        for (i in guessedWord.indices) {
            val char = guessedWord[i].lowercaseChar()
            if (!correctPositions[i] && letterFrequency[char] != null && letterFrequency[char]!! > 0) {
                presentPositions[i] = true
                // Samazina burtu skaitu frekvencē
                letterFrequency[char] = letterFrequency[char]!! - 1
                editTexts[currentActiveRow * columns + i].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.colorPresent)
                )
            }
        }

        // Trešā iterācija lai atzīmētu neesošos burtus
        for (i in guessedWord.indices) {
            if (!correctPositions[i] && !presentPositions[i]) {
                editTexts[currentActiveRow * columns + i].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.colorAbsent)
                )
            }
        }
    }

    // Funkcija lai sagatavotu nākamo rindu
    private fun advanceToNextRow() {
        // Atspējo pašreizējo rindu
        val currentStartIndex = currentActiveRow * columns
        val currentEndIndex = currentStartIndex + columns
        for (i in currentStartIndex until currentEndIndex) {
            editTexts[i].isEnabled = false
        }

        // Palielina rindu skaitu, lai iespējotu nākamo rindu
        if (currentActiveRow < rows - 1) {
            currentActiveRow++
            val newStartIndex = currentActiveRow * columns
            val newEndIndex = newStartIndex + columns
            for (i in newStartIndex until newEndIndex) {
                editTexts[i].isEnabled = true       // Iespējot EditText šūnas pašreizējai rindai
            }
            editTexts[newStartIndex].requestFocus() // Uzstādīt fokusu pirmajam EditText jaunajā rindā
        }
    }

    // Funkcija lai parādītu ziņojumu
    private fun showMessage(message: String) {
        // Izmanto Toast, lai parādītu ziņojumu
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // Funkcija, kas ielasa vārdus no failiem
    private fun readWordsFromAssets(): List<String> {
        val words = mutableListOf<String>()
        try {
            assets.open("five_letter_words.txt").bufferedReader().useLines { lines ->
                words.addAll(lines.toList())
            }
        } catch (e: IOException) {
            e.printStackTrace() // Apstrādā izņēmumus šeit
        }
        return words
    }

    // Dialogs par uzvaru
    private fun showWinDialog() {
        // Izmanto Builder klasi ērtai dialoga veidošanai
        val dialogBuilder = AlertDialog.Builder(this)

        dialogBuilder.setMessage("You won!") // Ziņojums par uzvaru
            .setPositiveButton("Play again") { _, _ ->
                // Lietotājs nospiedis pogu "Spēlēt vēlreiz", sākt jaunu spēli
                startNewGame()
            }
            .setNegativeButton("Exit") { _, _ ->
                // Lietotājs atcēlis dialogu, iziet no lietotnes
                finish()
            }
        // Izveidot un parādīt AlertDialog
        dialogBuilder.create().show()
    }

    // Sākt jaunu spēli
    private fun startNewGame() {
        // Atiestatīt spēli, lai sāktu no jauna
        currentActiveRow = 0
        currentWord = wordList.random()
        Log.d("WordleClone", "New chosen word: $currentWord") // Logot konsolē jauno izvēlēto vārdu, priekš testēšanas

        // Notīrīt visus EditText un iespējot tikai pirmo rindu
        for (i in editTexts.indices) {
            with(editTexts[i]) {
                text.clear()
                background = ContextCompat.getDrawable(context, R.drawable.cell_background)
                isEnabled = i / columns == currentActiveRow
            }
        }
    }
}