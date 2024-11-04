import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.io.*;
import java.net.*;
import java.util.*;

public class ScoreboardController {

    @FXML
    private ListView<String> scoreListView;

    private static final String MULTICAST_IP = "230.0.0.0";
    private static final int MULTICAST_PORT = 4446;
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private Thread listenerThread;

    public void startListening() {
        try {
            group = InetAddress.getByName(MULTICAST_IP);
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            multicastSocket.joinGroup(group);

            listenerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] buf = new byte[4096];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(packet);

                        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());

                        // Deserialize the data
                        List<String> topScores = parseTopScores(data);

                        // Update the UI
                        Platform.runLater(() -> {
                            ObservableList<String> items = FXCollections.observableArrayList(topScores);
                            scoreListView.setItems(items);
                        });

                    } catch (IOException e) {
                        if (multicastSocket.isClosed()) {
                            break;
                        }
                        e.printStackTrace();
                    }
                }
            });

            listenerThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> parseTopScores(byte[] data) throws IOException {
        List<String> topScores = new ArrayList<>();
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            int numScores = dis.readInt();
            for (int i = 0; i < numScores; i++) {
                String name = dis.readUTF();
                int score = dis.readInt();
                int level = dis.readInt();
                String time = dis.readUTF();

                String scoreStr = String.format("%-10s | Score: %-5d | Level: %-3d | Time: %s",
                        name, score, level, time);
                topScores.add(scoreStr);
            }
        }
        return topScores;
    }

    public void stopListening() {
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
        }
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                multicastSocket.leaveGroup(group);
                multicastSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}