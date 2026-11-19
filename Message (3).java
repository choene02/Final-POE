package quickchat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Represents one QuickChat message and holds the arrays used by the app.
 *
 * Arrays required by Part 3 (all populated while the program runs, no hard-coding):
 *  - sentMessages        : every message that was sent
 *  - disregardedMessages : every message that was disregarded
 *  - storedMessages      : every stored message (read back from the JSON file)
 *  - messageHashes / messageIds / allMessages : PARALLEL arrays, so index i in
 *    each one refers to the same message.
 */
public class Message {

    public static final int MAX_LENGTH = 250;

    // ---------- Arrays ----------
    private static final List<Message> sentMessages = new ArrayList<>();
    private static final List<Message> disregardedMessages = new ArrayList<>();
    private static final List<Message> storedMessages = new ArrayList<>();

    // Parallel arrays (same index = same message)
    private static final List<Message> allMessages = new ArrayList<>();
    private static final List<String> messageHashes = new ArrayList<>();
    private static final List<String> messageIds = new ArrayList<>();

    private static String storageFile = "storedMessages.json";
    private static String currentSender = "Unknown";
    private static final Random RANDOM = new Random();

    private final String messageId;
    private final int messageNumber;
    private final String sender;
    private final String recipient;
    private final String text;
    private final String messageHash;

    // Used by the app: ID is auto-generated, number = messages sent so far
    public Message(String recipient, String text) {
        this(generateMessageId(), sentMessages.size(), recipient, text);
    }

    // Used by the unit tests so the ID / number are known in advance
    public Message(String messageId, int messageNumber,
                   String recipient, String text) {
        this(messageId, messageNumber, currentSender, recipient, text, null);
    }

    // Full constructor. A hash read from the JSON file is kept exactly as stored.
    private Message(String messageId, int messageNumber, String sender,
                    String recipient, String text, String storedHash) {
        this.messageId = messageId;
        this.messageNumber = messageNumber;
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
        this.messageHash = (storedHash != null) ? storedHash : createMessageHash();
    }

    // ---------- Static helpers ----------

    // Random ten-digit number (leading zeros allowed, e.g. 0012345678)
    public static String generateMessageId() {
        return String.format("%010d",
                (long) (RANDOM.nextDouble() * 10_000_000_000L));
    }

    public static boolean isValidRecipient(String cell) {
        // Reuses the Part 1 checker (Login.checkCellPhoneNumber)
        return Login.checkCellPhoneNumber(cell);
    }

    // How many characters over the limit (0 or less = fine)
    public static int charactersOver(String text) {
        return text == null ? 0 : text.length() - MAX_LENGTH;
    }

    // ---------- Required methods (Part 2) ----------

    // Message ID must not be more than ten characters
    public boolean checkMessageID() {
        return messageId != null
                && !messageId.isEmpty()
                && messageId.length() <= 10;
    }

    public String checkRecipientCell() {
        if (isValidRecipient(recipient)) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not "
                + "contain an international code. Please correct the "
                + "number and try again.";
    }

    public String checkMessageLength() {
        int over = charactersOver(text);
        if (over <= 0) {
            return "Message ready to send.";
        }
        return "Message exceeds 250 characters by " + over
                + "; please reduce the size.";
    }

    // First two digits of ID : message number : FIRSTWORDLASTWORD (all caps)
    public String createMessageHash() {
        String idPart = (messageId != null && messageId.length() >= 2)
                ? messageId.substring(0, 2) : String.valueOf(messageId);

        String[] words = (text == null ? "" : text.trim()).split("\\s+");
        String first = clean(words[0]);
        String last = clean(words[words.length - 1]);

        return (idPart + ":" + messageNumber + ":" + first + last)
                .toUpperCase();
    }

    private static String clean(String word) {
        return word.replaceAll("[^A-Za-z0-9]", "");
    }

    // 1 = send, 2 = disregard, 3 = store
    public String sentMessage(int choice) {
        switch (choice) {
            case 1:
                sentMessages.add(this);
                registerInParallelArrays();
                return "Message successfully sent.";
            case 2:
                disregardedMessages.add(this);
                registerInParallelArrays();
                return "Press 0 to delete the message.";
            case 3:
                registerInParallelArrays();
                return storeMessage()
                        ? "Message successfully stored."
                        : "Message could not be written to the JSON file.";
            default:
                return "Invalid option. Please choose 1, 2 or 3.";
        }
    }

    // Adds this message to the parallel arrays (once only)
    private void registerInParallelArrays() {
        if (!messageIds.contains(messageId)) {
            allMessages.add(this);
            messageIds.add(messageId);
            messageHashes.add(messageHash);
        }
    }

    // All messages sent while the program is running
    public static String printMessages() {
        StringBuilder sb = new StringBuilder();
        for (Message m : sentMessages) {
            sb.append(m.details()).append("\n");
        }
        return sb.toString();
    }

    public static int returnTotalMessages() {
        return sentMessages.size();
    }

    // Order required by the brief: ID, Hash, Recipient, Message
    public String details() {
        return "Message ID: " + messageId
                + "\nMessage Hash: " + messageHash
                + "\nRecipient: " + recipient
                + "\nMessage: " + text;
    }

    // ---------- Part 3: features that use the arrays ----------

    // (a) Sender and recipient of every stored message
    public static String displaySenderAndRecipient() {
        if (storedMessages.isEmpty()) {
            return "No stored messages.";
        }
        StringBuilder sb = new StringBuilder();
        for (Message m : storedMessages) {
            sb.append("Sender: ").append(m.sender)
              .append(" | Recipient: ").append(m.recipient).append("\n");
        }
        return sb.toString();
    }

    // (b) Longest stored message (returns the message text)
    public static String longestStoredMessage() {
        Message longest = null;
        for (Message m : storedMessages) {
            if (longest == null || m.text.length() > longest.text.length()) {
                longest = m;
            }
        }
        return longest == null ? "No stored messages." : longest.text;
    }

    // (c) Search by message ID using the parallel arrays
    public static String searchByMessageId(String id) {
        int index = messageIds.indexOf(id);
        if (index < 0) {
            return "Message ID not found.";
        }
        Message m = allMessages.get(index);
        return "Recipient: " + m.recipient + ", Message: " + m.text;
    }

    // (d) All sent or stored message texts for one recipient
    public static List<String> searchByRecipient(String recipient) {
        List<String> found = new ArrayList<>();
        for (Message m : sentMessages) {
            if (m.recipient.equals(recipient)) {
                found.add(m.text);
            }
        }
        for (Message m : storedMessages) {
            if (m.recipient.equals(recipient)) {
                found.add(m.text);
            }
        }
        return found;
    }

    // (e) Delete a message using its hash; removes it from every array
    public static String deleteMessageByHash(String hash) {
        int index = -1;
        for (int i = 0; i < messageHashes.size(); i++) {
            if (messageHashes.get(i).equalsIgnoreCase(hash)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return "Message hash not found.";
        }

        Message target = allMessages.get(index);
        allMessages.remove(index);
        messageIds.remove(index);
        messageHashes.remove(index);

        removeById(sentMessages, target.messageId);
        removeById(disregardedMessages, target.messageId);
        if (removeById(storedMessages, target.messageId)) {
            writeStoredToFile();   // keep the JSON file in step with the array
        }
        return "Message: \"" + target.text + "\" successfully deleted.";
    }

    private static boolean removeById(List<Message> list, String id) {
        Iterator<Message> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().messageId.equals(id)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    // (f) Full report of all stored messages
    public static String storedMessagesReport() {
        if (storedMessages.isEmpty()) {
            return "No stored messages.";
        }
        StringBuilder sb = new StringBuilder();
        int number = 1;
        for (Message m : storedMessages) {
            sb.append("Message ").append(number++).append(":\n")
              .append("Message ID: ").append(m.messageId).append("\n")
              .append("Message Hash: ").append(m.messageHash).append("\n")
              .append("Sender: ").append(m.sender).append("\n")
              .append("Recipient: ").append(m.recipient).append("\n")
              .append("Message: ").append(m.text).append("\n")
              .append("--------------------------------------------------\n");
        }
        return sb.toString();
    }

    // ---------- JSON storage ----------

    public boolean storeMessage() {
        storedMessages.add(this);
        return writeStoredToFile();
    }

    // Writes the whole stored array to the JSON file
    private static boolean writeStoredToFile() {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < storedMessages.size(); i++) {
            Message m = storedMessages.get(i);
            json.append("  {\n")
                .append("    \"messageID\": \"").append(esc(m.messageId)).append("\",\n")
                .append("    \"messageHash\": \"").append(esc(m.messageHash)).append("\",\n")
                .append("    \"sender\": \"").append(esc(m.sender)).append("\",\n")
                .append("    \"recipient\": \"").append(esc(m.recipient)).append("\",\n")
                .append("    \"message\": \"").append(esc(m.text)).append("\"\n")
                .append("  }")
                .append(i < storedMessages.size() - 1 ? ",\n" : "\n");
        }
        json.append("]\n");

        try {
            Files.write(Paths.get(storageFile),
                    json.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads the JSON file back into the stored messages array.
     * Returns how many messages were loaded (0 if the file is missing/unreadable).
     */
    public static int loadStoredMessages() {
        Path path = Paths.get(storageFile);
        String content;
        try {
            if (!Files.exists(path)) {
                return 0;
            }
            content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return 0;
        }

        storedMessages.clear();
        for (Map<String, String> fields : parseJson(content)) {
            String hash = fields.get("messageHash");
            Message m = new Message(
                    fields.get("messageID"),
                    numberFromHash(hash),
                    fields.getOrDefault("sender", "Unknown"),
                    fields.get("recipient"),
                    fields.get("message"),
                    hash);
            storedMessages.add(m);
            m.registerInParallelArrays();
        }
        return storedMessages.size();
    }

    // The message number is the middle part of the hash (00:0:HITONIGHT -> 0)
    private static int numberFromHash(String hash) {
        try {
            return Integer.parseInt(hash.split(":")[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    // REFERENCE: this small parser was written for the simple JSON format this
    // app writes (an array of objects whose values are all strings). Replace
    // this line with any online source(s) you used to help you read the JSON
    // file - the brief requires you to attribute them.
    private static List<Map<String, String>> parseJson(String s) {
        List<Map<String, String>> objects = new ArrayList<>();
        Map<String, String> current = null;
        String key = null;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                current = new HashMap<>();
                key = null;
            } else if (c == '}') {
                if (current != null) {
                    objects.add(current);
                }
                current = null;
            } else if (c == '"') {
                StringBuilder token = new StringBuilder();
                i++;
                while (i < s.length() && s.charAt(i) != '"') {
                    char d = s.charAt(i);
                    if (d == '\\' && i + 1 < s.length()) {
                        char next = s.charAt(++i);
                        switch (next) {
                            case 'n': token.append('\n'); break;
                            case 'r': token.append('\r'); break;
                            case 't': token.append('\t'); break;
                            case 'u':
                                if (i + 4 < s.length()) {
                                    token.append((char) Integer.parseInt(
                                            s.substring(i + 1, i + 5), 16));
                                    i += 4;
                                }
                                break;
                            default: token.append(next);
                        }
                    } else {
                        token.append(d);
                    }
                    i++;
                }
                if (current != null) {
                    if (key == null) {
                        key = token.toString();
                    } else {
                        current.put(key, token.toString());
                        key = null;
                    }
                }
            }
        }
        return objects;
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---------- Getters / test support ----------

    public String getMessageId() { return messageId; }
    public String getMessageHash() { return messageHash; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getText() { return text; }

    public static List<Message> getSentMessages() { return Collections.unmodifiableList(sentMessages); }
    public static List<Message> getDisregardedMessages() { return Collections.unmodifiableList(disregardedMessages); }
    public static List<Message> getStoredMessages() { return Collections.unmodifiableList(storedMessages); }
    public static List<String> getMessageHashes() { return Collections.unmodifiableList(messageHashes); }
    public static List<String> getMessageIds() { return Collections.unmodifiableList(messageIds); }

    public static void setStorageFile(String path) { storageFile = path; }
    public static void setSender(String senderCell) { currentSender = senderCell; }

    public static void resetAll() {
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        allMessages.clear();
        messageHashes.clear();
        messageIds.clear();
    }
}
