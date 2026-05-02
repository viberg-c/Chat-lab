import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class Chat {
    public static void main(String[] args) {
        final int port = 5678;
        System.out.println("Starting Client");
        try {
            new Client(port, "localhost");

        }
        catch (IOException e) {
            System.out.println("Client failed, starting Server");
            new Server(port);
        }
    }
}

class Client {
    Socket mySocket;
    Client (int port, String ip) throws IOException{
        mySocket = new Socket(ip, port);
        new ChatParticipant(mySocket);
    }
}

class Server{
    Server (int port){
        try (ServerSocket myServerSocket = new ServerSocket(port)){
            Socket clientSocket = myServerSocket.accept();
            System.out.println("Klient anslöt!");

            new ChatParticipant(clientSocket);
        }
        catch (IOException e){
            System.out.println("Kan inte ansluta till servern" + e.getMessage());
        }

    }
}

class ChatParticipant implements ObjectStreamListener{
    private ObjectStreamManager myStreamManager;
    private ObjectInputStream   myInputStream;
    private ObjectOutputStream  myOutputStream;
    private GUI myGUI;
    private Socket mySocket;

    ChatParticipant (Socket socket) throws IOException {
        myOutputStream  = new ObjectOutputStream(socket.getOutputStream());
        myOutputStream.flush();
        myInputStream   = new ObjectInputStream(socket.getInputStream());
        myStreamManager = new ObjectStreamManager(1, myInputStream, this);
        myGUI           = new GUI(this);
        mySocket        = socket;
    }

    @Override
    public void objectReceived(int number, Object object, Exception exception){
        if (exception != null) {
            return;
        }
        if (object instanceof String) {
            String message = (String) object;
            myGUI.addMessageToTextArea("Den andra skrev: " + message);
        }
    }
    public void sendObject(Object object){
        try {
            myOutputStream.writeObject(object);
            myOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Kunde inte skicka meddelande");
        }
    }
    public void doExitButtonEvent() {
        try {
            if (myStreamManager != null) {
                myStreamManager.closeManager();
            }
            if (myOutputStream != null) {
                myOutputStream.close();
            }
            if (mySocket != null) {
                mySocket.close();
            }
            System.out.println("Allt stängdes ok!");

        } catch (IOException e) {
            System.err.println("Något gick fel vid stängning" + e.getMessage());
        } finally {
            myGUI.shutDownWindow();
        }
    }


}

class GUI{
    private JButton    sendButton = new JButton ("Send");
    private JButton    exitButton = new JButton ("Exit chat");
    private JFrame     frame      = new JFrame ("Beautiful Chat Window");
    private JTextField textField  = new JTextField();
    private JTextArea  textArea   = new JTextArea();

    private ChatParticipant participant;

    private final int FRAME_WIDTH = 400;
    private final int FRAME_HEIGHT = 500;

    public GUI(ChatParticipant participant){
        this.participant = participant;
        handleSendButtonEvent();
        handleExitButtonEvent();
        showGUI();
    }
    private void handleSendButtonEvent(){
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSendButtonEvent();
            }
        });
    }
    private void handleExitButtonEvent(){
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                participant.doExitButtonEvent();
            }
        });
    }
    private void doSendButtonEvent() {
        String text = textField.getText();
        boolean isEmpty = text.isEmpty();

        if (!isEmpty){
            participant.sendObject(text);
            textField.setText("");
            addMessageToTextArea("Jag: " + text);
        }
    }
    public void addMessageToTextArea(String msg) {
        textArea.append(msg + "\n");
    }
    private void showGUI(){
        setupLayout();
        frame.pack();
        frame.setVisible(true);
    }
    private void setupLayout(){
        JPanel fieldAndButtonContainer = new JPanel();
        JScrollPane scrollTextArea = new JScrollPane((textArea));

        fieldAndButtonContainer.setLayout(new BorderLayout());

        fieldAndButtonContainer.add(textField, BorderLayout.CENTER);
        fieldAndButtonContainer.add(sendButton, BorderLayout.EAST);

        frame.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

        frame.add(exitButton, BorderLayout.NORTH);
        frame.add(fieldAndButtonContainer, BorderLayout.SOUTH);
        frame.add(scrollTextArea, BorderLayout.CENTER);
    }
    public void shutDownWindow(){
        frame.dispose();
    }
}






/**
 * @author joachimparrow 2010
 * This is to read from an input stream in a separate thread, and call a callback
 * when something arrives.
 **/
class ObjectStreamManager {
    private final ObjectInputStream    theStream;
    private final ObjectStreamListener theListener;
    private final int                  theNumber;
    private volatile boolean           stopped = false;

    /**
     *
     *  This creates and starts a stream manager for a stream. The manager
     * will continually read from the stream and forward objects through the
     * objectReceived() method of the ObjectStreamListener parameter

     *
     * @param   number   The number you give to the manager. It will be included in
     * all calls to readObject. That way you can have the same callback serving several managers
     * since for each received object you get the identity of the manager.
     * @param stream The stream on which the manager should listen.
     * @param listener The object that has the callback objectReceived()
     *
     *
     */
    public ObjectStreamManager(int number, ObjectInputStream stream, ObjectStreamListener listener) {
        theNumber =   number;
        theStream =   stream;
        theListener = listener;
        new InnerListener().start();  // start to listen on a new thread.
    }

    // This private method accepts an object/exception pair and forwards them
    // to the callback, including also the manager number. The forwarding is scheduled
    // on the Swing thread through an anonymous inner class.

    private void callback(final Object object, final Exception exception) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        if (!stopped) {
                            theListener.objectReceived(theNumber, object, exception);
                            if (exception != null) closeManager();
                        }
                    }
                });
    }

    // This is where the actual reading takes place.
    private class InnerListener extends Thread {

        @Override
        public void run() {
            while (!stopped) {                            // as long as no one stopped me
                try {
                    callback(theStream.readObject(), null); // read an object and forward it
                } catch (Exception e) {                 // if Exception then forward it
                    callback(null, e);
                }
            }
            try {                   // I have been stopped: close stream
                theStream.close();
            } catch (IOException e) {
            }

        }
    }

    /**
     * Stop the manager and close the stream.
     **/
    public void closeManager() {
        stopped = true;
    }
}      // end of ObjectStreamManager


/**
 *
 * @author joachimparrow

 * This is the interface for the listener. It must have a method
 * objectReceived. Whenever reading from the stream results in an object
 * being read or exception being thrown, the object or exception is
 * forwarded to the listener through objectReceived().
 *
 *
 */
interface ObjectStreamListener {
    /**
     * This method is called whenever an object is received or an exception is thrown.
     * @param number    The number of the manager as defined in its constructor
     * @param object  The object received on the stream
     * @param exception     The exception thrown when reading from the stream.
     *          Can be IOException or ClassNotFoundException.
     *          One of name and exception will always be null.
     *          In case of an exception the manager and stream are closed.
     **/
    public void objectReceived(int number, Object object, Exception exception);
}


