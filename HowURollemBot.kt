package io.deltawave.primaryserver.roll

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.MessageEntity
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage

@ExperimentalStdlibApi
class HowURollemBot(
    botToken: String,
    private val rollParser: RollParser,
    private val specialMessage: (result: Int, specials: Set<String>) -> String,
    private val errorMessage: (u: String) -> String
) {
    private val bot = TelegramBot(botToken)

    init {
        bot.setUpdatesListener(UpdatesListener(::updatesListener))
    }

    private fun updatesListener(updates: List<Update>): Int {
        updates.forEach(::update)
        return UpdatesListener.CONFIRMED_UPDATES_ALL
    }

    private fun update(update: Update) {
        val message = update.editedMessage() ?: update.message()

        if(message?.text() == null) {
            return
        }

        val username = message.from().username()
            ?: message.from().firstName()
            ?: message.from().lastName()
        val text = message.text()
            .replace("@HowURollemTestBot", "")
            .replace("@HowURollemBot", "")
            .trim()

        if(!text.startsWith("/roll")) {
            return
        }

        val editMessage = if(update.editedMessage() != null) "Edit: " else ""

        val responseText = editMessage + try {
            val evaluator = rollParser.parse(text)
            val parsedText = buildString(evaluator::printParseTree)
            val fullEvaluation = buildString(evaluator::print)
            val fullPreview = "$parsedText ➞ $fullEvaluation"
            val preview = if(fullPreview.length <= 3000) {
                fullPreview
            } else {
                fullPreview.take(3000) + "..."
            }
            val specials = buildSet(evaluator::specialCircumstances)
            val result = evaluator.eval()
            """
                @$username rolled:
                 $preview
                <b>$result</b> ${specialMessage(result, specials)}
            """.trimIndent()
        } catch(ex: Exception) {
            println("Ex: $ex")
            errorMessage(username)
        }

        val response = SendMessage(
            message.chat().id(),
            responseText
        )
            .replyToMessageId(message.messageId())
            .parseMode(ParseMode.HTML)
        bot.execute(response)
    }
}