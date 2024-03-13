package EjerciciosFTPChat;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServidorChatUDP {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    private static List<InetSocketAddress> clientesConectados = new ArrayList<>();
    private static Map<String, PublicKey> clavePublicaClientes = new HashMap<>(); // Mapa para almacenar claves públicas de los clientes

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        // Aqui genero las claves para encriptar y desencriptar
        KeyPair keyPair = generarClave();
        PublicKey clavePublica = keyPair.getPublic();
        PrivateKey clavePrivada = keyPair.getPrivate();


        int opciones;
        Scanner lector = new Scanner(System.in);

        System.out.println("Selecciona la opcion a realizar:");
        System.out.println("1 - gestionar mensajes");
        System.out.println("2 - gestionar imagenes");
        opciones = lector.nextInt();
        lector.nextLine();

        switch (opciones) {
            case 1:
                // Aqui empiezo a configurar el servidor UDP
                try {
                    DatagramSocket socketServidor = new DatagramSocket(12345);
                    System.out.println("Servidor de chat UDP iniciado");


                    while (true) {
                        /**
                         * Aqui recibo los datos del cliente, para ello:
                         * creo un array de bytes para almacenar los datos que recibo
                         * creo un DatagramPacket para almacenar los datos que recibo
                         * y luego almaceno los datos del cliente en el paquete
                         */
                        byte[] datosRecibidos = new byte[1024];
                        DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
                        socketServidor.receive(paqueteRecibido);


                        /**
                         * Aqui recibo los datos recividos por el cliente en un mensaje
                         * para ello convierto los datos recividos por el cliente en una cadena de texto
                         */
                        String mensaje = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                        InetAddress direccionCliente = paqueteRecibido.getAddress();
                        int puertoCliente = paqueteRecibido.getPort();

                        String mensajeCodificado = stringEncriptado(mensaje, clavePublica);
                        System.out.println(YELLOW + "Mensaje encriptado" + RESET);
                        System.out.println(mensajeCodificado);
                        //System.out.println(desencriptar(mensajeCodificado,clavePrivada));

                        /**
                         * con este bucle for envio el mensaje recivido por el cliente con el formato dado
                         * a todos los clientes que esten conectados incluido el remitente
                         */

                        for (InetSocketAddress cliente : clientesConectados) {
                            enviarMensaje(desencriptar(mensajeCodificado, clavePrivada), socketServidor, cliente.getAddress(), cliente.getPort());
                        }
                        System.out.println(RED + "Mensaje enviado desencriptado" + RESET + "\n");

                        // Agregar el remitente a la lista de clientes conectados si no está presente
                        /**
                         * Aqui obtengo la direccion IP del cliente y su puerto
                         * con estos datos creo un objeto InetSocketAddress
                         * y posteriormente con estos datos verifico en el if si el remitente
                         * esta conectado, si no lo esta lo añade a la lista de clientes conectados
                         */
                        InetSocketAddress remitente = new InetSocketAddress(direccionCliente, puertoCliente);
                        if (!clientesConectados.contains(remitente)) {
                            clientesConectados.add(remitente);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                recibirArchivo();
                break;
        }


    }

    /**
     * @param mensaje Es la cadena de texto que envia el cliente
     * @param socketServidor Es el socket del servidor que se usa para enviar los datos
     * @param direccionCliente Direccion IP del cliente
     * @param puertoCliente puerto que usa el cliente para enviar el mensaje
     * @throws IOException
     */
    private static void enviarMensaje(String mensaje, DatagramSocket socketServidor, InetAddress direccionCliente, int puertoCliente) throws IOException {
        /**
         * Aqui primero convierto el mensaje que ha enviado el cliente en un array de bytes
         * despues creo un DatagramPacket con el array de bytes y la direccion y puerto del cliente
         * y finalmente envio el paquete al cliente
         */
        byte[] datosEnviar  = mensaje.getBytes();
        DatagramPacket paqueteEnviar = new DatagramPacket(datosEnviar, datosEnviar.length, direccionCliente, puertoCliente);
        socketServidor.send(paqueteEnviar);
    }

    private static void recibirArchivo() {
        try{

            // Creo un servidor TCP, porque sinceramente no he sido capaz de hacerlo con un UDP
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Servidor TCP iniciado");
            Socket clienteSocket = serverSocket.accept();

            InputStream inputStream = clienteSocket.getInputStream();

            // aqui creo este array de byte es el que va a contener la imagen
            // es bastante tamaño lo que puede almacenar por si se intentara pasar una imagen muy grande
            byte[] imagenArray = new byte[10000000];
            inputStream.read(imagenArray);

            //aqui leo el array con el BufferedImage y muestro la altura y ancho de la imagen recibida
            // luego le doy un formato de jpg y le indico la ruta de donde se va a guardar en el servidor
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(imagenArray));
            System.out.println("Imagen recibida " + imagen.getHeight() + " x " + imagen.getWidth());
            ImageIO.write(imagen, "jpg", new File("src\\EjerciciosFTPChat\\imagenesClientes\\" + imagen.toString().getBytes() + ".jpg"));

            inputStream.close();
            clienteSocket.close();
            serverSocket.close();
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
