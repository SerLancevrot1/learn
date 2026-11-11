import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

// JDK 21+. Каждый запрос получает свой виртуальный поток, а Semaphore
// ограничивает обращения к условному внешнему сервису двумя одновременно.
String loadOrder(int id, Semaphore serviceLimit) throws InterruptedException {
    serviceLimit.acquire();
    try {
        Thread.sleep(100); // Имитируем блокирующее ожидание ответа.
        return "Заказ " + id + ", виртуальный поток: " + Thread.currentThread().isVirtual();
    } finally {
        serviceLimit.release();
    }
}

{
    var serviceLimit = new Semaphore(2);
    List<Callable<String>> requests = List.of(
            () -> loadOrder(1, serviceLimit),
            () -> loadOrder(2, serviceLimit),
            () -> loadOrder(3, serviceLimit));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (var request : executor.invokeAll(requests)) {
            System.out.println(request.get());
        }
    }
}

/exit
