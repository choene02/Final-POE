package quickchat;

import chatapp.Message;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Part 3 unit tests. Uses the five test data messages from the brief:
 *  1 Sent, 2 Stored, 3 Disregard, 4 Sent (developer entry), 5 Stored.
 */
public class StoredMessagesTest {

    private static final String TEXT_1 = "Did you get the cake?";
    private static final String TEXT_2 = "Where are you? You are late! I have asked you to be on time.";
    private static final String TEXT_3 = "Yohoooo, I am at your gate.";
    private static final String TEXT_4 = "It is dinner time!";
    private static final String TEXT_5 = "Ok, I am leaving without you.";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File json;
    private Message m1, m2, m3, m4, m5;

    @Before
    public void setUp() {
        Message.resetAll();
        json = new File(tmp.getRoot(), "storedMessages.json");
        Message.setStorageFile(json.getAbsolutePath());
        Message.setSender("+27821234567");

        m1 = new Message("0000000001", 0, "+27834557896", TEXT_1);
        m2 = new Message("0000000002", 1, "+27838884567", TEXT_2);
        m3 = new Message("0000000003", 2, "+27834484567", TEXT_3);
        m4 = new Message("0000000004", 3, "0838884567", TEXT_4);
        m5 = new Message("0000000005", 4, "+27838884567", TEXT_5);

        m1.sentMessage(1);   // Sent
        m2.sentMessage(3);   // Stored
        m3.sentMessage(2);   // Disregard
        m4.sentMessage(1);   // Sent
        m5.sentMessage(3);   // Stored
    }

    private static List<String> texts(List<Message> messages) {
        List<String> result = new ArrayList<>();
        for (Message m : messages) {
            result.add(m.getText());
        }
        return result;
    }

    // ----- Arrays correctly populated -----
    @Test
    public void testSentMessagesArrayPopulated() {
        assertEquals(Arrays.asList(TEXT_1, TEXT_4), texts(Message.getSentMessages()));
    }

    @Test
    public void testDisregardedMessagesArrayPopulated() {
        assertEquals(Arrays.asList(TEXT_3), texts(Message.getDisregardedMessages()));
    }

    @Test
    public void testStoredMessagesArrayPopulated() {
        assertEquals(Arrays.asList(TEXT_2, TEXT_5), texts(Message.getStoredMessages()));
    }

    @Test
    public void testMessageHashArrayPopulated() {
        assertEquals(5, Message.getMessageHashes().size());
        assertEquals("00:0:DIDCAKE", Message.getMessageHashes().get(0));
        assertTrue(Message.getMessageHashes().contains(m2.getMessageHash()));
    }

    @Test
    public void testMessageIdArrayPopulated() {
        assertEquals(Arrays.asList("0000000001", "0000000002", "0000000003",
                "0000000004", "0000000005"), Message.getMessageIds());
    }

    // ----- Longest message -----
    @Test
    public void testLongestMessage() {
        assertEquals(TEXT_2, Message.longestStoredMessage());
    }

    @Test
    public void testLongestMessageWhenNoneStored() {
        Message.resetAll();
        assertEquals("No stored messages.", Message.longestStoredMessage());
    }

    // ----- Search for message ID -----
    @Test
    public void testSearchByMessageId() {
        assertEquals("Recipient: 0838884567, Message: " + TEXT_4,
                Message.searchByMessageId("0000000004"));
    }

    @Test
    public void testSearchByMessageIdNotFound() {
        assertEquals("Message ID not found.", Message.searchByMessageId("9999999999"));
    }

    // ----- Search by recipient -----
    @Test
    public void testSearchByRecipient() {
        assertEquals(Arrays.asList(TEXT_2, TEXT_5),
                Message.searchByRecipient("+27838884567"));
    }

    @Test
    public void testSearchByRecipientNotFound() {
        assertTrue(Message.searchByRecipient("+27800000000").isEmpty());
    }

    // ----- Delete using message hash -----
    @Test
    public void testDeleteMessageByHash() throws Exception {
        String hash = m2.getMessageHash();
        assertEquals("Message: \"" + TEXT_2 + "\" successfully deleted.",
                Message.deleteMessageByHash(hash));

        assertEquals(Arrays.asList(TEXT_5), texts(Message.getStoredMessages()));
        assertFalse(Message.getMessageHashes().contains(hash));
        assertFalse(Message.getMessageIds().contains("0000000002"));

        String content = new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8);
        assertFalse(content.contains(hash));
    }

    @Test
    public void testDeleteMessageHashNotFound() {
        assertEquals("Message hash not found.", Message.deleteMessageByHash("00:0:NOTHERE"));
        assertEquals(2, Message.getStoredMessages().size());
    }

    // ----- Reports -----
    @Test
    public void testDisplayReport() {
        String report = Message.storedMessagesReport();
        assertTrue(report.contains(m2.getMessageHash()));
        assertTrue(report.contains("+27838884567"));
        assertTrue(report.contains(TEXT_2));
        assertTrue(report.contains(m5.getMessageHash()));
        assertTrue(report.contains(TEXT_5));
    }

    @Test
    public void testDisplaySenderAndRecipient() {
        String output = Message.displaySenderAndRecipient();
        assertTrue(output.contains("Sender: +27821234567 | Recipient: +27838884567"));
    }

    // ----- Reading the JSON file into the array -----
    @Test
    public void testJsonFileReadIntoStoredArray() {
        List<String> hashesBefore = Arrays.asList(m2.getMessageHash(), m5.getMessageHash());

        assertEquals(2, Message.loadStoredMessages());
        assertEquals(Arrays.asList(TEXT_2, TEXT_5), texts(Message.getStoredMessages()));
        assertEquals(hashesBefore.get(0), Message.getStoredMessages().get(0).getMessageHash());
        assertEquals(hashesBefore.get(1), Message.getStoredMessages().get(1).getMessageHash());
        assertEquals("+27821234567", Message.getStoredMessages().get(0).getSender());
    }

    @Test
    public void testJsonRoundTripKeepsSpecialCharacters() {
        String tricky = "He said \"hi\"\\ and left\nnew line";
        Message m = new Message("0000000006", 5, "+27834557896", tricky);
        m.sentMessage(3);

        Message.loadStoredMessages();
        List<String> loaded = texts(Message.getStoredMessages());
        assertTrue(loaded.contains(tricky));
    }

    @Test
    public void testLoadWhenNoFileExists() {
        Message.setStorageFile(new File(tmp.getRoot(), "missing.json").getAbsolutePath());
        assertEquals(0, Message.loadStoredMessages());
    }
}
