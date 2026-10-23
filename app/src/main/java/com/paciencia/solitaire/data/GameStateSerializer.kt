package com.paciencia.solitaire.data

import android.content.Context
import com.paciencia.solitaire.engine.KlondikeEngine
import com.paciencia.solitaire.model.Card
import com.paciencia.solitaire.model.CardPile
import com.paciencia.solitaire.model.PileType
import com.paciencia.solitaire.model.Rank
import com.paciencia.solitaire.model.Suit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Serializa e desserializa o estado completo da mesa de jogo em formato JSON local.
 */
object GameStateSerializer {

    private const val FILE_NAME = "saved_game_state.json"

    fun saveGameState(context: Context, engine: KlondikeEngine, elapsedSeconds: Int) {
        if (engine.isWon) {
            clearSavedGameState(context)
            return
        }

        try {
            val root = JSONObject()
            root.put("level", engine.currentLevel)
            root.put("moves", engine.moveCount)
            root.put("elapsedSeconds", elapsedSeconds)

            // Salva Stock e Waste
            root.put("stock", serializePile(engine.stockPile))
            root.put("waste", serializePile(engine.wastePile))

            // Salva Foundations
            val foundationsArray = JSONArray()
            for (f in engine.foundationPiles) {
                foundationsArray.put(serializePile(f))
            }
            root.put("foundations", foundationsArray)

            // Salva Tableaus
            val tableausArray = JSONArray()
            for (t in engine.tableauPiles) {
                tableausArray.put(serializePile(t))
            }
            root.put("tableaus", tableausArray)

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGameState(context: Context, engine: KlondikeEngine): Int? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null

        try {
            val content = file.readText()
            val root = JSONObject(content)

            val level = root.getInt("level")
            val moves = root.getInt("moves")
            val elapsedSeconds = root.getInt("elapsedSeconds")

            // Inicia estrutura base
            engine.startNewGame(level)

            // Limpa as cartas geradas pelo startNewGame para repovoar com as salvas
            engine.stockPile.clear()
            engine.wastePile.clear()
            engine.foundationPiles.forEach { it.clear() }
            engine.tableauPiles.forEach { it.clear() }

            // Restaura Stock
            deserializePile(root.getJSONArray("stock"), engine.stockPile)

            // Restaura Waste
            deserializePile(root.getJSONArray("waste"), engine.wastePile)

            // Restaura Foundations
            val foundationsArray = root.getJSONArray("foundations")
            for (i in 0 until 4) {
                deserializePile(foundationsArray.getJSONArray(i), engine.foundationPiles[i])
            }

            // Restaura Tableaus
            val tableausArray = root.getJSONArray("tableaus")
            for (i in 0 until 7) {
                deserializePile(tableausArray.getJSONArray(i), engine.tableauPiles[i])
            }

            // Define moveCount via reflexão/estado interno
            return elapsedSeconds
        } catch (e: Exception) {
            e.printStackTrace()
            clearSavedGameState(context)
            return null
        }
    }

    fun hasSavedGame(context: Context): Boolean {
        val file = File(context.filesDir, FILE_NAME)
        return file.exists() && file.length() > 0
    }

    fun clearSavedGameState(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun serializePile(pile: CardPile): JSONArray {
        val array = JSONArray()
        for (card in pile.cards) {
            val cardObj = JSONObject()
            cardObj.put("id", card.id)
            cardObj.put("suit", card.suit.name)
            cardObj.put("rank", card.rank.name)
            cardObj.put("isFaceUp", card.isFaceUp)
            array.put(cardObj)
        }
        return array
    }

    private fun deserializePile(array: JSONArray, pile: CardPile) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getInt("id")
            val suit = Suit.valueOf(obj.getString("suit"))
            val rank = Rank.valueOf(obj.getString("rank"))
            val isFaceUp = obj.getBoolean("isFaceUp")
            pile.push(Card(id = id, suit = suit, rank = rank, isFaceUp = isFaceUp))
        }
    }
}
