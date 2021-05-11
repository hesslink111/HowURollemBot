package io.deltawave.primaryserver.roll

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.MessageEntity
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
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
        val messageText = message.text()
            .replace("@HowURollemTestBot", "")
            .replace("@HowURollemBot", "")
            .trim()

        if(!messageText.startsWith("/roll")) {
            return
        }

        val editMessage = if(update.editedMessage() != null) "Edit: " else ""

        // Evaluate roll and build response.
        val (responseText, entities, isError) = try {
            val evaluator = rollParser.parse(messageText)

            val specials = buildSet(evaluator::specialCircumstances)
            val result = evaluator.eval()

            val tStringBuilder = TStringBuilder()

            // Name
            tStringBuilder.append(editMessage)
            tStringBuilder.append("@$username") { o, l -> listOf(
                MessageEntity(MessageEntity.Type.text_mention, o, l)
                    .user(message.from())
            ) }
            tStringBuilder.append(": ")

            // Command
            val args = buildString { evaluator.printParseTree(this, command=true) }
            val command = "/roll$args"
            tStringBuilder.append(command, listOf(MessageEntity.Type.bot_command))
            tStringBuilder.append("\n")

            // Preview
            val parseTree = buildString { evaluator.printParseTree(this, command=false) }
            tStringBuilder.append(" ")
            tStringBuilder.append(parseTree)
            tStringBuilder.append(" ➞ ")
            evaluator.print(tStringBuilder)
            if(tStringBuilder.size > 3000 || tStringBuilder.entityCount > 90) {
                tStringBuilder.shortenToLeq(3000, 90)
                tStringBuilder.append("...")
            }
            tStringBuilder.append("\n")

            // Result
            tStringBuilder.append("$result ${specialMessage(result, specials)}", listOf(MessageEntity.Type.bold))

            val (text, entities) = tStringBuilder.toTString()

            Triple(text, entities, false)
        } catch(ex: Exception) {
            println("Ex: $ex")
            Triple(editMessage + errorMessage(username), emptyList(), true)
        }

        val response = SendMessage(
            message.chat().id(),
            responseText
        )
            .replyToMessageId(message.messageId())
            .entities(*entities.toTypedArray())

        // Show roll examples.
        if(isError) {
            response.replyMarkup(
                ReplyKeyboardMarkup(
                    arrayOf("/roll d20", "/roll 2d20"),
                    arrayOf("/roll 2d20kh", "/roll 2d20kl")
                )
                    .oneTimeKeyboard(true)
                    .selective(true)
            )
        }

        // Send message.
        bot.execute(response)
    }
}