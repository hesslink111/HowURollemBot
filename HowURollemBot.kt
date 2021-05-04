package io.deltawave.primaryserver.roll

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage

class HowURollemBot(
    botToken: String,
    private val rollParser: RollParser,
    private val specialMessage: (result: Int) -> String,
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

        if(message?.text() == null || !message.text().startsWith("/roll")) {
            return
        }

        val username = message.from().username()
            ?: message.from().firstName()
            ?: message.from().lastName()
        val text = message.text()

        val editMessage = if(update.editedMessage() != null) "Edit: " else ""

        val responseText = editMessage + try {
            val evaluator = rollParser.parse(text)
            val parsedText = buildString(evaluator::printParseTree)
            val fullEvaluation = buildString(evaluator::print)
            val result = evaluator.eval()
            """
                @$username rolled:
                 $parsedText ➞ $fullEvaluation
                <b>$result</b> ${specialMessage(result)}
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