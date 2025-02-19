package es.studium.psp3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Servidor extends JFrame {
    private static final int PUERTO = 6000;
    private int numeroAdivinar;
    private List<DataOutputStream> clientes = new ArrayList<>();
    private List<String> nombresClientes = new ArrayList<>();
    private JTextArea logArea;
    private ServerSocket servidor;
    private List<Socket> conexionesActivas = new ArrayList<>();
    private boolean servidorActivo = true;
    private boolean juegoTerminado = false;

    public Servidor() {
        setTitle("Servidor - Juego de Adivinar el Número");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Arial", Font.PLAIN, 14));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarServidor();
            }
        });

        setVisible(true);

        new Thread(this::iniciarServidor).start();
    }

    private void iniciarServidor() {
        reiniciarJuego();

        try {
            servidor = new ServerSocket();
            servidor.setReuseAddress(true);
            servidor.bind(new InetSocketAddress(PUERTO));

            log("Servidor iniciado en el puerto " + PUERTO);
            while (servidorActivo) {
                try {
                    Socket cliente = servidor.accept();
                    if (juegoTerminado) {
                        DataOutputStream salida = new DataOutputStream(cliente.getOutputStream());
                        salida.writeUTF("El juego ya ha terminado.");
                        cliente.close();
                        continue;
                    }
                    conexionesActivas.add(cliente);
                    DataOutputStream salida = new DataOutputStream(cliente.getOutputStream());
                    clientes.add(salida);
                    new HiloCliente(cliente, salida).start();
                } catch (IOException e) {
                    if (!servidorActivo) {
                        log("Servidor detenido.");
                        break;
                    }
                    log("Error aceptando cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log("Error en el servidor: " + e.getMessage());
        }
    }

    private void log(String mensaje) {
        SwingUtilities.invokeLater(() -> logArea.append(mensaje + "\n"));
    }

    private class HiloCliente extends Thread {
        private Socket cliente;
        private DataOutputStream salida;
        private DataInputStream entrada;
        private String nombre;

        public HiloCliente(Socket cliente, DataOutputStream salida) {
            this.cliente = cliente;
            this.salida = salida;
            try {
                this.entrada = new DataInputStream(cliente.getInputStream());
            } catch (IOException e) {
                log("Error iniciando conexión con un cliente.");
            }
        }

        @Override
        public void run() {
            try {
                nombre = entrada.readUTF();

                synchronized (nombresClientes) {
                    if (nombresClientes.contains(nombre)) {
                        salida.writeUTF("ERROR_NOMBRE");
                        cliente.close();
                        return;
                    }
                    nombresClientes.add(nombre);
                }

                log(nombre + " se ha conectado.");
                enviarMensajeATodos("Servidor: " + nombre + " se ha unido al juego.");

                while (servidorActivo) {
                    if (juegoTerminado) {
                        salida.writeUTF("El juego ya ha terminado.");
                        return;
                    }

                    int numeroApostado = entrada.readInt();
                    String mensaje;

                    if (numeroApostado == numeroAdivinar) {
                        mensaje = nombre + " piensa que el número es " + numeroApostado + ". ¡Y HA ACERTADO!";
                        log(mensaje);
                        enviarMensajeATodos(mensaje);
                        finalizarJuego(nombre);
                        return;
                    } else if (numeroApostado < numeroAdivinar) {
                        mensaje = nombre + " piensa que el número es " + numeroApostado + ". Pero el número es mayor.";
                    } else {
                        mensaje = nombre + " piensa que el número es " + numeroApostado + ". Pero el número es menor.";
                    }

                    log(mensaje);
                    enviarMensajeATodos(mensaje);
                    Thread.sleep(3000);
                }
            } catch (IOException | InterruptedException e) {
                log(nombre + " se ha desconectado inesperadamente.");
                enviarMensajeATodos("Servidor: " + nombre + " ha salido del juego.");
            }
        }
    }

    private void enviarMensajeATodos(String mensaje) {
        for (DataOutputStream cliente : clientes) {
            try {
                cliente.writeUTF(mensaje);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void finalizarJuego(String ganador) {
        juegoTerminado = true;
        log("Juego finalizado. " + ganador + " ha ganado.");
        enviarMensajeATodos("Servidor: " + ganador + " ha ganado el juego. Se cerrarán todas las conexiones.");
        enviarMensajeATodos("CERRAR_CLIENTES");

        for (Socket socket : conexionesActivas) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        conexionesActivas.clear();
        clientes.clear();
    }

    private void reiniciarJuego() {
        Random random = new Random();
        numeroAdivinar = random.nextInt(100) + 1;
        juegoTerminado = false;
        log("Número secreto generado: " + numeroAdivinar);
    }

    private void cerrarServidor() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Seguro que quieres cerrar el servidor?", "Confirmar cierre",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            servidorActivo = false;
            log("Cerrando servidor...");

            for (Socket socket : conexionesActivas) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                if (servidor != null && !servidor.isClosed()) {
                    servidor.close();
                    log("Servidor detenido correctamente.");
                }
            } catch (IOException e) {
                log("Error cerrando el servidor: " + e.getMessage());
            }

            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Servidor::new);
    }
}
