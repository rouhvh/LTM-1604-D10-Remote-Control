package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import Shared.IRemoteControl;

@SuppressWarnings("serial")
public class ChatWindow extends JDialog {
    private JTextArea chatArea;
    private JTextField messageField;
    private IRemoteControl remoteControl;
    private String clientName;

    public ChatWindow(Frame owner, IRemoteControl remoteControl, String clientName) {
        super(owner, "Chat", false); // false: không khóa cửa sổ cha
        this.remoteControl = remoteControl;
        this.clientName = clientName;

        setSize(400, 500);
        setLocationRelativeTo(owner); // Hiển thị ở giữa cửa sổ điều khiển
        setLayout(new BorderLayout(5, 5));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFocusable(false); // tránh giành focus
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(this::sendMessage);
        messageField.addActionListener(this::sendMessage); // Gửi khi nhấn Enter

        // 🔑 Xử lý focus cho chat window
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                messageField.requestFocusInWindow();
            }
            @Override
            public void windowClosed(WindowEvent e) {
                Window owner = getOwner();
                if (owner != null) owner.requestFocusInWindow();
            }
        });
    }

    private void sendMessage(ActionEvent e) {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                remoteControl.sendMessage(clientName + ": " + message);
                messageField.setText("");
            } catch (RemoteException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error sending message.", "Chat Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            // luôn focus lại vào ô nhập
            messageField.requestFocusInWindow();
        });
    }
}
