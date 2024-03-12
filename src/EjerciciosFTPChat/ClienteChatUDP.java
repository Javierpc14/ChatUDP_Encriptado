package EjerciciosFTPChat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class ClienteChatUDP {

    public static final String RESET = "\u001B[0m";
    public static final String CYAN = "\u001B[36m";
    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {



        int opciones;
        Scanner lector = new Scanner(System.in);

        System.out.println("Selecciona la opcion a realizar:");
        System.out.println("1 - enviar mensaje");
        System.out.println("2 - enviar archivo");
        opciones = lector.nextInt();
        lector.nextLine();

            switch (opciones) {
                case 1:
                    enviarMensaje();
                    break;
                case 2:
                    transferirArchivo();
                    break;
            }

    }

    private static void enviarMensaje() throws NoSuchAlgorithmException {
        // Aqui genero las claves para encriptar y desencriptar
        KeyPair keyPair = generarClave();
        PublicKey clavePublica = keyPair.getPublic();
        PrivateKey clavePrivada = keyPair.getPrivate();

            try {
                DatagramSocket socketCliente = new DatagramSocket();
                BufferedReader lectorConsola = new BufferedReader(new InputStreamReader(System.in));

                // Solicitar al usuario que ingrese su nombre
                System.out.print("Ingresa tu nombre: ");
                String nombreUsuario = lectorConsola.readLine();

                // Enviar la clave pública al servidor
//                InetAddress direccionServidor = InetAddress.getByName("localhost");
//                String clavePublicaEnviar = Base64.getEncoder().encodeToString(clavePublica.getEncoded());
//                DatagramPacket claveEnviar = new DatagramPacket(clavePublicaEnviar.getBytes(), clavePublicaEnviar.getBytes().length, direccionServidor, 12345);
//                socketCliente.send(claveEnviar);
                // Hilo para recibir y mostrar los mensajes del servidor
                Thread hiloRecepcion = new Thread(() -> {
                    try {
                        while (true) {
                            /**
                             * Aqui recibo datos del servidor, para ello:
                             * creo un array de bytes para almacenar los datos que recibo
                             * creo un DatagramPacket para almacenar los datos que recibo
                             */
                            byte[] datosRecibidos = new byte[1024];
                            DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
                            socketCliente.receive(paqueteRecibido);

                            /**
                             * Aqui convierto en una cadena de texto el mensaje recivido del FatagramPacket
                             */
                            String mensajeRecibido = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                            //desencriptar(mensajeRecibido,clavePrivada);
                            System.out.println(mensajeRecibido);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                hiloRecepcion.start();

                while (true) {
                    /**
                     *  Aquí solicito al usuario que ingrese un mensaje
                     *  una vez el cliente ha ingresado un mensaje lo codifico
                     */
                    System.out.println("Insertar mensaje");
                    String mensaje = lectorConsola.readLine();
//                String mensajeCodificado = stringEncriptado(mensaje,clavePublica);
//                System.out.println("Mensaje encriptado antes de enviar");

                    /**
                     * Aqui aplico un formato de hora para mostrar a que hora se ha enviado el mensaje
                     * y despues guardo la hora formateada en una variable
                     */
                    SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm:ss");
                    String horaMensaje = formatoHora.format(new Date());


                    String mensajeCompleto = CYAN + "Nombre: " + RESET + nombreUsuario + CYAN + " Hora: " + RESET + horaMensaje + CYAN + " Mensaje: " + RESET + mensaje;

                    // Enviar el mensaje al servidor
                    /**
                     * Aqui obtengo primero la direccion IP del servidor
                     * despues convierto el mensaje en un array de bytes
                     * luego creo un DatagramPacket el cual contiene el mensaje completo y la direccion del servidor
                     * y finalmente envio el paquete al servidor
                     */
                    InetAddress direccionServidor = InetAddress.getByName("localhost");
                    byte[] datosEnviar = mensajeCompleto.getBytes();
                    DatagramPacket paqueteEnviar = new DatagramPacket(datosEnviar, datosEnviar.length, direccionServidor, 12345);
                    socketCliente.send(paqueteEnviar);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private static void transferirArchivo() {
        try{
            Socket clienteSocket = new Socket("localhost", 1234);

            OutputStream outputStream = clienteSocket.getOutputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // aqui le pido al usuario que ingrese el nombre de la imagen, este se debe llamar igual a las imagenes que hay en la carpeta
            Scanner lector = new Scanner(System.in);
            System.out.println("Ingresa el nombre de la imagen a enviar");
            String lecturaIngresar = lector.nextLine();

            // aqui leo la imagen que envia el usuario de la carperta imagenes que es la que tiene asociada
            // luego la escribo con el formato jpg
            BufferedImage imagen = ImageIO.read(new File("src\\EjerciciosFTPChat\\imagenes\\" + lecturaIngresar));
            ImageIO.write(imagen, "jpg", byteArrayOutputStream);

            //aqui envio los datos de la imagen al servidor
            outputStream.write(byteArrayOutputStream.toByteArray());
            System.out.println("Se ha enviado la imagen");

            outputStream.close();
            clienteSocket.close();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyPair generarClave() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2028);
        return keyPairGenerator.generateKeyPair();
    }

    private static String stringEncriptado(String dato, PublicKey clavePublica) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cifrado = Cipher.getInstance("RSA");
        cifrado.init(Cipher.ENCRYPT_MODE,clavePublica);
        byte[] encriptado = cifrado.doFinal(dato.getBytes());
        return Base64.getEncoder().encodeToString(encriptado);
    }

    private static String desencriptar(String datoEncriptado, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cifrado = Cipher.getInstance("RSA");

        cifrado.init(Cipher.DECRYPT_MODE,privateKey);

        byte[] desencriptados = cifrado.doFinal(Base64.getDecoder().decode(datoEncriptado));
        return new String(desencriptados);
    }

}

