package com.example.steganobot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MySteganoBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final String botToken;
    private final SteganographyService steganographyService;

    private final Map<Long, String> userSecrets = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> decodeMode = new ConcurrentHashMap<>();

    @Value("${bot.name}")
    private String botName;

    public MySteganoBot(@Value("${bot.token}") String botToken, SteganographyService steganographyService) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.steganographyService = steganographyService;
    }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public LongPollingSingleThreadUpdateConsumer getUpdatesConsumer() { return this; }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) return;
        long chatId = update.getMessage().getChatId();

        try {
            // 1. TEXT COMMANDS & MESSAGES
            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();

                // THE MENU COMMANDS
                if (text.equals("/start")) {
                    sendText(chatId, "Welcome, Operative.\n\nTo HIDE a message: Just type your secret text.\nTo READ a message: Type /decode");
                    return;
                }

                if (text.equals("/decode")) {
                    decodeMode.put(chatId, true);
                    sendText(chatId, "Decode mode activated! Please send me the secret image as an uncompressed FILE (Document).");
                    return;
                }

                if (text.equals("/help")) {
                    sendText(chatId, "🛠 **How to use this tool:**\n\n**To Encode:**\n1. Type your secret message normally.\n2. Send a normal Photo.\n3. Save the Document it sends back.\n\n**To Decode:**\n1. Click /decode in the menu.\n2. Send the uncompressed Document back to me.");
                    return;
                }

                // If they accidentally click a weird command
                if (text.startsWith("/")) {
                    sendText(chatId, "Invalid command. Please use the Menu button.");
                    return;
                }

                // If it's normal text, save it as the secret!
                userSecrets.put(chatId, text);
                decodeMode.put(chatId, false);
                sendText(chatId, "Secure channel established. I have memorized your secret message. Please transmit the cover photograph.");
            }

            // 2. PHOTO / DOCUMENT PROCESSING
            else if (update.getMessage().hasPhoto() || update.getMessage().hasDocument()) {

                String fileId = "";

                // IF DECODING...
                if (decodeMode.getOrDefault(chatId, false)) {
                    if (update.getMessage().hasDocument()) {
                        fileId = update.getMessage().getDocument().getFileId();
                    } else {
                        sendText(chatId, "Error: You sent a normal Photo! Telegram compressed it and destroyed the cipher. You must send it as a 'File' or 'Document'.");
                        return;
                    }

                    sendText(chatId, "Image received. Running decryption algorithms...");

                    GetFile getFileRequest = new GetFile(fileId);
                    org.telegram.telegrambots.meta.api.objects.File file = telegramClient.execute(getFileRequest);
                    String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();

                    BufferedImage stegoImage = ImageIO.read(URI.create(fileUrl).toURL());
                    String extractedSecret = steganographyService.extractText(stegoImage);

                    sendText(chatId, "🔓 **SECRET MESSAGE EXTRACTED** 🔓\n\n" + extractedSecret);
                    decodeMode.put(chatId, false); // Reset mode
                }

                // IF ENCODING...
                else {
                    if (!userSecrets.containsKey(chatId)) {
                        sendText(chatId, "Error: No transmission to hide. Please send me the secret text first!");
                        return;
                    }

                    if (update.getMessage().hasPhoto()) {
                        fileId = update.getMessage().getPhoto().stream().max(Comparator.comparing(PhotoSize::getFileSize)).get().getFileId();
                    } else if (update.getMessage().hasDocument()) {
                        fileId = update.getMessage().getDocument().getFileId();
                    }

                    sendText(chatId, "I received your photo! Now embedding the cipher...");
                    String secretText = userSecrets.get(chatId);

                    GetFile getFileRequest = new GetFile(fileId);
                    org.telegram.telegrambots.meta.api.objects.File file = telegramClient.execute(getFileRequest);
                    String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();

                    BufferedImage originalImage = ImageIO.read(URI.create(fileUrl).toURL());
                    BufferedImage stegoImage = steganographyService.embedText(originalImage, secretText);

                    File tempFile = File.createTempFile("secret_image", ".png");
                    ImageIO.write(stegoImage, "png", tempFile);

                    SendDocument sendDocument = SendDocument.builder()
                            .chatId(chatId)
                            .document(new InputFile(tempFile))
                            .caption("Transmission complete. \n\nYour message is safely hidden inside this uncompressed PNG file.\n\nTo read it, select /decode from the menu and send this file back to me.")
                            .build();
                    telegramClient.execute(sendDocument);

                    userSecrets.remove(chatId);
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "An error occurred: " + e.getMessage());
        }
    }

    private void sendText(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}