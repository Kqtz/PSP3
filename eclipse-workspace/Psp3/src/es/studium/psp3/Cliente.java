package es.studium.psp3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class Cliente extends JFrame {
    private static final String HOST = "localhost";
    private static final int PUERTO = 6000;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private JTextField inputNumero, inputNombre;
    private JTextArea chatArea;
    private JButton enviarBtn, nuevoJugadorBtn, salirBtn, conectarBtn;
    private String nombre;
    private Socket socket;
    private boolean conectado = false;

    public Cliente() {
        setTitle("Cliente - Adivina el Número");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        setLocation(50, 50);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new FlowLayout());

        inputNombre = new JTextField(10);
        conectarBtn = new JButton("Conectar");

        panelSuperior.add(new JLabel("Nombre: "));
        panelSuperior.add(inputNombre);
        panelSuperior.add(conectarBtn);
        add(panelSuperior, BorderLayout.NORTH);

        JPanel panelInferior = new JPanel();
        panelInferior.setLayout(new FlowLayout());

        inputNumero = new JTextField(5);
        enviarBtn = new JButton("Enviar");
        nuevoJugadorBtn = new JButton("Nuevo Jugador");
        salirBtn = new JButton("Salir");

        panelInferior.add(new JLabel("Tu intento: "));
        panelInferior.add(inputNumero);
        panelInferior.add(enviarBtn);
        panelInferior.add(nuevoJugadorBtn);
        panelInferior.add(salirBtn);
        add(panelInferior, BorderLayout.SOUTH);

        conectarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                conectarServidor();
            }
        });

        enviarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarNumero();
            }
        });

        nuevoJugadorBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Cliente(); 
            }
        });

        salirBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                salirDelJuego();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salirDelJuego();
            }
        });

        setVisible(true);
    }

    private void conectarServidor() {
        try {
            nombre = inputNombre.getText().trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debes ingresar un nombre.");
                return;
            }

            socket = new Socket(HOST, PUERTO);
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());
            conectado = true;

            salida.writeUTF(nombre);

            String respuesta = entrada.readUTF();
            if (respuesta.equals("ERROR_NOMBRE")) {
                JOptionPane.showMessageDialog(this, "Ese nombre ya está en uso. Elige otro.");
                socket.close();
                return;
            }

            chatArea.append("Conectado al servidor como: " + nombre + "\n");

            conectarBtn.setEnabled(false);
            inputNombre.setEditable(false);

            new Thread(() -> {
                try {
                    while (conectado) {
                        String mensaje = entrada.readUTF();
                        if (mensaje.equals("CERRAR_CLIENTES")) {
                            JOptionPane.showMessageDialog(null, "El juego ha terminado.");
                            Thread.sleep(3000);
                            salirDelJuego();
                            return;
                        }
                        chatArea.append(mensaje + "\n");
                    }
                } catch (IOException | InterruptedException e) {
                    chatArea.append("Conexión cerrada.\n");
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void enviarNumero() {
        try {
            if (!conectado) {
                JOptionPane.showMessageDialog(this, "Debes conectarte primero.");
                return;
            }

            int numero = Integer.parseInt(inputNumero.getText());
            salida.writeInt(numero);
            inputNumero.setText("");
            inputNumero.setEnabled(false);
            enviarBtn.setEnabled(false);

            new Timer(3000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    inputNumero.setEnabled(true);
                    enviarBtn.setEnabled(true);
                }
            }).start();

        } catch (NumberFormatException | IOException e) {
            JOptionPane.showMessageDialog(this, "Introduce un número válido.");
        }
    }

    private void salirDelJuego() {
        try {
            if (conectado) {
                salida.writeUTF("Servidor: " + nombre + " ha salido del juego.");
                socket.close();
                conectado = false;
            }
            dispose(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Cliente::new);
    }
}
