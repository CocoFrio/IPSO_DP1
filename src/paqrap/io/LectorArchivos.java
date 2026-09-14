package paqrap.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import paqrap.model.Bloqueo;
import paqrap.model.MantenimientoPreventivo;
import paqrap.model.Nodo;
import paqrap.model.Pedido;

/**
 * Lectores de los archivos de entrada del curso.
 *
 * <p>Se aceptan tanto los nombres indicados en la especificacion como los
 * nombres de los archivos entregados en la carpeta data.</p>
 */
public final class LectorArchivos {

    private static final DateTimeFormatter HORA_VENTA =
            DateTimeFormatter.ofPattern("dd'd'HH'h'mm'm'")
                    .withResolverStyle(ResolverStyle.STRICT);

    private LectorArchivos() {
    }

    public static List<Pedido> leerVentas(Path archivo, int anio, int mes) throws IOException {
        YearMonth periodo = validarPeriodo(anio, mes);
        List<Pedido> pedidos = new ArrayList<>();
        leerLineas(archivo, (linea, numero) -> {
            String[] partes = separar(linea, ":", 2, archivo, numero);
            LocalDateTime fecha = parsearFechaVenta(partes[0], periodo, archivo, numero);
            String[] datos = separar(partes[1], ",", 5, archivo, numero);
            int posX = parsearEntero(datos[0], archivo, numero, "posX");
            int posY = parsearEntero(datos[1], archivo, numero, "posY");
            int cantidad = parsearEntero(datos[3], archivo, numero, "cantidad");
            int horasLimite = parsearEntero(datos[4], archivo, numero, "horas limite");
            if (datos[2].isEmpty()) {
                throw error(archivo, numero, "el identificador del cliente esta vacio");
            }
            String id = String.format("PED-%04d-%02d-%04d", anio, mes, pedidos.size() + 1);
            pedidos.add(new Pedido(id, datos[2], posX, posY, cantidad, horasLimite, fecha));
        });
        return pedidos;
    }

    public static List<Bloqueo> leerBloqueos(Path archivo, int anio, int mes) throws IOException {
        YearMonth periodo = validarPeriodo(anio, mes);
        List<Bloqueo> bloqueos = new ArrayList<>();
        leerLineas(archivo, (linea, numero) -> {
            String[] partes = separar(linea, ":", 2, archivo, numero);
            String[] intervalo = separar(partes[0], "-", 2, archivo, numero);
            LocalDateTime inicio = parsearFechaBloqueo(intervalo[0], periodo, archivo, numero);
            LocalDateTime fin = parsearFechaBloqueo(intervalo[1], periodo, archivo, numero);
            if (fin.isBefore(inicio)) {
                fin = fin.plusMonths(1);
            }
            String[] coordenadas = partes[1].split(",");
            if (coordenadas.length < 4 || coordenadas.length % 2 != 0) {
                throw error(archivo, numero, "la poligonal debe tener pares x,y");
            }
            List<Nodo> nodos = new ArrayList<>();
            for (int i = 0; i < coordenadas.length; i += 2) {
                nodos.add(new Nodo(
                        parsearEntero(coordenadas[i], archivo, numero, "posX"),
                        parsearEntero(coordenadas[i + 1], archivo, numero, "posY")));
            }
            bloqueos.add(new Bloqueo("BLQ-" + String.format("%04d", bloqueos.size() + 1),
                    nodos, inicio, fin));
        });
        return bloqueos;
    }

    public static List<MantenimientoPreventivo> leerMantenimientos(Path archivo) throws IOException {
        List<MantenimientoPreventivo> mantenimientos = new ArrayList<>();
        leerLineas(archivo, (linea, numero) -> {
            String[] partes = separar(linea, ":", 2, archivo, numero);
            if (!partes[0].matches("\\d{8}") || !partes[1].matches("[A-Za-z]{2}\\d{2}")) {
                throw error(archivo, numero, "formato esperado aaaammdd:TTNN");
            }
            try {
                LocalDate fecha = LocalDate.parse(partes[0],
                        DateTimeFormatter.BASIC_ISO_DATE.withResolverStyle(ResolverStyle.STRICT));
                mantenimientos.add(new MantenimientoPreventivo(
                        fecha, partes[1].substring(0, 2).toUpperCase(),
                        partes[1].substring(2)));
            } catch (DateTimeParseException ex) {
                throw error(archivo, numero, "fecha invalida");
            }
        });
        return mantenimientos;
    }

    private static LocalDateTime parsearFechaVenta(String valor, YearMonth periodo,
                                                    Path archivo, int numero) {
        try {
            return LocalDateTime.of(periodo.atDay(1), java.time.LocalTime.MIDNIGHT)
                    .withDayOfMonth(Integer.parseInt(valor.substring(0, 2)))
                    .with(java.time.LocalTime.from(HORA_VENTA.parse(valor)));
        } catch (RuntimeException ex) {
            throw error(archivo, numero, "hora de venta invalida");
        }
    }

    private static LocalDateTime parsearFechaBloqueo(String valor, YearMonth periodo,
                                                      Path archivo, int numero) {
        try {
            int dia = Integer.parseInt(valor.substring(0, 2));
            int hora = Integer.parseInt(valor.substring(3, 5));
            int minuto = Integer.parseInt(valor.substring(6, 8));
            LocalDate fecha = periodo.atDay(dia);
            return LocalDateTime.of(fecha, java.time.LocalTime.of(hora, minuto));
        } catch (RuntimeException ex) {
            throw error(archivo, numero, "fecha de bloqueo invalida");
        }
    }

    private static YearMonth validarPeriodo(int anio, int mes) {
        return YearMonth.of(anio, mes);
    }

    private static String[] separar(String valor, String separador, int cantidad,
                                    Path archivo, int numero) {
        String[] partes = valor.split(separador, -1);
        if (partes.length != cantidad) {
            throw error(archivo, numero, "cantidad de campos invalida");
        }
        for (int i = 0; i < partes.length; i++) {
            partes[i] = partes[i].trim();
        }
        return partes;
    }

    private static int parsearEntero(String valor, Path archivo, int numero, String campo) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            throw error(archivo, numero, campo + " invalido");
        }
    }

    private static IllegalArgumentException error(Path archivo, int numero, String mensaje) {
        return new IllegalArgumentException(archivo + ":" + numero + ": " + mensaje);
    }

    private interface LineaConsumer {
        void aceptar(String linea, int numero);
    }

    private static void leerLineas(Path archivo, LineaConsumer consumer) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            int numero = 0;
            while ((linea = reader.readLine()) != null) {
                numero++;
                linea = linea.trim();
                if (!linea.isEmpty() && !linea.startsWith("#")) {
                    consumer.aceptar(linea, numero);
                }
            }
        }
    }
}
