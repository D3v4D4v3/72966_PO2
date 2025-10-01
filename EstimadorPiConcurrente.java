import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EstimadorPiConcurrente {

    // ----------- Tarea individual de un hilo -----------
    static class Lanzador implements Callable<Long> {
        private final long intentos;

        Lanzador(long intentos) {
            this.intentos = intentos;
        }

        @Override
        public Long call() {
            SplittableRandom rng = new SplittableRandom();
            long aciertos = 0;

            for (long i = 0; i < intentos; i++) {
                double x = rng.nextDouble();
                double y = rng.nextDouble();
                if (x * x + y * y <= 1.0) {
                    aciertos++;
                }
            }
            return aciertos; // devuelve la cuenta parcial de este hilo
        }
    }

    public static void main(String[] args) throws Exception {
        long totalPuntos = 1_000_000L;   // puntos a lanzar
        int hilos = 4;                   // número de hilos

        // ========== MÉTODO SECUENCIAL ==========
        long inicioSec = System.nanoTime();
        long aciertosSec = 0;
        SplittableRandom rng = new SplittableRandom();

        for (long i = 0; i < totalPuntos; i++) {
            double x = rng.nextDouble();
            double y = rng.nextDouble();
            if (x * x + y * y <= 1.0) {
                aciertosSec++;
            }
        }

        double piSecuencial = 4.0 * aciertosSec / totalPuntos;
        double tiempoSec = (System.nanoTime() - inicioSec) / 1_000_000.0;

        // ========== MÉTODO PARALELO ==========
        long inicioPar = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<Long>> resultados = new ArrayList<>();

        long intentosPorHilo = totalPuntos / hilos;
        for (int i = 0; i < hilos; i++) {
            resultados.add(pool.submit(new Lanzador(intentosPorHilo)));
        }

        long aciertosTotales = 0;
        for (Future<Long> futuro : resultados) {
            aciertosTotales += futuro.get();
        }
        pool.shutdown();

        double piParalelo = 4.0 * aciertosTotales / totalPuntos;
        double tiempoPar = (System.nanoTime() - inicioPar) / 1_000_000.0;

        // ========== COMPARATIVAS ==========
        double speedup = tiempoSec / tiempoPar;
        double eficiencia = speedup / hilos;
        double overhead = hilos * tiempoPar - tiempoSec;

        // ----- Impresión de resultados -----
        System.out.println("\n=== Resumen Secuencial ===");
        System.out.printf("π ≈ %.8f%n", piSecuencial);
        System.out.printf("Tiempo Ts: %.3f ms%n", tiempoSec);

        System.out.println("\n=== Resumen Paralelo ===");
        System.out.printf("π ≈ %.8f%n", piParalelo);
        System.out.printf("Tiempo Tp (%d hilos): %.3f ms%n", hilos, tiempoPar);

        System.out.println("\n=== Métricas ===");
        System.out.printf("Speedup (S=Ts/Tp): %.3f%n", speedup);
        System.out.printf("Eficiencia (E=S/p): %.3f%n", eficiencia);
        System.out.printf("Overhead (To=p*Tp - Ts): %.3f ms%n", overhead);

        System.out.println("\nPuntos totales: " + totalPuntos);
        System.out.println("Puntos dentro del círculo: " + aciertosTotales);
        System.out.printf("Error absoluto: %.10f%n", Math.abs(piParalelo - Math.PI));
    }
}
